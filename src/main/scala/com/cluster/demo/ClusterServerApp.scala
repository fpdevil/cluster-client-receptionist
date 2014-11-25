package com.cluster.demo

import com.typesafe.config.ConfigFactory

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.routing._
import akka.contrib.pattern._
import akka.routing._

/**
 * @author Sampath
 *
 */
object ClusterServerApp {
  def main(args: Array[String]) {
    if (args.nonEmpty)
      System.setProperty("akka.remote.netty.tcp.port", args(0))

    val system = ActorSystem("ClusterSystem")
    /*
     * Router is an actor that receives messages and efficiently routes them to other actors, known as its routees
     * Create 4 Worker Actors (All under Router Actor)
     */

    val router = system.actorOf(Props[Worker].withRouter(RoundRobinRouter(nrOfInstances = 4)), name = "workers")
    Cluster(system)

    val clusterRouterSettings = ClusterRouterSettings(totalInstances = 100, routeesPath = "/user/workers", allowLocalRoutees = true, useRole = None)

    val clusterAwareWorkerRouter = system.actorOf(Props.empty.withRouter(ClusterRouterConfig(RoundRobinRouter(), clusterRouterSettings)), name = "workerRouter")

    ClusterReceptionistExtension(system).registerService(clusterAwareWorkerRouter)
  }
}

class Worker extends Actor with ActorLogging {
  def receive = {
    case msg: String => {
      log.info(s"Message received ${msg} Path: ${self.path}")
      // println(msg + " " + self.path)
      println(msg + " : " + akka.serialization.Serialization.serializedActorPath(self))
    }
  }
}