package edu.nus.hipci.cli

import java.nio.file.Paths
import java.security.MessageDigest
import scala.concurrent.Await
import scala.util.{Try, Success, Failure}
import scala.collection.JavaConversions
import scala.collection.immutable.{HashSet, HashMap}
import scala.io._
import akka.pattern._
import pl.project13.scala.rainbow._
import com.typesafe.config.{ConfigRenderOptions, Config}
import com.github.kxbmap.configs._
import edu.nus.hipci.core._
import edu.nus.hipci.hg._

sealed trait ConfigurationFactoryRequest

/**
 * Request to create a test configuration from a Config object
 *
 * @constructor Create a request from a config object
 * @param config The config object
 */
case class CreateTestConfiguration(config: Config)
  extends ConfigurationFactoryRequest

/**
 * Request to load a global application configuration from a Config object
 *
 * @constructor Create a request from a config object
 * @param config The config object
 */
case class LoadAppConfiguration(config: Config)
  extends ConfigurationFactoryRequest

/** Create AppConfiguration interactively by asking the user some questions. */
case object CreateAppConfigurationInteractively
  extends ConfigurationFactoryRequest

/** Singleton descriptor for [[ConfigurationFactory]] */
object ConfigurationFactory
  extends ComponentDescriptor[ConfigurationFactory] {
  override val subComponents = List(Hg)

  /**
   * Compute the SHA of a config object.
   *
   * @param config the config object.
   * @return SHA digest of the config object.
   */
  def computeConfigSHA(config: Config) : String = {
    val hash = config.hashCode().toString
    MessageDigest.getInstance("SHA-1")
      .digest(hash.getBytes("UTF-8"))
      .map("%02x".format(_))
      .mkString
  }
}

/** Creates a TestConfiguration from a Config object. */
class ConfigurationFactory extends CLIComponent {
  val descriptor = ConfigurationFactory

  protected def fromConfig(config: Config): Try[TestConfiguration] = {
    try {
      val tests = collectTestsFromConfig(config)
      val testID = computeTestID("foo", config)
      Success(TestConfiguration(testID, tests))
    } catch {
      case e:InvalidHipSpec => Failure(e)
    }
  }

  private def computeTestID(projectDirectory: String, config: Config) : String = {
    val hg = loadComponent(Hg)
    val revision = Await.result(hg ? GetCurrentRevision(Paths.get(projectDirectory)),
      timeout.duration)
    revision match {
      case RevisionDirty(rev) =>
        logger.error(DirtyRepository(projectDirectory).getMessage)
        rev + "@" + ConfigurationFactory.computeConfigSHA(config)
      case RevisionClean(rev) =>
        rev + "@" + ConfigurationFactory.computeConfigSHA(config)
      case MercurialError(_) => ""
    }
  }

  private def parseSingleHipSpec(spec: String) = {
    val token = spec.split('.')
    if (token.length != 2) {
      throw InvalidHipSpec(spec)
    } else {
      (token(0), token(1).toUpperCase.equals(HipSuccess))
    }
  }

  private def parseSingleSleekSpec(entailment: Int, spec: String) =
    if (!spec.contains(".")) {
      Map(entailment.toString -> spec.toUpperCase.equals(SleekValid))
    } else {
      val token = spec.split('.')
      if (token.length != 2) {
        throw InvalidHipSpec(spec)
      } else {
        val expectInfer = (1 to token(1).toInt)
          .foldRight(Map.empty[String, Boolean])({ (p, acc) =>
            acc + (s"$entailment.$p" -> true)
          })
        expectInfer + (entailment.toString -> token(0).equals(SleekValid))
      }
    }

  private def toGenTest(data: List[String]) = {
    data match {
      case List() => None
      case filename :: rest =>
        val (arguments, specs) = rest span(_.startsWith("-"))
        if (filename.endsWith(HipExtension)) {
          val hipSpec = specs.foldRight[Map[String, Boolean]](HashMap.empty)({
            (en, acc) => acc + parseSingleHipSpec(en)
          })
          Some(GenTest(filename, HipTest, arguments, hipSpec))
        } else {
          val indexedSpecs = specs.zipWithIndex.map((p) => p.copy(_2 = p._2 + 1))
          val sleekSpec = indexedSpecs.foldRight[Map[String, Boolean]](Map.empty)({
            (p, acc) => acc ++ parseSingleSleekSpec(p._2, p._1.toString)
          })
          Some(GenTest(filename, SleekTest, arguments, sleekSpec))
        }
    }
  }

  private def collectTestsFromConfig(config: Config) = {
    import scala.collection.JavaConverters._
    type JList[T] = java.util.List[T]

    val entries = JavaConversions asScalaSet config.entrySet
    entries.foldRight[Map[String, Set[GenTest]]](HashMap.empty)({
      (en, acc) =>
        val rawTestEntry = config.getValue(en.getKey).unwrapped().asInstanceOf[JList[JList[String]]]
          .asScala.toList.map(_.asScala.toList)
        val testPool = rawTestEntry.foldRight[HashSet[GenTest]](HashSet.empty)({
          (en, acc) => toGenTest(en).fold(acc)({ (t) => acc + t })
        })
        acc + ((en.getKey, testPool))
    })
  }

  private def loadAppConfig(config: Config) = {
    import AppConfiguration.Fields._
    val app = AppConfiguration.global
    app.projectDirectory = config.getOrElse[String](ProjectDirectory, app.projectDirectory)
    app.hipDirectory = config.getOrElse[String](HipDirectory, app.hipDirectory)
    app.sleekDirectory = config.getOrElse[String](SleekDirectory, app.sleekDirectory)
    app.daemonHost = config.getOrElse[String](DaemonHost, app.daemonHost)
    app.daemonPort = config.getOrElse[String](DaemonPort, app.daemonPort)
  }

  private def createAppConfigurationInteractively() = {
    type Question = (String, (String, AppConfiguration) => AppConfiguration)
    val runQuestion = { (previous: AppConfiguration, question: Question) =>
      Console.out.println(question._1.cyan)
      val line = Console.in.readLine()
      question._2(line, previous)
    }

    val runQuestions = { (questions: Seq[Question]) =>
      questions.foldLeft(AppConfiguration())(runQuestion)
    }

    val questions = Seq(
      ("Where is the HIP/SLEEK project directory located?. You can input relative path from this directory or " +
        " an absolute path",
        (input: String, appConfig:AppConfiguration) =>
          appConfig.copy(projectDirectory = input)),
      (s"What is the HIP test directory? default: [${AppConfiguration.global.hipDirectory }]",
        (input: String, appConfig:AppConfiguration) => appConfig.copy(hipDirectory = input)),

      (s"What is the SLEEK test directory? default: [${AppConfiguration.global.sleekDirectory }]",
        (input: String, appConfig:AppConfiguration) => appConfig.copy(sleekDirectory = input)),

      (s"What is the Daemon host addresss? default: [${AppConfiguration.global.daemonHost }]",
        (input: String, appConfig:AppConfiguration) => appConfig.copy(daemonHost = input)),

      (s"What is the Daemon port number? default: [${AppConfiguration.global.daemonPort }]",
        (input: String, appConfig:AppConfiguration) => appConfig.copy(daemonPort = input))
    )

    val config = AppConfiguration.toConfig(runQuestions(questions))

    import java.io._
    val renderOpts = ConfigRenderOptions.defaults().setOriginComments(false).setComments(false).setJson(false);
    val pw = new PrintWriter(Paths.get(HipciConf).toFile())
    pw.write(config.root().render(renderOpts))
    pw.close()
  }

  override def receive = {
    case CreateTestConfiguration(config) => sender ! fromConfig(config)
    case LoadAppConfiguration(config) => sender ! loadAppConfig(config)
    case CreateAppConfigurationInteractively => sender ! createAppConfigurationInteractively()
    case other => super.receive(other)
  }
}
