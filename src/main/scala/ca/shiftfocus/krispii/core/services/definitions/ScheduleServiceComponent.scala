package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.services.error.ServiceError
import ca.shiftfocus.uuid.UUID
import ca.shiftfocus.krispii.core.models._
import org.joda.time.LocalTime
import org.joda.time.LocalDate
import scala.concurrent.Future
import scalaz.\/

trait ScheduleServiceComponent {

  val scheduleService: ScheduleService

  trait ScheduleService {
    def list: Future[\/[ServiceError, IndexedSeq[CourseSchedule]]]
    def listByCourse(courseId: UUID): Future[\/[ServiceError, IndexedSeq[CourseSchedule]]]
    def find(id: UUID): Future[\/[ServiceError, CourseSchedule]]

    def create(courseId: UUID, day: LocalDate, startTime: LocalTime, endTime: LocalTime, description: String): Future[\/[ServiceError, CourseSchedule]]
    def update(id: UUID, version: Long, values: Map[String, Any]): Future[\/[ServiceError, CourseSchedule]]
    def delete(id: UUID, version: Long): Future[\/[ServiceError, CourseSchedule]]

    def isAnythingScheduledForUser(userId: UUID, currentDay: LocalDate, currentTime: LocalTime): Future[\/[ServiceError, Boolean]]
    def isProjectScheduledForUser(projectSlug: String, userId: UUID, currentDay: LocalDate, currentTime: LocalTime): Future[\/[ServiceError, Boolean]]
  }
}

