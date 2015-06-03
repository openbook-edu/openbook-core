package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models.User
import ca.shiftfocus.krispii.core.models.document.{Revision, Document}
import ca.shiftfocus.krispii.core.repositories.{DocumentRepository, UserRepository}
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.mauricio.async.db.Connection
import scala.concurrent.Future
import ws.kahn.ot._

import scalaz.\/

trait DocumentService extends Service[ErrorUnion#Fail] {
  val userRepository: UserRepository
  val documentRepository: DocumentRepository

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
  def find(id: UUID, version: Option[Long] = None): Future[\/[ErrorUnion#Fail, Document]]

  // List revisions of a document
  def listRevisions(documentId: UUID, fromVersion: Long = 0): Future[\/[ErrorUnion#Fail, IndexedSeq[Revision]]]

  def getHistory(documentId: UUID, granularity: Int = 10): Future[\/[ErrorUnion#Fail, IndexedSeq[Revision]]]

  def revert(documentId: UUID, version: Long, authorId: UUID): Future[\/[ErrorUnion#Fail, Document]]

  // Create a new document
  def create(id: UUID = UUID.randomUUID, owner: User, title: String, initialDelta: Delta): Future[\/[ErrorUnion#Fail, Document]]

  // Update a document (title only, to update contents, push a new revision)
  def update(id: UUID, version: Long, owner: User, editors: IndexedSeq[User], title: String): Future[\/[ErrorUnion#Fail, Document]]

  // Push a new revision of a document
  def push(id: UUID, version: Long, author: User, delta: Delta): Future[\/[ErrorUnion#Fail, PushResult]]

}