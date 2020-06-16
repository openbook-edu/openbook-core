package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.models.{Chat, User}
import ca.shiftfocus.krispii.core.error.RepositoryError
import java.util.UUID

import ca.shiftfocus.krispii.core.models.course.Course
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
  def delete(courseId: UUID, messageNum: Long)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Chat]]
}
