package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.course.Course
import com.github.mauricio.async.db.{Connection, RowData}
import org.joda.time.{DateTime, LocalDate, LocalTime}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.{-\/, \/, \/-}

class CourseScheduleExceptionRepositoryPostgres(
  val userRepository: UserRepository,
  val courseScheduleRepository: CourseScheduleRepository,
  val cacheRepository: CacheRepository
)
    extends CourseScheduleExceptionRepository with PostgresRepository[CourseScheduleException] {

  override val entityName = "CourseScheduleException"

  def constructor(row: RowData): CourseScheduleException = {
    CourseScheduleException(
      row("id").asInstanceOf[UUID],
      row("user_id").asInstanceOf[UUID],
      row("course_id").asInstanceOf[UUID],
      row("version").asInstanceOf[Long],
      row("day").asInstanceOf[LocalDate],
      row("start_time").asInstanceOf[LocalTime],
      row("end_time").asInstanceOf[LocalTime],
      row("reason").asInstanceOf[String],
      row("block").asInstanceOf[Boolean],
      row("created_at").asInstanceOf[DateTime],
      row("updated_at").asInstanceOf[DateTime]
    )
  }

  val Fields = "id, version, created_at, updated_at, user_id, course_id, day, start_time, end_time, reason, block"
  val Table = "course_schedule_exceptions"
  val QMarks = "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?"

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
       |SET user_id = ?, course_id = ?, day = ?, start_time = ?, end_time = ?, reason = ?, version = ?, updated_at = ?, block = ?
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
  override def list(user: User, course: Course) // format: OFF
                   (implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[CourseScheduleException]]] = { // format: ON
    cacheRepository.cacheSeqCourseScheduleException.getCached(cacheExceptionsKey(course.id, user.id)).flatMap {
      case \/-(schedules) => Future successful \/-(schedules)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          schedules <- lift(queryList(SelectForUserAndCourse, Array[Any](user.id, course.id)))
          _ <- lift(cacheRepository.cacheSeqCourseScheduleException.putCache(cacheExceptionsKey(course.id, user.id))(schedules, ttl))
        } yield schedules
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Find all schedule exceptions for a given course.
   */
  override def list(course: Course) // format: OFF
                   (implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[CourseScheduleException]]] = { // format: ON
    cacheRepository.cacheSeqCourseScheduleException.getCached(cacheExceptionsKey(course.id)).flatMap {
      case \/-(schedules) => Future successful \/-(schedules)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          schedules <- lift(queryList(SelectForCourse, Seq[Any](course.id)))
          _ <- lift(cacheRepository.cacheSeqCourseScheduleException.putCache(cacheExceptionsKey(course.id))(schedules, ttl))
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
  override def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, CourseScheduleException]] = {
    cacheRepository.cacheCourseScheduleException.getCached(cacheExceptionKey(id)).flatMap {
      case \/-(schedules) => Future successful \/-(schedules)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          schedule <- lift(queryOne(SelectOne, Seq[Any](id)))
          _ <- lift(cacheRepository.cacheCourseScheduleException.putCache(cacheExceptionKey(id))(schedule, ttl))
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
  override def insert(courseScheduleException: CourseScheduleException) // format: OFF
                     (implicit conn: Connection): Future[\/[RepositoryError.Fail, CourseScheduleException]] = { // format: ON
    for {
      inserted <- lift(queryOne(Insert, Array(
        courseScheduleException.id,
        1L,
        new DateTime,
        new DateTime,
        courseScheduleException.userId,
        courseScheduleException.courseId,
        courseScheduleException.day,
        courseScheduleException.startTime,
        courseScheduleException.endTime,
        courseScheduleException.reason,
        courseScheduleException.block
      )))
      _ <- lift(cacheRepository.cacheCourseScheduleException.removeCached(cacheExceptionKey(inserted.id)))
      _ <- lift(cacheRepository.cacheSeqCourseScheduleException.removeCached(cacheExceptionsKey(inserted.courseId)))
      _ <- lift(cacheRepository.cacheSeqCourseScheduleException.removeCached(cacheExceptionsKey(inserted.courseId, inserted.userId)))
    } yield inserted
  }

  /**
   * Update a course.
   *
   * @param courseScheduleException The courseScheduleException to be updated.
   * @return the updated course
   */
  override def update(courseScheduleException: CourseScheduleException) // format: OFF
                     (implicit conn: Connection): Future[\/[RepositoryError.Fail, CourseScheduleException]] = { // format: ON
    for {
      updated <- lift(queryOne(Update, Array(
        courseScheduleException.userId,
        courseScheduleException.courseId,
        courseScheduleException.day,
        courseScheduleException.startTime,
        courseScheduleException.endTime,
        courseScheduleException.reason,
        courseScheduleException.version + 1,
        new DateTime,
        courseScheduleException.block,
        courseScheduleException.id,
        courseScheduleException.version
      )))
      _ <- lift(cacheRepository.cacheCourseScheduleException.removeCached(cacheExceptionKey(updated.id)))
      _ <- lift(cacheRepository.cacheSeqCourseScheduleException.removeCached(cacheExceptionsKey(updated.courseId)))
      _ <- lift(cacheRepository.cacheSeqCourseScheduleException.removeCached(cacheExceptionsKey(updated.courseId, updated.userId)))
    } yield updated
  }

  /**
   * Delete a course.
   *
   * @param courseScheduleException The courseScheduleException to delete.
   * @return A boolean indicating whether the operation was successful.
   */
  override def delete(courseScheduleException: CourseScheduleException) // format: OFF
                     (implicit conn: Connection): Future[\/[RepositoryError.Fail, CourseScheduleException]] = { // format: ON
    for {
      deleted <- lift(queryOne(Delete, Array(courseScheduleException.id, courseScheduleException.version)))
      _ <- lift(cacheRepository.cacheCourseScheduleException.removeCached(cacheExceptionKey(deleted.id)))
      _ <- lift(cacheRepository.cacheSeqCourseScheduleException.removeCached(cacheExceptionsKey(deleted.courseId)))
      _ <- lift(cacheRepository.cacheSeqCourseScheduleException.removeCached(cacheExceptionsKey(deleted.courseId, deleted.userId)))
    } yield deleted
  }
}
