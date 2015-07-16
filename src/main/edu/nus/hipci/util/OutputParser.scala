package edu.nus.hipci.util

import akka.actor.Props
import edu.nus.hipci.core._

/**
 * Parses various output
 *
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
object OutputParser extends ComponentDescriptor {
  val name = "OutputParser"
  val props = Props[OutputParser]
  val subComponents = List()
}

class OutputParser extends Component {
  import request._
  import response._
  protected val descriptor = OutputParser

  def parseHipOutput(output: String) = {
    output.lines.foldLeft(Map.empty[String, Boolean])({ (acc, line) =>
      line match {
        case HipOutputRegex(name, result) =>
          acc + ((name, result.toUpperCase.equals(HipSuccess)))
        case _ => acc
      }
    })
  }

  def parseSleekOutput(output: String) = {
    output.lines.foldLeft(Map.empty[String, Boolean])({ (acc, line) =>
      line match {
        case SleekOutputRegex(number, result) =>
          acc + ((number, result.toUpperCase.equals(SleekValid)))
        case _ => acc
      }
    })
  }

  private def mkExtractor[T](separator: (String, T) => Boolean)(output:String, keys: Set[T]) = {
    keys.foldLeft(Map.empty[T, String])({ (acc, key) =>
      val extracted =
        output.lines.span((line) => !separator(line, key))
          ._2.takeWhile({ (line) => line.trim.size > 0 })
          .mkString(System.lineSeparator)
          .trim
      acc + ((key, extracted))
    })
  }

  def filterHipOutput(output: String, procedures: Set[String]) =
    mkExtractor[String]((line, procedure) => line match {
      case HipOutputRegex(name, result) => name.equals(procedure)
      case _ => false
    })(output, procedures)

  def filterSleekOutput(output: String, entailment: Set[Int]) =
    mkExtractor[Int]((line, ent) => line match {
      case SleekOutputRegex(number,_) =>
        Integer.parseInt(number).equals(ent)
      case _ => false
    })(output, entailment)

  override def receive = {
    case ParseHipOutput(output) =>
      sender ! ParsedHipOutput(parseHipOutput(output))
    case ParseSleekOutput(output) =>
      sender ! ParsedSleekOutput(parseSleekOutput(output))
    case FilterHipOutput(output, procedures) =>
      sender ! FilteredHipOutput(filterHipOutput(output, procedures))
    case FilterSleekOutput(output, entailment) =>
      sender ! FilteredSleekOutput(filterSleekOutput(output, entailment))
    case other => super.receive(other)
  }
}
