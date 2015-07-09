package scala.hipci.cli

import akka.actor.Props

import scala.hipci.common.Diff3
import scala.hipci.request._

/**
 * Render a report from test results
 *
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
object TestReporter extends CLIComponentDescriptor {
  val name = "DiffUtils"
  val props: Props = Props[TestReporter]
  val subComponents = List()
}

private class TestReporter extends CLIComponent {
  protected val descriptor = TestReporter

  // Taken from  https://stackoverflow.com/questions/7539831/scala-draw-table-to-console
  private object Tabulator {
    def format(table: Seq[Seq[Any]]) = table match {
      case Seq() => ""
      case _ =>
        val sizes = for (row <- table) yield (for (cell <- row) yield if (cell == null) 0 else cell.toString.length)
        val colSizes = for (col <- sizes.transpose) yield col.max
        val rows = for (row <- table) yield formatRow(row, colSizes)
        formatRows(rowSeparator(colSizes), rows)
    }

    def formatRows(rowSeparator: String, rows: Seq[String]): String =
      (rowSeparator :: rows.head :: rowSeparator :: rows.tail.toList ::: rowSeparator :: List()).mkString("\n")

    def formatRow(row: Seq[Any], colSizes: Seq[Int]) = {
      val cells = (for ((item, size) <- row.zip(colSizes)) yield if (size == 0) "" else ("%" + size + "s").format(item))
      cells.mkString("|", "|", "|")
    }

    def rowSeparator(colSizes: Seq[Int]) = colSizes map { "-" * _ } mkString("+", "+", "+")
  }

  private def makeTable(isDiff2: Boolean, diffSpec: List[(Any, Option[Any], Option[Any], Option[Any])]) = {
    diffSpec.foldRight(List.empty[List[String]])({ (x, acc) =>
      x match {
        case (spec, x, y, z) =>
          val render = (x: Option[_]) => x.map(_.toString).getOrElse("Not defined")
          if (isDiff2) {
            List(spec.toString, render(x), render(z)) :: acc
          } else {
            List(spec.toString, render(x), render(y), render(z)) :: acc
          }
      }
    })
  }

  private def diff2String(diff: Diff3[_,_]) = {
    val builder = StringBuilder.newBuilder
    val diffSpec = diff.specs.filter({ case (_,x,_,y) => !x.equals(y) })
    if (!diffSpec.isEmpty) {
      builder ++= s"Outcome of ${diff.output1} and ${diff.reference} are different"
      val header = List("spec", diff.output1, diff.output2)
      val content = makeTable(true, diffSpec)
      builder ++= Tabulator.format(header::content)
    }
    builder.toString
  }

  private def diff3String(diff: Diff3[_,_]) = {
    val builder = StringBuilder.newBuilder
    val diffSpec = diff.specs.filter({ case (_,x,y,z) => !(x.equals(y) && x.equals(z)) })
    if (!diffSpec.isEmpty) {
      builder ++= s"Outcome of ${diff.output1} and ${diff.reference} are different"
      val header = List("spec", diff.output1, diff.output2, "reference:" + diff.reference)
      val content = makeTable(false, diffSpec)
      builder ++= Tabulator.format(header::content)
    }
    builder.toString
  }

  override def receive = {
    case ReportDiff2String(diff3) => sender ! diff2String(diff3)
    case ReportDiff3String(diff3) => sender ! diff3String(diff3)
    case other => super.receive(other)
  }
}
