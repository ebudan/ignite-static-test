import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.net.HostAndPort;

/*
 * A sample cli app to test embedded Ignite and static IP discovery for
 * cluster setup, and to demonstrate cluster deadlock. (Ignite version 1.4.0, 
 * also confirmed for 1.5.0-SNAPSHOT.)
 * 
 * To run:
 *     java -jar build/libs/HelloIgnite-all-1.0.jar -a ADDRESS:PORT -p ADDRESS:PORT,ADDRESS:PORT,... [--task] 
 * 
 * Sets a node up at the discovery address+port specified by option -a, and 
 * connects to some of the nodes running at the address+port specified by -p. 
 * Without further parameters, waits for connections; if --task is specified, 
 * launches a sample compute task from Ignite documentation. 
 * 
 * To test with two local nodes:
 * 
 *     java -jar build/libs/HelloIgnite-all-1.0.jar -a 127.0.0.1:5000 -p 127.0.0.1:6000
 *     java -jar build/libs/HelloIgnite-all-1.0.jar -a 127.0.0.1:6000 -p 127.0.0.1:5000 --task
 */
public class Main {

    static final Logger log = LogManager.getLogger( Main.class );

    @Parameter( names = { "-t", "--task" }, description = "Perform sample task." )
    private boolean m_task;

    @Parameter( names = { "-a", "--addr" }, description = "Own IP:port address", required = true )
    private String m_addr;

    @Parameter( names = { "-p", "--peers" }, description = "Comma separated list of IP:port of Ignite peers. Will use given port for discovery, port+1 for node communications.", variableArity = true )
    private List<String> m_peers = new ArrayList<>();

    private Ignite m_ignite;

    private void go() {

        HostAndPort hp =
            HostAndPort.fromString( m_addr )
                       .withDefaultPort( 5000 )
                       .requireBracketsForIPv6();

        IgniteConfiguration icfg = new IgniteConfiguration();
        // Explicitly set node address:
        icfg.setLocalHost( hp.getHostText() );
               
        // Explicitly set discovery port:
        TcpDiscoverySpi spi = new TcpDiscoverySpi();
        spi.setLocalPort( hp.getPort() );       
        if( m_peers.size() > 0 ) {
            // NOTE: Ignite requires the local node to be among the cluster set! 
            // Otherwise, deadlock during topology join.
            m_peers.add( hp.toString() );
            TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
            ipFinder.setAddresses( m_peers ); 
            spi.setIpFinder( ipFinder );
        }
        icfg.setDiscoverySpi( spi );
        
        // Explicitly set communications port:
        TcpCommunicationSpi tcp = new TcpCommunicationSpi();
        tcp.setLocalPort( hp.getPort() + 1 );
        icfg.setCommunicationSpi( tcp );

        m_ignite = Ignition.start(icfg);

        // Leave a bit of time for other nodes to get up. 
        if( m_task ) {
            try {
                Thread.sleep( 3000 );
            } catch( InterruptedException e ) {
            }
            doSampleTask();
        }

        // Get out of the way while Ignite serves incoming compute calls.
        while( true ) {
            try {
                synchronized( this ) {
                    wait();
                }
            } catch( InterruptedException e ) {
            
            }
        }
    }

    // Sample task from Ignite tutorial.
    private void doSampleTask() {
        log.debug( "Launching a task." );
        Collection<IgniteCallable<Integer>> calls = new ArrayList<>();
        for (final String word : "Count characters using callable".split(" "))
          calls.add( () -> { 
              log.debug( "Counting " + word.length() + " chars" );
              return word.length();
          });
        Collection<Integer> res = m_ignite.compute().call(calls);
        int sum = res.stream().mapToInt(Integer::intValue).sum();       
        log.debug( "Total number of characters is '" + sum + "'." );
    }
    
    public static void main( String[] args ) {

        Main app = new Main();
        JCommander jc = new JCommander( app );
        try {
            jc.parse( args );
            app.go();
        } catch( ParameterException e ) {
            jc.usage();
            System.exit( 1 );
        }
    }
}