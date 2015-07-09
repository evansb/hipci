package edu.nus.hipci.daemon

import java.util.concurrent.TimeoutException
import scala.util.{Failure, Success}
import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.actor.{ActorRef, ActorContext, ActorSystem, Props}
import akka.util.Timeout
import akka.pattern._
import com.typesafe.config.ConfigFactory

import edu.nus.hipci.cli.Logger
import edu.nus.hipci.cli.request.Terminate
import edu.nus.hipci.common.{Component, ConfigSchema, ComponentDescriptor}

/**
 * Receives test submission and assign tickets.
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
object Daemon extends ComponentDescriptor {
  import request._
  val name = "Daemon"
  val props = Props[Daemon]
  val subComponents = List(TestExecutor)
  val defaultConfig = ConfigFactory.parseString(
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
  """.stripMargin)

  val defaultClientConfig = ConfigFactory.parseString(
    """
      | akka {
      |  actor {
      |    provider = "akka.remote.RemoteActorRefProvider"
      |  }
      |  remote {
      |    netty.tcp {
      |      hostname = "127.0.0.1"
      |      port = 0
      |    }
      |    log-sent-messages = on
      |    log-received-messages = on
      |  }
      |}
    """.stripMargin)

  def start() : ActorRef = {
    val system = ActorSystem("hipcid", defaultConfig)
    Daemon.register(system)
    system.actorOf(Daemon.props, "Daemon")
  }

  def getDaemon(context: ActorContext) = {
    context.actorSelection("akka.tcp://hipcid@localhost:2552/user/Daemon")
  }

  def stop(context: ActorContext) = {
    implicit val timeout = Timeout(1.seconds)
    try {
      Await.result(getDaemon(context) ? StopDaemon, 1.seconds)
      true
    } catch {
      case e:Throwable => e.isInstanceOf[TimeoutException]
    }
  }
}

class Daemon extends Component {
  import request._
  import response._
  private var system : ActorRef = null
  protected val descriptor = Daemon
  private val cache: mutable.HashMap[String, (Long, Promise[TestResult])] = mutable.HashMap.empty
  protected val logger = Logger("")

  private def submitTest(config: ConfigSchema)(implicit executionContext: ExecutionContext) = {
    val testExecutor = loadComponent(TestExecutor)
    val ticket = config.hashCode().toString
    logger.good(s"Submitting ${ticket} to daemon")
    cache.get(ticket) match {
      case None =>
        val promise = Await.result(testExecutor ? SubmitTest(config), 1.seconds).asInstanceOf[Promise[TestResult]]
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
          Await.result(promise.future, 1.seconds)
        } else {
          TestInQueue(ticket, since)
        }
    }
  }

  override def receive = {
    case Ping => sender ! ACK
    case Introduce(sys) =>
      logger.info(s"Introduced to ${sys}")
      system = sys
    case StopDaemon =>
      logger.info(s"Daemon stopping...")
      if (system != null) system ! Terminate(0)
    case SubmitTest(config) => sender ! submitTest(config)
    case CheckTicket(ticket) => sender ! checkTicket(ticket)
    case other => super.receive(other)
  }
}