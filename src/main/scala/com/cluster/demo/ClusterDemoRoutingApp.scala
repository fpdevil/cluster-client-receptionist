package com.cluster.demo

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.Props
import akka.routing._
import akka.cluster.routing._
import akka.cluster._
import akka.cluster.ClusterEvent._
import akka.contrib.pattern.ClusterReceptionistExtension
import akka.actor.ActorRef
import akka.actor.Actor
import akka.actor.RootActorPath
import akka.actor.Terminated
import akka.contrib.pattern.ClusterClient
import akka.actor.ActorLogging

case class ClusterMessage(company: String)
case class BackendRegistration(key: String)

/**
 * @author Sampath
 *
 */
object ClusterDemoRoutingApp extends App {

  args.length match {
    case 2 =>
      val port = args(0)
      val key = args(1)
      println(s"Starting ActorSystem on port no. $port with key. $key")
      start(port, key)
    case _ =>
      println(s"Valid port and role not provided...Will not Start.")
  }

  def start(port: String, key: String) = {
    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=${port}").
      withFallback(ConfigFactory.load())

    val system = ActorSystem("ClusterSystem", config)

    val frontend = system.actorOf(Props[FrontEndActor], name = "frontend")

    /*
     * Here Router is an Actor which proxies or supervises the mailbox for one or more child actors (Routees)
     * RoundRobinRouter sends the messages one by one through the list of routees. The same can be defined in Configuration File too
     * This link has more details http://doc.akka.io/docs/akka/2.1.2/scala/routing.html
     */
    val localBackendWorkerRouter = system.actorOf(Props[BackendWorker].
      withRouter(RoundRobinRouter(nrOfInstances = 16)), "workers")

    val backendRegisterActor = system.actorOf(Props(new WorkerRegisterActor(localBackendWorkerRouter, key)), name = "backend")
    Cluster(system)

    val clusterRouterSettings = ClusterRouterSettings(totalInstances = 100, routeesPath = "/user/frontend", allowLocalRoutees = true, useRole = None)

    val clusterAwareWorkerRouter = system.actorOf(Props.empty.withRouter(ClusterRouterConfig(RoundRobinRouter(), clusterRouterSettings)), name = "frontendRouter")

    ClusterReceptionistExtension(system).registerService(clusterAwareWorkerRouter)
  }
}

object ClusterDemoRoutingAppClient extends App {

  // Initializing the ActorRef
  var clusterClient: ActorRef = _
  args.length match {
    case 1 =>
      val key = args(0)
      println(s"The Client will be sending the Cluster Message with key: $key")
      val config = ConfigFactory.load("client-application.conf")
      val system = ActorSystem("client", config)
      // Node on 3001 should be started first
      val initialContacts = Set(system.actorSelection("akka.tcp://ClusterSystem@127.0.0.1:3001/user/receptionist"))
      clusterClient = system.actorOf(ClusterClient.props(initialContacts))
      Thread.sleep(5000)
      (1 to 100000) map { i => send(key) }
    case _ =>
      println("Invalid Arguments")
  }

  def send(key: String) = {
    Thread.sleep(500)
    println("sending key...")
    clusterClient ! ClusterClient.Send("/user/frontendRouter", ClusterMessage(key), localAffinity = true)
  }
}

class FrontEndActor extends Actor with ActorLogging {
  // Create an Empty List of Actors
  var actors = List.empty[(ActorRef, String)]
  def receive = {
    case message: ClusterMessage => {
      log.info(s"Received ClusterMessage ${message}")
      sendMessage(message)
    }
    case BackendRegistration(routingKey) => {
      log.info(s"sender: ${sender} | key: ${routingKey}")
      println(s"backend registration request arrived from sender: ${sender} for key ${routingKey}")
      // Concatenating to the List of Actors
      actors = (sender, routingKey) :: actors
      context.watch(sender)
    }
    case Terminated(terminatedActor) => {
      log.warning("Received a Terminated message from {}", terminatedActor)
      // actors = actors filterNot { actor => actor == terminatedActor }
      actors = actors.filter(_ != terminatedActor)
    }
  }

  def sendMessage(message: ClusterMessage) = {
    val (actor, key) = (actors.filter { case (actor, routingKey) => routingKey == message.company }).head
    actor ! message
  }
}

class WorkerRegisterActor(localBackendWorkerRouter: ActorRef, key: String) extends Actor with ActorLogging {
  val cluster = Cluster(context.system)
  var routingKey = key

  override def preStart(): Unit = {
    cluster.subscribe(self, classOf[MemberUp])
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }

  def receive = {
    case message: ClusterMessage => {
      log.info(s"Message ${message} at [${sender.path}]")
      localBackendWorkerRouter ! message
    }
    case status: CurrentClusterState => {
      log.info(s"Member Status ${status} at [${sender.path}]")
      status.members.filter(p => p.status == MemberStatus.Up).foreach(register)
    }
    case MemberUp(member) => {
      log.info(s"Registering the ClusterMember ${member}!!!")
      register(member)
    }
  }

  def register(member: Member) = context.actorSelection(RootActorPath(member.address) / "user" / "frontend") ! BackendRegistration(routingKey)
}

class BackendWorker extends Actor with ActorLogging {
  def receive = {
    case msg => {
      log.info(s"${msg} in ${self} at [${self.path}]")
      println(s"Message is: $msg in $self")
    }
  }
}