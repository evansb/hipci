package edu.nus.hipci.core

/**
 * Generic test definition for both reference and output test.
 *
 * @constructor Create a generic test object.
 * @param path The full absolute path of the test file.
 * @param kind The string representing kind of test, e.g "hip".
 * @param arguments Additional arguments when running the test.*
 * @param specs The mapping between spec name and its outcome.
 */
case class GenTest
(
  path: String,

  kind: String,

  arguments: Seq[String] = List.empty[String],

  specs: Map[String, Boolean] = Map.empty[String, Boolean]
  )
