package edu.nus.hipci.daemon

import org.scalatest.time.{Seconds, Span}

import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.util.Timeout
import akka.pattern._
import sorm._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.Matchers._
import edu.nus.hipci.core._

/**
 * Tests the functionality of the database access.
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
class DbAccessSpec extends FlatSpec {
  private object TestDb extends Instance(
    entities = Set(Entity[DbEntity]()),
    url = "jdbc:h2:mem:hipci-test",
    poolSize = 2)

  val system = ActorSystem("hipci-test")
  val subject = system.actorOf(DbAccess(TestDb).props, "DbAccess")
  implicit val akkaTimeout = Timeout(5.seconds)
  val patience = Span(10, Seconds)
  val mockTest = Map("Hello World" -> Set.empty[GenTest])
  val mockTest2 = Map("Hello New World" -> Set.empty[GenTest])

  "DbAccess" should "post and get entity" in {
    val config = TestConfiguration(testID = "commit", tests = mockTest)
    whenReady (subject ? Post(config), timeout(patience)) {
      case QueryOk(c) => config.asInstanceOf[TestConfiguration] shouldEqual c
    }
    whenReady (subject ? Get("commit"), timeout(patience)) {
      case QueryOk(c) => config.asInstanceOf[TestConfiguration] shouldEqual c
    }
  }

  it should "post and put entity" in {
    val config = TestConfiguration(testID = "commit2", tests = mockTest)
    whenReady (subject ? Post(config), timeout(patience)) {
      case QueryOk(c) => config.asInstanceOf[TestConfiguration] shouldEqual c
    }
    whenReady (subject ? Put("commit2", config.copy(tests = mockTest2)),
      timeout(patience)) {
      case QueryOk(c) => c.asInstanceOf[TestConfiguration].tests shouldEqual mockTest2
    }
  }

  it should "return error if get to non existing entity" in {
    whenReady (subject ? Get("commit3"), timeout(patience)) {
      _ shouldBe QueryNotFound
    }
  }

  it should "return error if put to non existing entity" in {
    whenReady (subject ? Put("commit4", TestConfiguration()), timeout(patience)) {
      _ shouldBe QueryNotFound
    }
  }
}

