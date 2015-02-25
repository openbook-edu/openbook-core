package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.fail.Fail
import ca.shiftfocus.krispii.core.models.User
import ca.shiftfocus.krispii.core.models.document._
import ca.shiftfocus.krispii.core.repositories.DocumentRepositoryComponent
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import org.joda.time.DateTime
import scala.concurrent.Future
import ws.kahn.ot.Delta

import scalaz.\/

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
    override def find(id: UUID): Future[\/[Fail, Document]] = {
      documentRepository.find(id)
    }

    /**
     * List a document's revisions.
     *
     * @param documentId
     * @param fromVersion
     * @return
     */
    override def listRevisions(documentId: UUID, fromVersion: Long = 0): Future[\/[Fail, IndexedSeq[Revision]]] = {
      (for {
        document <- lift(documentRepository.find(documentId))
        revisions <- lift(documentRepository.list(document, fromVersion))
      } yield revisions).run
    }

    /**
     * Create a new document.
     *
     * @param id
     * @param title
     * @param initialContent
     * @return
     */
    override def create(id: UUID = UUID.random, owner: User, title: String, initialDelta: Delta): Future[\/[Fail, Document]] = {
      transactional { implicit connection =>
        documentRepository.insert(
          Document(
            id = id,
            title = title,
            plaintext = initialDelta.applyTo(""),
            delta = initialDelta,
            owner = owner,
            editors = IndexedSeq.empty[User]
          )
        )
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
    override def update(id: UUID, version: Long, owner: User, editors: IndexedSeq[User], title: String): Future[\/[Fail, Document]] = {
      transactional { implicit connection =>
        (for {
          document <- lift(documentRepository.find(id))
          updated <- lift(documentRepository.update(document.copy(title = title, owner = owner, editors = editors)))
        } yield updated).run
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
     * @param delta the operation to be performed on the document
     * @return a [[PushResult]] object containing:
     *           document: the new version of the document
     *           revision: the newly pushed (and possibly transformed) revision
     *           serverOps: the server operations that have been applied more recently than this one,
     *                      transformed against this operation so that they can be applied to
     *                      the client's document. The client may need to transform them further
     *                      against any additional edits they have in the clientside operation buffer.
     */
    override def push(documentId: UUID, version: Long, author: User, delta: Delta): Future[\/[Fail, PushResult]] = {
      transactional { implicit connection =>
        (for {
          // 1. Get the document
          document <- lift(documentRepository.find(documentId))

          // 2. Look for the more recent server operations
          recentRevisions <- lift(documentRepository.list(document, version))

          pushResult <- {
            // If there were more recent revisions
            if (recentRevisions.nonEmpty) {
              val recentDeltas = recentRevisions.map(_.delta)
              val recentServerDelta = recentDeltas.foldLeft(recentDeltas.head) {
                (left: Delta, right: Delta) => left o right
              }
              val transformedDelta = recentServerDelta x delta

              for {
                // 4. Update the document with the latest text
                updatedDocument <- lift(documentRepository.update(document.copy(
                  plaintext = transformedDelta.applyTo(document.plaintext),
                  delta = document.delta o transformedDelta
                )))

                // 5. Insert the new revision into the history
                pushedRevision <- lift(documentRepository.insert(
                  Revision(documentId = document.id,
                    version = document.version + 1,
                    author = author,
                    delta = transformedDelta,
                    createdAt = Some(new DateTime))
                ))
              } yield PushResult(document = updatedDocument, revision = pushedRevision, serverOps = recentRevisions)
            }
            // If there were no more recent revisions
            else {
              for {
                updatedDocument <- lift(documentRepository.update(document.copy(
                  plaintext = delta.applyTo(document.plaintext),
                  delta = document.delta o delta
                )))

                // 5. Insert the new revision into the history
                pushedRevision <- lift(documentRepository.insert(
                  Revision(documentId = document.id,
                    version = document.version + 1,
                    author = author,
                    delta = delta,
                    createdAt = Some(new DateTime))
                ))
              } yield PushResult(document = updatedDocument, revision = pushedRevision, serverOps = recentRevisions)
            }
          }
        } yield pushResult).run
      }
    }
  }
}