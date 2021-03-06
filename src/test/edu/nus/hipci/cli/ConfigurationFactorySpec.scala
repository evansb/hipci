package edu.nus.hipci.cli

import org.scalatest.time.{Seconds, Span}

import scala.util.{Failure, Success}
import scala.collection.immutable.Map
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.util.Timeout
import akka.pattern._
import com.typesafe.config.ConfigFactory
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._

import edu.nus.hipci.core._

/**
 * Tests the functionality of TestConfigurationFactory
 *
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
class ConfigurationFactorySpec extends FlatSpec {
  val system = ActorSystem("hipci-test")
  val subject = ConfigurationFactory.register(system)

  val patience = Span(10, Seconds)
  implicit val akkaTimeout = Timeout(10.seconds)

  "TestConfigurationFactory" should "parse empty configuration" in {
    val config = ConfigFactory.parseString(
      """
        |
      """.stripMargin)
    whenReady(subject ? CreateTestConfiguration(config), timeout(patience)) {
      _ shouldEqual Success(TestConfiguration())
    }
  }

  it should "load app configuration" in {
    val config = ConfigFactory.parseString(
      """
        | project_directory = some_directory
        | hip_directory = custom/hip
        | sleek_directory = custom/sleek
        | daemon_host = "127.0.0.1"
        | daemon_port = 2552
      """.stripMargin)
    whenReady(subject ? LoadAppConfiguration(config), timeout(patience)) { _ =>
      AppConfiguration.global shouldEqual AppConfiguration(
        projectDirectory = "some_directory",
        hipDirectory = "custom/hip",
        sleekDirectory = "custom/sleek",
        daemonHost = "127.0.0.1",
        daemonPort = "2552")
    }
  }

  it should "parse empty configuration with some spec" in {
    val config = ConfigFactory.parseString(
      """
        | infinity = [
        |   [test.ss, -arg, --arg2, foo.SUCCESS, bar.FAIL],
        |   [test.slk, VALID.2, FAIL]
        | ]
      """.stripMargin)

    val pool = Set(
      GenTest(
        path = "test.ss",
        kind = "hip",
        arguments = List("-arg", "--arg2"),
        specs = Map("foo" -> true, "bar" -> false)
      ),
      GenTest(
        path = "test.slk",
        kind = "sleek",
        arguments = List(),
        specs = Map("1" -> true, "1.1" -> true, "1.2" -> true, "2" -> false)
      )
    )

    whenReady(subject ? CreateTestConfiguration(config), timeout(patience)) {
      _ shouldEqual Success(TestConfiguration(testID = "", tests = Map("infinity" -> pool)))
    }
  }

  it should "throw InvalidHipSpec on invalid spec" in {
    val config = ConfigFactory.parseString(
      """
      | infinity = [
      |   [test.ss, -arg, --arg2, foo/error]
      | ]
    """.stripMargin)
    whenReady(subject ? CreateTestConfiguration(config), timeout(patience)) {
      _ shouldEqual Failure(InvalidHipSpec("foo/error"))
    }
  }

  it should "allow empty spec on entries" in {
    val config = ConfigFactory.parseString(
      """
        | infinity = [
        |   [], [],
        |   [test.ss, -arg, --arg2, foo.SUCCESS, bar.FAIL]
        | ]
      """.
        stripMargin)
    val pool = Set(
      GenTest(
        path = "test.ss",
        kind = "hip",
        arguments = List("-arg", "--arg2"),
        specs = Map("foo" -> true, "bar" -> false)
      ))
    whenReady(subject ? CreateTestConfiguration(config), timeout(patience)) {
      _ shouldEqual Success(TestConfiguration(testID = "", tests = Map("infinity" -> pool)))
    }
  }
}
