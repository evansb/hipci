package edu.nus.hipci

import com.typesafe.config.ConfigFactory

/**
 * Application-wide constants.
 */
package object core {
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

  val DefaultClientConfig = ConfigFactory parseString
    """
      | akka {
      |  loglevel = "ERROR"
      |  actor {
      |    provider = "akka.remote.RemoteActorRefProvider"
      |  }
      |  remote {
      |    netty.tcp {
      |      hostname = "127.0.0.1"
      |      port = 0
      |    }
      |  }
      |}
    """.stripMargin

  val DefaultServerConfig = ConfigFactory.parseString(
    """
      | akka {
      |   loglevel = "ERROR"
      |   actor {
      |     provider = "akka.remote.RemoteActorRefProvider"
      |   }
      |   remote {
      |     netty.tcp {
      |       hostname = "127.0.0.1"
      |       port = 2552
      |     }
      |   }
      | }
    """.stripMargin)
}
