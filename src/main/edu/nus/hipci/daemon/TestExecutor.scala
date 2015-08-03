package edu.nus.hipci.daemon

import java.io.{File, ByteArrayOutputStream}
import java.nio.file.{Paths, Path}
import scala.util._
import scala.concurrent.duration._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise, ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.sys.process.Process
import scala.concurrent._
import akka.actor.ActorRef
import akka.pattern._
import edu.nus.hipci.core._
import edu.nus.hipci.util._

/** Descriptor for [[TestExecutor]] */
object TestExecutor extends ComponentDescriptor[TestExecutor] {
  override val subComponents = List(OutputParser)
}

/**
 * Execute [[TestConfiguration]].
 *
 * Does not have access to the database, which is managed by [[Daemon]].
 */
class TestExecutor extends Component {
  type T = GenTest

  protected val descriptor = TestExecutor

  private def runCommand(cmd: Seq[String], workingDir: File) = {
    val out = new ByteArrayOutputStream
    val process = (Process(cmd, workingDir) #> out).run()
    try {
      val future = Future(blocking(process.exitValue()))
      val exitValue = Await.result(future, 100.seconds)
      out.close()
      (exitValue, out.toString, "")
    } catch {
      case e:Throwable =>
        process.destroy()
        (1,"","")
    }
  }

  private def prepareRepository(config: TestConfiguration) = {
    val app = AppConfiguration.global
    val revision = config.testID.split("@")(0)
    val repo = Paths.get(app.projectDirectory).toFile
    runCommand(Seq("hg", "update", revision), repo) match {
      case (0, out, _) =>
        logger good s"Updated to revision $revision..."
        logger good s"Running make..."
        runCommand(Seq("make"), repo) match {
          case (0, _, _) =>
            logger good s"Make OK..."
            None
          case (1, _, cerr) =>
            logger bad s"Compile error:"
            logger bad cerr
            Some(CompilationError(cerr))
        }
      case (_, _, err) => Some(RuntimeError(err))
    }
  }

  private def parseOutput(output:String, base: T): T = {
    val parser = loadComponent(OutputParser)
    if (base.kind.equals("hip")) {
      val future = parser ? ParseHipOutput(output)
      val newSpecs = Await.result(future, 1.seconds) match {
        case ParsedHipOutput(spec) => spec
      }
      base.copy(specs = newSpecs)
    } else {
      val future = parser ? ParseSleekOutput(output)
      val newSpecs = Await.result(future, 1.seconds) match {
        case ParsedSleekOutput(spec) => spec
      }
      base.copy(specs = newSpecs)
    }
  }

  private def executeSingleSuite(baseDir: Path, hipDir: Path,
                         sleekDir: Path, name: String, pool: Set[GenTest],
                         timeout : Duration)
                        (implicit executionContext: ExecutionContext) = {
    var future = Future{ Set.empty[T] }
    val sysPath = baseDir.toAbsolutePath.toString + ":" + sys.env("PATH")
    logger.good(s"Suite $name")
    pool.foreach((t) => {
      val path =
        Paths.get(".")
          .resolve(if (t.kind.equals("hip")) { hipDir } else { sleekDir })
          .resolve(Paths.get(name, t.path))
      val command = baseDir.resolve(t.kind).toAbsolutePath.toString
      val cmd = Seq(command) ++ t.arguments.map(_.toString) ++ Seq(path.toString)
      logger.good(cmd mkString " ")
      future = future map {
        case oldSet =>
          val output = Await.result(Future {
            logger.good(s"Running ${ cmd.tail.mkString(" ") }")
            Process(cmd, Some(baseDir.toFile), "PATH" -> sysPath).!!
          }, timeout)
          logger result output
          val r = parseOutput(output, t)
          oldSet + r
      }
    })
    future
  }

  private def executeConfig(sender: ActorRef, config: TestConfiguration) = {
    val app = AppConfiguration.global
    val timeout = 10000
    val promise = Promise[TestResult]()
    val init = Future {
      prepareRepository(config) match {
        case Some(error) => promise.success(error)
        case None =>
          val initial = Future { Map.empty[String, Set[GenTest]] }
          config.tests.foldLeft(initial)({ (acc, entry) =>
            val name = entry._1
            val pool = entry._2
            acc map {
              case m =>
                val baseDir = Paths.get(app.projectDirectory)
                val hipDir = Paths.get(app.hipDirectory)
                val sleekDir = Paths.get(app.sleekDirectory)
                val result = Await.result(executeSingleSuite(baseDir, hipDir, sleekDir,
                  name, pool, timeout.millis), (timeout * pool.size).millis)
                m + ((name, result))
            }
          }).andThen({
            case Success(pool) =>
              logger.good(s"Test ${config.testID} finished")
              promise.success(TestComplete(config.copy(tests = pool)))
            case Failure(exc) =>
              logger.bad(s"Test ${config.testID} failed")
              promise.failure(exc)
          })
      }
    }
    promise
  }

  override def receive = {
    case SubmitTest(config) => sender ! executeConfig(sender(), config)
    case other => super.receive(other)
  }
}
