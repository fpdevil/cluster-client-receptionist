**cluster-client-receptionist**

This Scala Akka cluster will have message exchange in the using a Cluster Receptionist. Using Receptionist, An actor system that is not part of the cluster can communicate with actors somewhere in the cluster via this ClusterClient.
Akka documentation has detailed explanation about this pattern [Visit Akka doc](http://doc.akka.io/docs/akka/snapshot/contrib/cluster-client.html)

The project uses _**SBT 0.13.7**_ as build tool. This version supports the latest Akka and Scala libraries The System needs at a minimum 3 nodes for testing the akka actor message exchange. Cluster monitoring ensures that messages are received whenever a node either joins or leaves the cluster system. All the events would be recorded in the logs. For logging logback with slf4j is used. 

The messages here are routed using a RoundRobinRouter which spawns any number of child actors though in the example it just spwans 4 actors. Using RoundRobinActor we can issue a PoisonPill to kill all the actors at once by broadcasting the PoisonPill to Router. The functionality has not yet been added and will be added later. At this point of time 2 frontend nodes whould each get a text message from the Router. Additional functionality and Test cases will follow.

Here is how it works

1. On Terminal 1 start a Frontend Node as follows 
   sbt "run-main com.cluster.demo.ClusterServerApp 3001"

2. On Terminal 2 start a second Frontend Node 
   sbt "run-main com.cluster.demo.ClusterServerApp 3002"

3. On Terminal 3 start a Backend Node as follows 
   sbt "run-main com.cluster.demo.ClusterDemoApp"
   
Fron Terminal 3 Client will send messages to the other 2 which can be seen in the logs and the stdout. Here when port number is not specified a Random port is automatically allocated by the Akka kernel system.

Once all 3 are started we can see message exchange similar to following on Frontend Nodes

Frontend Node Output
```
Frontend Node Output
[info] Loading project definition from C:\scala\coursera\ClusterDemo\project
[info] Set current project to ClusterDemo (in build file:/C:/scala/coursera/ClusterDemo/)
[info] Running com.cluster.demo.ClusterServerApp 3002
[DEBUG] [11/25/2014 12:53:06.177] [run-main-0] [EventStream] StandardOutLogger started
[DEBUG] [11/25/2014 12:53:10.407] [run-main-0] [EventStream(akka://ClusterSystem)] logger log1-Slf4jLogger started
[DEBUG] [11/25/2014 12:53:10.410] [run-main-0] [EventStream(akka://ClusterSystem)] Default Loggers started
Greetings from ClusterDemoApp : akka.tcp://ClusterSystem@127.0.0.1:3002/user/workers/$a#1527706233
Greetings from ClusterDemoApp : akka.tcp://ClusterSystem@127.0.0.1:3002/user/workers/$b#-640367546
Greetings from ClusterDemoApp : akka.tcp://ClusterSystem@127.0.0.1:3002/user/workers/$c#-1763009449
Greetings from ClusterDemoApp : akka.tcp://ClusterSystem@127.0.0.1:3002/user/workers/$d#-115967090
Greetings from ClusterDemoApp : akka.tcp://ClusterSystem@127.0.0.1:3002/user/workers/$a#1527706233
Greetings from ClusterDemoApp : akka.tcp://ClusterSystem@127.0.0.1:3002/user/workers/$b#-640367546
Greetings from ClusterDemoApp : akka.tcp://ClusterSystem@127.0.0.1:3002/user/workers/$c#-1763009449
Greetings from ClusterDemoApp : akka.tcp://ClusterSystem@127.0.0.1:3002/user/workers/$d#-115967090
Greetings from ClusterDemoApp : akka.tcp://ClusterSystem@127.0.0.1:3002/user/workers/$a#1527706233
Greetings from ClusterDemoApp : akka.tcp://ClusterSystem@127.0.0.1:3002/user/workers/$b#-640367546
```


Client Node Output
```
[info] Loading project definition from C:\scala\coursera\ClusterDemo\project
[info] Set current project to ClusterDemo (in build file:/C:/scala/coursera/ClusterDemo/)
[info] Running com.cluster.demo.ClusterDemoApp
[DEBUG] [11/25/2014 12:53:28.665] [run-main-0] [EventStream] StandardOutLogger started
[DEBUG] [11/25/2014 12:53:31.877] [run-main-0] [EventStream(akka://client)] logger log1-Slf4jLogger started
[DEBUG] [11/25/2014 12:53:31.880] [run-main-0] [EventStream(akka://client)] Default Loggers started
sending message...
sending message...
sending message...
sending message...
sending message...
sending message...
```