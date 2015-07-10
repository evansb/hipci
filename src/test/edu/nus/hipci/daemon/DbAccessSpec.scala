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

  private object TestDb extends Instance (
    entities = Set(Entity[DbEntity]()),
    url = "jdbc:h2:mem:hipci-test",
    initMode = InitMode.DropAllCreate,
    poolSize = 12)
  val system = ActorSystem("hipci-test")
  val subject = system.actorOf(DbAccess(TestDb).props, "DbAccess")
  implicit val timeout = Timeout(1.seconds)

  "DbAccess" should "post and get entity" in {
    val config = ConfigSchema(sleekDirectory = "Hello World")
    val entity = DbEntity("commit", config.hashCode().toString, config)
    whenReady (subject ? Post(entity)) {
      _ shouldEqual QueryOk(entity)
    }
    whenReady (subject ? Get("commit", config.hashCode().toString)) {
      _ shouldEqual QueryOk(entity)
    }
  }

  it should "post and put entity" in {

  }

  it should "return error if get to non existing entity" in {

  }

  it should "return error if put to non existing entity" in {

  }



}

