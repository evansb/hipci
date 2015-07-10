package edu.nus.hipci.daemon

import akka.actor.ActorRef
import edu.nus.hipci.common.{DbEntity, ConfigSchema}

/**
 * Requests and Responses defined at this package
 */

object request {
  sealed abstract class Request
  case class SubmitTest(config: ConfigSchema) extends Request
  case class CheckTicket(ticket: String) extends Request
  case class Introduce(actorRef: ActorRef) extends Request
  case object StopDaemon extends Request
  case object Ping extends Request

  sealed abstract class DbQuery extends Request
  case class Post(content : DbEntity) extends DbQuery
  case class Get(commitID: String, configID: String) extends DbQuery
  case class Put(commitID: String, configID: String, content: DbEntity) extends DbQuery
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

  sealed abstract class DbQueryResult extends Response
  case class QueryOk(entity: DbEntity) extends DbQueryResult
  case object QueryNotFound extends DbQueryResult
}
