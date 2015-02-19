package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.services.error.Fail
import ca.shiftfocus.uuid.UUID
import ca.shiftfocus.krispii.core.models._
import org.joda.time.LocalTime
import org.joda.time.LocalDate
import scala.concurrent.Future
import scalaz.\/

trait ScheduleServiceComponent {

  val scheduleService: ScheduleService

  trait ScheduleService {
    def list: Future[\/[Fail, IndexedSeq[CourseSchedule]]]
    def listByCourse(courseId: UUID): Future[\/[Fail, IndexedSeq[CourseSchedule]]]
    def find(id: UUID): Future[\/[Fail, CourseSchedule]]

    def create(courseId: UUID, day: LocalDate, startTime: LocalTime, endTime: LocalTime, description: String): Future[\/[Fail, CourseSchedule]]
    def update(id: UUID, version: Long, values: Map[String, Any]): Future[\/[Fail, CourseSchedule]]
    def delete(id: UUID, version: Long): Future[\/[Fail, CourseSchedule]]

    def isAnythingScheduledForUser(userId: UUID, currentDay: LocalDate, currentTime: LocalTime): Future[\/[Fail, Boolean]]
    def isProjectScheduledForUser(projectSlug: String, userId: UUID, currentDay: LocalDate, currentTime: LocalTime): Future[\/[Fail, Boolean]]
  }
}

