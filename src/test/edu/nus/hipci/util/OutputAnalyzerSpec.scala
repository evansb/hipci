package edu.nus.hipci.util

import scala.collection.immutable.Map
import scala.concurrent.duration._
import akka.actor._
import akka.pattern._
import akka.util.Timeout
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._

import edu.nus.hipci.common._

/**
 * Test the functionality of OutputAnalyzer
 *
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
class OutputAnalyzerSpec extends FlatSpec {
  import request._
  val system = ActorSystem("hipci-test")
  val subject = system.actorOf(Props[OutputAnalyzer], "OutputAnalyzer")
  implicit val timeout = Timeout(1.seconds)

  "Output Analyzer" should "3 way analyze HIP test" in {
    val one =
      HipTest(
        path = "test.ss@a",
        arguments = List("-arg", "--arg2"),
        specs = Map( "foo" -> true, "bar" -> false)
      )
    val two =
      HipTest(
        path = "test.ss@b",
        arguments = List("-arg"),
        specs = Map( "foo" -> true)
      )
    val three =
      HipTest(
        path = "test.ss@c",
        arguments = List("-arg"),
        specs = Map( "foo" -> false, "bar" -> false)
      )
    whenReady(subject ? AnalyzeOutput(one, two, three)) {
      _ shouldEqual Diff3("test.ss@a", "test.ss@b", "test.ss@c",
        List(("-arg", true, true, true), ("--arg2", true, false, false)),
        List(("foo", Some(true), Some(true), Some(false)),
             ("bar", Some(false), None, Some(false))))
    }
  }
}
