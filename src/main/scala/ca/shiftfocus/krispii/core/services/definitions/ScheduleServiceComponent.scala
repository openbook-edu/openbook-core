package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.uuid.UUID
import ca.shiftfocus.krispii.core.models._
import org.joda.time.LocalTime
import org.joda.time.LocalDate
import scala.concurrent.Future

trait ScheduleServiceComponent {

  val scheduleService: ScheduleService

  trait ScheduleService {

    def list: Future[IndexedSeq[CourseSchedule]]
    def listByCourse(courseId: UUID): Future[IndexedSeq[CourseSchedule]]
    def find(id: UUID): Future[Option[CourseSchedule]]

    def create(courseId: UUID, day: LocalDate, startTime: LocalTime, endTime: LocalTime, description: String): Future[CourseSchedule]
    def update(id: UUID, version: Long, values: Map[String, Any]): Future[CourseSchedule]
    def delete(id: UUID, version: Long): Future[Boolean]

    def isAnythingScheduledForUser(userId: UUID, currentDay: LocalDate, currentTime: LocalTime): Future[Boolean]
    def isProjectScheduledForUser(projectSlug: String, userId: UUID, currentDay: LocalDate, currentTime: LocalTime): Future[Boolean]
  }
}

