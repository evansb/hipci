package edu.nus.hipci.hg

import java.io._
import java.nio.file.Path

import scala.sys.process._
import scala.sys.process.ProcessLogger
import akka.actor._

import edu.nus.hipci.common._

object Hg extends ComponentDescriptor {
  val name = "Hg"
  val props = Props[Hg]
  val subComponents = List.empty
}

class Hg extends Component {
  import request._
  import response._

  protected val descriptor = Hg
  private val logger = Logging.toStdout()

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

  private def getCurrentRevision(repo: Path) = {
    runCommand(Seq("hg", "id", "-i"), repo.toFile) match {
      case (0, out, _)  =>
        if (out.endsWith("+")) {
          RevisionDirty(out.trim().substring(0, out.length - 1))
        } else {
          RevisionClean(out.trim())
        }
      case (_, _, err) =>
        MercurialError(err)
    }
  }

  override def receive = {
    case GetCurrentRevision(repoDir) => sender ! getCurrentRevision(repoDir)
    case other => super.receive(other)
  }
}
