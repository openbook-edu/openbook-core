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

trait ClassScheduleExceptionRepositoryPostgresComponent extends ClassScheduleExceptionRepositoryComponent {
  self: PostgresDB =>

  override val sectionScheduleExceptionRepository: ClassScheduleExceptionRepository = new ClassScheduleExceptionRepositoryPSQL

  private class ClassScheduleExceptionRepositoryPSQL extends ClassScheduleExceptionRepository {
    def fields = Seq("class_id", "day", "start_time", "end_time")
    def table = "section_schedule_exceptions"
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

    val SelectForUserAndSection =
      s"""SELECT id, version, created_at, updated_at, $fieldsText
         |FROM $table
         |WHERE user_id = ?
         |  AND class_id = ?
         |ORDER BY id ASC
       """.stripMargin

    /**
     * Find all scheduling exceptions for one student in one section.
     *
     * @param conn An implicit connection object. Can be used in a transactional chain.
     * @return a vector of the returned courses
     */
    override def list(user: User, section: Class): Future[IndexedSeq[SectionScheduleException]] = {
      db.pool.sendQuery(SelectForUserAndSection).map { queryResult =>
        val scheduleList = queryResult.rows.get.map {
          item: RowData => SectionScheduleException(item)
        }
        scheduleList
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Find all schedule exceptions for a given section.
     */
    override def list(section: Class): Future[IndexedSeq[SectionScheduleException]] = {
      val cacheString = s"schedule.id_list.section[${section.id.string}]"
      db.pool.sendPreparedStatement(SelectBySectionId, Array[Any](section.id.bytes)).map { queryResult =>
        val scheduleList = queryResult.rows.get.map {
          item: RowData => SectionScheduleException(item)
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
    override def find(id: UUID): Future[Option[SectionScheduleException]] = {
      db.pool.sendPreparedStatement(SelectOne, Array[Any](id)).map { result =>
        result.rows.get.headOption match {
          case Some(rowData) => Option(SectionScheduleException(rowData))
          case None => None
        }
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Create a new section schedule exception.
     *
     * @param course The course to be inserted
     * @return the new course
     */
    override def insert(sectionScheduleException: SectionScheduleException)(implicit conn: Connection): Future[SectionScheduleException] = {
      val dayDT = sectionScheduleException.day.toDateTimeAtStartOfDay()
      val startTimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), sectionScheduleException.startTime.getHourOfDay(), sectionScheduleException.startTime.getMinuteOfHour, sectionScheduleException.startTime.getSecondOfMinute())
      val endTimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), sectionScheduleException.endTime.getHourOfDay(), sectionScheduleException.endTime.getMinuteOfHour, sectionScheduleException.endTime.getSecondOfMinute())

      conn.sendPreparedStatement(Insert, Array(
        sectionScheduleException.id.bytes,
        new DateTime,
        new DateTime,
        sectionScheduleException.classId.bytes,
        dayDT,
        startTimeDT,
        endTimeDT
      )).map {
        result => SectionScheduleException(result.rows.get.head)
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
    override def update(sectionScheduleException: SectionScheduleException)(implicit conn: Connection): Future[SectionScheduleException] = {
      val dayDT = sectionScheduleException.day.toDateTimeAtStartOfDay()
      val startTimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), sectionScheduleException.startTime.getHourOfDay(), sectionScheduleException.startTime.getMinuteOfHour, sectionScheduleException.startTime.getSecondOfMinute())
      val endTimeDT = new DateTime(dayDT.getYear(), dayDT.getMonthOfYear(), dayDT.getDayOfMonth(), sectionScheduleException.endTime.getHourOfDay(), sectionScheduleException.endTime.getMinuteOfHour, sectionScheduleException.endTime.getSecondOfMinute())

      conn.sendPreparedStatement(Update, Array(
        sectionScheduleException.classId.bytes,
        dayDT,
        startTimeDT,
        endTimeDT,
        new DateTime,
        sectionScheduleException.id.bytes,
        sectionScheduleException.version
      )).map {
        result => SectionScheduleException(result.rows.get.head)
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
    override def delete(sectionScheduleException: SectionScheduleException)(implicit conn: Connection): Future[Boolean] = {
      conn.sendPreparedStatement(Delete, Array(sectionScheduleException.id.bytes, sectionScheduleException.version)).map {
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
