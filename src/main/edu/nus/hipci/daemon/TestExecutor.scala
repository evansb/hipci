package edu.nus.hipci.daemon

import java.nio.file.{Paths, Path}
import scala.util._
import scala.concurrent.duration._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise, ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.sys.process.Process
import akka.actor.{ActorRef, Props}
import akka.pattern._

import edu.nus.hipci.core._
import edu.nus.hipci.util
import edu.nus.hipci.util._

/**
 * An interface for generic test executor
 *
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
object TestExecutor extends ComponentDescriptor {
  val name = "TestExecutor"
  val props = Props[TestExecutor]
  val subComponents = List(OutputAnalyzer, OutputParser)
}

class TestExecutor extends Component {
  import util.request._
  import util.response._
  import request._
  import response._

  type T = GenTest

  protected val descriptor = TestExecutor

  /**
   * Command name to execute.
   */
  private def getCommand(test: T): String = { test.kind }

  private def parseOutput(output:String, base: T): T = {
    val parser = loadComponent(OutputParser)
    if (base.kind.equals("hip")) {
      val newSpecs = Await.result(parser ? ParseHipOutput(output), 1.seconds) match {
        case ParsedHipOutput(spec) => spec
      }
      base.copy(specs = newSpecs)
    } else {
      val newSpecs = Await.result(parser ? ParseSleekOutput(output), 1.seconds) match {
        case ParsedSleekOutput(spec) => spec
      }
      base.copy(specs = newSpecs)
    }
  }

  /**
   * Execute a suite locally
   * @param name The name of the suite
   * @param pool The test pool
   * @return A promise that returns a test pool when resolved
   */
  def executeSingleSuite(baseDir: Path, hipDir: Path,
                         sleekDir: Path, name: String, pool: Set[GenTest],
                         timeout : Duration)
                        (implicit executionContext: ExecutionContext) = {
    var future = Future{ Set.empty[T] }
    val sysPath = baseDir.toAbsolutePath.toString + ":" + sys.env("PATH")
    logger.good(s"Suite ${name}")
    pool.foreach((t) => {
      val path = (if (t.kind.equals("hip")) { hipDir } else { sleekDir }).resolve(Paths.get(name, t.path))
      val command = baseDir.resolve(getCommand(t)).toAbsolutePath.toString
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

  /**
   * Execute a config schema locally
   * @param config The config schema
   * @return Output of the execution
   */
  private def executeConfig(sender: ActorRef, config: TestConfiguration)
  : Promise[TestResult] = {
    val promise = Promise[TestResult]
    val init = Future { Map.empty[String, Set[GenTest]] }
    config.tests.foldLeft(init)({ (acc, entry) =>
      val name = entry._1
      val pool = entry._2
      acc map {
        case m =>
          val baseDir = Paths.get(config.projectDirectory)
          val hipDir = Paths.get(config.hipDirectory)
          val sleekDir = Paths.get(config.sleekDirectory)
          val result = Await.result(executeSingleSuite(baseDir, hipDir, sleekDir,
            name, pool, config.timeout millis), (config.timeout * pool.size) millis)
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
    promise
  }

  override def receive = {
    case SubmitTest(config) => sender ! executeConfig(sender(), config)
    case other => super.receive(other)
  }
}
