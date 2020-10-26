package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.group.Course
import com.github.mauricio.async.db.{Connection, RowData}
import org.joda.time.{DateTime, LocalDate, LocalTime}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.{-\/, \/, \/-}

class CourseScheduleRepositoryPostgres(
    val cacheRepository: CacheRepository
) extends CourseScheduleRepository with PostgresRepository[CourseSchedule] {

  override val entityName = "CourseSchedule"

  def constructor(row: RowData): CourseSchedule = {
    CourseSchedule(
      row("id").asInstanceOf[UUID],
      row("version").asInstanceOf[Long],
      row("course_id").asInstanceOf[UUID],
      row("day").asInstanceOf[LocalDate],
      row("start_time").asInstanceOf[LocalTime],
      row("end_time").asInstanceOf[LocalTime],
      row("description").asInstanceOf[String],
      row("created_at").asInstanceOf[DateTime],
      row("updated_at").asInstanceOf[DateTime]
    )
  }

  val Fields = "id, version, created_at, updated_at, course_id, day, start_time, end_time, description"
  val Table = "course_schedules"
  val QMarks = "?, ?, ?, ?, ?, ?, ?, ?, ?"
  val OrderBy = "day ASC"

  val SelectAll = s"""
    |SELECT $Fields
    |FROM $Table
    |ORDER BY $OrderBy
  """.stripMargin

  val SelectOne = s"""
    |SELECT $Fields
    |FROM $Table
    |WHERE id = ?
  """.stripMargin

  val Insert = {
    s"""
      |INSERT INTO $Table ($Fields)
      |VALUES ($QMarks)
      |RETURNING $Fields
    """.stripMargin
  }

  val Update = {
    s"""
      |UPDATE $Table
      |SET course_id = ?, day = ?, start_time = ?, end_time = ?, description = ?, version = ?, updated_at = ?
      |WHERE id = ?
      |  AND version = ?
      |RETURNING $Fields
    """.stripMargin
  }

  val Delete =
    s"""
       |DELETE FROM $Table
       |WHERE id = ?
       |  AND version = ?
       |RETURNING $Fields
     """.stripMargin

  val SelectByCourseId = s"""
    |SELECT $Fields
    |FROM $Table
    |WHERE course_id = ?
    |ORDER BY $OrderBy, start_time ASC, end_time ASC
  """.stripMargin

  /**
   * List all schedules for a given group
   */
  override def list(course: Course)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[CourseSchedule]]] = {
    cacheRepository.cacheSeqCourseSchedule.getCached(cacheSchedulesKey(course.id)).flatMap {
      case \/-(schedules) => Future successful \/-(schedules)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          schedules <- lift(queryList(SelectByCourseId, Seq[Any](course.id)))
          _ <- lift(cacheRepository.cacheSeqCourseSchedule.putCache(cacheSchedulesKey(course.id))(schedules, ttl))
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
  override def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, CourseSchedule]] = {
    cacheRepository.cacheCourseSchedule.getCached(cacheScheduleKey(id)).flatMap {
      case \/-(schedules) => Future successful \/-(schedules)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          schedule <- lift(queryOne(SelectOne, Seq[Any](id)))
          _ <- lift(cacheRepository.cacheCourseSchedule.putCache(cacheScheduleKey(id))(schedule, ttl))
        } yield schedule
      case -\/(error) => Future successful -\/(error)
    }

  }

  /**
   * Create a new schedule.
   *
   * @param courseSchedule The group to be inserted
   * @return the new group
   */
  override def insert(courseSchedule: CourseSchedule)(implicit conn: Connection): Future[\/[RepositoryError.Fail, CourseSchedule]] = {
    for {
      newSchedule <- lift(queryOne(Insert, Seq[Any](
        courseSchedule.id,
        1L,
        new DateTime,
        new DateTime,
        courseSchedule.courseId,
        courseSchedule.day,
        courseSchedule.startTime,
        courseSchedule.endTime,
        courseSchedule.description
      )))
      _ <- lift(cacheRepository.cacheCourseSchedule.removeCached(cacheScheduleKey(newSchedule.id)))
      _ <- lift(cacheRepository.cacheSeqCourseSchedule.removeCached(cacheSchedulesKey(newSchedule.courseId)))
    } yield newSchedule
  }

  /**
   * Update a schedule.
   *
   * @param courseSchedule The courseSchedule to be updated.
   * @return the updated group
   */
  override def update(courseSchedule: CourseSchedule)(implicit conn: Connection): Future[\/[RepositoryError.Fail, CourseSchedule]] = {
    for {
      updated <- lift(queryOne(Update, Seq[Any](
        courseSchedule.courseId,
        courseSchedule.day,
        courseSchedule.startTime,
        courseSchedule.endTime,
        courseSchedule.description,
        courseSchedule.version + 1,
        new DateTime,
        courseSchedule.id,
        courseSchedule.version
      )))
      _ <- lift(cacheRepository.cacheCourseSchedule.removeCached(cacheScheduleKey(updated.id)))
      _ <- lift(cacheRepository.cacheSeqCourseSchedule.removeCached(cacheSchedulesKey(updated.courseId)))
    } yield updated
  }

  /**
   * Delete a schedule.
   *
   * @param courseSchedule The group to delete.
   * @return A boolean indicating whether the operation was successful.
   */
  override def delete(courseSchedule: CourseSchedule)(implicit conn: Connection): Future[\/[RepositoryError.Fail, CourseSchedule]] = {
    for {
      deleted <- lift(queryOne(Delete, Seq[Any](courseSchedule.id, courseSchedule.version)))
      _ <- lift(cacheRepository.cacheCourseSchedule.removeCached(cacheScheduleKey(deleted.id)))
      _ <- lift(cacheRepository.cacheSeqCourseSchedule.removeCached(cacheSchedulesKey(deleted.courseId)))
    } yield deleted
  }
}
