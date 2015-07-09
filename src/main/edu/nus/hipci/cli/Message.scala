package edu.nus.hipci.cli

import scala.language.existentials
import akka.actor.ActorSystem

import edu.nus.hipci.common.Diff3

/**
 * Requests and Responses defined at this package
 */

object request {
  sealed abstract class Request
  case class InitCLI(system: ActorSystem, args: Seq[String]) extends Request
  case class ParseArguments(arguments: Seq[String]) extends Request
  case class Config(config: com.typesafe.config.Config) extends Request
  case object ShowUsage extends Request
  case class Terminate(exitCode: Int) extends Request
  case object KeepAlive extends Request
  case class ReportDiff2String(diff3: Diff3[_,_]) extends Request
  case class ReportDiff3String(diff3: Diff3[_,_]) extends Request
}

object response {
  sealed abstract class Response
  case class ParsedHipOutput(output: Map[String, Boolean]) extends Response
  case class ParsedSleekOutput(output: Map[Int, Boolean]) extends Response
  case class FilteredHipOutput(output: Map[String, String]) extends Response
  case class FilteredSleekOutput(output: Map[Int, String]) extends Response
  case class ParsedArguments(command: Command) extends Response
}
