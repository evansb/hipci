package edu.nus.hipci.cli

import java.nio.file.Paths
import scala.concurrent.duration.Duration
import scala.util._
import scala.concurrent.Await
import akka.actor.Props
import akka.pattern.ask
import com.typesafe.config.ConfigFactory

import edu.nus.hipci.common.TestConfiguration
import edu.nus.hipci.daemon.Daemon
import edu.nus.hipci.daemon.request._
import edu.nus.hipci.daemon.response._


/**
 * Interpret command object and dispatch instructions to other components
 *
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
object CommandDispatcher extends CLIComponentDescriptor {
  val name = "CommandDispatcher"
  val subComponents = List(TestConfigurationFactory, CommandParser, Daemon, TestReporter)
  val props = Props[CommandDispatcher]
}

class CommandDispatcher extends CLIComponent {
  import request._
  val descriptor = CommandDispatcher
  def props() = Props[CommandDispatcher]

  sealed trait Dispatcher[-T] {
    def dispatch(command: T): request.Request
  }

  implicit object RunCommandDispatcher extends Dispatcher[RunCommand] {
    override def dispatch(command: RunCommand) = {
      val configFile = command.config
      val arguments = command.arguments
      val testConfigurationFactory = loadComponent(TestConfigurationFactory)
      val f = Paths.get(configFile).toFile
      logger.good(s"Using $configFile with ${
        if (arguments.isEmpty) {
          "no arguments"
        } else {
          arguments.foldRight("")((x, acc) => x + " " + acc)
        }
      }")
      if (f == null) {
        throw FileNotFound(configFile)
      } else {
        val future = testConfigurationFactory ? request.Config(ConfigFactory.parseFile(f))
        val testConfiguration = Await.result(future, timeout.duration)
          .asInstanceOf[Try[TestConfiguration]] match {
          case Success(schema) => schema
          case Failure(e) => throw e
        }
        lazy val numberOfTest = testConfiguration.tests.size
        lazy val totalNumberOfFiles = testConfiguration.tests.foldLeft(0)({ (acc, pair) => acc + pair._2.size })
        logger.good(s"Found $numberOfTest suites ($totalNumberOfFiles files)")
        val daemon = Daemon.getDaemon(context)
        val result = Await.result(daemon ? SubmitTest(testConfiguration), timeout.duration).asInstanceOf[TestResult]
        result match {
          case TestComplete(config) =>
            logger.good(s"Test ${config.testID} has been completed.")
            val reporter = loadComponent(TestReporter)
            val report = Await.result(reporter ? ReportSingleString(config), timeout.duration).asInstanceOf[String]
            Console.out.println(report)
          case TestInQueue(ticket, since) =>
            val delta = Duration.fromNanos(System.currentTimeMillis - since)
            logger.good(s"Ticket ${ticket} in queue since ${delta} ago")
          case TicketAssigned(ticket) =>
            logger.good(s"Ticket ${ticket} assigned. Run hipci status to check")
          case other =>
            logger.bad(s"Exception occurred ${other.toString}")
        }
      }
      Terminate(0)
    }
  }

  implicit object DiffCommandDispatcher extends Dispatcher[DiffCommand] {
    override def dispatch(command: DiffCommand): Request = {
      Terminate(0)
    }
  }

  implicit object StartCommandDispatcher extends Dispatcher[StartCommand] {
    override def dispatch(command: StartCommand): Request = {
      Daemon.start()
      Terminate(0)
    }
  }

  implicit object StopCommandDispatcher extends Dispatcher[StopCommand] {
    override def dispatch(command: StopCommand): Request = {
      Daemon.stop(context)
      Terminate(0)
    }
  }

  implicit object HelpCommandDispatcher extends Dispatcher[HelpCommand] {
    override def dispatch(command: HelpCommand): Request = {
      loadComponent(CommandParser) ! ShowUsage
      Terminate(0)
    }
  }

  implicit object EmptyCommandDispatcher extends Dispatcher[EmptyCommand] {
    override def dispatch(command: EmptyCommand): Request = {
      loadComponent(CommandParser) ! ShowUsage
      Terminate(2)
    }
  }

  implicit object AllDispatcher extends Dispatcher[Command] {
    override def dispatch(command: Command) = command match {
      case p@(RunCommand(_,_)) => dispatch(p)
      case p@(DiffCommand(_,_)) => dispatch(p)
      case p@(HelpCommand()) => dispatch(p)
      case p@(StartCommand()) => dispatch(p)
      case p@(StopCommand()) => dispatch(p)
      case p@(EmptyCommand()) => dispatch(p)
    }
  }

  override def receive = {
    case response.ParsedArguments(cmd) => sender ! AllDispatcher.dispatch(cmd)
    case other => super.receive(other)
  }
}

