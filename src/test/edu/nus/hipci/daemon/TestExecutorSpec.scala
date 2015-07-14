package edu.nus.hipci.daemon

import org.scalatest.time.{Seconds, Span}

import scala.collection.immutable.Map
import scala.concurrent._
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.pattern._
import akka.util.Timeout
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._

import edu.nus.hipci.common._

/**
 * Test the functionality of TestExecutor
 *
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
class TestExecutorSpec extends FlatSpec {
  import request._
  import response._

  val system = ActorSystem("hipci-test", Daemon.defaultClientConfig)
  TestExecutor.register(system)
  val subject = system.actorOf(TestExecutor.props, "TestExecutor")
  val patience = Span(10, Seconds)
  implicit val akkaTimeout = Timeout(10.seconds)
  val path = System.getenv().get("PATH")

  "TestExecutor" should "execute simple HIP/SLEEK test suite" in {
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
    whenReady(subject ? SubmitTest(config), timeout(patience)) {
      _ match {
        case promise: Promise[_] =>
          Await
            .result(promise.future, akkaTimeout.duration)
            .asInstanceOf[TestComplete].result shouldEqual config
        case _ => assert(false)
      }
    }
  }
}
