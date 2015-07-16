package edu.nus.hipci.util

import scala.collection.immutable.Map
import scala.concurrent.duration._
import akka.actor._
import akka.pattern._
import akka.util.Timeout
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._

import edu.nus.hipci.core._

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
      GenTest(
        path = "test.ss@a",
        kind = "hip",
        arguments = Set("-arg", "--arg2"),
        specs = Map( "foo" -> true, "bar" -> false)
      )
    val two =
      GenTest(
        path = "test.ss@b",
        kind = "hip",
        arguments = Set("-arg"),
        specs = Map( "foo" -> true)
      )
    val three =
      GenTest(
        path = "test.ss@c",
        kind = "hip",
        arguments = Set("-arg"),
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
