package edu.nus.hipci.core

import org.scalatest.Matchers._
import org.scalatest._

/**
 * Tests the functionality of the Generic Test
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
class TestConfigurationSpec extends FlatSpec {
  "TestConfiguration" should "support JSON deserialization" in {
    val config = TestConfiguration(
      projectDirectory = "vendor",
      hipDirectory = "../fixtures/hip",
      sleekDirectory = "../fixtures/sleek",
      timeout = 10000,
      tests = Map(
        "test" -> Set(
          GenTest(
            path = "test_hip.ss",
            kind = "hip",
            arguments = Set.empty,
            specs = Map("append" -> true)
          ),
          GenTest(
            path = "test_sleek.slk",
            kind = "sleek",
            arguments = Set.empty,
            specs = Map("1" -> false, "2" -> true)))))
    TestConfiguration.fromJSON(TestConfiguration.toJSON(config)) shouldEqual config
  }
}
