package scala.hipci.cli

import java.nio.file.Paths
import java.util.concurrent.Executors

import scala.concurrent.duration.Duration
import scala.hipci.executor.Daemon
import scala.hipci.request.SubmitTest
import scala.hipci.response.{TicketAssigned, TestInQueue, TestComplete, TestResult}
import scala.util.{Try, Failure, Success}
import scala.concurrent.{ExecutionContext, Future, Await}
import akka.actor.Props
import akka.pattern.ask
import com.typesafe.config.ConfigFactory
import scala.hipci.common.ConfigSchema
import scala.hipci.request

/**
 * Interpret command object and dispatch instructions to other components
 *
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
object CommandDispatcher extends CLIComponentDescriptor {
  val name = "CommandDispatcher"
  val subComponents = List(ConfigSchemaFactory, CommandParser, Daemon)
  val props = Props[CommandDispatcher]
}

class CommandDispatcher extends CLIComponent {
  val descriptor = CommandDispatcher
  def props() = Props[CommandDispatcher]

  protected def dispatch(command: Command) : Option[request.Terminate] = {
    val configSchemaFactory = loadComponent(ConfigSchemaFactory)
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
          val future = configSchemaFactory ? request.Config(ConfigFactory.parseFile(f))
          val configSchema = Await.result(future, timeout.duration)
            .asInstanceOf[Try[ConfigSchema]] match {
              case Success(schema) => schema
              case Failure(e) => throw e
            }
          lazy val numberOfTest = configSchema.tests.size
          lazy val totalNumberOfFiles = configSchema.tests.foldLeft(0)({(acc, pair) => acc + pair._2.size })
          logger.good(s"Found $numberOfTest suites ($totalNumberOfFiles files)")
          val daemon = Daemon.getDaemon(context)
          val result = Await.result(daemon ? SubmitTest(configSchema), timeout.duration).asInstanceOf[TestResult]
          result match {
            case TestComplete(completionTime, config) =>
              logger.good(s"Test completed")
            case TestInQueue(ticket, since) =>
              val delta = Duration.fromNanos(System.currentTimeMillis - since)
              logger.good(s"Ticket ${ticket} in queue since ${ delta } ago")
            case TicketAssigned(ticket) =>
              logger.good(s"Ticket ${ticket} assigned. Run hipci status to check")
          }
        }
        Some(request.Terminate(0))
      case HelpCommand =>
        Console.out.println(Await.result(commandParser ? request.ShowUsage, timeout.duration).asInstanceOf[String])
        Some(request.Terminate(0))
      case DiffCommand((revision1, revision2)) =>
        logger.good(s"Comparing $revision1 and $revision2")
        Some(request.Terminate(0))
      case StartCommand =>
        Daemon.start()
        None
      case StopCommand =>
        Daemon.stop()
        logger.bad(s"hipcid stopped successfuly")
        Some(request.Terminate(0))
      case EmptyCommand =>
        Some(request.Terminate(1))
    }
  }

  override def receive = {
    case Some(cmd) => sender ! dispatch(cmd.asInstanceOf[Command])
    case None => sender ! request.Terminate(1)
    case other => super.receive(other)
  }
}

