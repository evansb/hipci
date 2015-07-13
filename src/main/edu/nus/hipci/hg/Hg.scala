package edu.nus.hipci.hg

import java.io._
import java.nio.file.Path
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.sys.process._
import scala.sys.process.ProcessLogger

import edu.nus.hipci.cli.Logger

object Hg {
  private val logger = Logger("")

  private def runCommand(cmd: Seq[String], workingDir: File): (Int, String, String) = {
    val stdout = new ByteArrayOutputStream
    val stderr = new ByteArrayOutputStream
    val stdoutWriter = new PrintWriter(stdout)
    val stderrWriter = new PrintWriter(stderr)
    val exitValue = Process(cmd, workingDir).!(ProcessLogger(stdoutWriter.println, stderrWriter.println))
    stdoutWriter.close()
    stderrWriter.close()
    (exitValue, stdout.toString, stderr.toString)
  }

  def getCurrentRevision(repo: Path) : Future[Option[(String, Boolean)]] = Future {
    runCommand(Seq("hg", "identify"), repo.toFile) match {
      case (0, out, _)  =>
        if (out.endsWith("+")) {
          Some((out.substring(0, out.length - 1), true))
        } else {
          Some((out, false))
        }
      case (_, _, err) =>
        logger.error("Mercurial error:\n" + err + "\n")
        None
    }
  }
}
