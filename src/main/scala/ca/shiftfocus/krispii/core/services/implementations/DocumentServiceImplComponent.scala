package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.models.User
import ca.shiftfocus.krispii.core.models.document._
import ca.shiftfocus.krispii.core.repositories.DocumentRepositoryComponent
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import org.joda.time.DateTime
import scala.concurrent.Future
import ws.kahn.ot.Operation

trait DocumentServiceImplComponent extends DocumentServiceComponent {
  self: DocumentRepositoryComponent with
    PostgresDB =>

  override val documentService: DocumentService = new DocumentServiceImpl

  private class DocumentServiceImpl extends DocumentService {

    /**
     * Find a document.
     *
     * @param id
     * @return
     */
    override def find(id: UUID): Future[Option[Document]] = {
      documentRepository.find(id).recover { case exception => throw exception }
    }

    /**
     * List a document's revisions.
     *
     * @param documentId
     * @param fromVersion
     * @return
     */
    override def listRevisions(documentId: UUID, fromVersion: Long = 0): Future[IndexedSeq[Revision]] = {
      for {
        document <- documentRepository.find(documentId).map(_.get)
        revisions <- documentRepository.list(document, fromVersion).recover { case exception => throw exception }
      } yield revisions
    }

    /**
     * Create a new document.
     *
     * @param id
     * @param title
     * @param initialContent
     * @return
     */
    override def create(id: UUID = UUID.random, owner: User, title: String, initialContent: String): Future[Document] = {
      transactional { implicit connection =>
        documentRepository.insert(Document(id = id, title = title, content = initialContent, owner = owner, editors = IndexedSeq.empty[User]))
      }
    }

    /**
     * Update an existing document (metadata only).
     *
     * @param id
     * @param version
     * @param title
     * @return
     */
    override def update(id: UUID, version: Long, owner: User, editors: IndexedSeq[User], title: String): Future[Document] = {
      transactional { implicit connection =>
        for {
          document <- documentRepository.find(id).map(_.get)
          updated <- documentRepository.update(document.copy(title = title, owner = owner, editors = editors))
        } yield updated
      }
    }

    /**
     * Push a new revision.
     *
     * This method will push a new revision onto the document's revision history, transforming
     * the given operation against any revisions that have already been applied to the document.
     *
     * The push operation runs in a transaction so that if any one part fails, the entire
     * operation fails and rolls back.
     *
     * @param documentId the [[ca.shiftfocus.uuid.UUID]] of the document to update
     * @param version the version of the document to update from
     * @param operation the operation to be performed on the document
     * @return a [[PushResult]] object containing:
     *           document: the new version of the document
     *           revision: the newly pushed (and possibly transformed) revision
     *           serverOps: the server operations that have been applied more recently than this one,
     *                      transformed against this operation so that they can be applied to
     *                      the client's document. The client may need to transform them further
     *                      against any additional edits they have in the clientside operation buffer.
     */
    override def push(documentId: UUID, version: Long, author: User, operation: Operation): Future[PushResult] = {
      transactional { implicit connection =>
        for {
        // 1. Get the document
          document         <- documentRepository.find(documentId).map(_.get)

          // 2. Look for the more recent server operations
          recentRevisions  <- documentRepository.list(document, version)

          pushResult <- {
            // If there were more recent revisions
            if (recentRevisions.nonEmpty) {
              for {
                recentOperations <- Future successful recentRevisions.map(_.operation)
                recentServerOp   <- Future successful recentOperations.foldLeft(recentOperations.head) {
                  (left: Operation, right: Operation) => left composeWith right
                }

                // 3. Transform the client's operation and the server's operations against each other
                (xfServerOp, xfClientOp) <- Future successful Operation.transform(recentServerOp, operation)

                // 4. Update the document with the latest text
                updatedDocument <- documentRepository.update(document.copy(content = xfClientOp.applyTo(document.content)))

                // 5. Insert the new revision into the history
                pushedRevision <- documentRepository.insert(
                  Revision(documentId = document.id,
                    version = document.version + 1,
                    author = author,
                    operation = xfClientOp,
                    createdAt = Some(new DateTime))
                )
              } yield PushResult(document = updatedDocument, revision = pushedRevision, serverOps = recentRevisions)
            }
            // If there were no more recent revisions
            else {
              for {
                updatedDocument <- documentRepository.update(document.copy(content = operation.applyTo(document.content)))

                // 5. Insert the new revision into the history
                pushedRevision <- documentRepository.insert(
                  Revision(documentId = document.id,
                    version = document.version + 1,
                    author = author,
                    operation = operation,
                    createdAt = Some(new DateTime))
                )
              } yield PushResult(document = updatedDocument, revision = pushedRevision, serverOps = recentRevisions)
            }
          }
        } yield pushResult
      }
    }
  }
}