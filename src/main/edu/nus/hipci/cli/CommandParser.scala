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
      arg[String]("config file") required() action {
        (arg, previous) => previous match {
          case previous@RunCommand(_, _) => previous.copy(config = arg)
          case _ => EmptyCommand
        }
      }

    val params =
      arg[String]("arg1, arg2,...") optional() unbounded() action {
        (arg, previous) => previous match {
          case previous@RunCommand(_, oldArgs) => previous.copy(arguments = oldArgs ++ List(arg))
          case _ => EmptyCommand
        }
      }

    val revision1 =
      arg[String]("revision1") required() action {
        (arg, previous) => previous match {
          case previous@DiffCommand((_, s)) => previous.copy(revisions = (arg, s))
          case _ => EmptyCommand
        }
      }

    val revision2 = arg[String]("revision2") required() action {
      (arg, previous) => previous match {
        case previous@DiffCommand((f, _)) => previous.copy(revisions = (f, arg))
        case _ => EmptyCommand
      }
    }

    head(AppName, AppVersion)

    cmd("run").text("run <config file> [arg1, arg2...]").action({
      (_, previous) => previous match {
        case EmptyCommand => RunCommand("", List())
        case _ => EmptyCommand
      }
    }).children(configFile.children(params))

    cmd("diff").text("diff <revision 1> <revision 2>").action({
      (_, previous) => previous match {
        case EmptyCommand => DiffCommand(("", ""))
        case _ => EmptyCommand
      }
    }).children(revision1.children(revision2))

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