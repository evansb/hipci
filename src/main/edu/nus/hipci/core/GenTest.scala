package edu.nus.hipci.core

/**
 * Generic test definition for both reference and output test.
 *
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
case class GenTest
(
  path: String,

  kind: String,

  arguments: Seq[String] = List.empty[String],

  specs: Map[String, Boolean] = Map.empty[String, Boolean]
  )
