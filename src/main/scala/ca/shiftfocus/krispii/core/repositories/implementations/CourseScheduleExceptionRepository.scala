package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.{ResultSet, RowData, Connection}
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import org.joda.time.DateTime
import scala.concurrent.Future
import scalaz.{-\/, \/-, \/}

class CourseScheduleExceptionRepositoryPostgres(val userRepository: UserRepository,
                                                        val courseScheduleRepository: CourseScheduleRepository)
  extends CourseScheduleExceptionRepository with PostgresRepository[CourseScheduleException] {

  def constructor(row: RowData): CourseScheduleException = {
    val day = row("day").asInstanceOf[DateTime]
    val startTime = row("start_time").asInstanceOf[DateTime]
    val endTime = row("end_time").asInstanceOf[DateTime]

    CourseScheduleException(
      UUID(row("id").asInstanceOf[Array[Byte]]),
      UUID(row("user_id").asInstanceOf[Array[Byte]]),
      UUID(row("course_id").asInstanceOf[Array[Byte]]),
      row("version").asInstanceOf[Long],
      day.toLocalDate(),
      new DateTime(day.getYear(), day.getMonthOfYear(), day.getDayOfMonth(), startTime.getHourOfDay(), startTime.getMinuteOfHour, startTime.getSecondOfMinute()).toLocalTime(),
      new DateTime(day.getYear(), day.getMonthOfYear(), day.getDayOfMonth(), endTime.getHourOfDay(), endTime.getMinuteOfHour, endTime.getSecondOfMinute()).toLocalTime(),
      row("created_at").asInstanceOf[DateTime],
      row("updated_at").asInstanceOf[DateTime]
    )
  }

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
  override def list(user: User, course: Course)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[CourseScheduleException]]] = {
    queryList(SelectForUserAndCourse, Array[Any](user.id.bytes, course.id.bytes))
  }

  /**
   * Find all schedule exceptions for a given course.
   */
  override def list(course: Course)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[CourseScheduleException]]] = {
    queryList(SelectForCourse, Array[Any](course.id.bytes))
  }

  /**
   * Find a single entry by ID.
   *
   * @param id the UUID to search for
   * @param conn An implicit connection object. Can be used in a transactional chain.
   * @return an optional task if one was found
   */
  override def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, CourseScheduleException]] = {
    queryOne(SelectOne, Array[Any](id.bytes))
  }

  /**
   * Create a new course schedule exception.
   *
   * @param course The course to be inserted
   * @return the new course
   */
  override def insert(courseScheduleException: CourseScheduleException)(implicit conn: Connection): Future[\/[RepositoryError.Fail, CourseScheduleException]] = {
    val dayDT = courseScheduleException.day.toDateTimeAtStartOfDay()
    val startTimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), courseScheduleException.startTime.getHourOfDay(), courseScheduleException.startTime.getMinuteOfHour, courseScheduleException.startTime.getSecondOfMinute())
    val endTimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), courseScheduleException.endTime.getHourOfDay(), courseScheduleException.endTime.getMinuteOfHour, courseScheduleException.endTime.getSecondOfMinute())

    queryOne(Insert, Array(
      courseScheduleException.id.bytes,
      new DateTime,
      new DateTime,
      courseScheduleException.userId.bytes,
      courseScheduleException.courseId.bytes,
      dayDT,
      startTimeDT,
      endTimeDT
    ))
  }

  /**
   * Update a course.
   *
   * @param course The course to be updated.
   * @return the updated course
   */
  override def update(courseScheduleException: CourseScheduleException)(implicit conn: Connection): Future[\/[RepositoryError.Fail, CourseScheduleException]] = {
    val dayDT = courseScheduleException.day.toDateTimeAtStartOfDay()
    val startTimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), courseScheduleException.startTime.getHourOfDay(), courseScheduleException.startTime.getMinuteOfHour, courseScheduleException.startTime.getSecondOfMinute())
    val endTimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), courseScheduleException.endTime.getHourOfDay(), courseScheduleException.endTime.getMinuteOfHour, courseScheduleException.endTime.getSecondOfMinute())

    queryOne(Update, Array(
      courseScheduleException.userId.bytes,
      courseScheduleException.courseId.bytes,
      dayDT,
      startTimeDT,
      endTimeDT,
      courseScheduleException.version + 1,
      new DateTime,
      courseScheduleException.id.bytes,
      courseScheduleException.version
    ))
  }

  /**
   * Delete a course.
   *
   * @param course The course to delete.
   * @return A boolean indicating whether the operation was successful.
   */
  override def delete(courseScheduleException: CourseScheduleException)(implicit conn: Connection): Future[\/[RepositoryError.Fail, CourseScheduleException]] = {
    queryOne(Delete, Array(courseScheduleException.id.bytes, courseScheduleException.version))
  }
}
