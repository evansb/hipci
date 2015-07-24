package edu.nus.hipci.daemon

import scala.concurrent.duration._
import akka.actor._
import akka.pattern._
import akka.util.Timeout

/**
 * Factory for a simple forwarder actor that forwards all messages to another
 * actor.
 *
 * Useful in avoid dead letters when testing remote actors.
 */
object Forwarder {
  private class Fwd(to: ActorSelection) extends Actor {
    def receive = {
      case msg =>
        implicit val timeout = Timeout(10.seconds)
        sender ! (to ? msg)
    }
  }

  /**
   * Create a new forwarder actor
   * @param system The actor system the `receiver` lives.
   * @param receiver The receiver of the forwarded message
   * @return The [[ActorRef]] of the forwarder
   */
  def newForwarder(system: ActorSystem, receiver: ActorSelection) : ActorRef = {
    system.actorOf(Props.create(classOf[Fwd], receiver))
  }
}

