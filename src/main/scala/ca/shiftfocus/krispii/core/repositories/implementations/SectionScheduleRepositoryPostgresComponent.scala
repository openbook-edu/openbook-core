package ca.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.{RowData, Connection}
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib.ExceptionWriter
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import play.api.Play.current

import play.api.Logger
import scala.concurrent.Future
import org.joda.time.DateTime
import org.joda.time.LocalTime
import org.joda.time.LocalDate
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB

trait SectionScheduleRepositoryPostgresComponent extends SectionScheduleRepositoryComponent {
  self: PostgresDB =>

  override val sectionScheduleRepository: SectionScheduleRepository = new SectionScheduleRepositoryPSQL

  private class SectionScheduleRepositoryPSQL extends SectionScheduleRepository {
    def fields = Seq("section_id", "day", "start_time", "end_time", "description")

    def table = "section_schedules"

    def orderBy = "created_at ASC"

    val fieldsText = fields.mkString(", ")
    val questions = fields.map(_ => "?").mkString(", ")

    // User CRUD operations
    val SelectAll = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE status = 1
      ORDER BY $orderBy
    """

    val SelectOne = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE id = ?
        AND status = 1
    """

    val Insert = {
      val extraFields = fields.mkString(",")
      val questions = fields.map(_ => "?").mkString(",")
      s"""
        INSERT INTO $table (id, version, status, created_at, updated_at, $extraFields)
        VALUES (?, 1, 1, ?, ?, $questions)
        RETURNING id, version, created_at, updated_at, $fieldsText
      """
    }

    val Update = {
      val extraFields = fields.map(" " + _ + " = ? ").mkString(",")
      s"""
        UPDATE $table
        SET $extraFields , version = ?, updated_at = ?
        WHERE id = ?
          AND version = ?
          AND status = 1
        RETURNING id, version, created_at, updated_at, $fieldsText
      """
    }

    val Delete = s"""
      DELETE FROM $table WHERE id = ? AND version = ?
    """

    val SelectBySectionId = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE section_id = ?
        AND status = 1
      ORDER BY day asc, start_time asc, end_time asc
    """

    val IsAnythingScheduledForUser = s"""
      SELECT section_schedules.id
      FROM section_schedules
      INNER JOIN users_sections ON users_sections.section_id = section_schedules.section_id AND users_sections.user_id = ?
      WHERE section_schedules.day = ?
        AND section_schedules.start_time <= ?
        AND section_schedules.end_time >= ?
        AND section_schedules.status = 1
      ORDER BY day asc, start_time asc, end_time asc
    """

    val IsProjectScheduledForUser = s"""
      SELECT section_schedules.id
      FROM section_schedules
      INNER JOIN users_sections ON users_sections.section_id = section_schedules.section_id AND users_sections.user_id = ?
      INNER JOIN sections_projects ON sections_projects.section_id = section_schedules.section_id
      WHERE sections_projects.project_id = ?
        AND section_schedules.day = ?
        AND section_schedules.start_time <= ?
        AND section_schedules.end_time >= ?
        AND section_schedules.status = 1
    """

    /**
     *
     */
    override def isAnythingScheduledForUser(user: User, currentDay: LocalDate, currentTime: LocalTime)(implicit conn: Connection): Future[Boolean] = {
      for {
        result <- conn.sendPreparedStatement(IsAnythingScheduledForUser, Array(user.id.bytes, currentDay.toDateTimeAtStartOfDay(), currentTime.toDateTimeToday(), currentTime.toDateTimeToday()))
      }
      yield (result.rows.get.length > 0)
    }

    /**
     *
     */
    override def isProjectScheduledForUser(project: Project, user: User, currentDay: LocalDate, currentTime: LocalTime)(implicit conn: Connection): Future[Boolean] = {
      for {
        result <- conn.sendPreparedStatement(IsProjectScheduledForUser, Array(user.id.bytes, project.id.bytes, currentDay.toDateTimeAtStartOfDay(), currentTime.toDateTimeToday(), currentTime.toDateTimeToday()))
      }
      yield (result.rows.get.length > 0)
    }

    /**
     * Find all courses.
     *
     * @param conn An implicit connection object. Can be used in a transactional chain.
     * @return a vector of the returned courses
     */
    override def list(implicit conn: Connection): Future[IndexedSeq[SectionSchedule]] = {
      db.pool.sendQuery(SelectAll).map { queryResult =>
        val scheduleList = queryResult.rows.get.map {
          item: RowData => SectionSchedule(item)
        }
        scheduleList
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Find all schedules for a given section.
     */
    override def list(section: Section)(implicit conn: Connection): Future[IndexedSeq[SectionSchedule]] = {
      db.pool.sendPreparedStatement(SelectBySectionId, Array[Any](section.id.bytes)).map { queryResult =>
        val scheduleList = queryResult.rows.get.map {
          item: RowData => SectionSchedule(item)
        }
        scheduleList
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Find a single entry by ID.
     *
     * @param id the UUID to search for
     * @param conn An implicit connection object. Can be used in a transactional chain.
     * @return an optional task if one was found
     */
    override def find(id: UUID)(implicit conn: Connection): Future[Option[SectionSchedule]] = {
      db.pool.sendPreparedStatement(SelectOne, Array[Any](id)).map { result =>
        result.rows.get.headOption match {
          case Some(rowData) => Option(SectionSchedule(rowData))
          case None => None
        }
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Create a new course.
     *
     * @param course The course to be inserted
     * @return the new course
     */
    override def insert(sectionSchedule: SectionSchedule)(implicit conn: Connection): Future[SectionSchedule] = {
      val dayDT = sectionSchedule.day.toDateTimeAtStartOfDay()
      val startTimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), sectionSchedule.startTime.getHourOfDay(), sectionSchedule.startTime.getMinuteOfHour, sectionSchedule.startTime.getSecondOfMinute())
      val endTimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), sectionSchedule.endTime.getHourOfDay(), sectionSchedule.endTime.getMinuteOfHour, sectionSchedule.endTime.getSecondOfMinute())

      conn.sendPreparedStatement(Insert, Array(
        sectionSchedule.id.bytes,
        new DateTime,
        new DateTime,
        sectionSchedule.sectionId.bytes,
        dayDT,
        startTimeDT,
        endTimeDT,
        sectionSchedule.description
      )).map {
        result => SectionSchedule(result.rows.get.head)
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Update a course.
     *
     * @param course The course to be updated.
     * @return the updated course
     */
    override def update(sectionSchedule: SectionSchedule)(implicit conn: Connection): Future[SectionSchedule] = {
      val dayDT = sectionSchedule.day.toDateTimeAtStartOfDay()
      val startTimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), sectionSchedule.startTime.getHourOfDay(), sectionSchedule.startTime.getMinuteOfHour, sectionSchedule.startTime.getSecondOfMinute())
      val endTimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), sectionSchedule.endTime.getHourOfDay(), sectionSchedule.endTime.getMinuteOfHour, sectionSchedule.endTime.getSecondOfMinute())

      conn.sendPreparedStatement(Update, Array(
        sectionSchedule.sectionId.bytes,
        dayDT,
        startTimeDT,
        endTimeDT,
        sectionSchedule.description,
        new DateTime,
        sectionSchedule.id.bytes,
        sectionSchedule.version
      )).map {
        result => SectionSchedule(result.rows.get.head)
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Delete a course.
     *
     * @param course The course to delete.
     * @return A boolean indicating whether the operation was successful.
     */
    override def delete(sectionSchedule: SectionSchedule)(implicit conn: Connection): Future[Boolean] = {
      conn.sendPreparedStatement(Delete, Array(sectionSchedule.id.bytes, sectionSchedule.version)).map {
        result => {
          (result.rowsAffected > 0)
        }
      }.recover {
        case exception => {
          throw exception
        }
      }
    }
  }
}
