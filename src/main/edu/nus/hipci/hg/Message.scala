package edu.nus.hipci.hg

import java.nio.file.Path

object request {
  sealed abstract class Request

  case class GetCurrentRevision(repoDir: Path) extends Request
}

object response {
  sealed abstract class Response

  sealed abstract class CurrentRevisionResponse extends Response
  case class RevisionDirty(revision: String) extends CurrentRevisionResponse
  case class RevisionClean(revision: String) extends CurrentRevisionResponse
}
