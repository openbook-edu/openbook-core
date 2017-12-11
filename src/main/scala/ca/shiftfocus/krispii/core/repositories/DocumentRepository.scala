package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.models.document.Revision
import ca.shiftfocus.krispii.core.models.document.Document
import ca.shiftfocus.krispii.core.error.RepositoryError
import java.util.UUID
import com.github.mauricio.async.db.Connection
import concurrent.Future
import scalaz.\/

trait DocumentRepository extends Repository {
  val revisionRepository: RevisionRepository

  def find(id: UUID, version: Long = 0)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Document]]
  def insert(document: Document)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Document]]
  def update(document: Document)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Document]]
  def delete(docId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Document]]
}
