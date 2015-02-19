package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.fail._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.{ResultSet, RowData, Connection}
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import org.joda.time.DateTime
import scala.concurrent.Future
import scalaz.{-\/, \/-, \/}

trait CourseScheduleExceptionRepositoryPostgresComponent extends CourseScheduleExceptionRepositoryComponent {
  self: PostgresDB =>

  override val courseScheduleExceptionRepository: CourseScheduleExceptionRepository = new CourseScheduleExceptionRepositoryPSQL

  private class CourseScheduleExceptionRepositoryPSQL extends CourseScheduleExceptionRepository {
    val Fields = "id, version, created_at, updated_at, user_id, course_id, day, start_time, end_time"
    val Table = "course_schedule_exceptions"
    val QMarks = "?, ?, ?, ?, ?, ?, ?, ?, ?"

    // User CRUD operations
    val SelectAll =
      s"""
         |SELECT $Fields
         |FROM $Table
       """.stripMargin

    val SelectOne =
      s"""
         |SELECT $Fields
         |FROM $Table
         |WHERE id = ?
       """.stripMargin

    val Insert =
      s"""
         |INSERT INTO $Table ($Fields)
         |VALUES ($QMarks)
         |RETURNING $Fields
       """.stripMargin


    val Update =
      s"""
         |UPDATE $Table
         |SET user_id = ?, course_id = ?, day = ?, start_time = ?, end_time = ?, version = ?, updated_at = ?
         |WHERE id = ?
         |  AND version = ?
         |RETURNING $Fields
       """.stripMargin

    val Delete =
      s"""
         |DELETE FROM $Table WHERE id = ? AND version = ?
       """.stripMargin

    val SelectForCourse =
      s"""
         |SELECT $Fields
         |FROM $Table
         |WHERE course_id = ?
         |ORDER BY day asc, start_time asc, end_time asc
       """.stripMargin

    val SelectForUserAndCourse =
      s"""
         |SELECT $Fields
         |FROM $Table
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
    override def list(user: User, course: Course): Future[\/[Fail, IndexedSeq[CourseScheduleException]]] = {
      db.pool.sendPreparedStatement(SelectForUserAndCourse, Array[Any](user.id.bytes, course.id.bytes)).map { 
        result => buildCourseScheduleExceptionList(result.rows)
      }.recover {
        case exception => -\/(ExceptionalFail("Uncaught exception", exception))
      }
    }

    /**
     * Find all schedule exceptions for a given course.
     */
    override def list(course: Course): Future[\/[Fail, IndexedSeq[CourseScheduleException]]] = {
      db.pool.sendPreparedStatement(SelectForCourse, Array[Any](course.id.bytes)).map {
        result => buildCourseScheduleExceptionList(result.rows)
      }.recover {
        case exception => -\/(ExceptionalFail("Uncaught exception", exception))
      }
    }

    /**
     * Find a single entry by ID.
     *
     * @param id the UUID to search for
     * @param conn An implicit connection object. Can be used in a transactional chain.
     * @return an optional task if one was found
     */
    override def find(id: UUID): Future[\/[Fail, CourseScheduleException]] = {
      db.pool.sendPreparedStatement(SelectOne, Array[Any](id.bytes)).map {
        result => buildCourseScheduleException(result.rows)
      }.recover {
        case exception => -\/(ExceptionalFail("Uncaught exception", exception))
      }
    }

    /**
     * Create a new course schedule exception.
     *
     * @param course The course to be inserted
     * @return the new course
     */
    override def insert(courseScheduleException: CourseScheduleException)(implicit conn: Connection): Future[\/[Fail, CourseScheduleException]] = {
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
        result => buildCourseScheduleException(result.rows)
      }.recover {
        case exception => -\/(ExceptionalFail("Uncaught exception", exception))
      }
    }

    /**
     * Update a course.
     *
     * @param course The course to be updated.
     * @return the updated course
     */
    override def update(courseScheduleException: CourseScheduleException)(implicit conn: Connection): Future[\/[Fail, CourseScheduleException]] = {
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
        result => buildCourseScheduleException(result.rows)
      }.recover {
        case exception => -\/(ExceptionalFail("Uncaught exception", exception))
      }
    }

    /**
     * Delete a course.
     *
     * @param course The course to delete.
     * @return A boolean indicating whether the operation was successful.
     */
    override def delete(courseScheduleException: CourseScheduleException)(implicit conn: Connection): Future[\/[Fail, CourseScheduleException]] = {
      conn.sendPreparedStatement(Delete, Array(courseScheduleException.id.bytes, courseScheduleException.version)).map {
        result =>
          if (result.rowsAffected == 1) \/-(courseScheduleException)
          else -\/(NoResults("The query completed successfully, but nothing was deleted."))
      }.recover {
        case exception => -\/(ExceptionalFail("Uncaught exception", exception))
      }
    }

    /**
     * Build a TaskFeedback object from a database result.
     *
     * @param maybeResultSet the [[ResultSet]] from the database to use
     * @return
     */
    private def buildCourseScheduleException(maybeResultSet: Option[ResultSet]): \/[Fail, CourseScheduleException] = {
      try {
        maybeResultSet match {
          case Some(resultSet) => resultSet.headOption match {
            case Some(firstRow) => \/-(CourseScheduleException(firstRow))
            case None => -\/(NoResults("The query was successful but ResultSet was empty."))
          }
          case None => -\/(NoResults("The query was successful but no ResultSet was returned."))
        }
      }
      catch {
        case exception: NoSuchElementException => -\/(ExceptionalFail("Uncaught exception", exception))
      }
    }

    /**
     * Converts an optional result set into works list
     *
     * @param maybeResultSet the [[ResultSet]] from the database to use
     * @return
     */
    private def buildCourseScheduleExceptionList(maybeResultSet: Option[ResultSet]): \/[Fail, IndexedSeq[CourseScheduleException]] = {
      try {
        maybeResultSet match {
          case Some(resultSet) => \/-(resultSet.map(CourseScheduleException.apply))
          case None => -\/(NoResults("The query was successful but no ResultSet was returned."))
        }
      }
      catch {
        case exception: NoSuchElementException => -\/(ExceptionalFail(s"Invalid data: could not build a CourseScheduleException List from the rows returned.", exception))
      }
    }
  }
}
