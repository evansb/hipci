package scala.hipci.cli

import scala.hipci.common.Component
import scala.hipci.request

/**
 * Base class for all the CLI components
 *
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
abstract class CLIComponent extends Component {
  /**
   * Logger for the component.
   */
  protected val logger = Logger("")

  override def receive = {
    case request.Terminate(exitCode) =>
      val name = descriptor.name
      if (exitCode != 0) {
        logger.good(s"$name was terminated due to error.")
      } else {
        logger.good(s"$name was terminated successfully.")
      }
      context.stop(self)
    case other =>
      throw UnrecognizedCommand(sender.toString(), self.toString(), other.toString())
  }
}
