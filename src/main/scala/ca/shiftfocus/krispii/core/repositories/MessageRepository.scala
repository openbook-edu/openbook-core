package ca.shiftfocus.krispii.core.repositories

import java.util.UUID
import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.Message
import com.github.mauricio.async.db.Connection
import org.joda.time.DateTime
import scala.concurrent.Future
import scalaz.\/

trait MessageRepository extends Repository {
  def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Message]]
  def list(conversationId: UUID, limit: Int = 0, offset: Int = 0)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Message]]]
  def list(conversationId: UUID, afterDate: DateTime)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Message]]]
  def listNew(entityId: UUID, userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Message]]]
  def hasNew(entityId: UUID, userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Boolean]]
  def setLastRead(conversationId: UUID, userId: UUID, messageId: UUID, readAt: DateTime)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]
  def getLastRead(conversationId: UUID, userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Message]]
  def insert(course: Message)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Message]]
  def delete(course: Message)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Message]]
}
