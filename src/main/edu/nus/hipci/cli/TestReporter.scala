package edu.nus.hipci.cli

import pl.project13.scala.rainbow._
import edu.nus.hipci.core._

sealed trait TestReporterRequest

/**
 * Request to create a test report from a list of test configuration.
 *
 * @constructor Create a test report request.
 * @param configs List of test configuration to compare
 * @param diffOnly Whether only difference should be displayed
 */
case class ReportTestResult(configs: List[TestConfiguration], diffOnly: Boolean)
  extends TestReporterRequest

/** Singleton descriptor for [[TestReporter]]. */
object TestReporter extends ComponentDescriptor[TestReporter]

/** Create various forms of report from test results. */
class TestReporter extends CLIComponent {
  /** Descriptor of this component */
  val descriptor = TestReporter

  private object Tabulator {
    def format(table: Seq[Seq[Any]], color: Map[String, String => String])
    = table match {
      case Seq() => ""
      case _ =>
        val sizes = for (row <- table) yield for (cell <- row)
          yield if (cell == null) 0 else cell.toString.length
        val colSizes = for (col <- sizes.transpose) yield col.max
        val rows = for (row <- table) yield formatRow(row, colSizes, color)
        formatRows(rowSeparator(colSizes), rows)
    }

    def formatRows(rowSeparator: String, rows: Seq[String]): String = (
        rowSeparator ::
        rows.head ::
        rowSeparator ::
        rows.tail.toList :::
          rowSeparator ::
        List()).mkString("\n")

    def formatRow(row: Seq[Any], colSizes: Seq[Int],
                  color: Map[String, String => String]) = {
      val cells = for ((item, size) <- row.zip(colSizes))
        yield if (size == 0) "" else {
          val colored = color.getOrElse(item.toString, (s:String) => s)
          colored(("%" + size + "s").format(item))
        }
      cells.mkString("|", "|", "|")
    }

    def rowSeparator(colSizes: Seq[Int]) =
      colSizes map { "-" * _ } mkString("+", "+", "+")
  }

  private val coloring : Map[String, String => String] = Map(
    "SUCCESS" -> (c => c.green),
    "VALID" -> (c => c.green),
    "FAILURE" -> (c => c.red),
    "INVALID" -> (c => c.red)
  )

  private def suiteTestDiff(suite: String, configs: List[TestConfiguration],
                             onlyDiff: Boolean) = {
    val tableString = StringBuilder.newBuilder
    val testIDs = configs.map(_.testID)

    val specString = (path: String, b: Boolean) =>
      if (path.endsWith(HipExtension)) {
        if (b) "SUCCESS" else "FAILURE"
      } else {
        if (b) "VALID" else "INVALID"
      }

    configs.foldRight(Map.empty[(String,String,String), Boolean])({ (c, acc) =>
      c.tests.getOrElse(suite, Set.empty).foldRight(acc)({ (u, acc) =>
        u.specs.foldRight(acc)({ (v, acc) =>
          acc + (((c.testID, u.path , v._1), v._2))
        })
      })
    }).groupBy(_._1._2).foreach({ (u) =>
      val path = u._1
      val body =
        u._2.groupBy(_._1._3).foldRight(List.empty[List[String]])({ (u, acc) =>
          val proc = u._1
          val row = testIDs.map({ id =>
            u._2.get(id, path, proc)
                .map(s => specString(path, s))
                .getOrElse("NOT FOUND")
          })
          (proc::row) :: acc
        })
      if (!onlyDiff) {
        var table = body
        tableString ++= s"\nFile ${ path.bold.magenta.underlined }\n"
        table = header("proc", configs) :: table.sortBy(_.head)
        tableString ++= Tabulator.format(table, coloring)
        tableString ++= "\n"
      } else {
        var table = body.filter(s => s.tail.toSet.size != 1)
        if (table.nonEmpty) {
          tableString ++= s"\nFile ${ path.bold.magenta.underlined } "
          tableString ++= "differ".red
          tableString ++= "\n"
          table = header("proc", configs) :: table.sortBy(_.head)
          tableString ++= Tabulator.format(table, coloring)
          tableString ++= "\n"
        }
      }
    })
    val table = tableString.toString()
    if (!table.isEmpty) {
      s"--**-[${ suite.bold.cyan }]-**--" + table
    } else {
      table
    }
  }

  private def suiteDiff(suite: String, configs: List[TestConfiguration]) = {
    suite :: configs.map(_.tests.get(suite).isDefined)
                    .map({(b) => if (b) { "yes" } else { "no" } })
  }

  private def header(column: String, configs: List[TestConfiguration]) = {
    column :: configs.map((c) =>
      " " + c.testID.substring(0, Math.min(c.testID.length, 8)))
  }

  private def report(diff: List[TestConfiguration], diffOnly: Boolean) : String = {
    val tableString = StringBuilder.newBuilder

    // Difference in suite tested.
    val suiteNames = diff.map(_.tests.keySet).reduce((s, t) => s.union(t))
    val suiteDiffTable = suiteNames.map(suiteDiff(_, diff))
      .filter(_.tail.toSet.size != 1).toList
    if (suiteDiffTable.nonEmpty) {
      tableString ++= "Warning: Set of suites used are different\n".yellow
      tableString ++= Tabulator.format(header("suite", diff) ::
        suiteDiffTable.sortBy(_.head), coloring)
      tableString ++= "\n"
    }

    // For each suite and file, difference in outcome
    tableString ++= suiteNames.foldRight("")({ (x, acc) =>
      suiteTestDiff(x, diff, diffOnly) + acc
    })
    tableString.toString()
  }

  override def receive = {
    case ReportTestResult(result, diffOnly) => sender ! report(result, diffOnly)
    case other => super.receive(other)
  }
}
