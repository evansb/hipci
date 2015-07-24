package edu.nus.hipci.core

import java.nio.file.Paths
import scala.pickling._
import json._
import scala.pickling.Defaults._

/** A test configuration containing test results and test metadata. */
object TestConfiguration {
  private case class PTestConfiguration
  (testID: String, projectDirectory : String, hipDirectory : String,
   sleekDirectory : String, timeout: Long, tests: Map[String, Seq[String]])

  private implicit val genTest = Pickler.generate[GenTest]
  private implicit val testConf = Pickler.generate[PTestConfiguration]
  private implicit val staticOnly = scala.pickling.static.StaticOnly

  /** Field names in the file schema. */
  object Fields {
    val ProjectDirectory = "project_directory"
    val HipDirectory = "hip_directory"
    val SleekDirectory = "sleek_directory"
    val Timeout = "timeout"
  }

  /** Set of reserved keywords (i.e those that cannot be used as a suite). */
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

  /**
   * Return a JSON string of a test configuration.
   * @param test The test configuration
   */
  def toJSON(test: TestConfiguration) = toP(test).pickle.value

  /**
   * Return a test configuration from a JSON string.
   *
   * The JSON string must be created previously using [[toJSON]] method.
   * @param json The json string
   */
  def fromJSON(json: String) = fromP(json.unpickle[PTestConfiguration])
}

/**
 * A test configuration.
 *
 * @constructor Create a test configuration
 * @param testID The unique identifier of the configuration.
 * @param projectDirectory Base project directory relative from current dir.
 * @param hipDirectory Base HIP test directory relative from project dir.
 * @param sleekDirectory Base SLEEK test directory relative from project dir.
 * @param timeout Patience time per test file.
 * @param tests Test suites
 */
case class TestConfiguration
(
  testID: String = "",
  projectDirectory : String = ".",
  hipDirectory : String = Paths.get("examples", "working", "sleek").toString,
  sleekDirectory : String = Paths.get("examples", "working", "hip").toString,
  timeout: Long = 10000,
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


