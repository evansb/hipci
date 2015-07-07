package scala.hipci.cli

import java.nio.file.Paths
import scala.util.{Try, Failure, Success}
import scala.concurrent.Await
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
  val subComponents = List(ConfigSchemaFactory, CommandParser)
  val props = Props[CommandDispatcher]
}

class CommandDispatcher extends CLIComponent {
  val descriptor = CommandDispatcher
  def props() = Props[CommandDispatcher]

  protected def dispatch(command: Command) = {
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

        }
      case HelpCommand =>
        commandParser ? request.Call("showUsage")
      case DiffCommand((revision1, revision2)) =>
        logger.good(s"Comparing $revision1 and $revision2")
      case EmptyCommand =>
    }
  }

  override def receive = {
    case Some(cmd) => sender ! dispatch(cmd.asInstanceOf[Command])
    case None => sender ! request.Terminate(1)
    case other => super.receive(other)
  }
}

