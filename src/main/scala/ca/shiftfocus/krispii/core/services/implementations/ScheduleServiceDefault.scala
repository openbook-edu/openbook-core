package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import com.github.mauricio.async.db.Connection
import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services.datasource._
import ca.shiftfocus.uuid.UUID

import org.joda.time.LocalTime
import org.joda.time.LocalDate
import scala.concurrent.Future
import scalacache.ScalaCache
import scalaz.\/

class ScheduleServiceDefault(val db: DB,
                             val scalaCache: ScalaCachePool,
                             val authService: AuthService,
                             val schoolService: SchoolService,
                             val projectService: ProjectService,
                             val courseScheduleRepository: CourseScheduleRepository,
                             val courseScheduleExceptionRepository: CourseScheduleExceptionRepository) extends ScheduleService {

  implicit def conn: Connection = db.pool
  implicit def cache: ScalaCachePool = scalaCache

  /**
   * List all schedules for a specific course.
   *
   * @param id the UUID of the course to list for.
   * @return a vector of the given course's schedules
   */
  override def listSchedulesByCourse(id: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[CourseSchedule]]] = {
    for {
      course <- lift(schoolService.findCourse(id))
      schedules <- lift(courseScheduleRepository.list(course))
    }
    yield schedules
  }

  /**
   * Find a schedule by its id.
   *
   * @param id
   * @return
   */
  override def findSchedule(id: UUID): Future[\/[ErrorUnion#Fail, CourseSchedule]] = {
    courseScheduleRepository.find(id)
  }

  /**
   * Create a new course schedule.
   *
   * @param courseId the ID of the course this scheduled time belongs to
   * @param day the date on which this schedule is scheduled
   * @param startTime the time of day that the schedule starts
   * @param endTime the time of day that the schedule ends
   * @param description a brief description may be entered
   * @return the newly created course schedule
   */
  override def createSchedule(courseId: UUID, day: LocalDate, startTime: LocalTime, endTime: LocalTime, description: String): Future[\/[ErrorUnion#Fail, CourseSchedule]] = {
    transactional { implicit conn =>
      for {
        course <- lift(schoolService.findCourse(courseId))
        newSchedule = CourseSchedule(
            courseId = course.id,
            day = day,
            startTime = startTime,
            endTime = endTime,
            description = description
          )
        createdSchedule <- lift(courseScheduleRepository.insert(newSchedule))
      } yield createdSchedule
    }
  }

  /**
   * Update an existing course schedule.
   *
   * @param courseId the ID of the course this scheduled time belongs to
   * @param day the date on which this schedule is scheduled
   * @param startTime the time of day that the schedule starts
   * @param endTime the time of day that the schedule ends
   * @param description a brief description may be entered
   * @return the newly created course schedule
   */
  override def updateSchedule(id: UUID, version: Long,
                      courseId: Option[UUID],
                      day: Option[LocalDate],
                      startTime: Option[LocalTime],
                      endTime: Option[LocalTime],
                      description: Option[String]): Future[\/[ErrorUnion#Fail, CourseSchedule]] = {
    transactional { implicit conn =>
      for {
        courseSchedule <- lift(courseScheduleRepository.find(id))
        toUpdate = courseSchedule.copy(
          version = version,
          courseId = courseId.getOrElse(courseSchedule.courseId),
          day = day.getOrElse(courseSchedule.day),
          startTime = startTime.getOrElse(courseSchedule.startTime),
          endTime = endTime.getOrElse(courseSchedule.endTime),
          description = description.getOrElse(courseSchedule.description)
        )
        updatedSchedule <- lift(courseScheduleRepository.update(toUpdate))
      } yield updatedSchedule
    }
  }

  /**
   * Deletes a course schedule.
   *
   * @param id the ID of the schedule to delete
   * @param version the current version of the schedule for optimistic offline lock
   * @return a boolean indicating success or failure
   */
  override def deleteSchedule(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, CourseSchedule]] = {
    transactional { implicit conn =>
      for {
        courseSchedule <- lift(courseScheduleRepository.find(id))
        toDelete = courseSchedule.copy(version = version)
        isDeleted <- lift(courseScheduleRepository.delete(toDelete))
      } yield isDeleted
    }
  }

  /**
   * Lists schedule exceptions for all students for one course.
   *
   * @param courseId
   * @return
   */
  def listScheduleExceptionsByCourse(courseId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[CourseScheduleException]]] = {
    for {
      course <- lift(schoolService.findCourse(courseId))
      schedules <- lift(courseScheduleExceptionRepository.list(course))
    } yield schedules
  }

  /**
   * Find a specific schedule exception object by ID.
   *
   * @param id the unique identifier
   * @return a future disjunction containing either a failure, or the schedule exception
   */
  def findScheduleException(id: UUID): Future[\/[ErrorUnion#Fail, CourseScheduleException]] = {
    courseScheduleExceptionRepository.find(id)
  }

  /**
   *
   * @param userId
   * @param courseId
   * @param day
   * @param startTime
   * @param endTime
   * @param description
   * @return
   */
  def createScheduleException(userId: UUID, courseId: UUID, day: LocalDate, startTime: LocalTime, endTime: LocalTime, description: String): Future[\/[ErrorUnion#Fail, CourseScheduleException]] = {
    transactional { implicit conn =>
      for {
        course <- lift(schoolService.findCourse(courseId))
        user <- lift(authService.find(userId))
        newSchedule = CourseScheduleException(
          userId = user.id,
          courseId = course.id,
          day = day,
          startTime = startTime,
          endTime = endTime,
          reason = description
        )
        createdSchedule <- lift(courseScheduleExceptionRepository.insert(newSchedule))
      } yield createdSchedule
    }
  }

  /**
   *
   * @param id
   * @param version
   * @param day
   * @param startTime
   * @param endTime
   * @param description
   * @return
   */
  def updateScheduleException(id: UUID, version: Long, day: Option[LocalDate], startTime: Option[LocalTime], endTime: Option[LocalTime], description: Option[String]): Future[\/[ErrorUnion#Fail, CourseScheduleException]] = {
    for {
      existingException <- lift(courseScheduleExceptionRepository.find(id))
      _ <- predicate (existingException.version == version) (RepositoryError.OfflineLockFail)
      toUpdate = existingException.copy(
        version = version,
        day = day.getOrElse(existingException.day),
        startTime = startTime.getOrElse(existingException.startTime),
        endTime = endTime.getOrElse(existingException.endTime),
        reason = description.getOrElse(existingException.reason)
      )
      updatedSchedule <- lift(courseScheduleExceptionRepository.update(toUpdate))
    }  yield updatedSchedule
  }

  /**
   *
   * @param id
   * @param version
   * @return
   */
  def deleteScheduleException(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, CourseScheduleException]] = {
    transactional { implicit conn =>
      for {
        courseScheduleException <- lift(courseScheduleExceptionRepository.find(id))
        toDelete = courseScheduleException.copy(version = version)
        isDeleted <- lift(courseScheduleExceptionRepository.delete(toDelete))
      } yield isDeleted
    }
  }

  /**
   *
   * @param courseSlug
   * @param userId
   * @param currentDay
   * @param currentTime
   * @return
   */
  override def isCourseScheduledForUser(courseSlug: String, userId: UUID, currentDay: LocalDate, currentTime: LocalTime): Future[\/[ErrorUnion#Fail, Boolean]] = {
    for {
      course <- lift( schoolService.findCourse(courseSlug))
      scheduled <- lift(isCourseScheduledForUser(course, userId, currentDay, currentTime))
    } yield scheduled
  }

  /**
   *
   * @param courseId
   * @param userId
   * @param currentDay
   * @param currentTime
   * @return
   */
  override def isCourseScheduledForUser(courseId: UUID, userId: UUID, currentDay: LocalDate, currentTime: LocalTime): Future[\/[ErrorUnion#Fail, Boolean]] = {
    for {
      course <- lift( schoolService.findCourse(courseId))
      scheduled <- lift(isCourseScheduledForUser(course, userId, currentDay, currentTime))
    } yield scheduled
  }

  override def isCourseScheduledForUser(course: Course, userId: UUID, today: LocalDate, now: LocalTime): Future[\/[ErrorUnion#Fail, Boolean]] = {
    val fCourses = schoolService.listCoursesByUser(userId)
    for {
      courses <- lift(fCourses)
      _ <- predicate (courses.contains(course)) (ServiceError.BadPermissions("You must be a teacher or student of the relevant course to access this resource."))
      fSchedules = listSchedulesByCourse(course.id)
      fExceptions = listScheduleExceptionsByCourse(course.id)
      schedules <- lift(fSchedules)
      exceptions <- lift(fExceptions)
      userExceptions = exceptions.filter(_.userId == userId)
      scheduledForStudent = {
        schedules.exists({ schedule =>
          today.equals(schedule.day) && (
            now.equals(schedule.startTime) ||
              now.equals(schedule.endTime) ||
              (now.isBefore(schedule.endTime) && now.isAfter(schedule.startTime))
            )
        }) ||
        userExceptions.exists({ schedule =>
          today.equals(schedule.day) && (
            now.equals(schedule.startTime) ||
              now.equals(schedule.endTime) ||
              (now.isBefore(schedule.endTime) && now.isAfter(schedule.startTime))
            )
        })
      }
    } yield scheduledForStudent
  }
}
