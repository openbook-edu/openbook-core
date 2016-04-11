package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.models.{ User, Course, Chat }
import ca.shiftfocus.krispii.core.models.document.Revision
import ca.shiftfocus.krispii.core.models.document.Document
import ca.shiftfocus.krispii.core.error.RepositoryError
import java.util.UUID
import com.github.mauricio.async.db.Connection
import concurrent.Future
import scalaz.\/

trait ChatRepository extends Repository {
  def list(course: Course)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Chat]]]
  def list(course: Course, num: Long, offset: Long)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Chat]]]

  def list(course: Course, user: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Chat]]]
  def list(course: Course, user: User, num: Long, offset: Long)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Chat]]]

  def find(course: Course, messageNum: Long)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Chat]]

  def insert(chat: Chat)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Chat]]
  def update(chat: Chat)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Chat]]
}
