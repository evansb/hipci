package edu.nus.hipci.daemon

import scala.concurrent.duration._
import akka.actor._
import akka.pattern._
import akka.util.Timeout

object Forwarder {
  class Fwd(to: ActorSelection) extends Actor {
    def receive = {
      case msg =>
        implicit val timeout = Timeout(10.seconds)
        sender ! (to ? msg)
    }
  }
  def newForwarder(system: ActorSystem, receiver: ActorSelection) : ActorRef = {
    system.actorOf(Props.create(classOf[Fwd], receiver))
  }
}

