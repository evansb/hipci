package edu.nus.hipci.util

import edu.nus.hipci.common.GenTest


/**
 * Requests and Responses defined at this package
 */

object request {
  sealed abstract class Request
  case class ParseHipOutput(output: String) extends Request
  case class ParseSleekOutput(output: String) extends Request
  case class FilterHipOutput(output: String, procedures: Set[String]) extends Request
  case class FilterSleekOutput(output: String, entailment: Set[Int]) extends Request
  case class AnalyzeOutput[K,V](output1: GenTest[K,V], output2: GenTest[K,V], reference: GenTest[K,V]) extends Request
}


object response {
  sealed abstract class Response
  case class ParsedHipOutput(output: Map[String, Boolean]) extends Response
  case class ParsedSleekOutput(output: Map[Int, Boolean]) extends Response
  case class FilteredHipOutput(output: Map[String, String]) extends Response
  case class FilteredSleekOutput(output: Map[Int, String]) extends Response
  case object ACK
}
