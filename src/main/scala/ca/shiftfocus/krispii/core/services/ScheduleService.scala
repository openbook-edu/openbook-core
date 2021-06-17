package ca.shiftfocus.krispii.core.services

import java.util.UUID

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.group.{Course, Group}
import ca.shiftfocus.krispii.core.repositories.{GroupScheduleExceptionRepository, GroupScheduleRepository}
import org.joda.time.DateTime
import scalaz.\/

import scala.concurrent.Future

trait ScheduleService extends Service[ErrorUnion#Fail] {
  val authService: AuthService
  val omsService: OmsService
  val schoolService: SchoolService
  val projectService: ProjectService
  val groupScheduleRepository: GroupScheduleRepository
  val groupScheduleExceptionRepository: GroupScheduleExceptionRepository

  def listSchedules(group: Group): Future[\/[ErrorUnion#Fail, IndexedSeq[GroupSchedule]]]
  // list by IDs is only used in test code
  def listSchedulesByCourseId(courseId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[GroupSchedule]]]
  def listSchedulesByExamId(examId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[GroupSchedule]]]
  def listSchedulesByTeamId(teamId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[GroupSchedule]]]

  def findSchedule(id: UUID): Future[\/[ErrorUnion#Fail, GroupSchedule]]

  def createSchedule(group: Group, startDate: DateTime, endDate: DateTime, description: String): Future[\/[ErrorUnion#Fail, GroupSchedule]]
  def updateSchedule(id: UUID, version: Long, groupId: Option[UUID], startDate: Option[DateTime], endDate: Option[DateTime],
    description: Option[String]): Future[\/[ErrorUnion#Fail, GroupSchedule]]
  def deleteSchedule(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, GroupSchedule]]

  def isGroupScheduled(group: Group, current: DateTime): Future[\/[ErrorUnion#Fail, Boolean]]

  def listScheduleExceptions(group: Group): Future[\/[ErrorUnion#Fail, IndexedSeq[GroupScheduleException]]]
  def findScheduleException(id: UUID): Future[\/[ErrorUnion#Fail, GroupScheduleException]]
  def createScheduleException(userId: UUID, courseId: UUID, startDate: DateTime, endDate: DateTime,
    description: String): Future[\/[ErrorUnion#Fail, GroupScheduleException]]
  def createScheduleExceptions(userIds: IndexedSeq[UUID], courseId: UUID, startDate: DateTime, endDate: DateTime,
    description: String, exceptionIds: IndexedSeq[UUID] = IndexedSeq.empty[UUID]): Future[\/[ErrorUnion#Fail, IndexedSeq[GroupScheduleException]]]
  def updateScheduleException(id: UUID, version: Long, startDate: Option[DateTime], endDate: Option[DateTime],
    description: Option[String]): Future[\/[ErrorUnion#Fail, GroupScheduleException]]
  def deleteScheduleException(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, GroupScheduleException]]

  def isCourseScheduledForUser(courseSlug: String, userId: UUID, current: DateTime): Future[\/[ErrorUnion#Fail, Boolean]]
  def isCourseScheduledForUser(courseId: UUID, userId: UUID, current: DateTime): Future[\/[ErrorUnion#Fail, Boolean]]
  def isCourseScheduledForUser(course: Course, userId: UUID, current: DateTime): Future[\/[ErrorUnion#Fail, Boolean]]

}
