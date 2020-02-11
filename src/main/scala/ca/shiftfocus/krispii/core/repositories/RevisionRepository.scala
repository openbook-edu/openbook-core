package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.models.document.Revision
import ca.shiftfocus.krispii.core.models.document.Document
import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.Connection
import concurrent.Future
import scalaz.\/

trait RevisionRepository extends Repository {
  def list(document: Document, afterVersion: Long = 0, toVersion: Long = 0)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Revision]]]
  def find(document: Document, version: Long)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Revision]]
  def insert(revision: Revision)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Revision]]
}
