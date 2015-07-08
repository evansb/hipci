package scala.hipci.executor

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.actor.Props
import akka.pattern._
import scala.hipci.common.{ConfigSchema, ComponentDescriptor, Component}
import scala.hipci.request.{CheckTicket, SubmitTest}
import scala.hipci.response._

/**
 * Receives test submission and assign tickets.
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
object Daemon extends ComponentDescriptor {
  val name = "Daemon"
  val props = Props[Daemon]
  val subComponents = List(TestExecutor)
}

class Daemon extends Component {
  protected val descriptor: ComponentDescriptor = Daemon
  private val cache: mutable.HashMap[String, (Long, Promise[TestResult])] = mutable.HashMap.empty

  private def submitTest(config: ConfigSchema)(implicit executionContext: ExecutionContext) = {
    val testExecutor = loadComponent(TestExecutor)
    val ticket = config.hashCode().toString
    cache.get(ticket) match {
      case None =>
        val promise = Await.result(testExecutor ? SubmitTest(config), 10 milliseconds)
          .asInstanceOf[Promise[TestResult]]
        cache(ticket) = (System.currentTimeMillis, promise)
        TicketAssigned(ticket)
      case Some(_) => checkTicket(ticket)
    }
  }

  private def checkTicket(ticket: String) = {
    cache.get(ticket) match {
      case None => TicketNotFound(ticket)
      case Some((_, p)) =>
        if (p.isCompleted) {
          Await.result(p.future, 1 seconds)
        } else {
          TestInQueue
        }
    }
  }

  override def receive = {
    case SubmitTest(config: ConfigSchema) => sender ! submitTest(config)
    case CheckTicket(ticket: String) => sender ! checkTicket(ticket)
    case other => super.receive(other)
  }
}
