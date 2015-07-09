package edu.nus.hipci.common

import java.nio.file.Paths
import scala.collection.immutable.HashMap

/**
 * A schema of a configuration file used when running the CI.
 *
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */

object ConfigSchema {
  object Fields {
    val ProjectDirectory = "project_directory"
    val HipDirectory = "hip_directory"
    val SleekDirectory = "sleek_directory"
    val Timeout = "timeout"
  }

  def ReservedKeywords = {
    import Fields._
    Seq(ProjectDirectory, HipDirectory, SleekDirectory, Timeout).toSet
  }
}

case class ConfigSchema
(
  /* Base project directory, usually location of hip and sleek executable */
  projectDirectory : String = Paths.get(sys.env("HOME"), "hg", "sleekex").toString,

  /* Location of HIP test cases, relative to project directory unless specified absolute path */
  hipDirectory : String = Paths.get("examples", "working", "sleek").toString,

  /* Location of SLEEK test cases, relative to project directory unless given absolute path */
  sleekDirectory : String = Paths.get("examples", "working", "hip").toString,

  /* How long the system should wait for a test to run until giving up */
  timeout: Long = 10000,

  /* Test entries */
  tests: Map[String, TestPool[GenTest[_,_]]] = HashMap.empty
)


