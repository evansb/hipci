package edu.nus.hipci.cli

import scopt._
import edu.nus.hipci.core._

/** Base type of requests accepted by this component */
sealed trait CommandParserRequest

/**
 * Request to parse a sequence of arguments.
 *
 * @constructor Create a request to parse a sequence of arguments.
 * @param arguments The list of arguments to be parsed
 */
case class ParseArguments(arguments: Seq[String]) extends CommandParserRequest

/** Request object to show help usage based on parser */
case object ShowUsage extends CommandParserRequest

/** Base type of responses sent by this component */
sealed trait CommandParserResponse

/**
 * Parsed arguments as a response of [[edu.nus.hipci.cli.ParseArguments]]
 * requests.
 *
 * @constructor Create a response of parse argument request.
 * @param command The command object.
 */
case class ParsedArguments(command: Command) extends CommandParserResponse

/** Singleton descriptor for [[CommandParser]] */
object CommandParser extends ComponentDescriptor[CommandParser]

/**
 * Parses a list of arguments to a [[Command]] object
 */
class CommandParser extends CLIComponent {
  /** Descriptor of this component */
  val descriptor = CommandParser

  implicit object ZeroCommand extends Zero[Command] {
    def zero = EmptyCommand()
  }

  private object parser extends OptionParser[Command](AppName) {
    val configFile =
      arg[String]("config_file") required() action {
        (cf, previous) => previous match {
          case RunCommand(_, as) => RunCommand(cf, as)
          case _ => EmptyCommand()
        }
      }

    val configFile2 =
      arg[String]("config_file") required() action {
        (cf, previous) => previous match {
          case DiffCommand(_, as) => DiffCommand(cf, as)
          case _ => EmptyCommand()
        }
      }

    val params =
      arg[String]("suite") optional() unbounded() action {
        (a, previous) => previous match {
          case RunCommand(cf,as) => RunCommand(cf,a::as)
          case _ => EmptyCommand()
        }
      }

    val revisions = arg[String]("revision") required() unbounded() action {
      (revision, previous) => previous match {
        case DiffCommand(cf, rs) => DiffCommand(cf, revision::rs)
        case _ => EmptyCommand()
      }
    }

    head(AppName, AppVersion)

    cmd("run").text("Run a test configuration").action({
      (_, previous) => previous match {
        case EmptyCommand() => RunCommand("", List())
        case _ => EmptyCommand()
      }
    }).children(configFile.children(params))

    cmd("diff").text("Diff test outcome of several revisions").action({
      (_, previous) => previous match {
        case EmptyCommand() => DiffCommand()
        case _ => EmptyCommand()
      }
    }).children(configFile2.children(revisions))

    cmd("start") text "Start the hipci daemon" action {
      (_, previous) => previous match {
        case EmptyCommand() => StartCommand()
        case _ => EmptyCommand()
      }
    }

    cmd("stop") text "Stop the hipci daemon" action {
      (_, previous) => previous match {
        case EmptyCommand() => StopCommand()
        case _ => EmptyCommand()
      }
    }

    cmd("help") text "Show the usage help" action {
      (_, previous) => previous match {
        case EmptyCommand() => HelpCommand()
        case _ => EmptyCommand()
      }
    }
  }

  override def receive = {
    case ParseArguments(args) =>
      sender ! ParsedArguments(parser.parse(args,
        EmptyCommand()).getOrElse(EmptyCommand()))
    case ShowUsage => sender ! parser.usage
    case other => super.receive(other)
  }
}