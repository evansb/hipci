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
import sorm._

import edu.nus.hipci.core._

/**
 * Test the functionality of TestExecutor
 *
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
class DaemonSpec extends FlatSpec {
  object mockDatabase extends Instance(
    entities = Set(Entity[DbEntity]()),
    url = "jdbc:h2:mem:hipci",
    initMode = InitMode.Create,
    poolSize = 20)
  new Thread(new Main(mockDatabase)).start()

  val system = ActorSystem("hipci-test", AppConfiguration.getClientConfig())
  val daemon = system.actorSelection("akka.tcp://hipci@127.0.0.1:2552/user/Daemon")
  val subject = Forwarder.newForwarder(system, daemon)
  implicit val timeout = Timeout(10.seconds)
  val path = System.getenv().get("PATH")

  AppConfiguration.global.projectDirectory = "vendor"
  AppConfiguration.global.hipDirectory = "../fixtures/hip"
  AppConfiguration.global.sleekDirectory = "../fixtures/sleek"

  "Daemon" should "assign ticket for test request and get the result later" in {
    val config = TestConfiguration(
      tests = Map(
        "test" -> Set(
          GenTest(
            path = "test_hip.ss",
            kind = "hip",
            arguments = List.empty,
            specs = Map("append" -> true)
          ))))
    var result : TestResult = null
    whenReady(subject ? SubmitTest(config)) {
      case promise: Promise[_] =>
        result = Await.result(promise.future, 2.seconds).asInstanceOf[TicketAssigned]
      case _ => assert(false)
    }
    Thread.sleep(2000)
    val ticket = result.asInstanceOf[TicketAssigned].ticket
    whenReady (subject ? CheckTicket(ticket)) {
      case promise: Promise[_] =>
        Await.result(promise.future, 2.seconds).isInstanceOf[TestComplete] shouldBe true
      case _ => assert(false)
    }
  }

  it should "respond to ping with ack" in {
    whenReady (subject ? Ping) {
      case promise: Promise[_] =>
        Await.result(promise.future, 2.seconds) shouldEqual ACK
      case _ => assert(false)
    }
  }


}
