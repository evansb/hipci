package edu.nus.hipci.daemon

import scala.collection.immutable.Map
import scala.concurrent._
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.pattern._
import akka.util.Timeout
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import edu.nus.hipci.core._

/**
 * Test the functionality of TestExecutor
 *
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
class TestExecutorSpec extends FlatSpec {
  val system = ActorSystem("hipci-test")
  TestExecutor.register(system)
  val subject = system.actorOf(TestExecutor.props, "TestExecutor")
  val patience = Span(10, Seconds)
  implicit val akkaTimeout = Timeout(10.seconds)
  val path = System.getenv().get("PATH")

  AppConfiguration.global.projectDirectory = "vendor"
  AppConfiguration.global.hipDirectory = "../fixtures/hip"
  AppConfiguration.global.sleekDirectory = "../fixtures/sleek"

  "TestExecutor" should "execute simple HIP/SLEEK test suite" in {
    val config = TestConfiguration(
      tests = Map(
        "test" -> Set(
          GenTest(
            path = "test_hip.ss",
            kind = "hip",
            arguments = List.empty,
            specs = Map("append" -> true)
          ),
          GenTest(
            path = "test_sleek.slk",
            kind = "sleek",
            arguments = List.empty,
            specs = Map("1" -> false, "2" -> true)))))
    whenReady(subject ? SubmitTest(config), timeout(patience)) {
      case promise: Promise[_] =>
        Await
          .result(promise.future, akkaTimeout.duration)
          .asInstanceOf[TestComplete].result shouldEqual config
      case _ => assert(false)
    }
  }
}
