package edu.nus.hipci.common

/**
 * Generic test definition for both reference and output test.
 *
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
case class GenTest
(
  path: String,

  kind: String,

  arguments: Set[String] = Set.empty,

  specs: Map[String, Boolean] = Map.empty
  )
