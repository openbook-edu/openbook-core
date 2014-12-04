package ca.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import org.joda.time.LocalTime
import org.joda.time.LocalDate
import scala.concurrent.Future

trait ClassScheduleRepositoryComponent {
  val sectionScheduleRepository: SectionScheduleRepository

  trait SectionScheduleRepository {
    def list(implicit conn: Connection): Future[IndexedSeq[ClassSchedule]]
    def list(section: Class)(implicit conn: Connection): Future[IndexedSeq[ClassSchedule]]
    def find(id: UUID)(implicit conn: Connection): Future[Option[ClassSchedule]]
    def insert(sectionSchedule: ClassSchedule)(implicit conn: Connection): Future[ClassSchedule]
    def update(sectionSchedule: ClassSchedule)(implicit conn: Connection): Future[ClassSchedule]
    def delete(sectionSchedule: ClassSchedule)(implicit conn: Connection): Future[Boolean]

    def isAnythingScheduledForUser(user: User, currentDay: LocalDate, currentTime: LocalTime)(implicit conn: Connection): Future[Boolean]
    def isProjectScheduledForUser(project: Project, user: User, currentDay: LocalDate, currentTime: LocalTime)(implicit conn: Connection): Future[Boolean]
  }
}
