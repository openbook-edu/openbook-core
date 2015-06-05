package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import java.util.UUID
import com.github.mauricio.async.db.{ResultSet, RowData, Connection}
import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.DateTime
import org.joda.time.LocalTime
import org.joda.time.LocalDate
import scala.concurrent.Future
import scalacache.ScalaCache
import scalaz.{\/-, \/, -\/}

class CourseScheduleRepositoryPostgres extends CourseScheduleRepository with PostgresRepository[CourseSchedule] {

  override val entityName = "CourseSchedule"

  def constructor(row: RowData): CourseSchedule = {
    val day = row("day").asInstanceOf[DateTime]
    val startTime = row("start_time").asInstanceOf[DateTime]
    val endTime = row("end_time").asInstanceOf[DateTime]

    CourseSchedule(
      row("id").asInstanceOf[UUID],
      row("version").asInstanceOf[Long],
      row("course_id").asInstanceOf[UUID],
      day.toLocalDate(),
      new DateTime(day.getYear(), day.getMonthOfYear(), day.getDayOfMonth(), startTime.getHourOfDay(),
                   startTime.getMinuteOfHour, startTime.getSecondOfMinute()).toLocalTime(),
      new DateTime(day.getYear(), day.getMonthOfYear(), day.getDayOfMonth(), endTime.getHourOfDay(),
                   endTime.getMinuteOfHour, endTime.getSecondOfMinute()).toLocalTime(),
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
   * List all schedules for a given class
   */
  override def list(course: Course)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[CourseSchedule]]] = {
    cache.getCached[IndexedSeq[CourseSchedule]](cacheSchedulesKey(course.id)).flatMap {
      case \/-(schedules) => Future successful \/-(schedules)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          schedules <- lift(queryList(SelectByCourseId, Seq[Any](course.id)))
          _ <- lift(cache.putCache[IndexedSeq[CourseSchedule]](cacheSchedulesKey(course.id))(schedules, ttl))
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
  override def find(id: UUID)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, CourseSchedule]] = {
    cache.getCached[CourseSchedule](cacheScheduleKey(id)).flatMap {
      case \/-(schedules) => Future successful \/-(schedules)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          schedule <- lift(queryOne(SelectOne, Seq[Any](id)))
          _ <- lift(cache.putCache[CourseSchedule](cacheScheduleKey(id))(schedule, ttl))
        } yield schedule
      case -\/(error) => Future successful -\/(error)
    }

  }

  /**
   * Create a new schedule.
   *
   * @param courseSchedule The course to be inserted
   * @return the new course
   */
  override def insert(courseSchedule: CourseSchedule)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, CourseSchedule]] = {
    val dayDT = courseSchedule.day.toDateTimeAtStartOfDay()
    val startTimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), courseSchedule.startTime.getHourOfDay(),
                                   courseSchedule.startTime.getMinuteOfHour, courseSchedule.startTime.getSecondOfMinute())
    val endTimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), courseSchedule.endTime.getHourOfDay(),
                                 courseSchedule.endTime.getMinuteOfHour, courseSchedule.endTime.getSecondOfMinute())

    for {
      newSchedule <- lift(queryOne(Insert, Seq[Any](
        courseSchedule.id,
        1L,
        new DateTime,
        new DateTime,
        courseSchedule.courseId,
        dayDT,
        startTimeDT,
        endTimeDT,
        courseSchedule.description
      )))
      _ <- lift(cache.removeCached(cacheScheduleKey(newSchedule.id)))
      _ <- lift(cache.removeCached(cacheSchedulesKey(newSchedule.courseId)))
    } yield newSchedule
  }

  /**
   * Update a schedule.
   *
   * @param courseSchedule The courseSchedule to be updated.
   * @return the updated course
   */
  override def update(courseSchedule: CourseSchedule)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, CourseSchedule]] = {
    val dayDT = courseSchedule.day.toDateTimeAtStartOfDay()
    val startTimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), courseSchedule.startTime.getHourOfDay(),
                                   courseSchedule.startTime.getMinuteOfHour, courseSchedule.startTime.getSecondOfMinute())
    val endTimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), courseSchedule.endTime.getHourOfDay(),
                                 courseSchedule.endTime.getMinuteOfHour, courseSchedule.endTime.getSecondOfMinute())

    for {
      updated <- lift(queryOne(Update, Seq[Any](
        courseSchedule.courseId,
        dayDT,
        startTimeDT,
        endTimeDT,
        courseSchedule.description,
        courseSchedule.version + 1,
        new DateTime,
        courseSchedule.id,
        courseSchedule.version
      )))
      _ <- lift(cache.removeCached(cacheScheduleKey(updated.id)))
      _ <- lift(cache.removeCached(cacheSchedulesKey(updated.courseId)))
    } yield updated
  }

  /**
   * Delete a schedule.
   *
   * @param courseSchedule The course to delete.
   * @return A boolean indicating whether the operation was successful.
   */
  override def delete(courseSchedule: CourseSchedule)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, CourseSchedule]] = {
    for {
      deleted <- lift(queryOne(Delete, Seq[Any](courseSchedule.id, courseSchedule.version)))
      _ <- lift(cache.removeCached(cacheScheduleKey(deleted.id)))
      _ <- lift(cache.removeCached(cacheSchedulesKey(deleted.courseId)))
    } yield deleted
  }
}
