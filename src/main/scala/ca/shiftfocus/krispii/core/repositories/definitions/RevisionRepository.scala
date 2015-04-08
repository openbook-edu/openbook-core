package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.models.document.Revision
import ca.shiftfocus.krispii.core.models.document.Document
import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.Connection
import concurrent.Future
import scalaz.\/

trait RevisionRepository extends Repository {
  def list(document: Document, version: Long = 0)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Revision]]]
  def insert(revision: Revision)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Revision]]
}