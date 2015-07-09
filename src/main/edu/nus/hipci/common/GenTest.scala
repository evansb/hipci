package edu.nus.hipci.common

/**
 * Generic test definition for both reference and output test.
 *
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
sealed trait GenTest[S,T] {
  type specsT = specs.type
  val path: String
  val arguments: List[String]
  val specs : Map[S,T]
}

/**
 * Hip test definition
 */
case class HipTest(path: String, arguments: List[String], specs: Map[String, Boolean] = Map.empty)
  extends GenTest[String, Boolean] {
}

/**
 * Sleek test definition
 */
case class SleekTest(path: String, arguments: List[String], specs: Map[Int, Boolean] = Map.empty)
  extends GenTest[Int, Boolean] {
}

