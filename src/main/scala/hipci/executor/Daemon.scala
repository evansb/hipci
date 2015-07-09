package scala.hipci.executor

import akka.remote.RemoteTransportException
import com.typesafe.config.{ConfigFactory, Config}

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.actor.{ActorContext, ActorSystem, Props}
import akka.pattern._
import scala.hipci.cli.Logger
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

  private val DefaultConfig : Config = ConfigFactory.parseString(
  """
    | akka {
    |   actor {
    |     provider = "akka.remote.RemoteActorRefProvider"
    |   }
    |   remote {
    |     netty.tcp {
    |       hostname = "localhost"
    |       port = 2552
    |     }
    |    log-sent-messages = on
    |    log-received-messages = on
    |   }
    | }
  """.stripMargin
  )

  def start() = {
    try {
      val system = ActorSystem("hipcid", DefaultConfig)
      Daemon.register(system)
      system.actorOf(Daemon.props, "Daemon")
    } catch {
      case e:RemoteTransportException => ()
    }
  }

  def getDaemon(context: ActorContext) = {
    val ref = context.actorSelection("akka.tcp://hipcid@localhost:2552/user/Daemon")
    ref
  }

  def stop() = {
    ActorSystem("hipcid").terminate()
  }
}

class Daemon extends Component {
  protected val descriptor: ComponentDescriptor = Daemon
  private val cache: mutable.HashMap[String, (Long, Promise[TestResult])] = mutable.HashMap.empty
  protected val logger = Logger("")

  private def submitTest(config: ConfigSchema)(implicit executionContext: ExecutionContext) = {
    val testExecutor = loadComponent(TestExecutor)
    val ticket = config.hashCode().toString
    logger.good(s"Submitting ${ticket} to daemon")
    cache.get(ticket) match {
      case None =>
        val promise = Await.result(testExecutor ? SubmitTest(config), 1 seconds).asInstanceOf[Promise[TestResult]]
        cache(ticket) = (System.currentTimeMillis, promise)
        TicketAssigned(ticket)
      case Some(_) => checkTicket(ticket)
    }
  }

  private def checkTicket(ticket: String) = {
    cache.get(ticket) match {
      case None => TicketNotFound(ticket)
      case Some((since, promise)) =>
        if (promise.isCompleted) {
          Await.result(promise.future, 1 seconds)
        } else {
          TestInQueue(ticket, since)
        }
    }
  }

  override def receive = {
    case SubmitTest(config) => sender ! submitTest(config)
    case CheckTicket(ticket) => sender ! checkTicket(ticket)
    case other => super.receive(other)
  }
}
