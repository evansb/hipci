package edu.nus.hipci.cli

/** Base trait for command object */
trait Command

/**
 * Models a command to create an application configuration file int
 * interactively.
 */
case object InitCommand extends Command

/**
 * Models a command to run a test configuration.
 *
 * @constructor Creates a run command.
 * @param config The path to the test config file used.
 * @param arguments Additional arguments for the run command.
 *                  Does nothing for now
 */
case class RunCommand(config: String = "", arguments: List[String] = List())
  extends Command

/**
 * Models a command to analyse difference in test outcome across revisions.
 *
 * @constructor Creates a diff command.
 * @param config The path to the test config file used.
 * @param revisions List of revision id to be compared.
 */
case class DiffCommand(config: String ="", revisions: List[String] = List())
  extends Command

/**
 * Models a command to show usage help.
 *
 * @constructor Creates a help command.
 */
case object HelpCommand extends Command

/**
 * Models a command that does nothing.
 *
 * It is a zero value for [[ Command ]]
 *
 * @constructor Creates an empty command.
 */
case object EmptyCommand extends Command

/**
 * Models a command to start the Daemon process.
 *
 * @constructor Creates a start command.
 */
case object StartCommand extends Command

/**
 * Models a command to stop the Daemon process.
 *
 * @constructor Creates a stop command.
 */
case object StopCommand extends Command

/** A command can be dispatched to return a request back to the component */
trait Dispatcher[-T <: Command] {

  /** Dispatch a command object */
  def dispatch(command: T): CLIComponentRequest
}
