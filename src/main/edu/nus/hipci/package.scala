package edu.nus

/**
 * Package-wide constants
 *
 * @author Evan Sebastian
 */
package object hipci {
  def AppName = "hipci"

  def AppVersion = "1.0.0"

  def HipSpecSeparator = "."

  def HipSuccess = "SUCCESS"

  def HipFail = "FAIL"

  def SleekValid = "VALID"

  def SleekInvalid = "INVALID"

  val HipOutputRegex = """Procedure\s(\w*)\$\S*\s(\w*).*$""".r

  val SleekOutputRegex = """Entail\s(\d*):\s(\w*).*$""".r

  val HipExtension = ".ss"

  val SleekExtension = ".slk"

  val HipTest = "hip"

  val SleekTest = "sleek"
}
