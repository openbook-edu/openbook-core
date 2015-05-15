package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.{ResultSet, RowData, Connection}
import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.DateTime
import scala.concurrent.Future
import scalacache.ScalaCache
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
      row("reason").asInstanceOf[String],
      row("created_at").asInstanceOf[DateTime],
      row("updated_at").asInstanceOf[DateTime]
    )
  }

  val Fields = "id, version, created_at, updated_at, user_id, course_id, day, start_time, end_time, reason"
  val Table = "course_schedule_exceptions"
  val QMarks = "?, ?, ?, ?, ?, ?, ?, ?, ?, ?"

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
       |SET user_id = ?, course_id = ?, day = ?, start_time = ?, end_time = ?, reason = ?, version = ?, updated_at = ?
       |WHERE id = ?
       |  AND version = ?
       |RETURNING $Fields
     """.stripMargin

  val Delete =
    s"""
       |DELETE FROM $Table
       |WHERE id = ?
       |  AND version = ?
       |RETURNING $Fields
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
  override def list(user: User, course: Course)(implicit conn: Connection, cache: ScalaCache): Future[\/[RepositoryError.Fail, IndexedSeq[CourseScheduleException]]] = {
    getCached[IndexedSeq[CourseScheduleException]](cacheExceptionsKey(course.id, user.id)).flatMap {
      case \/-(schedules) => Future successful \/-(schedules)
      case -\/(RepositoryError.NoResults) =>
        for {
          schedules <- lift(queryList(SelectForUserAndCourse, Array[Any](user.id.bytes, course.id.bytes)))
          _ <- lift(putCache[IndexedSeq[CourseScheduleException]](cacheExceptionsKey(course.id, user.id))(schedules, ttl))
        } yield schedules
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Find all schedule exceptions for a given course.
   */
  override def list(course: Course)(implicit conn: Connection, cache: ScalaCache): Future[\/[RepositoryError.Fail, IndexedSeq[CourseScheduleException]]] = {
    getCached[IndexedSeq[CourseScheduleException]](cacheExceptionsKey(course.id)).flatMap {
      case \/-(schedules) => Future successful \/-(schedules)
      case -\/(RepositoryError.NoResults) =>
        for {
          schedules <- lift(queryList(SelectForCourse, Seq[Any](course.id.bytes)))
          _ <- lift(putCache[IndexedSeq[CourseScheduleException]](cacheExceptionsKey(course.id))(schedules, ttl))
        } yield schedules
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Find a single entry by ID.
   *
   * @param id the UUID to search for
   * @param conn An implicit connection object. Can be used in a transactional chain.
   * @return an optional task if one was found
   */
  override def find(id: UUID)(implicit conn: Connection, cache: ScalaCache): Future[\/[RepositoryError.Fail, CourseScheduleException]] = {
    getCached[CourseScheduleException](cacheExceptionKey(id)).flatMap {
      case \/-(schedules) => Future successful \/-(schedules)
      case -\/(RepositoryError.NoResults) =>
        for {
          schedule <- lift(queryOne(SelectOne, Seq[Any](id.bytes)))
          _ <- lift(putCache[CourseScheduleException](cacheExceptionKey(id))(schedule, ttl))
        } yield schedule
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Create a new course schedule exception.
   *
   * @param courseScheduleException The courseScheduleException to be inserted
   * @return the new course
   */
  override def insert(courseScheduleException: CourseScheduleException)(implicit conn: Connection, cache: ScalaCache): Future[\/[RepositoryError.Fail, CourseScheduleException]] = {
    val dayDT = courseScheduleException.day.toDateTimeAtStartOfDay()
    val startTimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), courseScheduleException.startTime.getHourOfDay(), courseScheduleException.startTime.getMinuteOfHour, courseScheduleException.startTime.getSecondOfMinute())
    val endTimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), courseScheduleException.endTime.getHourOfDay(), courseScheduleException.endTime.getMinuteOfHour, courseScheduleException.endTime.getSecondOfMinute())

    for {
      inserted <- lift(queryOne(Insert, Array(
        courseScheduleException.id.bytes,
        1L,
        new DateTime,
        new DateTime,
        courseScheduleException.userId.bytes,
        courseScheduleException.courseId.bytes,
        dayDT,
        startTimeDT,
        endTimeDT,
        courseScheduleException.reason
      )))
      _ <- lift(removeCached(cacheExceptionsKey(inserted.courseId)))
      _ <- lift(removeCached(cacheExceptionsKey(inserted.courseId, inserted.userId)))
    } yield inserted
  }

  /**
   * Update a course.
   *
   * @param courseScheduleException The courseScheduleException to be updated.
   * @return the updated course
   */
  override def update(courseScheduleException: CourseScheduleException)(implicit conn: Connection, cache: ScalaCache): Future[\/[RepositoryError.Fail, CourseScheduleException]] = {
    val dayDT = courseScheduleException.day.toDateTimeAtStartOfDay()
    val startTimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), courseScheduleException.startTime.getHourOfDay(), courseScheduleException.startTime.getMinuteOfHour, courseScheduleException.startTime.getSecondOfMinute())
    val endTimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), courseScheduleException.endTime.getHourOfDay(), courseScheduleException.endTime.getMinuteOfHour, courseScheduleException.endTime.getSecondOfMinute())

    for {
      updated <- lift(queryOne(Update, Array(
        courseScheduleException.userId.bytes,
        courseScheduleException.courseId.bytes,
        dayDT,
        startTimeDT,
        endTimeDT,
        courseScheduleException.reason,
        courseScheduleException.version + 1,
        new DateTime,
        courseScheduleException.id.bytes,
        courseScheduleException.version
      )))
      _ <- lift(removeCached(cacheExceptionsKey(updated.courseId)))
      _ <- lift(removeCached(cacheExceptionsKey(updated.courseId, updated.userId)))
    } yield updated
  }

  /**
   * Delete a course.
   *
   * @param courseScheduleException The courseScheduleException to delete.
   * @return A boolean indicating whether the operation was successful.
   */
  override def delete(courseScheduleException: CourseScheduleException)(implicit conn: Connection, cache: ScalaCache): Future[\/[RepositoryError.Fail, CourseScheduleException]] = {
    for {
      deleted <- lift(queryOne(Delete, Array(courseScheduleException.id.bytes, courseScheduleException.version)))
      _ <- lift(removeCached(cacheExceptionsKey(deleted.courseId)))
      _ <- lift(removeCached(cacheExceptionsKey(deleted.courseId, deleted.userId)))
    } yield deleted
  }
}
