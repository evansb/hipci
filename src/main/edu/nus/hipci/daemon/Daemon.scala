package edu.nus.hipci.daemon

import java.util.concurrent.TimeoutException

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.actor._
import akka.pattern._
import akka.util.Timeout
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
      val props = Props.create(classOf[Daemon], this, dbAccess)
      val subComponents = List(dbAccess, TestExecutor)
    }, dbAccess)
  }

  def get(context: ActorRefFactory) : Option[ActorRef] = {
    val path = "akka.tcp://hipci@127.0.0.1:2552/user/Daemon"
    val daemon = context.actorSelection(path)
    implicit val timeout = Timeout(1.second)
    Await.result(daemon ? Identify(path), 1.second) match {
      case ActorIdentity(_, ref) => ref
      case _ => None
    }
  }
}

class Daemon(desc: ComponentDescriptor, dbAccess: ComponentDescriptor)
  extends Component {
  import request._
  import response._

  protected val descriptor = desc
  private val cache = mutable.HashMap.empty[String, (Long, Promise[TestResult])]

  private def submitTest(config: TestConfiguration)
                        (implicit e: ExecutionContext) = {
    val testExecutor = loadComponent(TestExecutor)
    val dbActor = loadComponent(dbAccess)
    val ticket = config.testID
    logger.good(s"Received ticket ${ticket}")
    cache.get(ticket) match {
      case None =>
        logger.good(s"Not found in cache...")
        val result = findInDatabase(ticket)
        result match {
          case TestComplete(_) => result
          case TicketNotFound(_) =>
            logger.good(s"Not found in database...")
            val now = System.currentTimeMillis()
            val promise = Await.result(testExecutor?SubmitTest(config), 1.seconds)
              .asInstanceOf[Promise[TestResult]]
            promise.future.onSuccess ({
              case TestComplete(result) => dbActor ! Post(result)
            })
            cache(ticket) = (now, promise)
            logger.good(s"Ticket ${ticket} in queue...")
            TicketAssigned(ticket)
        }
      case Some(_) =>
        checkTicket(ticket)
    }
  }

  private def findInDatabase(ticket: String) = {
    val dbActor = loadComponent(dbAccess)
    Await.result(dbActor ? Get(ticket), timeout.duration) match {
      case QueryNotFound =>
        logger bad s"Not found in database..."
        TicketNotFound(ticket)
      case QueryOk(config) =>
        logger good s"CACHE ${ticket}"
        cache(ticket) = (System.currentTimeMillis(),
          Promise.successful(TestComplete(config)))
        TestComplete(config)
    }
  }

  private def checkTicket(ticket: String) = {
    cache.get(ticket) match {
      case None => findInDatabase(ticket)
      case Some((since, promise)) =>
        try {
          Await.result(promise.future, 100.millis)
        } catch {
          case e:TimeoutException => TestInQueue(ticket, since)
        }
    }
  }

  override def receive = {
    case Ping => sender ! ACK
    case StopDaemon =>
      logger.bad(s"Stopping daemon...")
      Thread.currentThread().interrupt()
    case SubmitTest(config) => sender ! submitTest(config)
    case CheckTicket(ticket) => sender ! checkTicket(ticket)
    case other => super.receive(other)
  }
}
