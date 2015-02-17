package ca.shiftfocus.krispii.core.services

import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services.datasource._
import ca.shiftfocus.uuid.UUID
import play.api.Logger
import org.joda.time.LocalTime
import org.joda.time.LocalDate
import scala.concurrent.Future

trait ScheduleServiceImplComponent extends ScheduleServiceComponent {
  self: UserRepositoryComponent with
        CourseRepositoryComponent with
        CourseScheduleRepositoryComponent with
        ProjectRepositoryComponent with
        DB =>

  override val scheduleService: ScheduleService = new ScheduleServiceImpl

  private class ScheduleServiceImpl extends ScheduleService {

    /**
     * List all course schedules.
     */
    override def list: Future[IndexedSeq[CourseSchedule]] = {
      courseScheduleRepository.list(db.pool)
    }

    /**
     * List all schedules for a specific course.
     *
     * @param id the UUID of the course to list for.
     * @return a vector of the given course's schedules
     */
    override def listByCourse(id: UUID): Future[IndexedSeq[CourseSchedule]] = {
      for {
        course <- courseRepository.find(id).map(_.get)
        schedules <- courseScheduleRepository.list(course)(db.pool)
      }
      yield schedules
    }.recover {
      case exception => throw exception
    }

    override def find(id: UUID): Future[Option[CourseSchedule]] = {
      courseScheduleRepository.find(id)(db.pool).recover {
        case exception => throw exception
      }
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
    override def create(courseId: UUID, day: LocalDate, startTime: LocalTime, endTime: LocalTime, description: String): Future[CourseSchedule] = {
      transactional { implicit connection =>
        for {
          course <- courseRepository.find(courseId).map(_.get)
          newSchedule <- courseScheduleRepository.insert(CourseSchedule(
            courseId = course.id,
            day = day,
            startTime = startTime,
            endTime = endTime,
            description = description
          ))
        }
        yield newSchedule
      }.recover {
        case exception => throw exception
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
    override def update(id: UUID, version: Long, values: Map[String, Any]): Future[CourseSchedule] = {
      transactional { implicit connection =>
        for {
          courseSchedule <- courseScheduleRepository.find(id).map(_.get)
          updatedSchedule <- courseScheduleRepository.update(// Create the user object that will be updated into the database, copying
            // data fields if they were provided.
            courseSchedule.copy(
              version = version,
              courseId = values.get("courseId") match {
                case Some(courseId: UUID) => courseId
                case _ => courseSchedule.courseId
              },
              day = values.get("day") match {
                case Some(day: LocalDate) => day
                case _ => courseSchedule.day
              },
              startTime = values.get("startTime") match {
                case Some(startTime: LocalTime) => startTime
                case _ => courseSchedule.startTime
              },
              endTime = values.get("endTime") match {
                case Some(endTime: LocalTime) => endTime
                case _ => courseSchedule.endTime
              },
              description = values.get("description") match {
                case Some(description: String) => description
                case _ => courseSchedule.description
              }
            ))
        }
        yield updatedSchedule
      }.recover {
        case exception => throw exception
      }
    }

    /**
     * Deletes a course schedule.
     *
     * @param id the ID of the schedule to delete
     * @param version the current version of the schedule for optimistic offline lock
     * @return a boolean indicating success or failure
     */
    override def delete(id: UUID, version: Long): Future[Boolean] = {
      transactional { implicit connection =>
        for {
          courseSchedule <- courseScheduleRepository.find(id).map(_.get.copy(version = version))
          isDeleted <- courseScheduleRepository.delete(courseSchedule)
        }
        yield isDeleted
      }.recover {
        case exception => throw exception
      }
    }

    /**
     * Checks if any projects in any courses are scheduled for a particular user.
     *
     */
    override def isAnythingScheduledForUser(userId: UUID, currentDay: LocalDate, currentTime: LocalTime): Future[Boolean] = {
      for {
        user <- userRepository.find(userId).map(_.get)
        somethingScheduled <- courseScheduleRepository.isAnythingScheduledForUser(user, currentDay, currentTime)(db.pool)
      }
      yield somethingScheduled
    }.recover {
      case exception => throw exception
    }

    /**
     * Check if a project is scheduled in any of a user's courses.
     */
    override def isProjectScheduledForUser(projectSlug: String, userId: UUID, currentDay: LocalDate, currentTime: LocalTime): Future[Boolean] = {
      val fProject = projectRepository.find(projectSlug).map(_.get)
      val fUser = userRepository.find(userId).map(_.get)
      for {
        project <- fProject
        user <- fUser
        projectScheduled <- courseScheduleRepository.isProjectScheduledForUser(project, user, currentDay, currentTime)(db.pool)
      }
      yield projectScheduled
    }.recover {
      case exception => throw exception
    }
  }
}
