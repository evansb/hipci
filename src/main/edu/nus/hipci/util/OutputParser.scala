package edu.nus.hipci.util

import edu.nus.hipci.core._

/** Request handled by [[OutputParser]] */
sealed trait OutputParserRequest

/**
 * @constructor Create a request to parse a HIP output
 * @param output The output
 */
case class ParseHipOutput(output: String) extends OutputParserRequest

/**
 * @constructor Create a request to parse a SLEEK output
 * @param output The output
 */
case class ParseSleekOutput(output: String) extends OutputParserRequest

/**
 * @constructor Create a request to filter certain procedures from a HIP output
 * @param output The output
 * @param procs List of filtered procedures
 */
case class FilterHipOutput(output: String, procs: Set[String])
  extends OutputParserRequest

/**
 * @constructor Create a request to filter certain procedures from a SLEEK output
 * @param output The output
 * @param procs List of filtered procedures
 */
case class FilterSleekOutput(output: String, procs: Set[String])
  extends OutputParserRequest

/** Response emitted by [[OutputParser]] */
sealed trait OutputParserResponse

/**
 * @constructor Create a parsed HIP output.
 * @param output Mapping between procedure names and its outcome
 */
case class ParsedHipOutput(output: Map[String, Boolean])
  extends OutputParserResponse

/**
 * @constructor Create a parsed SLEEK output.
 * @param output Mapping between procedure names and its outcome.
 */
case class ParsedSleekOutput(output: Map[String, Boolean])
  extends OutputParserResponse

/**
 * @constructor Create a filtered HIP output.
 * @param output Mapping between procedure names and its outcome.
 */
case class FilteredHipOutput(output: Map[String, String])
  extends OutputParserRequest

/**
 * @constructor Create a filtered SLEEK output.
 * @param output Mapping between procedure names and its outcome.
 */
case class FilteredSleekOutput(output: Map[String, String])
  extends OutputParserResponse

/**
 * Parses various output
 *
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
object OutputParser extends ComponentDescriptor[OutputParser]

class OutputParser extends Component {
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

  private def mkExtractor[T](separator: (String, T) => Boolean)
                            (output:String, keys: Set[T]) = {
    keys.foldLeft(Map.empty[T, String])({ (acc, key) =>
      val extracted =
        output.lines.span((line) => !separator(line, key))
          ._2.takeWhile({ (line) => line.trim.nonEmpty })
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

  def filterSleekOutput(output: String, entailment: Set[String]) =
    mkExtractor[String]((line, ent) => line match {
      case SleekOutputRegex(number,_) => number.equals(ent)
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
