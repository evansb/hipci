package scala.hipci.executor

import java.math.BigInteger
import java.security.SecureRandom
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Promise, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.Props
import scala.hipci.common.{ConfigSchema, ComponentDescriptor, Component}
import scala.hipci.request.SubmitTest
import scala.hipci.response.{TestResult, CompilationError, TestComplete}

/**
 * Receives test submission and assign tickets.
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
object Daemon extends ComponentDescriptor {
  val name = "Daemon"
  val props = Props[Daemon]
  val subComponents = List.empty
}

class Daemon extends Component {
  protected val descriptor: ComponentDescriptor = Daemon
  private val cache: mutable.HashMap[String, Promise[TestResult]] = mutable.HashMap.empty
  private val random = new SecureRandom()

  private def createTicket(): String = {
    new BigInteger(130, random).toString(32)
  }

  private def submitTest(config: ConfigSchema)(implicit executionContext: ExecutionContext) = {
    val ticket = createTicket()
    val promise = Promise[TestResult]
    val future = Future {
      promise success CompilationError("error")
    }
    cache(ticket) = promise
    ticket
  }

  override def receive = {
    case SubmitTest(config: ConfigSchema) => sender ! submitTest(config)
    case other => super.receive(other)
  }
}
