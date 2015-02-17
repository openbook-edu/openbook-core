package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.models.document.Revision
import ca.shiftfocus.krispii.core.models.document.Document
import ca.shiftfocus.krispii.core.repositories.error.RepositoryError
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.Connection
import concurrent.Future
import scalaz.{\/, EitherT}

trait DocumentRepositoryComponent {
  val documentRepository: DocumentRepository

  trait DocumentRepository {
    def find(id: UUID): Future[\/[RepositoryError, Document]]
    def insert(document: Document)(implicit conn: Connection): Future[\/[RepositoryError, Document]]
    def update(document: Document)(implicit conn: Connection): Future[\/[RepositoryError, Document]]

    def list(document: Document, version: Long = 0): Future[\/[RepositoryError, IndexedSeq[Revision]]]
    def insert(revision: Revision)(implicit conn: Connection): Future[\/[RepositoryError, Revision]]

    protected def lift = EitherT.eitherT[Future, RepositoryError, Document] _
    protected def liftList = EitherT.eitherT[Future, RepositoryError, IndexedSeq[Document]] _

    protected def liftRev = EitherT.eitherT[Future, RepositoryError, Revision] _
    protected def liftRevList = EitherT.eitherT[Future, RepositoryError, IndexedSeq[Revision]] _
  }
}