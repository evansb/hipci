package scala.hipci.common

import scala.collection.immutable.HashSet

/**
 * An immutable collection of test metadata
 *
 * @author Evan Sebastian
 */
case class TestPool[T <: GenTest[_,_]](pool: Set[T] = HashSet.empty) extends Set[T] {
  override def contains(elem: T) = {
    pool.contains(elem)
  }

  override def +(elem: T) = {
    this.copy(pool = pool.+(elem))
  }

  override def -(elem: T) = {
    this.copy(pool = pool.-(elem))
  }

  override def iterator = pool.iterator
}
