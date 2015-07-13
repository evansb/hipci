package edu.nus.hipci.common

import pl.project13.scala.rainbow._

/**
 * Wraps the log4s logger to include distinct colors on INFO, WARN, and ERROR
 *
 * @author Evan Sebastian<evanlhoini@gmail.com>
 */
case class Logger(name : String) {
  private val logger : org.log4s.Logger = org.log4s.getLogger(name)
  def info(msg : String) = logger.info(msg.underlined)
  def warn(msg : String) = logger.warn(msg.yellow.bold)
  def error(msg : String) = logger.error(msg.red.bold)
  def debug(msg : String) = logger.debug(msg)
  def good(msg : String) = logger.info(msg.green)
  def bad(msg : String) = logger.info(msg.red)
}

object Logging {
  def toStdout(name: String = "") = Logger(name)
}
