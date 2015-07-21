package edu.nus.hipci.core

import scala.concurrent.duration._
import akka.actor._
import akka.util.Timeout
import pl.project13.scala.rainbow._

/**
 * Base class for all the CLI components
 *
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
abstract class Component extends Actor with ActorLogging {
  /**
   * The descriptor of the component.
   */
  protected val descriptor : ComponentDescriptor

  /**
   * Wraps Akka logger with colors.
   */
  protected object logger {
    def info(str : String) = {
      Console.out.println(str.underlined)
      log.info(str.underlined)
    }

    def error(str: String) = {
      Console.out.println(str.red)
      log.error(str.red)
    }

    def good(str : String) = {
      Console.out.println(str.green)
      log.info(str.green)
    }

    def bad(str: String) = {
      Console.out.println(str.red)
      log.info(str.red)
    }

    def result(str: String) = {
      Console.out.println(str.cyan)
      log.info(str.cyan)
    }
  }

  /**
   * Register a component to an ActorSystem using its descriptor.
   * Also recursively register its sub-components.
   * @param system The actor system in which this component will be registered
   * @param descriptor The descriptor of this component
   * @return An actor reference of the component
   */
  def registerComponent(system: ActorSystem, descriptor: ComponentDescriptor) : ActorRef = {
    descriptor.subComponents.foreach((s) => {
      descriptor.actors(s.name) = registerComponent(system, s)
    })
    system.actorOf(descriptor.props)
  }

  /**
   * Load a pre-registered sub component to get its ActorRef
   * @param component Descriptor of the component to be loaded
   * @return ActorRef of the component.
   */
  def loadComponent(component: ComponentDescriptor) : ActorRef =
    descriptor.actors(component.name)

  /**
   * Default timeout for a sub commands (10 seconds)
   * Please override this field if the component is expected to take shorter to finish
   */
  implicit val timeout = Timeout(10.seconds)

  override def receive = {
    case _ => ()
  }
}
