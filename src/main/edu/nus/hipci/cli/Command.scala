package edu.nus.hipci.cli

/**
 * Command object that will be parsed from arguments and passed to
 * CommandDispatcher.
 *
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
sealed abstract class Command

/**
 * Models a run command.
 */
case class RunCommand(config: String = "", arguments: List[String] = List()) extends Command

/**
 * Models a diff command.
 */
case class DiffCommand(configFile: String ="",
                       revisions: List[String] = List())
  extends Command

/**
 * Models a help command.
 */
case object HelpCommand extends Command

/**
 * Models an empty command, also a zero value for Command
 */
case object EmptyCommand extends Command

/**
 * Models a start daemon command
 */
case object StartCommand extends Command

/**
 * Models a stop daemon command
 */
case object StopCommand extends Command

