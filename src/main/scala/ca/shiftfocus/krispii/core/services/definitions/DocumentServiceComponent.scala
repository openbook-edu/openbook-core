package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.models.User
import ca.shiftfocus.krispii.core.models.document.{Revision, Document}
import ca.shiftfocus.krispii.core.services.error.ServiceError
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import com.github.mauricio.async.db.Connection
import scala.concurrent.Future
import ws.kahn.ot._

import scalaz.\/

trait DocumentServiceComponent {
  val documentService: DocumentService

  trait DocumentService {

    /**
     * A case class encapsulating the result of a push operation.
     *
     * @param document the updated copy of the document
     * @param revision the pushed revision that as it was actually stored
     *                 in the database, transformed against more recent revisions.
     *                 This can be sent to other clients to update their document.
     * @param serverOps The more recent revisions that have been applied to the server since
     *                  this revision's version.
     */
    case class PushResult(
      document: Document,
      revision: Revision,
      serverOps: IndexedSeq[Revision]
    )

    // Find a document
    def find(id: UUID): Future[\/[ServiceError, Document]]

    // List revisions of a document
    def listRevisions(documentId: UUID, fromVersion: Long = 0): Future[\/[ServiceError, IndexedSeq[Revision]]]

    // Create a new document
    def create(id: UUID = UUID.random, owner: User, title: String, initialDelta: Delta): Future[\/[ServiceError, Document]]

    // Update a document (title only, to update contents, push a new revision)
    def update(id: UUID, version: Long, owner: User, editors: IndexedSeq[User], title: String): Future[\/[ServiceError, Document]]

    // Push a new revision of a document
    def push(id: UUID, version: Long, author: User, delta: Delta): Future[\/[ServiceError, PushResult]]

  }
}
