package edu.nus.hipci.cli

import edu.nus.hipci.core._

/** Base type of requests accepted by this component */
sealed trait CLIComponentRequest

/** Base type of responses sent by this component */
sealed trait CLIComponentResponse

/**
 * A request to terminate a component.
 *
 * @constructor Create a request to terminate a component.
 * @param exitCode Optional exit code, default to successful termination (0).
 */
case class Terminate(exitCode: Int = 0) extends CLIComponentRequest

/**
 * CLIException constructors defined for this package.
 */
abstract class CLIException(message: String)
  extends scala.RuntimeException(s"Terminated due to error:\n\n$message")

case class ParseError(str: String, file: String = "unknown",
                      ln: Int = 0, col: Int = 0)
  extends CLIException(
    s"""
       | Parse error in $file ln $ln col $ln
        | $str
    """.stripMargin)

case class InvalidHipSpec(spec: String)
  extends CLIException(
    s"""
       | Invalid HIP function specification:
       |   $spec
        | HIP function specification should looks like
        | foo.SUCCESS  foo.FAIL
        |
        | Any other forms will throws this error message.
    """.stripMargin)

case class DirtyRepository(path: String)
  extends CLIException(
    s"""
       | The following repository has uncommitted changes and cannot be tested
       |   $path
    """.stripMargin)

case class RevisionNotFound(revision: String)
  extends CLIException(
    s"""
        | Revision not found: $revision
        |
        | Please make sure you use correct Mercurial revision format such as
        | a8b2s or ann2_auto:tip
    """.stripMargin)

case class UnrecognizedCommand(sender: String, receiver: String, message: String)
  extends CLIException(
    s"""
       | Unrecognized message
       |  Send by: $sender
        |  To: $receiver
        |  Message:
        |    $message
    """.stripMargin)

/**
 * Base class for all the CLI Components.
 *
 * Extends [[edu.nus.hipci.core.Component]] to listen for common
 * CLI operation such as Termination with exit code, and to throw exception
 * with appropriate error message in case of unknown request.
 */
abstract class CLIComponent extends Component {
  override def receive = {
    case Terminate(exitCode) =>
      if (exitCode != 0) {
        logger.bad(s"${ descriptor.name } was terminated due to error.")
      } else {
        logger.good(s"${ descriptor.name } was terminated successfully.")
      }
      context.stop(self)
    case other =>
      throw UnrecognizedCommand(sender().toString, this.descriptor.name,
        other.toString)
  }
}
