package scala.hipci.common

import akka.actor.{ActorSystem, ActorRef, Props}
import scala.collection.mutable

/**
 *  Stores the metadata of a component
 */
abstract class ComponentDescriptor {
  /**
   * The unique name of this component
   */
  val name : String

  /**
   * List of component dependencies
   */
  val subComponents : List[ComponentDescriptor]

  /**
   * Props of this component.
   * When a descriptor is accompanied by a class, it should use the props
   * based on that class.
   */
  val props : Props

  /**
   * Mappings between components and its ActorRef
   */
  val actors : mutable.Map[String, ActorRef] = mutable.Map.empty

  /**
   * Register this descriptor to a system and get its actor ref
   * @param system
   */
  def register(system: ActorSystem) : ActorRef = {
    this.subComponents.foreach((s) => {
      actors(s.name) = s.register(system)
    })
    system.actorOf(props)
  }
}
