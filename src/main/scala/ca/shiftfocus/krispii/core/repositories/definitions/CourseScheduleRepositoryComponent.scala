package ca.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import org.joda.time.LocalTime
import org.joda.time.LocalDate
import scala.concurrent.Future

trait CourseScheduleRepositoryComponent {
  val courseScheduleRepository: CourseScheduleRepository

  trait CourseScheduleRepository {
    def list(implicit conn: Connection): Future[IndexedSeq[CourseSchedule]]
    def list(course: Course)(implicit conn: Connection): Future[IndexedSeq[CourseSchedule]]
    def find(id: UUID)(implicit conn: Connection): Future[Option[CourseSchedule]]
    def insert(courseSchedule: CourseSchedule)(implicit conn: Connection): Future[CourseSchedule]
    def update(courseSchedule: CourseSchedule)(implicit conn: Connection): Future[CourseSchedule]
    def delete(courseSchedule: CourseSchedule)(implicit conn: Connection): Future[Boolean]

    def isAnythingScheduledForUser(user: User, currentDay: LocalDate, currentTime: LocalTime)(implicit conn: Connection): Future[Boolean]
    def isProjectScheduledForUser(project: Project, user: User, currentDay: LocalDate, currentTime: LocalTime)(implicit conn: Connection): Future[Boolean]
  }
}
