package edu.nus.hipci.cli

import java.nio.file.Paths
import scala.concurrent.Await
import akka.actor.ActorSystem
import akka.pattern._
import com.typesafe.config.ConfigFactory
import edu.nus.hipci.core._

sealed trait MainRequest
case class InitCLI(args: Seq[String]) extends MainRequest

/** Singleton descriptor of the [[Main]] class. */
object Main extends ComponentDescriptor[Main] with App {
  override val subComponents = List(CommandParser, CommandDispatcher, ConfigurationFactory)
  val system = ActorSystem(AppName, AppConfiguration.getClientConfig())
  this.register(system)
  system.actorOf(props, name) ! InitCLI(args)
}

/** Entry point of the CLI Application */
class Main extends CLIComponent {
  val descriptor = Main

  override def receive = {
    case InitCLI(args) =>
      val hipciConf = Paths.get(HipciConf).toFile()
      if (hipciConf.exists()) {
        val configFactory = loadComponent(ConfigurationFactory)
        configFactory ! LoadAppConfiguration(ConfigFactory.parseFile(hipciConf))
      } else {
      }
      val commandParser = loadComponent(CommandParser)
      val commandDispatcher = loadComponent(CommandDispatcher)
      val request = ParseArguments(args)
      val command = Await.result(commandParser? request, timeout.duration)

      command match {
        case InitCommand | EmptyCommand | HelpCommand => commandDispatcher ! command
        case _ => if (hipciConf.exists()) {
          commandDispatcher ? command
        } else {
          logger bad s"File $HipciConf not found. Run hipci init to generate one"
          System.exit(1)
        }
      }
    case Terminate(ec) => System.exit(ec)
    case KeepAlive => ()
    case () => ()
    case other => super.receive(other)
  }
}
