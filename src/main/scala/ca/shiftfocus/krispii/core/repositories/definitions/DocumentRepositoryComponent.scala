package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.{Document, DocumentRevision}
import ca.shiftfocus.krispii.core.models.work.{Work, LongAnswerWork}
import ca.shiftfocus.krispii.core.models.tasks.Task
import ca.shiftfocus.ot._
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.Connection
import concurrent.Future

trait DocumentRepositoryComponent {
  val documentRepository: DocumentRepository

  trait DocumentRepository {

    // Fetch a group of documents, optionally with their revision histories.
    def list(documentIds: IndexedSeq[UUID]): Future[IndexedSeq[Document]]

    // Fetch (optionally just a slice of) a document's revision history.
    def revisions(documentId: UUID, fromVer: Option[Long] = None, toVer: Option[Long] = None): Future[IndexedSeq[DocumentRevision]]

    // Fetch a single document, optionally with its revision history.
    def find(documentId: UUID, withHistory: Boolean = false, fromVer: Option[Long] = None, toVer: Option[Long] = None): Future[Option[Document]]

    // Create a new document. Creates a new, empty, document with the given ID.
    def create(documentId: UUID)(implicit connection: Connection): Future[Document]

    // Update an existing document by "pushing" a new revision.
    // Returns a tuple containing an instance of the new revision, and a checksum of the current document at this
    // version number.
    def push(documentId: UUID, version: Long, revisions: IndexedSeq[Operation])(implicit connection: Connection): Future[Option[(DocumentRevision, Array[Byte])]]

    // Delete a document and its revision history.
    def delete(documentId: UUID, version: Long)(implicit connection: Connection): Future[Boolean]

  }
}