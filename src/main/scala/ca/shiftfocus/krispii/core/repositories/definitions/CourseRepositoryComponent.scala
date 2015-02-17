package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.repositories.error.RepositoryError
import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future
import scalaz.{\/, EitherT}

trait CourseRepositoryComponent {
  self: UserRepositoryComponent with
        ProjectRepositoryComponent =>
  val courseRepository: CourseRepository

  trait CourseRepository {
    def list: Future[\/[RepositoryError, IndexedSeq[Course]]]
    def list(project: Project): Future[\/[RepositoryError, IndexedSeq[Course]]]
    def list(users: IndexedSeq[User]): Future[\/[RepositoryError, Map[UUID, IndexedSeq[Course]]]]
    def list(user: User, asTeacher: Boolean = false): Future[\/[RepositoryError, IndexedSeq[Course]]]

    def find(courseId: UUID): Future[\/[RepositoryError, Course]]

    def insert(course: Course)(implicit conn: Connection): Future[\/[RepositoryError, Course]]
    def update(course: Course)(implicit conn: Connection): Future[\/[RepositoryError, Course]]
    def delete(course: Course)(implicit conn: Connection): Future[\/[RepositoryError, Course]]

    def addUser(user: User, course: Course)(implicit conn: Connection): Future[\/[RepositoryError, Course]]
    def removeUser(user: User, course: Course)(implicit conn: Connection): Future[\/[RepositoryError, Course]]

    def addUsers(course: Course, users: IndexedSeq[User])(implicit conn: Connection): Future[\/[RepositoryError, Course]]
    def removeUsers(course: Course, users: IndexedSeq[User])(implicit conn: Connection): Future[\/[RepositoryError, Course]]
    def removeAllUsers(course: Course)(implicit conn: Connection): Future[\/[RepositoryError, Course]]

    def findUserForTeacher(student: User, teacher: User): Future[\/[RepositoryError, User]]
    def hasProject(user: User, project: Project)(implicit conn: Connection): Future[\/[RepositoryError, Boolean]]

    protected def lift = EitherT.eitherT[Future, RepositoryError, Course] _
    protected def liftList = EitherT.eitherT[Future, RepositoryError, IndexedSeq[Course]] _
    protected def liftMap = EitherT.eitherT[Future, RepositoryError, Map[UUID, IndexedSeq[Course]]] _
  }
}
