package edu.nus.hipci.daemon

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.actor._
import akka.pattern._
import sorm.Instance

import edu.nus.hipci.core._

/**
 * Receives test submission and assign tickets.
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
object Daemon {

  def withDbAccess(db: Instance) = {
    val dbAccess = DbAccess(db)
    (new ComponentDescriptor {
      val name = "Daemon"
      val props = Props.create(classOf[Daemon], db)
      val subComponents = List(dbAccess, TestExecutor)
    }, dbAccess)
  }

  def get(context: ActorContext) = {
    context.actorSelection("akka.tcp://hipci@127.0.0.1:2552/user/Daemon")
  }
}

class Daemon(dbi : Instance) extends Component {
  import request._
  import response._

  protected val logger = Logging.toStdout("")
  protected val (descriptor, dbAccess) = Daemon.withDbAccess(dbi)
  private val cache = mutable.HashMap.empty[String, (Long, Promise[TestResult])]

  private def submitTest(config: TestConfiguration)
                        (implicit e: ExecutionContext) = {
    val testExecutor = loadComponent(TestExecutor)
    val ticket = config.testID
    logger.good(s"Submitting ${ticket} to daemon")
    cache.get(ticket) match {
      case None =>
        findInDatabase(ticket) getOrElse {
          val now = System.currentTimeMillis()
          val promise = Await.result(testExecutor?SubmitTest(config), 1.seconds)
            .asInstanceOf[Promise[TestResult]]
          cache(ticket) = (now, promise)
          TicketAssigned(ticket)
        }
      case Some(_) =>
        checkTicket(ticket)
    }
  }

  private def findInDatabase(ticket: String) = {
    val dbActor = loadComponent(dbAccess)
    Await.result(dbActor ? Get(ticket), timeout.duration) match {
      case QueryNotFound => None
      case QueryOk(config) =>
        cache(ticket) = (System.currentTimeMillis(),
          Promise.successful(TestComplete(config)))
        Some(TestComplete(config))
    }
  }

  private def checkTicket(ticket: String) = {
    cache.get(ticket) match {
      case None =>
        findInDatabase(ticket) getOrElse TicketNotFound(ticket)
      case Some((since, promise)) =>
        TestInQueue(ticket, since)
    }
  }

  override def receive = {
    case Ping => sender ! ACK
    case Introduce(sys) => logger.good(s"hipci daemon started")
    case StopDaemon => logger.bad(s"stopping daemon...")
    case SubmitTest(config) => sender ! submitTest(config)
    case CheckTicket(ticket) => sender ! checkTicket(ticket)
    case other => super.receive(other)
  }
}
