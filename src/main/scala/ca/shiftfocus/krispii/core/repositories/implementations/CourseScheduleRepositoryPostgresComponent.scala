package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.fail._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.{ResultSet, RowData, Connection}
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import org.joda.time.DateTime
import org.joda.time.LocalTime
import org.joda.time.LocalDate
import scala.concurrent.Future
import scalaz.{\/-, \/, -\/}

trait CourseScheduleRepositoryPostgresComponent extends CourseScheduleRepositoryComponent with PostgresRepository {
  self: PostgresDB =>

  override val courseScheduleRepository: CourseScheduleRepository = new CourseScheduleRepositoryPSQL

  private class CourseScheduleRepositoryPSQL extends CourseScheduleRepository {
    val Fields = "id, version, created_at, updated_at, course_id, day, start_time, end_time, description"
    val Table = "course_schedules"
    val QMarks = "?, ?, ?, ?, ?"

    // User CRUD operations
    val SelectAll = s"""
      SELECT $Fields
      FROM $Table
      ORDER BY day ASC
    """

    val SelectOne = s"""
      SELECT $Fields
      FROM $Table
      WHERE id = ?
    """

    val Insert = {
      s"""
        INSERT INTO $Table ($Fields)
        VALUES ($QMarks)
        RETURNING $Fields
      """
    }

    val Update = {
      s"""
        UPDATE $Table
        SET course_id = ?, day = ?, start_time = ?, and_time = ?, description = ?, version = ?, updated_at = ?
        WHERE id = ?
          AND version = ?
        RETURNING $Fields
      """
    }

    val Delete =
      s"""
         |DELETE FROM $Table
         |WHERE id = ?
         |  AND version = ?
       """.stripMargin

    val SelectByCourseId = s"""
      SELECT $Fields
      FROM $Table
      WHERE course_id = ?
      ORDER BY day asc, start_time asc, end_time asc
    """

    val IsAnythingScheduledForUser = s"""
      SELECT $Table.id
      FROM $Table
      INNER JOIN users_courses
      ON users_courses.course_id = $Table.course_id
        AND users_courses.user_id = ?
      WHERE $Table.day = ?
        AND $Table.start_time <= ?
        AND $Table.end_time >= ?
      ORDER BY day asc, start_time asc, end_time asc
    """

    val IsProjectScheduledForUser = s"""
      SELECT $Table.id
      FROM $Table
      INNER JOIN users_courses
      ON users_courses.course_id = $Table.course_id
        AND users_courses.user_id = ?
      INNER JOIN projects
      ON projects.course_id = $Table.course_id
      WHERE projects.id = ?
        AND $Table.day = ?
        AND $Table.start_time <= ?
        AND $Table.end_time >= ?
    """

    /**
     *
     */
    override def isAnythingScheduledForUser(user: User, currentDay: LocalDate, currentTime: LocalTime)(implicit conn: Connection): Future[\/[Fail, Boolean]] = {
      val dayDT = currentDay.toDateTimeAtStartOfDay()
      val TimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), currentTime.getHourOfDay(), currentTime.getMinuteOfHour, currentTime.getSecondOfMinute())

      conn.sendPreparedStatement(IsAnythingScheduledForUser, Array(user.id.bytes, dayDT, TimeDT, TimeDT)).map {
        result =>
          if (result.rows.get.length > 0) \/-(true)
          else \/-(false)
      }.recover {
        case exception: Throwable => throw exception
      }
    }

    /**
     *
     */
    override def isProjectScheduledForUser(project: Project, user: User, currentDay: LocalDate, currentTime: LocalTime)(implicit conn: Connection): Future[\/[Fail, Boolean]] = {
      val dayDT = currentDay.toDateTimeAtStartOfDay()
      val TimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), currentTime.getHourOfDay(), currentTime.getMinuteOfHour, currentTime.getSecondOfMinute())

      conn.sendPreparedStatement(IsProjectScheduledForUser, Array(user.id.bytes, project.id.bytes, dayDT, TimeDT, TimeDT)).map {
        result =>
          if (result.rows.get.length > 0) \/-(true)
          else \/-(false)
      }.recover {
        case exception: Throwable => throw exception
      }
    }

    /**
     * List all schedules for a given class
     */
    override def list(course: Course)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[CourseSchedule]]] = {
      db.pool.sendPreparedStatement(SelectByCourseId, Array[Any](course.id.bytes)).map { queryResult =>
        buildEntityList(queryResult.rows, CourseSchedule.apply)
      }.recover {
        case exception: Throwable => throw exception
      }
    }

    /**
     * Find a single entry by ID.
     *
     * @param id the UUID to search for
     * @param conn An implicit connection object. Can be used in a transactional chain.
     * @return an optional task if one was found
     */
    override def find(id: UUID)(implicit conn: Connection): Future[\/[Fail, CourseSchedule]] = {
      db.pool.sendPreparedStatement(SelectOne, Array[Any](id.bytes)).map {
        result => buildEntity(result.rows, CourseSchedule.apply)
      }.recover {
        case exception: Throwable => throw exception
      }
    }

    /**
     * Create a new schedule.
     *
     * @param courseSchedule The course to be inserted
     * @return the new course
     */
    override def insert(courseSchedule: CourseSchedule)(implicit conn: Connection): Future[\/[Fail, CourseSchedule]] = {
      val dayDT = courseSchedule.day.toDateTimeAtStartOfDay()
      val startTimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), courseSchedule.startTime.getHourOfDay(), courseSchedule.startTime.getMinuteOfHour, courseSchedule.startTime.getSecondOfMinute())
      val endTimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), courseSchedule.endTime.getHourOfDay(), courseSchedule.endTime.getMinuteOfHour, courseSchedule.endTime.getSecondOfMinute())

      conn.sendPreparedStatement(Insert, Array(
        courseSchedule.id.bytes,
        new DateTime,
        new DateTime,
        courseSchedule.courseId.bytes,
        dayDT,
        startTimeDT,
        endTimeDT,
        courseSchedule.description
      )).map {
        result => buildEntity(result.rows, CourseSchedule.apply)
      }.recover {
        case exception: Throwable => throw exception
      }
    }

    /**
     * Update a schedule.
     *
     * @param courseSchedule The courseSchedule to be updated.
     * @return the updated course
     */
    override def update(courseSchedule: CourseSchedule)(implicit conn: Connection): Future[\/[Fail, CourseSchedule]] = {
      val dayDT = courseSchedule.day.toDateTimeAtStartOfDay()
      val startTimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), courseSchedule.startTime.getHourOfDay(), courseSchedule.startTime.getMinuteOfHour, courseSchedule.startTime.getSecondOfMinute())
      val endTimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), courseSchedule.endTime.getHourOfDay(), courseSchedule.endTime.getMinuteOfHour, courseSchedule.endTime.getSecondOfMinute())

      conn.sendPreparedStatement(Update, Array(
        courseSchedule.courseId.bytes,
        dayDT,
        startTimeDT,
        endTimeDT,
        courseSchedule.description,
        courseSchedule.version + 1,
        new DateTime,
        courseSchedule.id.bytes,
        courseSchedule.version
      )).map {
        result => buildEntity(result.rows, CourseSchedule.apply)
      }.recover {
        case exception: Throwable => throw exception
      }
    }

    /**
     * Delete a schedule.
     *
     * @param courseSchedule The course to delete.
     * @return A boolean indicating whether the operation was successful.
     */
    override def delete(courseSchedule: CourseSchedule)(implicit conn: Connection): Future[\/[Fail, CourseSchedule]] = {
      conn.sendPreparedStatement(Delete, Array(courseSchedule.id.bytes, courseSchedule.version)).map {
        result =>
          if (result.rowsAffected == 1) \/-(courseSchedule)
          else -\/(GenericFail("The query completed without error, but nothing was deleted."))
      }.recover {
        case exception: Throwable => throw exception
      }
    }
  }
}
