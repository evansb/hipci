package scala.hipci.executor

import akka.actor.{ActorSystem}
import akka.pattern._
import akka.util.Timeout
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._

import scala.collection.immutable.Map
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.hipci.common.{HipTest, TestPool, ConfigSchema}
import scala.hipci.request._
import scala.hipci.response.{TestComplete, TestResult, TicketAssigned}

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

  "Daemon" should "assign ticket for test request and get the result later" in {
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
    val result = Await.result(subject ? SubmitTest(config), 1 seconds).asInstanceOf[TestResult]
    result.isInstanceOf[TicketAssigned] shouldBe true
    Thread.sleep(1000)
    val ticket = result.asInstanceOf[TicketAssigned].ticket
    whenReady (subject ? CheckTicket(ticket)) {
      case (n) => n.isInstanceOf[TestComplete] shouldBe true
    }
  }
}
