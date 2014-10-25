package com.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.{RowData, Connection}
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import com.shiftfocus.krispii.core.lib.{UUID, ExceptionWriter}
import com.shiftfocus.krispii.core.models._
import play.api.Play.current

import play.api.Logger
import scala.concurrent.Future
import org.joda.time.DateTime
import com.shiftfocus.krispii.core.services.datasource.PostgresDB

trait CourseRepositoryPostgresComponent extends CourseRepositoryComponent {
  self: PostgresDB =>

  override val courseRepository: CourseRepository = new CourseRepositoryPSQL

  private class CourseRepositoryPSQL extends CourseRepository {
    def fields = Seq("name")
    def table = "courses"
    def orderBy = "name ASC"
    val fieldsText = fields.mkString(", ")
    val questions = fields.map(_ => "?").mkString(", ")

    val SelectAll = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE status = 1
      ORDER BY $orderBy
    """

    val SelectOne = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE id = ?
        AND status = 1
    """

    val Insert = {
      val extraFields = fields.mkString(",")
      val questions = fields.map(_ => "?").mkString(",")
      s"""
        INSERT INTO $table (id, version, status, created_at, updated_at, $extraFields)
        VALUES (?, 1, 1, ?, ?, $questions)
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
          AND status = 1
        RETURNING id, version, created_at, updated_at, $fieldsText
      """
    }

    val Delete = s"""
      DELETE FROM $table WHERE id = ? AND version = ?
    """

    /**
     * Find all courses.
     *
     * @param conn An implicit connection object. Can be used in a transactional chain.
     * @return a vector of the returned courses
     */
    override def list: Future[IndexedSeq[Course]] = {
      db.pool.sendQuery(SelectAll).map { queryResult =>
        queryResult.rows.get.map {
          item: RowData => Course(item)
        }
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
    override def find(id: UUID): Future[Option[Course]] = {
      db.pool.sendPreparedStatement(SelectOne, Array[Any](id.bytes)).map { result =>
        result.rows.get.headOption match {
          case Some(rowData) => Some(Course(rowData))
          case None => None
        }
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Create a new course.
     *
     * @param course The course to be inserted
     * @return the new course
     */
    override def insert(course: Course)(implicit conn: Connection): Future[Course] = {
      conn.sendPreparedStatement(Insert, Array(
        course.id.bytes,
        new DateTime,
        new DateTime,
        course.name
      )).map {
        result => Course(result.rows.get.head)
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
    override def update(course: Course)(implicit conn: Connection): Future[Course] = {
      conn.sendPreparedStatement(Update, Array(
        course.name,
        new DateTime,
        course.id,
        course.version
      )).map {
        result => Course(result.rows.get.head)
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
    override def delete(course: Course)(implicit conn: Connection): Future[Boolean] = {
      conn.sendPreparedStatement(Delete, Array(course.id.bytes, course.version)).map {
        result => (result.rowsAffected > 0)
      }.recover {
        case exception => {
          throw exception
        }
      }
    }
  }
}
