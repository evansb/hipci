package edu.nus.hipci.core

import akka.actor.{ActorSystem, ActorRef, Props}
import scala.collection.mutable

/**
 *  Stores the metadata of a component
 */
abstract class ComponentDescriptor[T : Manifest] {
  /**
   * The unique name of this component
   */
  val name : String =  manifest[T].runtimeClass.getCanonicalName

  /**
   * List of component dependencies
   */
  val subComponents : List[ComponentDescriptor[_ <: Component]] =
    List.empty[ComponentDescriptor[_ <: Component]]

  /**
   * Props of this component.
   * When a descriptor is accompanied by a class, it should use the props
   * based on that class.
   */
  def props : Props = Props.create(manifest[T].runtimeClass)

  /**
   * Mappings between components and its ActorRef
   */
  val actors : mutable.Map[String, ActorRef] = mutable.Map.empty

  /**
   * Register this descriptor to a system and get its actor ref
   * @param system The system this descriptor to be registered to
   */
  def register(system: ActorSystem) : ActorRef = {
    this.subComponents.foreach((s) => {
      actors(s.name) = s.register(system)
    })
    system.actorOf(props)
  }
}
