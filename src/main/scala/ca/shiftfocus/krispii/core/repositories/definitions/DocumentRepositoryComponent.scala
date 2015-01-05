package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.models.document.Revision
import ca.shiftfocus.krispii.core.models.document.Document
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.Connection
import concurrent.Future

trait DocumentRepositoryComponent {
  val documentRepository: DocumentRepository

  trait DocumentRepository {

    // Find a single document
    def find(id: UUID): Future[Option[Document]]

    // Create a new document
    def insert(document: Document)(implicit conn: Connection): Future[Document]

    // Update an existing document
    def update(document: Document)(implicit conn: Connection): Future[Document]

    // List revisions of a document
    def list(document: Document, version: Long = 0): Future[IndexedSeq[Revision]]

    // Insert a new revision of a document
    def insert(revision: Revision)(implicit conn: Connection): Future[Revision]

  }
}