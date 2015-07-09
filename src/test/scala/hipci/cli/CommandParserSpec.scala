package scala.hipci.cli

import akka.actor.{Props, ActorSystem}
import akka.pattern._
import akka.util.Timeout
import org.scalatest.concurrent.ScalaFutures._
import scala.concurrent.duration._
import scala.hipci.request
import org.scalatest._
import Matchers._

/**
 * Tests the functionality of the command parser.
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
class CommandParserSpec extends FlatSpec {
  val system = ActorSystem("hipci-test")
  val subject = system.actorOf(Props[CommandParser], "CommandParser")
  implicit val timeout = Timeout(1.seconds)

  "CommandParser" should "parse hipci run app.conf" in {
    whenReady(subject ? request.Arguments(Seq("run", "app.conf"))) {
      _ shouldEqual Some(RunCommand("app.conf", List()))
    }
  }

  it should "parse hipci run app.conf arg1 arg2" in {
    whenReady(subject ? request.Arguments(Seq("run", "app.conf", "arg1", "arg2"))) {
      _ shouldEqual Some(RunCommand("app.conf", List("arg1", "arg2")))
    }
  }

  it should "parse hipci diff rev1 rev2" in {
    whenReady(subject ? request.Arguments(Seq("diff", "rev1", "rev2"))) {
      _ shouldEqual Some(DiffCommand(("rev1","rev2")))
    }
  }

  it should "parse hipci help" in {
    whenReady(subject ? request.Arguments(Seq("help"))) {
      _ shouldEqual Some(HelpCommand)
    }
  }

  it should "parse hipci start" in {
    whenReady(subject ? request.Arguments(Seq("start"))) {
      _ shouldEqual Some(StartCommand)
    }
  }

  it should "parse hipci stop" in {
    whenReady(subject ? request.Arguments(Seq("stop"))) {
      _ shouldEqual Some(StopCommand)
    }
  }

  it should "fail to parse hipci run" in {
    whenReady(subject ? request.Arguments(Seq("run"))) {
      _ shouldEqual None
    }
  }

  it should "fail to parse hipci diff rev1" in {
    whenReady(subject ? request.Arguments(Seq("diff", "rev1"))) {
      _ shouldEqual None
    }
  }
}
