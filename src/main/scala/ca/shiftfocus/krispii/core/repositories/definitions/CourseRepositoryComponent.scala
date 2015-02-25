package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.fail.Fail
import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future
import scalaz.{\/, EitherT}

trait CourseRepositoryComponent extends FutureMonad {
  self: UserRepositoryComponent with
        ProjectRepositoryComponent =>
  val courseRepository: CourseRepository

  trait CourseRepository {
    def list: Future[\/[Fail, IndexedSeq[Course]]]
    def list(project: Project): Future[\/[Fail, IndexedSeq[Course]]]
    def list(users: IndexedSeq[User]): Future[\/[Fail, Map[UUID, IndexedSeq[Course]]]]
    def list(user: User, asTeacher: Boolean = false): Future[\/[Fail, IndexedSeq[Course]]]

    def find(courseId: UUID): Future[\/[Fail, Course]]

    def insert(course: Course)(implicit conn: Connection): Future[\/[Fail, Course]]
    def update(course: Course)(implicit conn: Connection): Future[\/[Fail, Course]]
    def delete(course: Course)(implicit conn: Connection): Future[\/[Fail, Course]]

    def addUser(user: User, course: Course)(implicit conn: Connection): Future[\/[Fail, Course]]
    def removeUser(user: User, course: Course)(implicit conn: Connection): Future[\/[Fail, Course]]

    def addUsers(course: Course, users: IndexedSeq[User])(implicit conn: Connection): Future[\/[Fail, Course]]
    def removeUsers(course: Course, users: IndexedSeq[User])(implicit conn: Connection): Future[\/[Fail, Course]]
    def removeAllUsers(course: Course)(implicit conn: Connection): Future[\/[Fail, Course]]

    def findUserForTeacher(student: User, teacher: User): Future[\/[Fail, User]]
    def hasProject(user: User, project: Project)(implicit conn: Connection): Future[\/[Fail, Boolean]]
  }
}
