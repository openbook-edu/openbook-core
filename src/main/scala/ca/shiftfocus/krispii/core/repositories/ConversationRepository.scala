package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.{Conversation, User}
import com.github.mauricio.async.db.{Connection}
import org.joda.time.DateTime

import scala.concurrent.Future
import scalaz.\/

trait ConversationRepository extends Repository {
  def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Conversation]]
  def list(entityId: UUID, limit: Int = 0, offset: Int = 0)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Conversation]]]
  def list(entityId: UUID, afterDate: DateTime)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Conversation]]]
  def listByUser(entityId: UUID, userId: UUID, limit: Int = 0, offset: Int = 0)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Conversation]]]
  def listByUser(entityId: UUID, userId: UUID, afterDate: DateTime)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Conversation]]]
  def addMember(conversation: Conversation, newMember: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Conversation]]
  def insert(conversation: Conversation)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Conversation]]
  def delete(conversation: Conversation)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Conversation]]
}
