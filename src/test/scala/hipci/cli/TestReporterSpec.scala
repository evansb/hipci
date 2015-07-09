package scala.hipci.cli

import akka.actor.{Props, ActorSystem}
import akka.pattern._
import akka.util.Timeout
import org.scalatest.concurrent.ScalaFutures._
import scala.concurrent.duration._
import org.scalatest._
import Matchers._
import scala.hipci.common.Diff3
import scala.hipci.request._

/**
 * Tests the functionality of the test reporter.
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
class TestReporterSpec extends FlatSpec {
  val system = ActorSystem("hipci-test")
  val subject = system.actorOf(Props[TestReporter], "TestReporter")
  implicit val timeout = Timeout(1.seconds)

  "TestReporter" should "generate empty diff2 string if the outcome is same" in {
    val diff3 = Diff3("hello", "hello", "hello", List.empty, List(("hi", Some(1), Some(1), Some(1))))
    whenReady(subject ? ReportDiff2String(diff3)) { _ shouldEqual "" }
  }

  it should "generate empty diff3 string if the outcome is same" in {
    val diff3 = Diff3("hello", "hello", "hello", List.empty, List(("hi", Some(1), Some(1), Some(1))))
    whenReady(subject ? ReportDiff3String(diff3)) { _ shouldEqual "" }
  }
}

