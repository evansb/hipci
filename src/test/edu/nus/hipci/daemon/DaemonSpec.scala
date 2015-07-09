package edu.nus.hipci.daemon

import scala.collection.immutable.Map
import scala.concurrent.{Promise, Await}
import scala.concurrent.duration._
import akka.actor._
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
class DaemonSpec extends FlatSpec {
  import request._
  import response._
  Daemon.start()
  val system = ActorSystem("hipci-test")
  val daemon = system.actorSelection("akka.tcp://hipcid@localhost:2552/user/Daemon")
  val subject = Forwarder.newForwarder(system, daemon)
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
    val result = Await.result(subject ? SubmitTest(config), 1.seconds)
    result.asInstanceOf[TestResult].isInstanceOf[TicketAssigned] shouldBe true
    Thread.sleep(2000)
    val ticket = result.asInstanceOf[TicketAssigned].ticket
    whenReady (subject ? CheckTicket(ticket)) {
      case (n) => n.isInstanceOf[TestComplete] shouldBe true
    }
  }

  it should "respond to ping with ack" in {
    whenReady (subject ? Ping) {
      _ shouldEqual ACK
    }
  }


}
