package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.repositories.CourseScheduleRepository
import ca.shiftfocus.uuid.UUID
import ca.shiftfocus.krispii.core.models._
import org.joda.time.LocalTime
import org.joda.time.LocalDate
import scala.concurrent.Future
import scalaz.\/

trait ScheduleService extends Service[ErrorUnion#Fail] {
  val authService: AuthService
  val schoolService: SchoolService
  val projectService: ProjectService
  val courseScheduleRepository: CourseScheduleRepository

  def listSchedulesByCourse(courseId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[CourseSchedule]]]
  def findSchedule(id: UUID): Future[\/[ErrorUnion#Fail, CourseSchedule]]

  def createSchedule(courseId: UUID, day: LocalDate, startTime: LocalTime, endTime: LocalTime, description: String): Future[\/[ErrorUnion#Fail, CourseSchedule]]
  def updateSchedule(id: UUID, version: Long, courseId: Option[UUID], day: Option[LocalDate], startTime: Option[LocalTime], endTime: Option[LocalTime], description: Option[String]): Future[\/[ErrorUnion#Fail, CourseSchedule]]
  def deleteSchedule(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, CourseSchedule]]

  def listScheduleExceptionsByCourse(courseId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[CourseScheduleException]]]
  def findScheduleException(id: UUID): Future[\/[ErrorUnion#Fail, CourseScheduleException]]

  def createScheduleException(userId: UUID, courseId: UUID, day: LocalDate, startTime: LocalTime, endTime: LocalTime, description: String): Future[\/[ErrorUnion#Fail, CourseScheduleException]]
  def updateScheduleException(id: UUID, version: Long, day: Option[LocalDate], startTime: Option[LocalTime], endTime: Option[LocalTime], description: Option[String]): Future[\/[ErrorUnion#Fail, CourseScheduleException]]
  def deleteScheduleException(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, CourseScheduleException]]

  def isAnythingScheduledForUser(userId: UUID, currentDay: LocalDate, currentTime: LocalTime): Future[\/[ErrorUnion#Fail, Boolean]]
  def isProjectScheduledForUser(projectSlug: String, userId: UUID, currentDay: LocalDate, currentTime: LocalTime): Future[\/[ErrorUnion#Fail, Boolean]]
}
