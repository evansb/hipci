package edu.nus.hipci.cli

import scala.concurrent.Await
import akka.actor.ActorSystem
import akka.pattern._
import edu.nus.hipci.core._

sealed trait MainRequest
case class InitCLI(args: Seq[String]) extends MainRequest
case object KeepAlive extends MainRequest

/** Singleton descriptor of the [[Main]] class. */
object Main extends ComponentDescriptor with App {
  override val subComponents = List(CommandParser, CommandDispatcher)

  val system = ActorSystem(AppName, DefaultClientConfig)
  this.register(system)
  system.actorOf(props, name) ! InitCLI(args)
}

/** Entry point of the CLI Application */
private class Main extends CLIComponent {
  val descriptor = Main

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
