Apache Ignite embedded + programmatic static clustering test
============================================================

A simple sample cli app to test embedded [Ignite](https://ignite.apache.org/) 
and static IP discovery for cluster setup. 

##Building

    gradle depJar

To generate Eclipse project dependencies:

    gradle eclipse

...and import as new Java project.


##Running

    java -jar build/libs/ignite-example-static-discovery-all-1.0.jar -a ADDRESS:PORT -p ADDRESS:PORT,ADDRESS:PORT,... [--task] 

Sets up a node at the discovery address+port specified by option -a, and 
connects to some of the nodes running at the address+port specified by -p. 
Without further parameters, waits for connections; if --task is specified, 
launches a sample compute task from Ignite documentation. 

To test with two local nodes, one launching the task and both 
participating in computation:

     java -jar build/libs/ignite-example-static-discovery-all-1.0.jar -a 127.0.0.1:5000 -p 127.0.0.1:6000
     java -jar build/libs/ignite-example-static-discovery-all-1.0.jar -a 127.0.0.1:6000 -p 127.0.0.1:5000 --task

