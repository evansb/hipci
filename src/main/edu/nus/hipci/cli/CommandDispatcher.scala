package edu.nus.hipci.cli

import java.nio.file.Paths
import scala.concurrent.duration._
import scala.util._
import scala.concurrent.Await
import akka.pattern.ask
import akka.actor._
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import edu.nus.hipci.core._
import edu.nus.hipci.daemon._
import edu.nus.hipci.daemon.{Main => DaemonMain}
import edu.nus.hipci.hg._

/** Base type of requests accepted by this component */
sealed trait CommandDispatcherRequest

/** Base type of responses sent by this component */
sealed trait CommandDispatcherResponse

/** Singleton descriptor for [[CommandDispatcher]] */
object CommandDispatcher extends ComponentDescriptor[CommandDispatcher] {
  /** Sub-components of the descriptor */
  override val subComponents = List(ConfigurationFactory, CommandParser,
    TestReporter, Hg)
}

/**
 * Dispatch [[Command]] objects to other components.
 *
 * This component bridges Main and Daemon process.
 */
class CommandDispatcher extends CLIComponent {

  /** Descriptor of this component */
  val descriptor = CommandDispatcher

  lazy private val testConfigurationFactory =
    loadComponent(ConfigurationFactory)
  lazy private val reporter = loadComponent(TestReporter)
  lazy private val hg = loadComponent(Hg)
  lazy private val commandParser = loadComponent(CommandParser)

  private def getTestConfiguration(path: String) = {
    val configFile = Paths.get(path).toFile
    if (configFile == null) throw FileNotFound(path)
    val config = ConfigFactory parseFile configFile
    val future = testConfigurationFactory ? CreateTestConfiguration(config)
    Await result(future, timeout.duration) match {
      case Success(testConfiguration) =>
        testConfiguration.asInstanceOf[TestConfiguration]
      case Failure(e) => throw e
    }
  }

  /** Dispatch [[InitCommand]] instances */
  private object InitCommandDispatcher {
    def dispatch() = {
      implicit val timeout = Timeout(10000.seconds)
      Await.result(testConfigurationFactory ? CreateAppConfigurationInteractively, timeout.duration)
      Terminate(0)
    }
  }

  /** Dispatch [[RunCommand]] instances */
  private object RunCommandDispatcher extends Dispatcher[RunCommand] {
    override def dispatch(command: RunCommand) = {
      // Create the TestConfiguration from configuration file.
      val testConfiguration = getTestConfiguration(command.config)

      // Log number of suites and total number of files.
      lazy val numberOfTest = testConfiguration.tests.size
      lazy val totalNumberOfFiles = testConfiguration.tests.map(_._2.size).sum
      logger good s"Found $numberOfTest suites in ($totalNumberOfFiles files)."

      // Submit the test to Daemon.
      Daemon.get(context).map({ (daemon) =>
        logger good s"Submitting test to Daemon."
        Await.result(daemon ? SubmitTest(testConfiguration), timeout.duration)
      }).getOrElse(throw new DaemonNotFound) match {
        case TestComplete(config) =>
          logger good s"Test ${config.testID} has been completed."
          val reference = testConfiguration.copy(testID = "expected")
          val results = List(reference, config)
          val future = reporter ? ReportTestResult(results, diffOnly = false)
          val report = Await.result(future, timeout.duration)
          Console.out.println(report)
          Terminate(0)
        case TestInQueue(ticket, since) =>
          logger good s"Ticket $ticket in queue."
          Terminate(0)
        case TicketAssigned(ticket) =>
          logger good s"Ticket $ticket assigned. Run hipci status to check"
          Terminate(0)
        case other =>
          logger bad s"Don't know how to handle $other"
          Terminate(1)
      }
    }
  }

  /** Dispatch [[DiffCommand]] instances */
  private object DiffCommandDispatcher extends Dispatcher[DiffCommand] {
    override def dispatch(command: DiffCommand) = {
      // Compute the list of test configuration keys.
      val revisions = command.revisions
      val configFile = Paths.get(command.config).toFile
      if (configFile == null) throw FileNotFound(command.config)
      val config = ConfigFactory.parseFile(configFile)
      val configHash = ConfigurationFactory.computeConfigSHA(config)

      // From the repository, get the list of absolute revision hashes
      val key = AppConfiguration.Fields.ProjectDirectory
      val repoDir = Paths.get(config.getString(key))
      if (repoDir.toFile == null) throw FileNotFound(repoDir.toString)
      val future1 = hg ? GetRevisionHash(repoDir, revisions)
      val revisionHashes = Await.result(future1, timeout.duration)
        .asInstanceOf[List[String]]

      // Fetch the test results from the Daemon.
      val testResults = Daemon.get(context).map({ (daemon) =>
        revisionHashes.map({ (hash) =>
          val testID = s"$hash@$configHash"
          Await.result(daemon ? CheckTicket(testID), timeout.duration)
        })
      }).getOrElse(throw new DaemonNotFound)
        .asInstanceOf[List[TestResult]]

      // Split into completed and incomplete tests
      val init = (List.empty[TestConfiguration], List.empty[String])
      val (completedTests, incompleteTests) =
        testResults.foldRight(init)({ (r, acc) => r match {
            case TestComplete(result) => acc.copy(_1 = result :: acc._1)
            case TicketNotFound(ticket) => acc.copy(_2 = ticket :: acc._2)
            case TestInQueue(ticket, _) => acc.copy(_2 = ticket :: acc._2)
            case _ => acc
          }
        })

      // Warn the user for incomplete tests
      if (incompleteTests.isEmpty) {
        logger bad "These following revisions has not done the test:\n"
        incompleteTests.foreach { (ticket) =>
          logger bad s"${ ticket.split("@")(0)}\n"
        }
      }

      // Get the test report and print it
      val future2 = reporter ? ReportTestResult(completedTests, diffOnly = true)
      val report = Await.result(future2, timeout.duration)
      Console.out.println(report)
      Terminate(0)
    }
  }

  /** Dispatch [[StartCommand]] instances */
  private object StartCommandDispatcher {
    def dispatch() = {
      Daemon.get(context) map { (d) =>
        logger good "Daemon is already running"
      } getOrElse {
        val daemonThread = new Thread(new DaemonMain(DaemonMain.defaultDatabase))
        daemonThread.start()
      }
      KeepAlive
    }
  }

  /** Dispatch [[StopCommand]] instances */
  private object StopCommandDispatcher {
    def dispatch() = {
      Daemon.get(context) map { (d) =>
        logger good "Terminating daemon"
        d ! StopDaemon
      } getOrElse (logger bad "Daemon is not running")
      Terminate(0)
    }
  }

  /** Dispatch [[HelpCommand]] instances */
  private object HelpCommandDispatcher {
    def dispatch() = {
      val usage = Await.result(commandParser ? ShowUsage,
        timeout.duration).asInstanceOf[String]
      Console.out.println(usage)
      Terminate(0)
    }
  }

  /** Dispatch [[EmptyCommand]] instances */
  private object EmptyCommandDispatcher {
    def dispatch() = {
      commandParser ! ShowUsage
      Terminate(2)
    }
  }

  private object AllDispatcher extends Dispatcher[Command] {
    override def dispatch(command: Command) = command match {
      case InitCommand => InitCommandDispatcher.dispatch()
      case p@(RunCommand(_, _)) => RunCommandDispatcher.dispatch(p)
      case p@(DiffCommand(_, _)) => DiffCommandDispatcher.dispatch(p)
      case HelpCommand => HelpCommandDispatcher.dispatch()
      case StartCommand => StartCommandDispatcher.dispatch()
      case StopCommand => StopCommandDispatcher.dispatch()
      case EmptyCommand => EmptyCommandDispatcher.dispatch()
    }
  }

  override def receive = {
    case cmd:Command => sender ! AllDispatcher.dispatch(cmd)
    case other => super.receive(other)
  }
}

/** Exception thrown when the Daemon is not found */
case class DaemonNotFound() extends CLIException(
  """
    | Could not find a running daemon process.
    | Did you run hipci start on the server?
  """.stripMargin)

/** Exception thrown when a file is not found */
case class FileNotFound(filename: String) extends CLIException(
  s"""
     | Unable to locate configuration file:
     |   $filename
  """.stripMargin)

/** Exception thrown when a repository can not be found */
case class InvalidRepository(path: String) extends CLIException(
  s"""
     | The following path does not exist or is not a mercurial repository:
     |   $path
  """.stripMargin)
