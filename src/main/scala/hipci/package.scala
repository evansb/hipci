package scala.hipci

import scala.hipci.common.{ConfigSchema, TestPool, Diff3, GenTest}

/**
 * Package-wide constants
 *
 * @author Evan Sebastian
 */
package object constant {

  /**
   * Name of the application
   * @return The name of the application
   */
  def AppName = "hipci"

  /**
   * Current version of the application
   * @return The version of the application
   */
  def AppVersion = "1.0.0"

  def HipSpecSeparator = "."

  def HipSuccess = "SUCCESS"

  def HipFail = "FAIL"

  def SleekValid = "VALID"

  def SleekInvalid = "INVALID"

  val HipOutputRegex = """Procedure\s(\w*)\$\S*\s(\w*).*$""".r

  val SleekOutputRegex = """Entail\s(\d*):\s(\w*).*$""".r
}

/**
 * Some message wrapper for values that could not be passed as messages
 */
package object request {
  case class Arguments(arguments : Seq[String])
  case class Config(config: com.typesafe.config.Config)
  case class Call(method: String)
  case class Terminate(exitCode: Int)
  case class ParseHipOutput(output: String)
  case class ParseSleekOutput(output: String)
  case class FilterHipOutput(output: String, procedures: Set[String])
  case class FilterSleekOutput(output: String, entailment: Set[Int])
  case class AnalyzeOutput[K,V](output1: GenTest[K,V], output2: GenTest[K,V], reference: GenTest[K,V])
  case class SubmitTest(config: ConfigSchema)
  case class CheckTicket(ticket: String)
}

/**
 * Some message wrapper for values that could not be passed as messages
 */
package object response {
  case class ParsedHipOutput(output: Map[String, Boolean])
  case class ParsedSleekOutput(output: Map[Int, Boolean])
  case class FilteredHipOutput(output: Map[String, String])
  case class FilteredSleekOutput(output: Map[Int, String])

  abstract class TestResult()
  case object TestInQueue extends TestResult()
  case class TicketAssigned(ticket: String) extends TestResult()
  case class TicketNotFound(ticket: String) extends TestResult()
  case class TestComplete(duration: Long, result: ConfigSchema) extends TestResult()
  case class CompilationError(error: String) extends TestResult()
  case class RuntimeError(error: String) extends TestResult()
}

