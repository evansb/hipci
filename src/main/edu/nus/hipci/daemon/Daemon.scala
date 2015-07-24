package edu.nus.hipci.daemon

import java.util.concurrent.TimeoutException
import scala.collection.mutable
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.actor._
import akka.pattern._
import akka.util.Timeout
import sorm.Instance
import edu.nus.hipci.core._

/** Request handled by [[Daemon]] class */
sealed trait DaemonRequest

/** Request to terminate the Daemon */
case object StopDaemon extends DaemonRequest

/** Check if a Daemon is alive */
case object Ping extends DaemonRequest

/** Submit a test configuration to Daemon */
case class SubmitTest(config: TestConfiguration) extends DaemonRequest

/** Check the outcome or status of a ticket */
case class CheckTicket(ticket: String) extends DaemonRequest

/** Response emitted by this component */
sealed trait DaemonResponse

/** Test result */
sealed trait TestResult

/** @constructor Create a response to [[Ping]] */
case object ACK

/**
 * @constructor Response for pending test
 *
 * @param ticket The test ticket
 * @param since The time since the ticket is in queue
 */
case class TestInQueue(ticket: String, since: Long)
  extends TestResult with DaemonResponse

/**
 * Response for pending ticket
 *
 * @param ticket The test ticket
 */
case class TicketAssigned(ticket: String) extends TestResult with DaemonResponse

/**
 * Response for unknown request ticket
 *
 * @param ticket The test ticket
 */
case class TicketNotFound(ticket: String) extends TestResult with DaemonResponse

/**
 * @constructor Create a response for completed test
 *
 * @param result The test result
 */
case class TestComplete(result: TestConfiguration)
  extends TestResult with DaemonResponse

/**
 * @constructor Create a response for compilation error
 *
 * @param error The error explanation.
 */
case class CompilationError(error: String) extends TestResult with DaemonResponse

/**
 * @constructor Create a response for runtime error
 *
 * @param error The error explanation
 */
case class RuntimeError(error: String) extends TestResult with DaemonResponse

/**
 * A factory for Daemon descriptor and getter for Daemon actors.
 */
object Daemon {

  /**
   * Returns a pair of [[Daemon]], [[DbAccess]] component descriptor.
   *
   * @param db The database instance used by this Daemon.
   */
  def withDbAccess(db: Instance) = {
    val dbAccess = DbAccess(db)
    (new ComponentDescriptor {
      override val props = Props.create(classOf[Daemon], this, dbAccess)
      override val subComponents = List(dbAccess, TestExecutor)
    }, dbAccess)
  }

  /**
   * Find and return a running Daemon instance
   *
   * @param context The context to look for.
   * @return A daemon actor reference if exist.
   */
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

/**
 * Runs in background to serve request from the client.
 */
class Daemon(desc: ComponentDescriptor[_ <: Component],
             dbAccess: ComponentDescriptor[_ <: Component]) extends Component {
  protected val descriptor = desc
  private val cache = mutable.HashMap.empty[String, (Long, Promise[TestResult])]

  private def submitTest(config: TestConfiguration)
                        (implicit e: ExecutionContext) = {
    val testExecutor = loadComponent(TestExecutor)
    val dbActor = loadComponent(dbAccess)
    val ticket = config.testID
    logger.good(s"Received ticket $ticket")
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
              case TestComplete(r) => dbActor ! Post(r)
            })
            cache(ticket) = (now, promise)
            logger.good(s"Ticket $ticket in queue...")
            TicketAssigned(ticket)
          case _ => TicketNotFound(ticket)
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
        logger good s"CACHE $ticket"
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
