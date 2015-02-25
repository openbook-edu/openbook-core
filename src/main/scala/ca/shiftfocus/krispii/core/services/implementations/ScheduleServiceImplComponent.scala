package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.fail.Fail
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services.datasource._
import ca.shiftfocus.uuid.UUID
import play.api.Logger
import org.joda.time.LocalTime
import org.joda.time.LocalDate
import scala.concurrent.Future
import scalaz.\/

trait ScheduleServiceImplComponent extends ScheduleServiceComponent {
  self: UserRepositoryComponent with
        CourseRepositoryComponent with
        CourseScheduleRepositoryComponent with
        ProjectRepositoryComponent with
        DB =>

  override val scheduleService: ScheduleService = new ScheduleServiceImpl

  private class ScheduleServiceImpl extends ScheduleService {

    /**
     * List all schedules for a specific course.
     *
     * @param id the UUID of the course to list for.
     * @return a vector of the given course's schedules
     */
    override def listByCourse(id: UUID): Future[\/[Fail, IndexedSeq[CourseSchedule]]] = {
      (for {
        course <- lift(courseRepository.find(id))
        schedules <- lift(courseScheduleRepository.list(course)(db.pool))
      }
      yield schedules).run
    }

    /**
     * Find a schedule by its id.
     *
     * @param id
     * @return
     */
    override def find(id: UUID): Future[\/[Fail, CourseSchedule]] = {
      courseScheduleRepository.find(id)(db.pool)
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
    override def create(courseId: UUID, day: LocalDate, startTime: LocalTime, endTime: LocalTime, description: String): Future[\/[Fail, CourseSchedule]] = {
      transactional { implicit connection =>
        (for {
          course <- lift(courseRepository.find(courseId))
          newSchedule = CourseSchedule(
              courseId = course.id,
              day = day,
              startTime = startTime,
              endTime = endTime,
              description = description
            )
          createdSchedule <- lift(courseScheduleRepository.insert(newSchedule))
        }
        yield createdSchedule).run
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
    override def update(id: UUID, version: Long,
                        courseId: Option[UUID],
                        day: Option[LocalDate],
                        startTime: Option[LocalTime],
                        endTime: Option[LocalTime],
                        description: Option[String]): Future[\/[Fail, CourseSchedule]] = {
      transactional { implicit connection =>
        (for {
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
        }
        yield updatedSchedule).run
      }
    }

    /**
     * Deletes a course schedule.
     *
     * @param id the ID of the schedule to delete
     * @param version the current version of the schedule for optimistic offline lock
     * @return a boolean indicating success or failure
     */
    override def delete(id: UUID, version: Long): Future[\/[Fail, CourseSchedule]] = {
      transactional { implicit connection =>
        (for {
          courseSchedule <- lift(courseScheduleRepository.find(id))
          toDelete = courseSchedule.copy(version = version)
          isDeleted <- lift(courseScheduleRepository.delete(toDelete))
        }
        yield isDeleted).run
      }
    }

    /**
     * Checks if any projects in any courses are scheduled for a particular user.
     *
     */
    override def isAnythingScheduledForUser(userId: UUID, currentDay: LocalDate, currentTime: LocalTime): Future[\/[Fail, Boolean]] = {
      (for {
        user <- lift(userRepository.find(userId))
        somethingScheduled <- lift(courseScheduleRepository.isAnythingScheduledForUser(user, currentDay, currentTime)(db.pool))
      }
      yield somethingScheduled).run
    }

    /**
     * Check if a project is scheduled in any of a user's courses.
     */
    override def isProjectScheduledForUser(projectSlug: String, userId: UUID, currentDay: LocalDate, currentTime: LocalTime): Future[\/[Fail, Boolean]] = {
      val fProject = projectRepository.find(projectSlug)
      val fUser = userRepository.find(userId)

      (for {
        project <- lift(fProject)
        user <- lift(fUser)
        projectScheduled <- lift(courseScheduleRepository.isProjectScheduledForUser(project, user, currentDay, currentTime)(db.pool))
      }
      yield projectScheduled).run
    }
  }
}
