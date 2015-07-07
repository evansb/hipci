package scala.hipci.util

import akka.actor.Props

import scala.hipci.constant
import scala.hipci.common.{ComponentDescriptor, Component}
import scala.hipci.{response, request}

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
  protected val descriptor = OutputParser

  def parseHipOutput(output: String) = {
    output.lines.foldLeft(Map.empty[String, Boolean])({ (acc, line) =>
      line match {
        case constant.HipOutputRegex(name, result) =>
          acc + ((name, result.toUpperCase.equals(constant.HipSuccess)))
        case _ => acc
      }
    })
  }

  def parseSleekOutput(output: String) = {
    output.lines.foldLeft(Map.empty[Int, Boolean])({ (acc, line) =>
      line match {
        case constant.SleekOutputRegex(number, result) =>
          acc + ((Integer.parseInt(number), (result.toUpperCase.equals(constant.SleekValid))))
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
      case constant.HipOutputRegex(name, result) => name.equals(procedure)
      case _ => false
    })(output, procedures)

  def filterSleekOutput(output: String, entailment: Set[Int]) =
    mkExtractor[Int]((line, ent) => line match {
      case constant.SleekOutputRegex(number,_) =>
        Integer.parseInt(number).equals(ent)
      case _ => false
    })(output, entailment)

  override def receive = {
    case request.ParseHipOutput(output) =>
      sender ! response.ParsedHipOutput(parseHipOutput(output))
    case request.ParseSleekOutput(output) =>
      sender ! response.ParsedSleekOutput(parseSleekOutput(output))
    case request.FilterHipOutput(output, procedures) =>
      sender ! response.FilteredHipOutput(filterHipOutput(output, procedures))
    case request.FilterSleekOutput(output, entailment) =>
      sender ! response.FilteredSleekOutput(filterSleekOutput(output, entailment))
    case other => super.receive(other)
  }
}
