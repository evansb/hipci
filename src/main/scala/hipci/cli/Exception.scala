package scala.hipci.cli

/**
 * Exception constructors defined for this package.
 * @author Evan Sebastian
 */
sealed abstract class Exception(message: String)
  extends scala.RuntimeException(s"Terminated due to error:\n\n$message")

case class ParseError(str: String, file: String = "unknown", ln: Int = 0, col: Int = 0)
  extends Exception(
    s"""
      | Parse error in $file ln $ln col $ln
      | $str
    """.stripMargin)

case class InvalidHipSpec(spec: String)
  extends Exception(
    s"""
      | Invalid HIP function specification:
      |   $spec
      | HIP function specification should looks like
      | foo.SUCCESS  foo.FAIL
      |
      | Any other forms will throws this error message.
    """.stripMargin)

case class FileNotFound(filename: String)
  extends Exception(
    s"""
       | Unable to locate configuration file:
       |   $filename
    """.stripMargin)

case class UnrecognizedCommand(sender: String, receiver: String, message: String)
  extends Exception(
    s"""
       | Unrecognized message
       |  Send by: $sender
       |  To: $receiver
       |  Message:
       |    $message
    """.stripMargin)
