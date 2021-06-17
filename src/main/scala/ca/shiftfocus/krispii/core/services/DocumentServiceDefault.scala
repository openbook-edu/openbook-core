package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models.document._
import ca.shiftfocus.krispii.core.repositories.{DocumentRepository, RevisionRepository, UserRepository}
import ca.shiftfocus.krispii.core.services.datasource.DB
import java.util.UUID

import ca.shiftfocus.krispii.core.models.user.User
import com.github.mauricio.async.db.Connection

import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.DateTime

import scala.concurrent.Future
import ca.shiftfocus.otlib.{Delete, Delta}
import scalaz.{-\/, \/, \/-}

class DocumentServiceDefault(
    val db: DB,
    val userRepository: UserRepository,
    val documentRepository: DocumentRepository,
    val revisionRepository: RevisionRepository
) extends DocumentService {

  implicit def conn: Connection = db.pool

  /**
   * Find a document.
   *
   * @param id
   * @return
   */
  override def find(id: UUID, version: Option[Long] = None): Future[\/[ErrorUnion#Fail, Document]] = {
    documentRepository.find(id, version.getOrElse(0))
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
   * Get a list of revisions spaced by "granularity"
   *
   * @param documentId
   * @param granularity
   * @return
   */
  override def getHistory(documentId: UUID, granularity: Int = maxGranularity): Future[\/[ErrorUnion#Fail, IndexedSeq[Revision]]] = {
    if (granularity > 0 && granularity <= maxGranularity && granularity % granularityStep == 0) {
      for {
        document <- lift(documentRepository.find(documentId))
        gran = if (document.version < defaultGranularity) 1 else granularity
        interval = if (document.version < defaultGranularity) 1 else document.version / granularity
        versions = (1 until granularity).map(_ * interval).filter(_ <= document.version)
        revisions <- liftSeq(versions.map { version =>
          revisionRepository.find(document, version).map {
            case \/-(revision) => \/-(Some(revision))
            case -\/(noResults: RepositoryError.NoResults) => \/-(None)
            case -\/(error) => -\/(error)
          }
        })
      } yield revisions.flatten
    }
    else {
      Future successful \/.left(ServiceError.BadInput("Granularity must be a positive multiple of 5 no greater than 100."))
    }
  }

  /**
   * Revert a document to a previous revision. Note that this doesn't erase anything
   * from the document's history, rather the "reversion" will be inserted as a new
   * revision that clears the document and inserts the reverted text.
   *
   * @param documentId the document to revert
   * @param version the version of the document to revert to
   * @param authorId the ID of the user who will own the new revision
   * @return
   */
  override def revert(documentId: UUID, version: Long, authorId: UUID): Future[\/[ErrorUnion#Fail, Document]] = {
    transactional { implicit conn =>
      for {
        current <- lift(find(documentId))

        _ <- predicate(current.version > version)(ServiceError.BadInput("Can only revert to a _previous_ revision."))

        revertTo <- lift(find(documentId, Some(version)))
        author <- lift(userRepository.find(authorId))

        newDelta = if (current.delta.isNoOp) revertTo.delta else Delete(current.delta.targetLength) +: revertTo.delta
        newDocument = current.delta.compose(newDelta)

        _ <- predicate(newDocument.isDocument)(ServiceError.BadInput("Document Delta must contain only inserts"))

        updatedDocument <- lift(documentRepository.update(current.copy(
          delta = newDocument
        )))

        pushedRevision <- lift(revisionRepository.insert(
          Revision(
            documentId = documentId,
            version = current.version + 1,
            authorId = author.id,
            delta = newDelta,
            createdAt = new DateTime
          )
        ))
      } yield updatedDocument
    }
  }

  /**
   * Create a new document.
   *
   * @param id
   * @param title
   * @param initialDelta TODO - can have default value Delta(IndexedSeq.empty[Operation]), if it is not empty it should have record in document_revisions table
   * @return
   */
  override def create(id: UUID = UUID.randomUUID, owner: User, title: String, initialDelta: Delta): Future[\/[ErrorUnion#Fail, Document]] = {
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
        _ <- predicate(document.version == version)(ServiceError.OfflineLockFail)
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
   * @param documentId the java.util.UUID of the document to update
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
        _ <- predicate(!delta.isNoOp)(ServiceError.BadInput("Delta must not be a NOOP"))

        // 1. Get the document
        document <- lift(documentRepository.find(documentId))

        // 2. Look for the more recent server operations
        recentRevisions <- lift(revisionRepository.list(document, version))

        // TODO - create method: workRepository.findByDocument to get work

        pushResult <- {
          // If there were more recent revisions
          if (recentRevisions.nonEmpty) {
            val recentDeltas = recentRevisions.map(_.delta)
            val recentServerDelta =
              if (recentDeltas.length == 1) {
                recentDeltas.head
              }
              else recentDeltas.tail.foldLeft(recentDeltas.head) {
                (left: Delta, right: Delta) =>
                  {
                    left o right
                  }
              }
            val transformedDelta = recentServerDelta x delta
            val newDelta = document.delta o transformedDelta
            if (newDelta.targetLength > 1024000) {
              lift(Future successful \/.left[ErrorUnion#Fail, PushResult] {
                ServiceError.BusinessLogicFail("Document cannot be longer than 1,024,000 characters.")
              })
            }
            else {
              for {
                _ <- predicate(newDelta.isDocument)(ServiceError.BadInput("Document Delta must contain only inserts"))
                // 4. Update the document with the latest text
                updatedDocument <- lift(documentRepository.update(document.copy(
                  delta = newDelta
                )))
                // TODO - update Work version, updated_at also
                // 5. Insert the new revision into the history
                pushedRevision <- lift(revisionRepository.insert(
                  Revision(
                    documentId = document.id,
                    version = document.version + 1,
                    authorId = author.id,
                    delta = transformedDelta,
                    createdAt = new DateTime
                  )
                ))
              } yield PushResult(document = updatedDocument, revision = pushedRevision, serverOps = recentRevisions)
            }
          } // If there were no more recent revisions
          else {
            val newDelta = document.delta o delta
            if (newDelta.targetLength > 1024000) {
              lift(Future successful \/.left[ErrorUnion#Fail, PushResult] {
                ServiceError.BusinessLogicFail("Document cannot be longer than 1,024,000 characters.")
              })
            }
            else {
              for {
                _ <- predicate(newDelta.isDocument)(ServiceError.BadInput("Document Delta must contain only inserts"))
                updatedDocument <- lift(documentRepository.update(document.copy(
                  delta = newDelta
                )))

                // TODO - update Work version, updated_at also
                // 5. Insert the new revision into the history
                pushedRevision <- lift(revisionRepository.insert(
                  Revision(
                    documentId = document.id,
                    version = document.version + 1,
                    authorId = author.id,
                    delta = delta,
                    createdAt = new DateTime
                  )
                ))
              } yield PushResult(document = updatedDocument, revision = pushedRevision, serverOps = recentRevisions)
            }
          }
        }
      } yield pushResult
    }
  }
}
