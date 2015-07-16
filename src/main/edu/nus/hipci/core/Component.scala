package edu.nus.hipci.core

import scala.concurrent.duration._
import akka.actor._
import akka.util.Timeout

/**
 * Base class for all the CLI components
 *
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
abstract class Component extends Actor {
  /**
   * The descriptor of the component
   */
  protected val descriptor : ComponentDescriptor

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
