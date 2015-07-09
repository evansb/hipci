package scala.hipci.cli

import com.typesafe.config.ConfigFactory

import scala.concurrent.{Future, Await}
import akka.actor.{ActorSystem, Props}
import akka.pattern._
import scala.hipci.executor.Daemon
import scala.hipci.{constant, request}

/**
 * Defines an entry point of the CLI
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
object CLIApp extends CLIComponentDescriptor with App {
  val name = "Main"
  val props = Props[CLIApp]
  val subComponents = List(CommandParser, CommandDispatcher)

  if (args.length == 1 && args(0).equals("start")) {
    Daemon.start()
  } else {
    val system = ActorSystem(constant.AppName, Daemon.defaultClientConfig)
    System.setProperty(org.slf4j.impl.SimpleLogger.SHOW_THREAD_NAME_KEY, "false")
    System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO")
    system.actorOf(Props[CLIApp], "Main") ! (system,args)
  }
}

private class CLIApp extends CLIComponent {
  val descriptor = CLIApp

  override def receive = {
    case (system, args) =>
      descriptor.register(system.asInstanceOf[ActorSystem])
      val commandParser = loadComponent(CommandParser)
      val commandDispatcher = loadComponent(CommandDispatcher)
      val wrappedArgs = request.Arguments(args.asInstanceOf[Array[String]])
      val command = Await.result(commandParser?wrappedArgs, timeout.duration).asInstanceOf[Option[Command]]
      val exitCode = Await.result(commandDispatcher?command, timeout.duration).asInstanceOf[Option[request.Terminate]]
      exitCode match {
        case Some(request.Terminate(ec)) => System.exit(ec)
        case _ => ()
      }
    case other => super.receive(other)
  }
}
