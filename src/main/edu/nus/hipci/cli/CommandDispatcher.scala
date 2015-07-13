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

  protected def dispatch(command: Command) : request.Request = {
    val testConfigurationFactory = loadComponent(TestConfigurationFactory)
    val commandParser = loadComponent(CommandParser)
    command match {
      case RunCommand(configFile, arguments) =>
        val f = Paths.get(configFile).toFile
        logger.good(s"Using $configFile with ${
          if (arguments.isEmpty) {
            "no arguments"
          } else {
            arguments.foldRight("")((x,acc) => x + " " + acc)
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
          lazy val totalNumberOfFiles = testConfiguration.tests.foldLeft(0)({(acc, pair) => acc + pair._2.size })
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
              logger.good(s"Ticket ${ticket} in queue since ${ delta } ago")
            case TicketAssigned(ticket) =>
              logger.good(s"Ticket ${ticket} assigned. Run hipci status to check")
            case other => super.receive(other)
          }
        }
        Terminate(0)
      case HelpCommand =>
        Console.out.println(Await.result(commandParser ? request.ShowUsage, timeout.duration).asInstanceOf[String])
        Terminate(0)
      case DiffCommand((revision1, revision2)) =>
        logger.good(s"Comparing $revision1 and $revision2")
        Terminate(0)
      case StartCommand =>
        KeepAlive(Daemon.start())
      case StopCommand =>
        Daemon.stop(context)
        Terminate(0)
      case EmptyCommand =>
        Terminate(1)
    }
  }

  override def receive = {
    case response.ParsedArguments(cmd) => sender ! dispatch(cmd)
    case other => super.receive(other)
  }
}

