package com.shiftfocus.krispii.common.services

import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import com.shiftfocus.krispii.common.models._
import com.shiftfocus.krispii.common.repositories._
import com.shiftfocus.krispii.common.services.datasource._
import com.shiftfocus.krispii.lib.UUID
import play.api.Logger
import org.joda.time.LocalTime
import org.joda.time.LocalDate
import scala.concurrent.Future

trait ScheduleServiceImplComponent extends ScheduleServiceComponent {
  self: UserRepositoryComponent with
        SectionRepositoryComponent with
        SectionScheduleRepositoryComponent with
        ProjectRepositoryComponent with
        DB =>

  override val scheduleService: ScheduleService = new ScheduleServiceImpl

  private class ScheduleServiceImpl extends ScheduleService {

    /**
     * List all section schedules.
     */
    override def list: Future[IndexedSeq[SectionSchedule]] = {
      sectionScheduleRepository.list(db.pool)
    }

    /**
     * List all schedules for a specific section.
     *
     * @param id the UUID of the section to list for.
     * @return a vector of the given section's schedules
     */
    override def listBySection(id: UUID): Future[IndexedSeq[SectionSchedule]] = {
      for {
        section <- sectionRepository.find(id).map(_.get)
        schedules <- sectionScheduleRepository.list(section)(db.pool)
      }
      yield schedules
    }.recover {
      case exception => throw exception
    }

    override def find(id: UUID): Future[Option[SectionSchedule]] = {
      sectionScheduleRepository.find(id)(db.pool).recover {
        case exception => throw exception
      }
    }

    /**
     * Create a new section schedule.
     *
     * @param sectionId the ID of the section this scheduled time belongs to
     * @param day the date on which this schedule is scheduled
     * @param startTime the time of day that the schedule starts
     * @param endTime the time of day that the schedule ends
     * @param description a brief description may be entered
     * @return the newly created section schedule
     */
    override def create(sectionId: UUID, day: LocalDate, startTime: LocalTime, endTime: LocalTime, description: String): Future[SectionSchedule] = {
      transactional { implicit connection =>
        for {
          section <- sectionRepository.find(sectionId).map(_.get)
          newSchedule <- sectionScheduleRepository.insert(SectionSchedule(
            sectionId = section.id,
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
     * Update an existing section schedule.
     *
     * @param sectionId the ID of the section this scheduled time belongs to
     * @param day the date on which this schedule is scheduled
     * @param startTime the time of day that the schedule starts
     * @param endTime the time of day that the schedule ends
     * @param description a brief description may be entered
     * @return the newly created section schedule
     */
    override def update(id: UUID, version: Long, values: Map[String, Any]): Future[SectionSchedule] = {
      transactional { implicit connection =>
        for {
          sectionSchedule <- sectionScheduleRepository.find(id).map(_.get)
          updatedSchedule <- sectionScheduleRepository.update(// Create the user object that will be updated into the database, copying
            // data fields if they were provided.
            sectionSchedule.copy(
              version = version,
              sectionId = values.get("sectionId") match {
                case Some(sectionId: UUID) => sectionId
                case _ => sectionSchedule.sectionId
              },
              day = values.get("day") match {
                case Some(day: LocalDate) => day
                case None => sectionSchedule.day
              },
              startTime = values.get("startTime") match {
                case Some(startTime: LocalTime) => startTime
                case None => sectionSchedule.startTime
              },
              endTime = values.get("endTime") match {
                case Some(endTime: LocalTime) => endTime
                case None => sectionSchedule.endTime
              },
              description = values.get("description") match {
                case Some(description: String) => description
                case None => sectionSchedule.description
              }
            ))
        }
        yield updatedSchedule
      }.recover {
        case exception => throw exception
      }
    }

    /**
     * Deletes a section schedule.
     *
     * @param id the ID of the schedule to delete
     * @param version the current version of the schedule for optimistic offline lock
     * @return a boolean indicating success or failure
     */
    override def delete(id: UUID, version: Long): Future[Boolean] = {
      transactional { implicit connection =>
        for {
          sectionSchedule <- sectionScheduleRepository.find(id).map(_.get.copy(version = version))
          isDeleted <- sectionScheduleRepository.delete(sectionSchedule)
        }
        yield isDeleted
      }.recover {
        case exception => throw exception
      }
    }

    /**
     * Checks if any projects in any sections are scheduled for a particular user.
     *
     */
    override def isAnythingScheduledForUser(userId: UUID, currentDay: LocalDate, currentTime: LocalTime): Future[Boolean] = {
      for {
        user <- userRepository.find(userId).map(_.get)
        somethingScheduled <- sectionScheduleRepository.isAnythingScheduledForUser(user, currentDay, currentTime)(db.pool)
      }
      yield somethingScheduled
    }.recover {
      case exception => throw exception
    }

    /**
     * Check if a project is scheduled in any of a user's sections.
     */
    override def isProjectScheduledForUser(projectSlug: String, userId: UUID, currentDay: LocalDate, currentTime: LocalTime): Future[Boolean] = {
      val fProject = projectRepository.find(projectSlug).map(_.get)
      val fUser = userRepository.find(userId).map(_.get)
      for {
        project <- fProject
        user <- fUser
        projectScheduled <- sectionScheduleRepository.isProjectScheduledForUser(project, user, currentDay, currentTime)(db.pool)
      }
      yield projectScheduled
    }.recover {
      case exception => throw exception
    }
  }
}
