package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.repositories.error.RepositoryError
import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import org.joda.time.LocalTime
import org.joda.time.LocalDate
import scala.concurrent.Future
import scalaz.{\/, EitherT}

trait CourseScheduleRepositoryComponent {
  val courseScheduleRepository: CourseScheduleRepository

  trait CourseScheduleRepository {
    def list(implicit conn: Connection): Future[\/[RepositoryError, IndexedSeq[CourseSchedule]]]
    def list(course: Course)(implicit conn: Connection): Future[\/[RepositoryError, IndexedSeq[CourseSchedule]]]

    def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError, CourseSchedule]]

    def insert(courseSchedule: CourseSchedule)(implicit conn: Connection): Future[\/[RepositoryError, CourseSchedule]]
    def update(courseSchedule: CourseSchedule)(implicit conn: Connection): Future[\/[RepositoryError, CourseSchedule]]
    def delete(courseSchedule: CourseSchedule)(implicit conn: Connection): Future[\/[RepositoryError, CourseSchedule]]

    def isAnythingScheduledForUser(user: User, currentDay: LocalDate, currentTime: LocalTime)(implicit conn: Connection): Future[\/[RepositoryError, Boolean]]
    def isProjectScheduledForUser(project: Project, user: User, currentDay: LocalDate, currentTime: LocalTime)(implicit conn: Connection): Future[\/[RepositoryError, Boolean]]

    protected def lift = EitherT.eitherT[Future, RepositoryError, CourseSchedule] _
    protected def liftList = EitherT.eitherT[Future, RepositoryError, IndexedSeq[CourseSchedule]] _
  }
}
