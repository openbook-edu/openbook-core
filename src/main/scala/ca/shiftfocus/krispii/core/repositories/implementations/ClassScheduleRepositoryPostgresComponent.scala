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

trait ClassScheduleRepositoryPostgresComponent extends ClassScheduleRepositoryComponent {
  self: PostgresDB =>

  override val sectionScheduleRepository: SectionScheduleRepository = new SectionScheduleRepositoryPSQL

  private class SectionScheduleRepositoryPSQL extends SectionScheduleRepository {
    def fields = Seq("class_id", "day", "start_time", "end_time", "description")

    def table = "class_schedules"

    def orderBy = "created_at ASC"

    val fieldsText = fields.mkString(", ")
    val questions = fields.map(_ => "?").mkString(", ")

    // User CRUD operations
    val SelectAll = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM $table
      ORDER BY $orderBy
    """

    val SelectOne = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE id = ?
    """

    val Insert = {
      val extraFields = fields.mkString(",")
      val questions = fields.map(_ => "?").mkString(",")
      s"""
        INSERT INTO $table (id, version, created_at, updated_at, $extraFields)
        VALUES (?, 1, ?, ?, $questions)
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
        RETURNING id, version, created_at, updated_at, $fieldsText
      """
    }

    val Delete = s"""
      DELETE FROM $table WHERE id = ? AND version = ?
    """

    val SelectBySectionId = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE class_id = ?
      ORDER BY day asc, start_time asc, end_time asc
    """

    val IsAnythingScheduledForUser = s"""
      SELECT $table.id
      FROM $table
      INNER JOIN users_classes
      ON users_classes.class_id = $table.class_id
        AND users_classes.user_id = ?
      WHERE $table.day = ?
        AND $table.start_time <= ?
        AND $table.end_time >= ?
      ORDER BY day asc, start_time asc, end_time asc
    """

    val IsProjectScheduledForUser = s"""
      SELECT $table.id
      FROM $table
      INNER JOIN users_classes
      ON users_classes.class_id = $table.class_id
        AND users_classes.user_id = ?
      INNER JOIN projects
      ON projects.class_id = $table.class_id
      WHERE projects.id = ?
        AND $table.day = ?
        AND $table.start_time <= ?
        AND $table.end_time >= ?
    """

    /**
     *
     */
    override def isAnythingScheduledForUser(user: User, currentDay: LocalDate, currentTime: LocalTime)(implicit conn: Connection): Future[Boolean] = {
      val dayDT = currentDay.toDateTimeAtStartOfDay()
      val TimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), currentTime.getHourOfDay(), currentTime.getMinuteOfHour, currentTime.getSecondOfMinute())

      for {
        result <- conn.sendPreparedStatement(IsAnythingScheduledForUser, Array(user.id.bytes, dayDT, TimeDT, TimeDT))
      }
      yield (result.rows.get.length > 0)
    }

    /**
     *
     */
    override def isProjectScheduledForUser(project: Project, user: User, currentDay: LocalDate, currentTime: LocalTime)(implicit conn: Connection): Future[Boolean] = {
      val dayDT = currentDay.toDateTimeAtStartOfDay()
      val TimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), currentTime.getHourOfDay(), currentTime.getMinuteOfHour, currentTime.getSecondOfMinute())

      for {
        result <- conn.sendPreparedStatement(IsProjectScheduledForUser, Array(user.id.bytes, project.id.bytes, dayDT, TimeDT, TimeDT))
      }
      yield (result.rows.get.length > 0)
    }

    /**
     * List all schedules.
     *
     * @param conn An implicit connection object. Can be used in a transactional chain.
     * @return a vector of the returned courses
     */
    override def list(implicit conn: Connection): Future[IndexedSeq[ClassSchedule]] = {
      db.pool.sendQuery(SelectAll).map { queryResult =>
        val scheduleList = queryResult.rows.get.map {
          item: RowData => ClassSchedule(item)
        }
        scheduleList
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * List all schedules for a given class
     */
    override def list(section: Class)(implicit conn: Connection): Future[IndexedSeq[ClassSchedule]] = {
      db.pool.sendPreparedStatement(SelectBySectionId, Array[Any](section.id.bytes)).map { queryResult =>
        val scheduleList = queryResult.rows.get.map {
          item: RowData => ClassSchedule(item)
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
    override def find(id: UUID)(implicit conn: Connection): Future[Option[ClassSchedule]] = {
      db.pool.sendPreparedStatement(SelectOne, Array[Any](id.bytes)).map { result =>
        result.rows.get.headOption match {
          case Some(rowData) => Some(ClassSchedule(rowData))
          case None => None
        }
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Create a new schedule.
     *
     * @param course The course to be inserted
     * @return the new course
     */
    override def insert(sectionSchedule: ClassSchedule)(implicit conn: Connection): Future[ClassSchedule] = {
      val dayDT = sectionSchedule.day.toDateTimeAtStartOfDay()
      val startTimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), sectionSchedule.startTime.getHourOfDay(), sectionSchedule.startTime.getMinuteOfHour, sectionSchedule.startTime.getSecondOfMinute())
      val endTimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), sectionSchedule.endTime.getHourOfDay(), sectionSchedule.endTime.getMinuteOfHour, sectionSchedule.endTime.getSecondOfMinute())

      conn.sendPreparedStatement(Insert, Array(
        sectionSchedule.id.bytes,
        new DateTime,
        new DateTime,
        sectionSchedule.classId.bytes,
        dayDT,
        startTimeDT,
        endTimeDT,
        sectionSchedule.description
      )).map {
        result => ClassSchedule(result.rows.get.head)
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Update a schedule.
     *
     * @param course The course to be updated.
     * @return the updated course
     */
    override def update(sectionSchedule: ClassSchedule)(implicit conn: Connection): Future[ClassSchedule] = {
      val dayDT = sectionSchedule.day.toDateTimeAtStartOfDay()
      val startTimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), sectionSchedule.startTime.getHourOfDay(), sectionSchedule.startTime.getMinuteOfHour, sectionSchedule.startTime.getSecondOfMinute())
      val endTimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), sectionSchedule.endTime.getHourOfDay(), sectionSchedule.endTime.getMinuteOfHour, sectionSchedule.endTime.getSecondOfMinute())

      conn.sendPreparedStatement(Update, Array(
        sectionSchedule.classId.bytes,
        dayDT,
        startTimeDT,
        endTimeDT,
        sectionSchedule.description,
        (sectionSchedule.version + 1),
        new DateTime,
        sectionSchedule.id.bytes,
        sectionSchedule.version
      )).map {
        result => ClassSchedule(result.rows.get.head)
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Delete a schedule.
     *
     * @param course The course to delete.
     * @return A boolean indicating whether the operation was successful.
     */
    override def delete(sectionSchedule: ClassSchedule)(implicit conn: Connection): Future[Boolean] = {
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
