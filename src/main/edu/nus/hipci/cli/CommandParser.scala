package edu.nus.hipci.cli

import akka.actor.Props
import scopt._
import edu.nus.hipci._

/**
 * Parses a command line arguments to a Command object.
 *
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
object CommandParser extends CLIComponentDescriptor {
  val name = "CommandParser"
  val subComponents = List.empty
  val props = Props[CommandParser]
}

class CommandParser extends CLIComponent {
  val descriptor: CLIComponentDescriptor = CommandParser

  implicit object ZeroCommand extends Zero[Command] {
    def zero = EmptyCommand
  }

  protected object parser extends OptionParser[Command](AppName) {
    val configFile =
      arg[String]("config_file") required() action {
        (cf, previous) => previous match {
          case RunCommand(_, as) => RunCommand(cf, as)
          case _ => EmptyCommand
        }
      }

    val configFile2 =
      arg[String]("config_file") required() action {
        (cf, previous) => previous match {
          case DiffCommand(_, as) => DiffCommand(cf, as)
          case _ => EmptyCommand
        }
      }

    val params =
      arg[String]("suite") optional() unbounded() action {
        (a, previous) => previous match {
          case RunCommand(cf,as) => RunCommand(cf,a::as)
          case _ => EmptyCommand
        }
      }

    val revisions = arg[String]("revision") required() unbounded() action {
      (revision, previous) => previous match {
        case DiffCommand(cf, rs) => DiffCommand(cf, revision::rs)
        case _ => EmptyCommand
      }
    }

    head(AppName, AppVersion)

    cmd("run").text("Run a test configuration").action({
      (_, previous) => previous match {
        case EmptyCommand => RunCommand("", List())
        case _ => EmptyCommand
      }
    }).children(configFile.children(params))

    cmd("diff").text("Diff test outcome of several revisions").action({
      (_, previous) => previous match {
        case EmptyCommand => DiffCommand()
        case _ => EmptyCommand
      }
    }).children(configFile2.children(revisions))

    cmd("start") text "Start the hipci daemon" action {
      (_, previous) => previous match {
        case EmptyCommand => StartCommand
        case _ => EmptyCommand
      }
    }

    cmd("stop") text "Stop the hipci daemon" action {
      (_, previous) => previous match {
        case EmptyCommand => StopCommand
        case _ => EmptyCommand
      }
    }

    cmd("help") text "Show the usage help" action {
      (_, previous) => previous match {
        case EmptyCommand => HelpCommand
        case _ => EmptyCommand
      }
    }
  }

  import request._
  import response._
  override def receive = {
    case ParseArguments(args) =>
      sender ! ParsedArguments(parser.parse(args, EmptyCommand).getOrElse(EmptyCommand))
    case ShowUsage => sender ! parser.usage
    case other => super.receive(other)
  }
}