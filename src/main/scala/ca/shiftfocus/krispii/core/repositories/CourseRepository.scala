package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.Connection
import ca.shiftfocus.krispii.core.models._
import java.util.UUID

import ca.shiftfocus.krispii.core.models.group.Course
import ca.shiftfocus.krispii.core.models.user.User

import scala.concurrent.Future
import scalaz.\/

trait CourseRepository extends Repository {
  val userRepository: UserRepository

  def list(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Course]]]
  def list(project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Course]]]
  def list(users: IndexedSeq[User])(implicit conn: Connection): Future[\/[RepositoryError.Fail, Map[UUID, IndexedSeq[Course]]]]
  def list(user: User, asTeacher: Boolean = false)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Course]]]

  def find(courseId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Course]]
  def find(slug: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Course]]

  def insert(course: Course)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Course]]
  def update(course: Course)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Course]]
  def delete(course: Course)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Course]]

  def addUser(user: User, course: Course)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]
  def removeUser(user: User, course: Course)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]

  def addUsers(course: Course, users: IndexedSeq[User])(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]
  def removeUsers(course: Course, users: IndexedSeq[User])(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]
  def removeAllUsers(course: Course)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]

  def hasProject(user: User, project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Boolean]]
}
