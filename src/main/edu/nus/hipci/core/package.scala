package edu.nus.hipci

/*
 * Application-wide constants.
 */
package object core {
  def AppName = "hipci"

  def AppVersion = "1.0.0"

  def HipSpecSeparator = "."

  def HipSuccess = "SUCCESS"

  def HipFail = "FAIL"

  def SleekValid = "VALID"

  def SleekInvalid = "FAIL"

  val HipOutputRegex =
    """Procedure\s(\w*)\$\S*\s(\w*).*$""".r

  val SleekOutputRegex =
    """Entail\s\(?(\d*)\)?\s?:\s(\w*).*$""".r

  val SleekExpectInferRegex =
    """Expect_Infer\s((?:\w|\.)*):\s(\w*).*$""".r

  val HipExtension = ".ss"

  val SleekExtension = ".slk"

  val HipTest = "hip"

  val SleekTest = "sleek"

  val HipciConf = "hipci.conf"
}
