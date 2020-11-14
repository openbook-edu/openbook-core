package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error._
import com.github.mauricio.async.db.Connection

import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services.datasource._
import java.util.UUID

import ca.shiftfocus.krispii.core.models.group._
import ca.shiftfocus.krispii.core.models.user.User
import org.joda.time.LocalTime
import org.joda.time.LocalDate

import scala.concurrent.Future
import scalaz.{-\/, \/, \/-}

class ScheduleServiceDefault(
    val db: DB,
    val authService: AuthService,
    val omsService: OmsService,
    val schoolService: SchoolService,
    val projectService: ProjectService,
    val groupScheduleRepository: GroupScheduleRepository,
    val groupScheduleExceptionRepository: GroupScheduleExceptionRepository
) extends ScheduleService {

  implicit def conn: Connection = db.pool

  /**
   * List all schedules for a specific group.
   *
   * @param group the course/exam/team to list for.
   * @return a vector of the given group's schedules
   */
  override def listSchedules(group: Group): Future[\/[ErrorUnion#Fail, IndexedSeq[GroupSchedule]]] = {
    groupScheduleRepository.list(group)
  }

  /**
   * List all schedules for a specific course.
   *
   * @param id the UUID of the course to list for.
   * @return a vector of the given course's schedules
   */
  override def listSchedulesByCourseId(id: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[GroupSchedule]]] = {
    for {
      course <- lift(schoolService.findCourse(id))
      schedules <- lift(groupScheduleRepository.list(course.asInstanceOf[Group]))
    } yield schedules
  }

  /**
   * List all schedules for a specific exam.
   *
   * @param id the UUID of the exam to list for.
   * @return a vector of the given exam's schedules
   */
  override def listSchedulesByExamId(id: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[GroupSchedule]]] = {
    for {
      group <- lift(omsService.findExam(id))
      schedules <- lift(groupScheduleRepository.list(group))
    } yield schedules
  }

  /**
   * List all schedules for a specific team.
   *
   * @param id the UUID of the team to list for.
   * @return a vector of the given exam's schedules
   */
  override def listSchedulesByTeamId(id: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[GroupSchedule]]] = {
    for {
      group <- lift(omsService.findExam(id))
      schedules <- lift(groupScheduleRepository.list(group))
    } yield schedules
  }

  /**
   * Find a schedule by its id.
   *
   * @param id
   * @return
   */
  override def findSchedule(id: UUID): Future[\/[ErrorUnion#Fail, GroupSchedule]] = {
    groupScheduleRepository.find(id)
  }

  /**
   * Create a new group schedule.
   *
   * @param group the course, exam or team for which a time is to be scheduled
   * @param startDay the date on which the scheduled period starts
   * @param endDay the date on which the scheduled period ends
   * @param startTime the time of startDay that the scheduled period starts
   * @param endTime the time of endDay that the scheduled period ends
   * @param description a brief description may be entered
   * @return the newly created group schedule
   */
  override def createSchedule(
    group: Group,
    startDay: LocalDate,
    endDay: LocalDate,
    startTime: LocalTime,
    endTime: LocalTime,
    description: String
  ): Future[\/[ErrorUnion#Fail, GroupSchedule]] = {
    val newSchedule = GroupSchedule(
      groupId = group.id,
      startDay = startDay,
      endDay = endDay,
      startTime = startTime,
      endTime = endTime,
      description = description
    )
    groupScheduleRepository.insert(newSchedule)
  }

  /**
   * Update an existing group schedule.
   *
   * @param groupId the ID of the group this scheduled time belongs to
   * @param startDay the date on which the scheduled period starts
   * @param endDay the date on which the scheduled period ends
   * @param startTime the time of startDay that the scheduled period starts
   * @param endTime the time of endDay that the scheduled period ends
   * @param description a brief description may be entered
   * @return the newly created group schedule
   */
  override def updateSchedule(id: UUID, version: Long,
    groupId: Option[UUID],
    startDay: Option[LocalDate],
    endDay: Option[LocalDate],
    startTime: Option[LocalTime],
    endTime: Option[LocalTime],
    description: Option[String]): Future[\/[ErrorUnion#Fail, GroupSchedule]] = {
    transactional { implicit conn =>
      for {
        groupSchedule <- lift(groupScheduleRepository.find(id))
        _ <- predicate(groupSchedule.version == version)(ServiceError.OfflineLockFail)
        toUpdate = groupSchedule.copy(
          version = version,
          groupId = groupId.getOrElse(groupSchedule.groupId),
          startDay = startDay.getOrElse(groupSchedule.startDay),
          endDay = startDay.getOrElse(groupSchedule.endDay),
          startTime = startTime.getOrElse(groupSchedule.startTime),
          endTime = endTime.getOrElse(groupSchedule.endTime),
          description = description.getOrElse(groupSchedule.description)
        )
        updatedSchedule <- lift(groupScheduleRepository.update(toUpdate))
      } yield updatedSchedule
    }
  }

  /**
   * Deletes a group schedule.
   *
   * @param id the ID of the schedule to delete
   * @param version the current version of the schedule for optimistic offline lock
   * @return a boolean indicating success or failure
   */
  override def deleteSchedule(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, GroupSchedule]] = {
    transactional { implicit conn =>
      for {
        groupSchedule <- lift(groupScheduleRepository.find(id))
        _ <- predicate(groupSchedule.version == version)(ServiceError.OfflineLockFail)
        toDelete = groupSchedule.copy(version = version)
        isDeleted <- lift(groupScheduleRepository.delete(toDelete))
      } yield isDeleted
    }
  }

  /**
   * Internal helper function to tell if a schedule or schedule exception is active
   * @param schedule the schedule or schedule exception to be queried
   * @param today a LocalDate to be queried
   * @param now a LocalTime to be queried
   * @return Boolean
   */
  def isScheduledNow(schedule: Schedule, today: LocalDate, now: LocalTime): Boolean =
    (today.equals(schedule.startDay) ||
      today.equals(schedule.endDay) ||
      (now.isBefore(schedule.endDay) && now.isAfter(schedule.startDay))) &&
      (now.equals(schedule.startTime) ||
        now.equals(schedule.endTime) ||
        (now.isBefore(schedule.endTime) && now.isAfter(schedule.startTime)))

  /**
   * Is a course/exam/team scheduled for the given day and time, irrespective of any user-specific exceptions?
   * @param group the course/exam/team
   * @param today LocalDate
   * @param now LocalTime
   * @return Boolean if the schedule can be looked up, or an error
   */
  override def isGroupScheduled(group: Group, today: LocalDate, now: LocalTime): Future[\/[ErrorUnion#Fail, Boolean]] =
    if (!group.enabled)
      Future successful \/-(false)
    else if (!group.schedulingEnabled)
      Future successful \/-(true)
    else listSchedules(group).map {
      case \/-(schedules) => \/-(schedules.exists({ schedule => isScheduledNow(schedule, today, now) }))
      case -\/(error) => -\/(error)
    }

  /**
   * Lists schedule exceptions for all students/scorers of one course/exam/team.
   *
   * @param group The group for which to list exceptions
   * @return
   */
  def listScheduleExceptions(group: Group): Future[\/[ErrorUnion#Fail, IndexedSeq[GroupScheduleException]]] =
    groupScheduleExceptionRepository.list(group)

  /**
   * Find a specific schedule exception object by ID.
   *
   * @param id the unique identifier
   * @return a future disjunction containing either a failure, or the schedule exception
   */
  def findScheduleException(id: UUID): Future[\/[ErrorUnion#Fail, GroupScheduleException]] = {
    groupScheduleExceptionRepository.find(id)
  }

  /**
   * TODO: adapt this to exams and teams when necessary
   * @param userId the student for whom an exception should be created
   * @param courseId the ID of the course this exception belongs to
   * @param startDay the date on which the scheduled period starts
   * @param endDay the date on which the scheduled period ends
   * @param startTime the time of startDay that the scheduled period starts
   * @param endTime the time of endDay that the scheduled period ends
   * @param description a brief description may be entered
   * @return
   */
  def createScheduleException(
    userId: UUID,
    courseId: UUID,
    startDay: LocalDate,
    endDay: LocalDate,
    startTime: LocalTime,
    endTime: LocalTime,
    description: String
  ): Future[\/[ErrorUnion#Fail, GroupScheduleException]] = {
    transactional { implicit conn =>
      for {
        course <- lift(schoolService.findCourse(courseId))
        user <- lift(authService.find(userId))
        usersInCourse <- lift(schoolService.listStudents(courseId))
        _ <- predicate(usersInCourse.contains(user))({
          ServiceError.BusinessLogicFail("User specified not in course")
        })
        newSchedule = GroupScheduleException(
          userId = user.id,
          groupId = course.id,
          startDay = startDay,
          endDay = endDay,
          startTime = startTime,
          endTime = endTime,
          reason = description
        )
        createdSchedule <- lift(groupScheduleExceptionRepository.insert(newSchedule))
      } yield createdSchedule
    }
  }

  /**
   * TODO: adapt this to exams and teams when necessary
   * @param userIds list of the student for whom exceptions should be created
   * @param courseId the ID of the course the exceptions are to be created for
   * @param startDay the date on which the scheduled period starts
   * @param endDay the date on which the scheduled period ends
   * @param startTime the time of startDay that the scheduled period starts
   * @param endTime the time of endDay that the scheduled period ends
   * @param description a brief description may be entered
   * @return
   */
  override def createScheduleExceptions(
    userIds: IndexedSeq[UUID],
    courseId: UUID,
    startDay: LocalDate,
    endDay: LocalDate,
    startTime: LocalTime,
    endTime: LocalTime,
    description: String,
    exceptionIds: IndexedSeq[UUID] = IndexedSeq.empty[UUID]
  ): Future[\/[ErrorUnion#Fail, IndexedSeq[GroupScheduleException]]] =
    transactional { implicit conn =>
      for {
        _ <- predicate(exceptionIds.isEmpty || exceptionIds.length == userIds.length)({
          ServiceError.BusinessLogicFail("Invalid number of exception ids")
        })
        course <- lift(schoolService.findCourse(courseId))
        usersSpecified <- lift(serializedT(userIds)(authService.find(_)))
        usersSpecifiedInd = usersSpecified.zipWithIndex
        usersInCourse <- lift(schoolService.listStudents(courseId))
        areUsersPresent = usersSpecified.forall((us: User) => usersInCourse.contains(us))
        _ <- predicate(areUsersPresent)({
          ServiceError.BusinessLogicFail("User(s) specified not in group")
        })

        newScheduleExceptions = usersSpecifiedInd.map {
          case (us, idx) => GroupScheduleException(
            id = exceptionIds(idx),
            userId = us.id,
            groupId = course.id,
            startDay = startDay,
            endDay = endDay,
            startTime = startTime,
            endTime = endTime,
            reason = description
          )
        }
        createdSchedules <- lift(serializedT(newScheduleExceptions)(groupScheduleExceptionRepository.insert))
      } yield createdSchedules
    }

  /**
   *
   * @param id the unique ID of an existing exception
   * @param version the version of the existing exception
   * @param startDay the date on which the scheduled period starts
   * @param endDay the date on which the scheduled period ends
   * @param startTime the time of startDay that the scheduled period starts
   * @param endTime the time of endDay that the scheduled period ends
   * @param description a brief description may be entered
   * @return
   */
  def updateScheduleException(id: UUID, version: Long, startDay: Option[LocalDate], endDay: Option[LocalDate], startTime: Option[LocalTime], endTime: Option[LocalTime],
    description: Option[String]): Future[\/[ErrorUnion#Fail, GroupScheduleException]] = {
    transactional { implicit conn =>
      for {
        existingException <- lift(groupScheduleExceptionRepository.find(id))
        _ <- predicate(existingException.version == version)(ServiceError.OfflineLockFail)
        toUpdate = existingException.copy(
          version = version,
          startDay = startDay.getOrElse(existingException.startDay),
          startTime = startTime.getOrElse(existingException.startTime),
          endTime = endTime.getOrElse(existingException.endTime),
          reason = description.getOrElse(existingException.reason)
        )
        updatedSchedule <- lift(groupScheduleExceptionRepository.update(toUpdate))
      } yield updatedSchedule
    }
  }

  /**
   * Delete a schedule exception
   * @param id The unique ID of the existing exception
   * @param version The version of the existing exception
   * @return
   */
  def deleteScheduleException(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, GroupScheduleException]] = {
    transactional { implicit conn =>
      for {
        groupScheduleException <- lift(groupScheduleExceptionRepository.find(id))
        _ <- predicate(groupScheduleException.version == version)(ServiceError.OfflineLockFail)
        toDelete = groupScheduleException.copy(version = version)
        isDeleted <- lift(groupScheduleExceptionRepository.delete(toDelete))
      } yield isDeleted
    }
  }

  /**
   * Is the user scheduled to take part in the course specified by its slug?
   * @param courseSlug unique name of the course
   * @param userId unique ID of the user
   * @param currentDay LocalDate
   * @param currentTime LocalTime
   * @return Boolean, if the schedule can be looked up, or an error
   */
  override def isCourseScheduledForUser(
    courseSlug: String,
    userId: UUID,
    currentDay: LocalDate,
    currentTime: LocalTime
  ): Future[\/[ErrorUnion#Fail, Boolean]] = {
    for {
      course <- lift(schoolService.findCourse(courseSlug))
      scheduled <- lift(isCourseScheduledForUser(course, userId, currentDay, currentTime))
    } yield scheduled
  }

  /**
   * Is the user scheduled to take part in the course specified by its unique ID?
   * @param courseId unique ID of the course
   * @param userId unique ID of the user
   * @param currentDay LocalDate
   * @param currentTime LocalTime
   * @return Boolean, if the schedule can be looked up, or an error
   */
  override def isCourseScheduledForUser(courseId: UUID, userId: UUID, currentDay: LocalDate, currentTime: LocalTime): Future[\/[ErrorUnion#Fail, Boolean]] = {
    for {
      course <- lift(schoolService.findCourse(courseId))
      scheduled <- lift(isCourseScheduledForUser(course, userId, currentDay, currentTime))
    } yield scheduled
  }

  override def isCourseScheduledForUser(course: Course, userId: UUID, today: LocalDate, now: LocalTime): Future[\/[ErrorUnion#Fail, Boolean]] = {
    val fCourses = schoolService.listCoursesByUser(userId)
    if (!course.schedulingEnabled) {
      for {
        courses <- lift(fCourses)
        _ <- predicate(courses.contains(course)) {
          ServiceError.BadPermissions("You must be a teacher or student of the relevant group to access this resource.")
        }
      } yield course.enabled
    }
    else {
      for {
        courses <- lift(fCourses)
        _ <- predicate(courses.contains(course)) {
          ServiceError.BadPermissions("You must be a teacher or student of the relevant group to access this resource.")
        }
        schedules <- lift(listSchedules(course))
        exceptions <- lift(listScheduleExceptions(course))
        userExceptions = exceptions.filter(_.userId == userId)
        blockedUserExceptions = userExceptions.filter(_.block == true)
        regUserExceptions = userExceptions.filter(_.block == false)
        scheduledForStudent = course.enabled &&
          schedules.exists({ schedule => isScheduledNow(schedule, today, now) }) ||
          !blockedUserExceptions.exists({ scheduleEx => isScheduledNow(scheduleEx, today, now) }) &&
          regUserExceptions.exists({ scheduleEx => isScheduledNow(scheduleEx, today, now) })
      } yield scheduledForStudent
    }
  }

}