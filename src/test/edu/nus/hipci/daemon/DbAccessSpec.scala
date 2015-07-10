package edu.nus.hipci.daemon

import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.util.Timeout
import akka.pattern._
import sorm._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures._
import Matchers._
import edu.nus.hipci.common._

/**
 * Tests the functionality of the database access.
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
class DbAccessSpec extends FlatSpec {
  import edu.nus.hipci.daemon.request._
  import edu.nus.hipci.daemon.response._

  private object TestDb extends Instance(
    entities = Set(Entity[GenTest](), Entity[ConfigSchema]()),
    url = "jdbc:h2:mem:hipci-test")

  val system = ActorSystem("hipci-test")
  val subject = system.actorOf(DbAccess(TestDb).props, "DbAccess")
  implicit val timeout = Timeout(1.seconds)

  "DbAccess" should "post and get entity" in {
    val config = ConfigSchema(testID = "commit", sleekDirectory = "Hello World")
    whenReady (subject ? Post(config)) {
      _ match {
        case QueryOk(config) => config.asInstanceOf[ConfigSchema] shouldEqual config
      }
    }
    whenReady (subject ? Get("commit")) {
      _ match {
        case QueryOk(config) => config.asInstanceOf[ConfigSchema] shouldEqual config
      }
    }
  }

  it should "post and put entity" in {
    val config = ConfigSchema(testID = "commit2", sleekDirectory = "Hello World")
    whenReady (subject ? Post(config)) {
      _ match {
        case QueryOk(config) => config.asInstanceOf[ConfigSchema] shouldEqual config
      }
    }
    whenReady (subject ? Put("commit2", config.copy(sleekDirectory = "Hello New World"))) {
      _ match {
        case QueryOk(config) => config.asInstanceOf[ConfigSchema].sleekDirectory shouldEqual "Hello New World"
      }
    }
  }

  it should "return error if get to non existing entity" in {
    whenReady (subject ? Get("commit3")) {
      _ shouldBe QueryNotFound
    }
  }

  it should "return error if put to non existing entity" in {
    whenReady (subject ? Put("commit4", ConfigSchema())) {
      _ shouldBe QueryNotFound
    }
  }
}

