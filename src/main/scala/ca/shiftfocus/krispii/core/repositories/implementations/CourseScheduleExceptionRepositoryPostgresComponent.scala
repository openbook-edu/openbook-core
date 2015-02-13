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

trait CourseScheduleExceptionRepositoryPostgresComponent extends CourseScheduleExceptionRepositoryComponent {
  self: PostgresDB =>

  override val courseScheduleExceptionRepository: CourseScheduleExceptionRepository = new CourseScheduleExceptionRepositoryPSQL

  private class CourseScheduleExceptionRepositoryPSQL extends CourseScheduleExceptionRepository {
    def fields = Seq("user_id", "course_id", "day", "start_time", "end_time")
    def table = "course_schedule_exceptions"
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

    val SelectByCourseId = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE course_id = ?
      ORDER BY day asc, start_time asc, end_time asc
    """

    val SelectForUserAndCourse =
      s"""SELECT id, version, created_at, updated_at, $fieldsText
         |FROM $table
         |WHERE user_id = ?
         |  AND course_id = ?
         |ORDER BY id ASC
       """.stripMargin

    /**
     * Find all scheduling exceptions for one student in one course.
     *
     * @param conn An implicit connection object. Can be used in a transactional chain.
     * @return a vector of the returned courses
     */
    override def list(user: User, course: Course): Future[IndexedSeq[CourseScheduleException]] = {
      db.pool.sendPreparedStatement(SelectForUserAndCourse, Array[Any](user.id.bytes, course.id.bytes)).map { queryResult =>
        val scheduleList = queryResult.rows.get.map {
          item: RowData => CourseScheduleException(item)
        }
        scheduleList
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Find all schedule exceptions for a given course.
     */
    override def list(course: Course): Future[IndexedSeq[CourseScheduleException]] = {
      val cacheString = s"schedule.id_list.course[${course.id.string}]"
      db.pool.sendPreparedStatement(SelectByCourseId, Array[Any](course.id.bytes)).map { queryResult =>
        val scheduleList = queryResult.rows.get.map {
          item: RowData => CourseScheduleException(item)
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
    override def find(id: UUID): Future[Option[CourseScheduleException]] = {
      db.pool.sendPreparedStatement(SelectOne, Array[Any](id.bytes)).map { result =>
        result.rows.get.headOption match {
          case Some(rowData) => Some(CourseScheduleException(rowData))
          case None => None
        }
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Create a new course schedule exception.
     *
     * @param course The course to be inserted
     * @return the new course
     */
    override def insert(courseScheduleException: CourseScheduleException)(implicit conn: Connection): Future[CourseScheduleException] = {
      val dayDT = courseScheduleException.day.toDateTimeAtStartOfDay()
      val startTimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), courseScheduleException.startTime.getHourOfDay(), courseScheduleException.startTime.getMinuteOfHour, courseScheduleException.startTime.getSecondOfMinute())
      val endTimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), courseScheduleException.endTime.getHourOfDay(), courseScheduleException.endTime.getMinuteOfHour, courseScheduleException.endTime.getSecondOfMinute())

      conn.sendPreparedStatement(Insert, Array(
        courseScheduleException.id.bytes,
        new DateTime,
        new DateTime,
        courseScheduleException.userId.bytes,
        courseScheduleException.courseId.bytes,
        dayDT,
        startTimeDT,
        endTimeDT
      )).map {
        result => CourseScheduleException(result.rows.get.head)
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
    override def update(courseScheduleException: CourseScheduleException)(implicit conn: Connection): Future[CourseScheduleException] = {
      val dayDT = courseScheduleException.day.toDateTimeAtStartOfDay()
      val startTimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), courseScheduleException.startTime.getHourOfDay(), courseScheduleException.startTime.getMinuteOfHour, courseScheduleException.startTime.getSecondOfMinute())
      val endTimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), courseScheduleException.endTime.getHourOfDay(), courseScheduleException.endTime.getMinuteOfHour, courseScheduleException.endTime.getSecondOfMinute())

      conn.sendPreparedStatement(Update, Array(
        courseScheduleException.userId.bytes,
        courseScheduleException.courseId.bytes,
        dayDT,
        startTimeDT,
        endTimeDT,
        (courseScheduleException.version + 1),
        new DateTime,
        courseScheduleException.id.bytes,
        courseScheduleException.version
      )).map {
        result => CourseScheduleException(result.rows.get.head)
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
    override def delete(courseScheduleException: CourseScheduleException)(implicit conn: Connection): Future[Boolean] = {
      conn.sendPreparedStatement(Delete, Array(courseScheduleException.id.bytes, courseScheduleException.version)).map {
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
