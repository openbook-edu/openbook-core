package ca.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import org.joda.time.LocalTime
import org.joda.time.LocalDate
import scala.concurrent.Future

trait SectionScheduleRepositoryComponent {
  val sectionScheduleRepository: SectionScheduleRepository

  trait SectionScheduleRepository {
    def list(implicit conn: Connection): Future[IndexedSeq[SectionSchedule]]
    def list(section: Section)(implicit conn: Connection): Future[IndexedSeq[SectionSchedule]]
    def find(id: UUID)(implicit conn: Connection): Future[Option[SectionSchedule]]
    def insert(sectionSchedule: SectionSchedule)(implicit conn: Connection): Future[SectionSchedule]
    def update(sectionSchedule: SectionSchedule)(implicit conn: Connection): Future[SectionSchedule]
    def delete(sectionSchedule: SectionSchedule)(implicit conn: Connection): Future[Boolean]

    def isAnythingScheduledForUser(user: User, currentDay: LocalDate, currentTime: LocalTime)(implicit conn: Connection): Future[Boolean]
    def isProjectScheduledForUser(project: Project, user: User, currentDay: LocalDate, currentTime: LocalTime)(implicit conn: Connection): Future[Boolean]
  }
}
