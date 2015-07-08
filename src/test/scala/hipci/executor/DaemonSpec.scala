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
import scala.hipci.response.TicketAssigned
import scala.hipci.util.OutputParser

/**
 * Test the functionality of TestExecutor
 *
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
class DaemonSpec extends FlatSpec {
  val system = ActorSystem("hipci-test")
  Daemon.register(system)
  val subject = system.actorOf(Daemon.props, "Daemon")
  implicit val timeout = Timeout(1.seconds)
  val path = System.getenv().get("PATH")

  "Daemon" should "assign ticket for test request" in {
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
          )))))
    whenReady (subject ? SubmitTest(config)) {
      _.isInstanceOf[TicketAssigned] shouldBe true
    }
  }
}
