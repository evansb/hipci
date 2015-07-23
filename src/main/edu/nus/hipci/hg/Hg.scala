package edu.nus.hipci.hg

import java.io._
import java.nio.file.Path

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.sys.process._
import akka.actor._
import edu.nus.hipci.cli._
import edu.nus.hipci.core._

object Hg extends ComponentDescriptor {
  val name = "Hg"
  val props = Props[Hg]
  val subComponents = List.empty
}

class Hg extends Component {
  import request._
  import response._

  protected val descriptor = Hg

  private def runCommand(cmd: Seq[String], workingDir: File) = {
    val out = new ByteArrayOutputStream
    val process = (Process(cmd, workingDir) #> out).run()
    try {
      val future = Future(blocking(process.exitValue()))
      val exitValue = Await.result(future, 5.seconds)
      out.close()
      (exitValue, out.toString(), "")
    } catch {
      case e:Throwable =>
        process.destroy()
        (1,"","")
    }
  }

  private def whenRepoExists(repo: Path)(f: Path => Response): Response = {
    if (repo.resolve(".hg").toFile().exists()) {
      f(repo)
    } else {
      MercurialError(s"Mercurial repository does not exists in ${f.toString()}")
    }
  }

  private def getCurrentRevision(repo: Path) : Response = {
    whenRepoExists(repo) { (repo) =>
      runCommand(Seq("hg", "id", "-i"), repo.toFile) match {
        case (0, out, _) =>
          if (out.endsWith("+")) {
            RevisionDirty(out.trim().substring(0, out.length - 1))
          } else {
            RevisionClean(out.trim())
          }
        case (_, _, err) =>
          MercurialError(err)
      }
    }
  }

  private def getRevisionHash(repo: Path, revisions: List[String]) = {
    whenRepoExists(repo) { (repo) =>
      RevisionHash(revisions.map { (revision) =>
        runCommand(Seq("hg", "id", "-i", "-r", revision), repo.toFile) match {
          case (0, out, _) => out.trim()
          case (_, _, err) => throw new RevisionNotFound(revision)
        }
      })
    }
  }

  override def receive = {
    case GetCurrentRevision(repoDir) => sender() ! getCurrentRevision(repoDir)
    case GetRevisionHash(repoDir, revisions) =>
      sender() ! getRevisionHash(repoDir, revisions)
    case other => super.receive(other)
  }
}
