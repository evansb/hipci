package edu.nus.hipci.core

import java.nio.file.Paths

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
