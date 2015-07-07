package scala.hipci.executor

import akka.actor.{ActorSystem, Props}
import akka.pattern._
import akka.util.Timeout
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._

import scala.collection.immutable.Map
import scala.concurrent.{Await, Promise}
import scala.concurrent.duration._
import scala.hipci.common.{SleekTest, HipTest, TestPool, ConfigSchema}
import scala.hipci.request._
import scala.hipci.util.OutputParser

/**
 * Test the functionality of TestExecutor
 *
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
class TestExecutorSpec extends FlatSpec {
  val system = ActorSystem("hipci-test")
  TestExecutor.actors("OutputParser") = system.actorOf(Props[OutputParser], "OutputParser")
  val subject = system.actorOf(Props[TestExecutor], "TestExecutor")
  implicit val timeout = Timeout(1.seconds)
  val path = System.getenv().get("PATH")

  "TestExecutor" should "execute simple HIP/SLEEK test suite" in {
    val config = ConfigSchema(
      projectDirectory = "vendor",
      hipDirectory = "../fixtures/hip",
      sleekDirectory = "../fixtures/sleek",
      timeout = 10000,
      tests = Map(
        "test" -> TestPool(Set(
          HipTest(
            path = "test_hip.ss",
            arguments = List.empty,
            specs = Map("append" -> true)
          ),
          SleekTest(
            path = "test_sleek.slk",
            arguments = List.empty,
            specs = Map(1 -> false, 2 -> true))))))
    whenReady(subject ? SubmitTest(config)) {
      _ match {
        case promise: Promise[ConfigSchema] =>
          Await.result(promise.future, 2 seconds) shouldEqual config.tests
        case _ => false shouldEqual true
      }
    }
  }

  it should "execute simple SLEEK file" in {

  }

}
