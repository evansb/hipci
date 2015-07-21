package edu.nus.hipci.cli

import edu.nus.hipci.core._

/**
 * Base class for all the CLI components
 *
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
abstract class CLIComponent extends Component {
  import request._
  override def receive = {
    case Terminate(exitCode) =>
      val name = descriptor.name
      if (exitCode != 0) {
        logger.bad(s"$name was terminated due to error.")
      } else {
        logger.good(s"$name was terminated successfully.")
      }
      context.stop(self)
    case other =>
      throw UnrecognizedCommand(sender.toString(), self.toString(), other.toString())
  }
}
