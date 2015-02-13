package ca.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future

trait CourseScheduleExceptionRepositoryComponent {
  val courseScheduleExceptionRepository: CourseScheduleExceptionRepository

  trait CourseScheduleExceptionRepository {
    def list(course: Course): Future[IndexedSeq[CourseScheduleException]]
    def list(user: User, course: Course): Future[IndexedSeq[CourseScheduleException]]
    def find(id: UUID): Future[Option[CourseScheduleException]]
    def insert(courseSchedule: CourseScheduleException)(implicit conn: Connection): Future[CourseScheduleException]
    def update(courseSchedule: CourseScheduleException)(implicit conn: Connection): Future[CourseScheduleException]
    def delete(courseSchedule: CourseScheduleException)(implicit conn: Connection): Future[Boolean]
  }
}
