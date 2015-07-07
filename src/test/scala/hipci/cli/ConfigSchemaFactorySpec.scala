package scala.hipci.cli

import akka.actor.{Props, ActorSystem}
import akka.util.Timeout
import akka.pattern._
import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, FlatSpec}
import scala.util.{Failure, Success}
import scala.collection.immutable.Map
import scala.concurrent.duration._
import scala.hipci.common._
import scala.hipci.request
import org.scalatest.concurrent.ScalaFutures._

/**
 * Tests the functionality of ConfigSchemaFactory
 *
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
class ConfigSchemaFactorySpec extends FlatSpec with Matchers {
  val system = ActorSystem("hipci-test")
  val subject = system.actorOf(Props[ConfigSchemaFactory], "ConfigSchemaFactory")
  implicit val timeout = Timeout(1.seconds)

  "ConfigSchemaFactory" should "parse empty configuration" in {
    val defaultConfig = ConfigSchema()
    val config = ConfigFactory.parseString(
      """
        |
      """.stripMargin)
    whenReady(subject ? request.Config(config)) {
      _ shouldEqual Success(defaultConfig)
    }
  }

  it should "parse some configuration with empty spec" in {
    val defaultConfig = ConfigSchema()
    val config = ConfigFactory.parseString(
      """
        | project_directory = /root/some
        | hip_directory = custom/hip
        | sleek_directory = custom/sleek
        | timeout = 2000
      """.stripMargin)
    whenReady(subject ? request.Config(config)) {
      _ shouldEqual Success(defaultConfig.copy(
        projectDirectory = "/root/some",
        hipDirectory = "custom/hip",
        sleekDirectory = "custom/sleek",
        timeout = 2000
      ))
    }
  }

  it should "parse empty configuration with some spec" in {
    val defaultConfig = ConfigSchema()
    val config = ConfigFactory.parseString(
      """
        | infinity = [
        |   [test.ss, -arg, --arg2, foo.SUCCESS, bar.FAIL],
        |   [test.slk, VALID, INVALID]
        | ]
      """.stripMargin)

    val pool : Set[GenTest[_,_]] = Set(
      HipTest(
        path = "test.ss",
        arguments = List("-arg", "--arg2"),
        specs = Map("foo" -> true, "bar" -> false)
      ),
      SleekTest(
        path = "test.slk",
        arguments = List(),
        specs = Map(1 -> true, 2 -> false)
      )
    )

    whenReady(subject ? request.Config(config)) {
      _ shouldEqual Success(defaultConfig.copy(tests = Map("infinity" -> TestPool(pool))))
    }
  }

  it should "throw InvalidHipSpec on invalid spec" in {
    val config = ConfigFactory.parseString(
      """
      | infinity = [
      |   [test.ss, -arg, --arg2, foo/error]
      | ]
    """.stripMargin)
    whenReady(subject ? request.Config(config)) {
      _ shouldEqual Failure(InvalidHipSpec("foo/error"))
    }
  }

  it should "allow empty spec on entries" in {
    val defaultConfig = ConfigSchema()
    val config = ConfigFactory.parseString(
      """
        | infinity = [
        |   [], [],
        |   [test.ss, -arg, --arg2, foo.SUCCESS, bar.FAIL]
        | ]
      """.
        stripMargin)
    val pool : Set[GenTest[_,_]] = Set(
      HipTest(
        path = "test.ss",
        arguments = List("-arg", "--arg2"),
        specs = Map( "foo" -> true, "bar" -> false)
      ))
    whenReady(subject ? request.Config(config)) {
      _ shouldEqual Success(defaultConfig.copy(tests = Map("infinity" -> TestPool(pool))))
    }
  }
}
