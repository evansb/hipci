package edu.nus.hipci.core

import java.nio.file.Paths

import com.typesafe.config.{ConfigValueFactory, ConfigFactory}

/** Application Configuration constants */
object AppConfiguration {
  /** Field names in the file schema. */
  object Fields {
    val ProjectDirectory = "project_directory"
    val HipDirectory = "hip_directory"
    val SleekDirectory = "sleek_directory"
    val DaemonHost = "daemon_host"
    val DaemonPort = "daemon_port"
  }

  /** Global App configuration */
  val global : AppConfiguration = AppConfiguration()

  def getClientConfig() = ConfigFactory parseString
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

  def getServerConfig() = ConfigFactory.parseString(
    s"""
      | akka {
      |   loglevel = "ERROR"
      |   actor {
      |     provider = "akka.remote.RemoteActorRefProvider"
      |   }
      |   remote {
      |     netty.tcp {
      |       hostname = "${ global.daemonHost }"
      |       port = ${ global.daemonPort }
      |     }
      |   }
      | }
    """.stripMargin)

  def toConfig(config: AppConfiguration) = {
    import Fields._
    ConfigFactory.empty()
      .withValue(ProjectDirectory, ConfigValueFactory.fromAnyRef(config.projectDirectory))
      .withValue(HipDirectory, ConfigValueFactory.fromAnyRef(config.projectDirectory))
      .withValue(SleekDirectory, ConfigValueFactory.fromAnyRef(config.projectDirectory))
      .withValue(DaemonHost, ConfigValueFactory.fromAnyRef(config.daemonHost))
      .withValue(DaemonPort, ConfigValueFactory.fromAnyRef(config.daemonPort))
  }
}


/**
 * Models Application Configuration
 * @param projectDirectory The HIP/SLEEK project directory where the executables are located.
 * @param hipDirectory HIP base test directory
 * @param sleekDirectory SLEEK base test directory
 * @param daemonHost Hostname of the Daemon process
 * @param daemonPort Port number of the Daemon process
 */
case class AppConfiguration (
  var projectDirectory : String = ".",
  var hipDirectory : String = Paths.get("examples", "working", "sleek").toString,
  var sleekDirectory : String = Paths.get("examples", "working", "hip").toString,
  var daemonHost: String = "127.0.0.1",
  var daemonPort: String = "2552"
)
