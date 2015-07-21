package edu.nus.hipci.core

import java.nio.file.Paths
import scala.collection.immutable.HashMap
import scala.pickling._
import json._
import scala.pickling.Defaults._

/**
 * A schema of a configuration file used when running the CI.
 *
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */

object TestConfiguration {
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

  def toJSON(test: TestConfiguration) : String = {
    implicit val genTest = Pickler.generate[GenTest]
    implicit val testConf = Pickler.generate[TestConfiguration]
    test.pickle.value
  }

  def fromJSON(json: String) : TestConfiguration = {
    implicit val genTest = Pickler.generate[GenTest]
    implicit val testConf = Pickler.generate[TestConfiguration]
    json.unpickle[TestConfiguration]
  }
}

case class TestConfiguration
(
  /* ID of the test, taken from commit ID and hash of the config file */
  testID: String = "",

  /* Base project directory, usually location of hip and sleek executable */
  projectDirectory : String = ".",

  /* Location of HIP test cases, relative to project directory unless specified absolute path */
  hipDirectory : String = Paths.get("examples", "working", "sleek").toString,

  /* Location of SLEEK test cases, relative to project directory unless given absolute path */
  sleekDirectory : String = Paths.get("examples", "working", "hip").toString,

  /* How long the system should wait for a test to run until giving up */
  timeout: Long = 10000,

  /* Test entries */
  tests: Map[String, Set[GenTest]] = HashMap.empty
) {
  /**
   * Returns the copy of the test configuration without ID.
   * @return A copy of the test configuration without the ID
   */
  def withoutID() = {
    this.copy(testID = "")
  }
}


