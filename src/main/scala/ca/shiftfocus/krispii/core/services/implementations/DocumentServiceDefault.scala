package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models.User
import ca.shiftfocus.krispii.core.models.document._
import ca.shiftfocus.krispii.core.repositories.{RevisionRepository, UserRepository, DocumentRepository}
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.Connection
import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.DateTime
import scala.concurrent.Future
import ws.kahn.ot.Delta

import scalaz.\/

class DocumentServiceDefault(val db: Connection,
                             val userRepository: UserRepository,
                             val documentRepository: DocumentRepository,
                             val revisionRepository: RevisionRepository) extends DocumentService {

  implicit def conn: Connection = db

  /**
   * Find a document.
   *
   * @param id
   * @return
   */
  override def find(id: UUID): Future[\/[ErrorUnion#Fail, Document]] = {
    documentRepository.find(id)
  }

  /**
   * List a document's revisions.
   *
   * @param documentId
   * @param fromVersion
   * @return
   */
  override def listRevisions(documentId: UUID, fromVersion: Long = 0): Future[\/[ErrorUnion#Fail, IndexedSeq[Revision]]] = {
    for {
      document <- lift(documentRepository.find(documentId))
      revisions <- lift(revisionRepository.list(document, fromVersion))
    } yield revisions
  }

  /**
   * Create a new document.
   *
   * @param id
   * @param title
   * @param initialDelta
   * @return
   */
  override def create(id: UUID = UUID.random, owner: User, title: String, initialDelta: Delta): Future[\/[ErrorUnion#Fail, Document]] = {
    transactional { implicit conn =>
      documentRepository.insert(
        Document(
          id = id,
          title = title,
          delta = initialDelta,
          ownerId = owner.id
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
  override def update(id: UUID, version: Long, owner: User, editors: IndexedSeq[User], title: String): Future[\/[ErrorUnion#Fail, Document]] = {
    transactional { implicit conn =>
      for {
        document <- lift(documentRepository.find(id))
        updated <- lift(documentRepository.update(document.copy(title = title, ownerId = owner.id)))
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
   * @param delta the operation to be performed on the document
   * @return a [[PushResult]] object containing:
   *           document: the new version of the document
   *           revision: the newly pushed (and possibly transformed) revision
   *           serverOps: the server operations that have been applied more recently than this one,
   *                      transformed against this operation so that they can be applied to
   *                      the client's document. The client may need to transform them further
   *                      against any additional edits they have in the clientside operation buffer.
   */
  override def push(documentId: UUID, version: Long, author: User, delta: Delta): Future[\/[ErrorUnion#Fail, PushResult]] = {
    transactional { implicit conn =>
      for {
        _ <- predicate (!delta.isNoOp) (ServiceError.BadInput("Delta must not be a NOOP"))

        // 1. Get the document
        document <- lift(documentRepository.find(documentId))

        // 2. Look for the more recent server operations
        recentRevisions <- lift(revisionRepository.list(document, version))

        pushResult <- {
          // If there were more recent revisions
          if (recentRevisions.nonEmpty) {
            val recentDeltas = recentRevisions.map(_.delta)
            val recentServerDelta =
              if (recentDeltas.length == 1)
                recentDeltas.head
              else recentDeltas.tail.foldLeft(recentDeltas.head) {
                (left: Delta, right: Delta) => {
                  left o right
                }
              }
            val transformedDelta = recentServerDelta x delta
            val newDelta = document.delta o transformedDelta
            for {
              _ <- predicate (newDelta.isDocument) (ServiceError.BadInput("Document Delta must contain only inserts"))
              // 4. Update the document with the latest text
              updatedDocument <- lift(documentRepository.update(document.copy(
                delta = newDelta
              )))

              // 5. Insert the new revision into the history
              pushedRevision <- lift(revisionRepository.insert(
                Revision(documentId = document.id,
                         version = document.version + 1,
                         authorId = author.id,
                         delta = transformedDelta,
                         createdAt = new DateTime)
              ))
            } yield PushResult(document = updatedDocument, revision = pushedRevision, serverOps = recentRevisions)
          }
          // If there were no more recent revisions
          else {
            val newDelta = document.delta o delta
            for {
              _ <- predicate (newDelta.isDocument) (ServiceError.BadInput("Document Delta must contain only inserts"))
              updatedDocument <- lift(documentRepository.update(document.copy(
                delta = newDelta
              )))

              // 5. Insert the new revision into the history
              pushedRevision <- lift(revisionRepository.insert(
                Revision(documentId = document.id,
                  version = document.version + 1,
                  authorId = author.id,
                  delta = delta,
                  createdAt = new DateTime)
              ))
            } yield PushResult(document = updatedDocument, revision = pushedRevision, serverOps = recentRevisions)
          }
        }
      } yield pushResult
    }
  }
}