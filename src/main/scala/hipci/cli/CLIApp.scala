package scala.hipci.cli

import scala.concurrent.{Future, Await}
import akka.actor.{ActorSystem, Props}
import akka.pattern._
import scala.hipci.{constant, request}

/**
 * Defines an entry point of the CLI
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
object CLIApp extends CLIComponentDescriptor with App {
  val name = "Main"
  val props = Props[CLIApp]
  val subComponents = List(CommandParser, CommandDispatcher)
  val system = ActorSystem(constant.AppName)

  System.setProperty(org.slf4j.impl.SimpleLogger.SHOW_THREAD_NAME_KEY, "false")
  System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO")

  system.actorOf(Props[CLIApp], "Main") ! (system, args)
}

private class CLIApp extends CLIComponent {
  val descriptor = CLIApp

  override def receive = {
    case (system, args) =>
      registerComponent(system.asInstanceOf[ActorSystem], CLIApp)
      val commandParser = loadComponent(CommandParser)
      val commandDispatcher = loadComponent(CommandDispatcher)
      val wrappedArgs = request.Arguments(args.asInstanceOf[Array[String]])
      val command = Await.result(commandParser?wrappedArgs, timeout.duration).asInstanceOf[Option[Command]]
      Await.result(commandDispatcher?command, timeout.duration)

      import context._
      Future { context.self ! request.Terminate(0) } andThen { case _ => System.exit(0) }
    case other => super.receive(other)
  }
}
