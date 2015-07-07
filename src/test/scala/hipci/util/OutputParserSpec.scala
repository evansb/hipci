package scala.hipci.util

import java.nio.file.Paths

import akka.actor.{ActorSystem, Props}
import akka.pattern._
import akka.util.Timeout
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._

import scala.collection.immutable.Map
import scala.concurrent.duration._
import scala.hipci.request._
import scala.hipci.response._

/**
 * Test the functionality of the output parsers.
 *
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
class OutputParserSpec extends FlatSpec {
  val system = ActorSystem("hipci-test")
  val subject = system.actorOf(Props[OutputParser], "OutputParser")
  implicit val timeout = Timeout(1.seconds)

  "OutputParser" should "parse hip output correctly" in {
    val output = scala.io.Source.fromFile(Paths.get("fixtures","ll-output.txt").toFile()).mkString
    whenReady(subject ? ParseHipOutput(output)) {
      _ shouldEqual ParsedHipOutput(Map("append" -> true, "append2" -> true, "create_list" -> false))
    }
  }

  it should "parse sleek output correctly" in {
    val output = scala.io.Source.fromFile(Paths.get("fixtures","sleek8-output.txt").toFile()).mkString
    whenReady(subject ? ParseSleekOutput(output)) {
      _ shouldEqual ParsedSleekOutput(Map(1 -> true, 9-> true, 10 -> false, 15 -> false, 17 -> false))
    }
  }

  it should "extract hip output given a set of procedure names" in {
    val output = scala.io.Source.fromFile(Paths.get("fixtures","ll-output.txt").toFile()).mkString
    whenReady(subject ? FilterHipOutput(output, Set("append", "create_list"))) {
      _ shouldEqual FilteredHipOutput(
        Map(
          "append" ->
            """
              |Procedure append$node~node SUCCESS.
              |Some output here
              |More output here
              |  Even with spaces
            """.stripMargin.trim,
          "create_list" ->
            "Procedure create_list$int FAIL.".trim
        ))
    }
  }

  it should "extract sleek output given a set of entailments" in {
    val output = scala.io.Source.fromFile(Paths.get("fixtures","sleek8-output.txt").toFile()).mkString
    whenReady(subject ? FilterSleekOutput(output, Set(1, 10, 17))) {
      _ shouldEqual FilteredSleekOutput(
        Map(
          1 -> "Entail 1: Valid.",
          10 ->
            ("Entail 10: Fail.(may) cause:UnionR[ 9<n |-  11<=n. LOCS:[63;1] (may-bug), 9<n |" +
            "-  exists(b_1577:0<=b_1577 & (b_1577+11)<=n). LOCS:[63;0] (may-bug)]"),
          17 ->
            """
              |Entail 17: Fail.(may) cause: (((r1=x & a_2205=0) | (1<=a_2205 & x!=null))) &
              | (((r_2233=r1 & a_2221=0) | (1<=a_2221 & r1!=null))) &
              | (((p=r_2233 & b_2222=0) | (1<=b_2222 & r_2233!=null))) |-  r_2233=p. LOCS:[0;1;91] (may-bug)
            """.stripMargin.trim
        ))
    }
  }
}
