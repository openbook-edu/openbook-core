package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.repositories.error.RepositoryError
import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future
import scalaz.{EitherT, \/}

trait CourseScheduleExceptionRepositoryComponent {
  self: CourseScheduleRepositoryComponent with
        UserRepositoryComponent with
        CourseRepositoryComponent =>

  val courseScheduleExceptionRepository: CourseScheduleExceptionRepository

  trait CourseScheduleExceptionRepository {
    def list(course: Course): Future[\/[RepositoryError, IndexedSeq[CourseScheduleException]]]
    def list(user: User, course: Course): Future[\/[RepositoryError, IndexedSeq[CourseScheduleException]]]
    def find(id: UUID): Future[\/[RepositoryError, CourseScheduleException]]
    def insert(courseSchedule: CourseScheduleException)(implicit conn: Connection): Future[\/[RepositoryError, CourseScheduleException]]
    def update(courseSchedule: CourseScheduleException)(implicit conn: Connection): Future[\/[RepositoryError, CourseScheduleException]]
    def delete(courseSchedule: CourseScheduleException)(implicit conn: Connection): Future[\/[RepositoryError, CourseScheduleException]]

    protected def lift = EitherT.eitherT[Future, RepositoryError, CourseScheduleException] _
    protected def liftList = EitherT.eitherT[Future, RepositoryError, IndexedSeq[CourseScheduleException]] _
  }
}
