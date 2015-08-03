package edu.nus.hipci.core

import scala.concurrent.duration._
import akka.actor._
import akka.util.Timeout
import pl.project13.scala.rainbow._

/** Base class for all the components */
abstract class Component extends Actor with ActorLogging {
  /** The descriptor of the component. */
  protected val descriptor : ComponentDescriptor[_ <: Component]

  /** Wraps Akka logger with colors. */
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
   *
   * Also recursively register its sub-components to the actor system.
   * @param system The actor system in which this component will be registered
   * @param descriptor The descriptor of this component
   * @return An actor reference of the component
   */
  def registerComponent(system: ActorSystem,
                        descriptor: ComponentDescriptor[_<:Component])
  : ActorRef = {
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
  def loadComponent(component: ComponentDescriptor[_ <: Component]) : ActorRef =
    descriptor.actors(component.name)

  /**
   * Default timeout for a sub commands (30 seconds)
   * Please override this field if the component is expected to take shorter to finish
   */
  implicit val timeout = Timeout(30.seconds)

  /** Default receive behaviour is ignore all messages */
  override def receive = {
    case _ => ()
  }
}
