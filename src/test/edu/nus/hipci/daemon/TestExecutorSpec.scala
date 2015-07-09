package edu.nus.hipci.daemon

import scala.collection.immutable.Map
import scala.concurrent._
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.pattern._
import akka.util.Timeout
import org.scalatest.FlatSpec
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
  implicit val timeout = Timeout(10.seconds)
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
        case promise: Promise[_] =>
          assert(Await.result(promise.future, 2.seconds).asInstanceOf[TestComplete].result.equals(config))
        case _ => assert(false)
      }
    }
  }
}