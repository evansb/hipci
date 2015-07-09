package edu.nus.hipci.daemon

import edu.nus.hipci.common.ConfigSchema

/**
 * Requests and Responses defined at this package
 */

object request {
  sealed abstract class Request
  case class SubmitTest(config: ConfigSchema) extends Request
  case class CheckTicket(ticket: String) extends Request
  case object Ping
}

object response {
  abstract class Response
  sealed abstract class TestResult extends Response
  case class TestInQueue(ticket: String, since: Long) extends TestResult
  case class TicketAssigned(ticket: String) extends TestResult
  case class TicketNotFound(ticket: String) extends TestResult
  case class TestComplete(duration: Long, result: ConfigSchema) extends TestResult
  case class CompilationError(error: String) extends TestResult
  case class RuntimeError(error: String) extends TestResult
  case object ACK
}
