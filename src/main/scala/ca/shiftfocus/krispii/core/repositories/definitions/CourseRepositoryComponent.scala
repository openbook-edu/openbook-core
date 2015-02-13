package ca.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future

trait CourseRepositoryComponent {
  val courseRepository: CourseRepository

  trait CourseRepository {
    def list: Future[IndexedSeq[Course]]
    def list(project: Project): Future[IndexedSeq[Course]]
    def list(users: IndexedSeq[User]): Future[Map[UUID, IndexedSeq[Course]]]
    def list(user: User, asTeacher: Boolean = false): Future[IndexedSeq[Course]]

    def find(courseId: UUID): Future[Option[Course]]

    def insert(course: Course)(implicit conn: Connection): Future[Course]
    def update(course: Course)(implicit conn: Connection): Future[Course]
    def delete(course: Course)(implicit conn: Connection): Future[Boolean]

    def addUser(user: User, course: Course)(implicit conn: Connection): Future[Boolean]
    def removeUser(user: User, course: Course)(implicit conn: Connection): Future[Boolean]

    def addUsers(course: Course, users: IndexedSeq[User])(implicit conn: Connection): Future[Boolean]
    def removeUsers(course: Course, users: IndexedSeq[User])(implicit conn: Connection): Future[Boolean]
    def removeAllUsers(course: Course)(implicit conn: Connection): Future[Boolean]
    def findUserForTeacher(student: User, teacher: User): Future[Option[User]]

    def hasProject(user: User, project: Project)(implicit conn: Connection): Future[Boolean]
  }
}
