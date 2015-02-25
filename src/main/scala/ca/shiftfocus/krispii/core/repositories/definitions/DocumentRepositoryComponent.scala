package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.lib.FutureMonad
import ca.shiftfocus.krispii.core.models.document.Revision
import ca.shiftfocus.krispii.core.models.document.Document
import ca.shiftfocus.krispii.core.fail.Fail
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.Connection
import concurrent.Future
import scalaz.{\/, EitherT}

trait DocumentRepositoryComponent extends FutureMonad {
  val documentRepository: DocumentRepository

  trait DocumentRepository {
    def find(id: UUID): Future[\/[Fail, Document]]
    def insert(document: Document)(implicit conn: Connection): Future[\/[Fail, Document]]
    def update(document: Document)(implicit conn: Connection): Future[\/[Fail, Document]]

    def list(document: Document, version: Long = 0): Future[\/[Fail, IndexedSeq[Revision]]]
    def insert(revision: Revision)(implicit conn: Connection): Future[\/[Fail, Revision]]
  }
}