package edu.nus.hipci.core

import scala.pickling._
import json._
import scala.pickling.Defaults._

/** A test configuration containing test results and test metadata. */
object TestConfiguration {
  private case class PTestConfiguration(testID: String, tests: Map[String, Seq[String]])

  private implicit val genTest = Pickler.generate[GenTest]
  private implicit val testConf = Pickler.generate[PTestConfiguration]
  private implicit val staticOnly = scala.pickling.static.StaticOnly

  private def toP(test: TestConfiguration): PTestConfiguration = {
    PTestConfiguration(test.testID, test.tests.mapValues(_.toSeq .map(_.pickle.value).map(identity)))
  }

  private def fromP(test: PTestConfiguration): TestConfiguration = {
    TestConfiguration(test.testID, test.tests.mapValues(_.map(_.unpickle[GenTest]).toSet).map(identity))
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
 * @param tests Test suites
 */
case class TestConfiguration
(
  testID: String = "",
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


