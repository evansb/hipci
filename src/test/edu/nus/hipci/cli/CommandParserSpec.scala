package edu.nus.hipci.cli

import scala.concurrent.duration._
import akka.actor.{Props, ActorSystem}
import akka.pattern._
import akka.util.Timeout
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest._
import org.scalatest.Matchers._

/**
 * Tests the functionality of the command parser.
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
class CommandParserSpec extends FlatSpec {
  val system = ActorSystem("hipci-test")
  val subject = system.actorOf(Props[CommandParser], "CommandParser")
  implicit val timeout = Timeout(1.seconds)

  "CommandParser" should "parse hipci run app.conf" in {
    whenReady(subject ? ParseArguments(Seq("run", "app.conf"))) {
      _ shouldEqual ParsedArguments(RunCommand("app.conf", List()))
    }
  }

  it should "parse hipci run app.conf arg1 arg2" in {
    whenReady(subject ? ParseArguments(Seq("run", "app.conf", "arg1", "arg2"))) {
      _ shouldEqual ParsedArguments(RunCommand("app.conf", List("arg2", "arg1")))
    }
  }

  it should "parse hipci diff rev1 rev2" in {
    whenReady(subject ? ParseArguments(Seq("diff", "app.conf", "rev1",
      "rev2"))) {
      _ shouldEqual ParsedArguments(DiffCommand("app.conf", List("rev2", "rev1")))
    }
  }

  it should "parse hipci help" in {
    whenReady(subject ? ParseArguments(Seq("help"))) {
      _ shouldEqual ParsedArguments(HelpCommand())
    }
  }

  it should "parse hipci start" in {
    whenReady(subject ? ParseArguments(Seq("start"))) {
      _ shouldEqual ParsedArguments(StartCommand())
    }
  }

  it should "parse hipci stop" in {
    whenReady(subject ? ParseArguments(Seq("stop"))) {
      _ shouldEqual ParsedArguments(StopCommand())
    }
  }

  it should "fail to parse hipci run" in {
    whenReady(subject ? ParseArguments(Seq("run"))) {
      _ shouldEqual ParsedArguments(EmptyCommand())
    }
  }

  it should "fail to parse hipci diff rev1" in {
    whenReady(subject ? ParseArguments(Seq("diff", "rev1"))) {
      _ shouldEqual ParsedArguments(EmptyCommand())
    }
  }
}
