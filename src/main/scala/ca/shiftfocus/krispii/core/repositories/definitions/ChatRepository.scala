package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.models.Chat
import ca.shiftfocus.krispii.core.models.document.Revision
import ca.shiftfocus.krispii.core.models.document.Document
import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.Connection
import concurrent.Future
import scalaz.\/

trait ChatRepository extends Repository {
  def list(courseId: UUID): Future[\/[RepositoryError.Fail, IndexedSeq[Chat]]]
  def list(courseId: UUID, num: Long, offset: Long): Future[\/[RepositoryError.Fail, IndexedSeq[Chat]]]

  def list(courseId: UUID, userId: UUID): Future[\/[RepositoryError.Fail, IndexedSeq[Chat]]]
  def list(courseId: UUID, userId: UUID,  num: Long, offset: Long): Future[\/[RepositoryError.Fail, IndexedSeq[Chat]]]

  def find(courseId: UUID, messageNum: Long)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Chat]]

  def insert(chat: Chat)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Chat]]
  def update(chat: Chat)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Chat]]
}