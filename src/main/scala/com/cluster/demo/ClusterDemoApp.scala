package com.cluster.demo

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.contrib.pattern.ClusterClient

/*
 * Start 2 Nodes of this Cluster in the folowing way with SBT
 * 1. sbt "run-main com.cluster.demo.ClusterServerApp 3001"
 * 2. sbt "run-main com.cluster.demo.ClusterServerApp 3002"
 * 
 * Client will send messaged to the Cluster in the following way
 * sbt "run-main com.cluster.demo.ClusterDemoApp"
 *
 * This will show message flow in the Cluster Nodes
 */

/**
 * @author Sampath
 *
 */
object ClusterDemoApp extends App {

  val config = ConfigFactory.load("client-application.conf")
  val system = ActorSystem("client", config)

  // ClusterSystem must match with the name in Configuration File
  val initialContacts = Set(
    system.actorSelection("akka.tcp://ClusterSystem@127.0.0.1:3001/user/receptionist"),
    system.actorSelection("akka.tcp://ClusterSystem@127.0.0.1:3002/user/receptionist"))

  val clusterClient = system.actorOf(ClusterClient.props(initialContacts))

  // Thread.sleep is used only for the demo. Could be dangerous in Production
  // Scheduler can be used instead of sleep as in the Next Demo
  Thread.sleep(10000)

  (1 to 10000).map(_ => send)

  /*
   * As per the Documentation...
   * You can send messages via the `ClusterClient` to any actor in the cluster
   * that is registered in the [[ClusterReceptionist]].
   * Messages are wrapped in [[ClusterClient.Send]], [[ClusterClient.SendToAll]]
   * or [[ClusterClient.Publish]].
   * The message will be delivered to one recipient with a matching path, if any such
   * exists. If several entries match the path the message will be delivered
   * to one random destination.
   */

  def send = {
    Thread.sleep(500)
    println("sending message...")
    clusterClient ! ClusterClient.Send("/user/workerRouter", "Greetings from ClusterDemoApp", localAffinity = true)
  }

}