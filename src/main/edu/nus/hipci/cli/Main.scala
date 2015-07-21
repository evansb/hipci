package edu.nus.hipci.cli

import scala.concurrent.Await
import akka.actor.{ActorSystem, Props}
import akka.pattern._

import edu.nus.hipci.core._
import edu.nus.hipci.cli.request.InitCLI

/**
 * Entry point of the CLI application.
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
object Main extends CLIComponentDescriptor with App {
  val name = "Main"
  val props = Props[Main]
  val subComponents = List(CommandParser, CommandDispatcher)
  val system = ActorSystem(AppName, DefaultClientConfig)

  this.register(system)
  system.actorOf(Props[Main], "Main") ! InitCLI(args)
}

private class Main extends CLIComponent {
  val descriptor = Main

  import request._
  override def receive = {
    case InitCLI(args) =>
      val commandParser = loadComponent(CommandParser)
      val commandDispatcher = loadComponent(CommandDispatcher)
      val request = ParseArguments(args)
      val command = Await.result(commandParser? request, timeout.duration)
      commandDispatcher ! command
    case Terminate(ec) => System.exit(ec)
    case KeepAlive => ()
    case other => super.receive(other)
  }
}
