package edu.nus.hipci.cli

import java.nio.file.Paths
import java.security.MessageDigest
import scala.concurrent.Await
import scala.util.{Try, Success, Failure}
import scala.collection.JavaConversions
import scala.collection.immutable.{HashSet, HashMap}
import scala.concurrent.duration._
import akka.actor.Props
import akka.pattern._
import com.typesafe.config.Config
import com.github.kxbmap.configs._

import edu.nus.hipci._
import edu.nus.hipci.core._
import edu.nus.hipci.hg.Hg

/**
 * Creates a TestConfiguration from a Config object.
 *
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
object TestConfigurationFactory extends CLIComponentDescriptor {
  val name = "TestConfigurationFactory"
  val subComponents = List(Hg)
  val props = Props[TestConfigurationFactory]

  def computeConfigSHA(config: Config) : String = {
    val hash = config.hashCode().toString()
    MessageDigest.getInstance("SHA-1")
      .digest(hash.getBytes("UTF-8"))
      .map("%02x".format(_))
      .mkString
  }
}

class TestConfigurationFactory extends CLIComponent {
  import hg.request._
  import hg.response._

  val descriptor = TestConfigurationFactory

  protected def fromConfig(config: Config): Try[TestConfiguration] = {
    import TestConfiguration.Fields._
    try {
      val defaultSchema = TestConfiguration()
      val projectDirectory = config.getOrElse[String](ProjectDirectory, defaultSchema.projectDirectory)
      val hipDirectory = config.getOrElse[String](HipDirectory, defaultSchema.hipDirectory)
      val sleekDirectory = config.getOrElse[String](SleekDirectory, defaultSchema.sleekDirectory)
      val timeout = config.getOrElse[Long](Timeout, defaultSchema.timeout)
      val tests = collectTestsFromConfig(config)
      val testID = computeTestID(projectDirectory, config)
      Success(TestConfiguration(testID, projectDirectory, hipDirectory, sleekDirectory, timeout, tests))
    } catch {
      case e:InvalidHipSpec => Failure(e)
    }
  }

  private def computeTestID(projectDirectory: String, config: Config) : String = {
    val hg = loadComponent(Hg)
    val revision = Await.result(
      hg ? GetCurrentRevision(Paths.get(projectDirectory)),
      timeout.duration)
    revision match {
      case RevisionDirty(rev) =>
        logger.error(DirtyRepository(projectDirectory).getMessage)
        rev + "@" + TestConfigurationFactory.computeConfigSHA(config)
      case RevisionClean(rev) =>
        rev + "@" + TestConfigurationFactory.computeConfigSHA(config)
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

  private def parseSingleSleekSpec(spec: String) =
    spec.toUpperCase.equals(SleekValid)

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
            (p, acc) => acc + ((p._2.toString, parseSingleSleekSpec(p._1)))
          })
          Some(GenTest(filename, SleekTest, arguments, sleekSpec))
        }
    }
  }

  private def collectTestsFromConfig(config: Config) = {
    import TestConfiguration._
    import scala.collection.JavaConverters._
    type JList[T] = java.util.List[T]

    val entries = JavaConversions asScalaSet config.entrySet filterNot((e) => ReservedKeywords.contains(e.getKey))
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

  override def receive = {
    case request.Config(config) => sender ! fromConfig(config)
    case other => super.receive(other)
  }
}
