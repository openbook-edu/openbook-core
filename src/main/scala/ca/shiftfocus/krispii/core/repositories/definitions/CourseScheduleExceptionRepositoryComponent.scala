package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.fail.Fail
import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future
import scalaz.{EitherT, \/}

trait CourseScheduleExceptionRepositoryComponent extends FutureMonad {
  self: CourseScheduleRepositoryComponent with
        UserRepositoryComponent with
        CourseRepositoryComponent =>

  val courseScheduleExceptionRepository: CourseScheduleExceptionRepository

  trait CourseScheduleExceptionRepository {
    def list(course: Course)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[CourseScheduleException]]]
    def list(user: User, course: Course)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[CourseScheduleException]]]

    def find(id: UUID)(implicit conn: Connection): Future[\/[Fail, CourseScheduleException]]

    def insert(courseSchedule: CourseScheduleException)(implicit conn: Connection): Future[\/[Fail, CourseScheduleException]]
    def update(courseSchedule: CourseScheduleException)(implicit conn: Connection): Future[\/[Fail, CourseScheduleException]]
    def delete(courseSchedule: CourseScheduleException)(implicit conn: Connection): Future[\/[Fail, CourseScheduleException]]
  }
}
