package edu.nus.hipci.core

import java.nio.file.Paths
import scala.pickling._
import json._
import scala.pickling.Defaults._

/**
 * A schema of a configuration file used when running the CI.
 *
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */

object TestConfiguration {
  private case class PTestConfiguration
  (testID: String, projectDirectory : String, hipDirectory : String,
   sleekDirectory : String, timeout: Long, tests: Map[String, Seq[String]])

  private implicit val genTest = Pickler.generate[GenTest]
  private implicit val testConf = Pickler.generate[PTestConfiguration]
  private implicit val staticOnly = scala.pickling.static.StaticOnly

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

  private def toP(test: TestConfiguration): PTestConfiguration = {
    PTestConfiguration(test.testID, test.projectDirectory, test.hipDirectory,
      test.sleekDirectory, test.timeout, test.tests.mapValues(
        _.toSeq .map(_.pickle.value).map(identity)))
  }

  private def fromP(test: PTestConfiguration): TestConfiguration = {
    TestConfiguration(test.testID, test.projectDirectory, test.hipDirectory,
      test.sleekDirectory, test.timeout,
      test.tests.mapValues(_.map(_.unpickle[GenTest]).toSet).map(identity))
  }

  def toJSON(test: TestConfiguration) = toP(test).pickle.value


  def fromJSON(json: String) = fromP(json.unpickle[PTestConfiguration])
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
  tests: Map[String, Set[GenTest]] = Map.empty
) {
  /**
   * Returns the copy of the test configuration without ID.
   * @return A copy of the test configuration without the ID
   */
  def withoutID() = {
    this.copy(testID = "")
  }
}


