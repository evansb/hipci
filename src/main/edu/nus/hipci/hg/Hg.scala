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

/** Request handled by this component */
sealed trait HgRequest

/**
 * @constructor Create a request get current revision id
 * @param repoDir The path to the repository
 */
case class GetCurrentRevision(repoDir: Path) extends HgRequest

/**
 * @constructor Create a request get the absolute revision id
 * @param repoDir The path to the repository
 * @param revisions List of possibly relative revision id
 */
case class GetRevisionHash(repoDir: Path, revisions: List[String])
  extends HgRequest

/** Response emitted by this component */
sealed trait HgResponse

/**
 * @constructor Create a response that contains list of revision id
 * @param hashes List of revision id
 */
case class RevisionHash(hashes: List[String]) extends HgResponse

sealed abstract class CurrentRevisionResponse extends HgResponse
case class RevisionDirty(revision: String) extends CurrentRevisionResponse
case class RevisionClean(revision: String) extends CurrentRevisionResponse
case class MercurialError(err: String) extends CurrentRevisionResponse

/** Singleton descriptor to [[Hg]] */
object Hg extends ComponentDescriptor[Hg]

/** Interacts with Mercurial to respond to repository related queries */
class Hg extends Component {

  protected val descriptor = Hg

  private def runCommand(cmd: Seq[String], workingDir: File) = {
    val out = new ByteArrayOutputStream
    val process = (Process(cmd, workingDir) #> out).run()
    try {
      val future = Future(blocking(process.exitValue()))
      val exitValue = Await.result(future, 5.seconds)
      out.close()
      (exitValue, out.toString, "")
    } catch {
      case e:Throwable =>
        process.destroy()
        (1,"","")
    }
  }

  private def whenRepoExists(repo: Path)(f: Path => HgResponse) = {
    if (repo.resolve(".hg").toFile.exists()) {
      f(repo)
    } else {
      MercurialError(s"Mercurial repository does not exists in ${f.toString()}")
    }
  }

  private def getCurrentRevision(repo: Path) = {
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
