package edu.nus.hipci.cli

import scala.concurrent.Await
import akka.actor.{ActorSystem, Props}
import akka.pattern._
import response.ParsedArguments

import edu.nus.hipci._
import edu.nus.hipci.daemon.Daemon
import edu.nus.hipci.cli.request.InitCLI

/**
 * Entry point of the CLI application.
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
object CLIApp extends CLIComponentDescriptor with App {
  val name = "Main"
  val props = Props[CLIApp]
  val subComponents = List(CommandParser, CommandDispatcher)

  val system = ActorSystem(AppName, Daemon.defaultClientConfig)
  System.setProperty(org.slf4j.impl.SimpleLogger.SHOW_THREAD_NAME_KEY, "false")
  System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO")
  system.actorOf(Props[CLIApp], "Main") ! InitCLI(system, args)
}

private class CLIApp extends CLIComponent {
  val descriptor = CLIApp

  import request._
  override def receive = {
    case InitCLI(system, args) =>
      descriptor.register(system)
      val commandParser = loadComponent(CommandParser)
      val commandDispatcher = loadComponent(CommandDispatcher)
      val request = ParseArguments(args)
      val command = Await.result(commandParser? request, timeout.duration).asInstanceOf[ParsedArguments]
      commandDispatcher ! command
    case Terminate(ec) => System.exit(ec)
    case KeepAlive => ()
    case other => super.receive(other)
  }
}
