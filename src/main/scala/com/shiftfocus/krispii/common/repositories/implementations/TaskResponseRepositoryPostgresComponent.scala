package com.shiftfocus.krispii.common.repositories

import com.github.mauricio.async.db.{RowData, Connection}
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import com.shiftfocus.krispii.lib.{UUID, ExceptionWriter}
import com.shiftfocus.krispii.common.models._
import play.api.Play.current
import play.api.cache.Cache
import play.api.Logger
import scala.concurrent.Future
import org.joda.time.DateTime
import com.shiftfocus.krispii.common.services.datasource.PostgresDB

trait TaskResponseRepositoryPostgresComponent extends TaskResponseRepositoryComponent {
  self: PostgresDB =>

  /**
   * with this trait's version of the ProjectRepository.
   */
  val taskResponseRepository: TaskResponseRepository = new TaskResponseRepositoryPSQL

  /**
   * A concrete implementation of the TaskResponseRepository.
   */
  private class TaskResponseRepositoryPSQL extends TaskResponseRepository {
    def fields = Seq("response", "is_complete")
    def table = "student_responses"
    def orderBy = "surname ASC, givenname ASC"
    val fieldsText = fields.mkString(", ")
    val questions = fields.map(_ => "?").mkString(", ")

    /*
     * SQL queries.
     *
     * Following are the postgres-compatible SQL statements to be used by the
     * class methods below.
     */

    val Insert = {
      val extraFields = fields.mkString(",")
      val questions = fields.map(_ => "?").mkString(",")
      s"""
        INSERT INTO $table (user_id, task_id, revision, version, status, created_at, updated_at, $extraFields)
        VALUES (?, ?, ?, 1, 1, ?, ?, $questions)
        RETURNING user_id, task_id, revision, version, created_at, updated_at, $fieldsText
      """
    }

    val Update = {
      val extraFields = fields.map(" " + _ + " = ? ").mkString(",")
      s"""
        UPDATE $table
        SET $extraFields , version = ?, updated_at = ?
        WHERE user_id = ?
          AND task_id = ?
          AND revision = ?
          AND version = ?
          AND status = 1
        RETURNING user_id, task_id, revision, version, created_at, updated_at, $fieldsText
      """
    }

    val SelectAll = s"""
      SELECT user_id, task_id, revision, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE status = 1
      ORDER BY id asc
    """

    val SelectRevisionsById = s"""
      SELECT user_id, task_id, revision, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE user_id = ?
        AND task_id = ?
        AND status = 1
      ORDER BY revision DESC
    """

    val SelectLatest = s"""
      SELECT user_id, task_id, revision, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE user_id = ?
        AND task_id = ?
        AND status = 1
      ORDER BY revision DESC
      LIMIT 1
    """

    val SelectLatestByProject = s"""
      SELECT user_id, task_id, revision, student_responses.version, student_responses.created_at, student_responses.updated_at, $fieldsText
      FROM $table, parts, projects, tasks
      WHERE user_id = ?
        AND projects.id = ?
        AND parts.id = tasks.part_id
        AND projects.id = parts.project_id
        AND student_responses.task_id = tasks.id
        AND student_responses.status = 1
        AND revision = (SELECT MAX(revision) FROM student_responses WHERE user_id= ? AND task_id=tasks.id)
    """

    val SelectOne = s"""
      SELECT user_id, task_id, revision, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE user_id = ?
        AND task_id = ?
        AND revision = ?
        AND status = 1
      LIMIT 1
    """

    val SelectByUserId = s"""
      SELECT sr1.user_id, sr1.task_id, sr1.revision, sr1.version, sr1.created_at, sr1.updated_at, sr1.response
      FROM student_responses sr1 LEFT JOIN student_responses sr2
        ON (sr1.user_id = sr2.user_id AND sr1.task_id < sr2.task_id)
      WHERE user_id = ?
        AND sr1.task_id IS NULL
        AND status = 1
      ORDER BY revision DESC
    """

    val SelectByTaskId = s"""
      SELECT sr1.user_id, sr1.task_id, sr1.revision, sr1.version, sr1.created_at, sr1.updated_at, sr1.response
      FROM student_responses sr1 LEFT JOIN student_responses sr2
        ON (sr1.task_id = sr2.task_id AND sr1.user_id < sr2.user_id)
      WHERE task_id = ?
        AND sr1.user_id IS NULL
        AND status = 1
      ORDER BY revision DESC
    """

    val Delete = s"""
      UPDATE $table
      SET status = 0
      WHERE user_id = ?
        AND task_id = ?
        AND version = ?
    """

    val DeleteByTask = s"""
      DELETE FROM $table
      WHERE task_id = ?
    """

    val Purge = s"""
      DELETE FROM $table
      WHERE user_id = ?
        AND task_id = ?
    """

    val ForceCompleteStepOne = s"""
      INSERT INTO $table (user_id, task_id, revision, version, status, created_at, updated_at, response, is_complete)
      SELECT users_sections.user_id, ? as task_id, 1, 1, 1, ? as created_at, ? as updated_at, '[no response, forced complete]' as response, 't' as is_complete
      FROM users_sections
      WHERE users_sections.section_id = ?
        AND NOT EXISTS (
          SELECT user_id, task_id
          FROM $table
          WHERE user_id = users_sections.user_id
            AND task_id = ?
        )
    """

    val ForceCompleteStepTwo = s"""
      UPDATE student_responses sr1
      SET is_complete = 't'
      FROM student_responses sr2
      INNER JOIN users_sections ON users_sections.user_id = sr2.user_id
      WHERE users_sections.section_id = ?
        AND sr2.task_id = ?
        AND sr1.user_id = sr2.user_id
        AND sr1.task_id = sr2.task_id
        AND sr2.status = 1
    """

    /**
     * Force complete a task for all students in a given section. This actually updates
     * the student_responses table to set all responses isComplete status to true.
     *
     * NB: this is a two-step procedure and should be run in a transaction.
     *
     * @param task the task to force to complete
     * @param section the section of students to force completion for
     */
    override def forceComplete(task: Task, section: Section)(implicit conn: Connection): Future[Boolean] = {
      for {
        result1 <- conn.sendPreparedStatement(ForceCompleteStepOne, Array[Any](task.id.bytes, new DateTime, new DateTime, section.id.bytes, task.id.bytes))
        result2 <- conn.sendPreparedStatement(ForceCompleteStepTwo, Array[Any](section.id.bytes, task.id.bytes))
      }
      yield {
        if (result1.rowsAffected > 0 && result2.rowsAffected > 0) {
          true
        }
        else {
          false
        }
      }
    }.recover {
      case exception => throw exception
    }

    /**
     * Find a specific revision of a task response.
     *
     * @param user the [[User]] whose response it is
     * @param task the [[Task]] this response is for
     * @param revision the revision of the response to fetch
     * @return an optional [[TaskResponse]] object
     */
    override def find(user: User, task: Task, revision: Long)(implicit conn: Connection): Future[Option[TaskResponse]] = {
      for {
        result <- conn.sendPreparedStatement(SelectOne, Array[Any](user.id.bytes, task.id.bytes, revision))
      }
      yield result.rows.get.headOption match {
        case Some(rowData) => Some(TaskResponse(rowData))
        case None => None
      }
    }.recover {
      case exception => throw exception
    }

    /**
     * Find the latest revision of a task response.
     *
     * @param user the [[User]] whose response it is
     * @param task the [[Task]] this response is for
     * @return an optional [[TaskResponse]] object
     */
    override def find(user: User, task: Task)(implicit conn: Connection): Future[Option[TaskResponse]] = {
      for {
        result <- conn.sendPreparedStatement(SelectLatest, Array[Any](user.id.bytes, task.id.bytes))
      }
      yield result.rows.get.headOption match {
        case Some(rowData) => Some(TaskResponse(rowData))
        case None => None
      }
    }.recover {
      case exception => throw exception
    }

    /**
     * List all revisions of a task response.
     *
     * @param user the [[User]] whose response it is
     * @param task the [[Task]] this response is for
     * @return an array of [[TaskResponse]] objects representing each revision
     */
    override def list(user: User, task: Task)(implicit conn: Connection): Future[IndexedSeq[TaskResponse]] = {
      for {
        queryResult <- conn.sendPreparedStatement(SelectRevisionsById, Array[Any](user.id.bytes, task.id.bytes))
      }
      yield queryResult.rows.get.map {
        item: RowData => TaskResponse(item)
      }
    }.recover {
      case exception => throw exception
    }

    /**
     * List a user's latest revisions for each task in a project.
     *
     * @param user the [[User]] whose response it is
     * @param project the [[Project]] this response is for
     * @return an array of [[TaskResponse]] objects representing each response
     */
    override def list(user: User, project: Project)(implicit conn: Connection): Future[IndexedSeq[TaskResponse]] = {
      for {
        queryResult <- conn.sendPreparedStatement(SelectLatestByProject, Array[Any](user.id.bytes, project.id.bytes, user.id.bytes))
      }
      yield queryResult.rows.get.map {
        item: RowData => TaskResponse(item)
      }
    }.recover {
      case exception => throw exception
    }

    /**
     * List a user's latest revisions for all task responses for all projects.
     *
     * @param user the [[User]] whose response it is
     * @return an array of [[TaskResponse]] objects representing each response
     */
    override def list(user: User)(implicit conn: Connection): Future[IndexedSeq[TaskResponse]] = {
      for {
        queryResult <- conn.sendPreparedStatement(SelectByUserId, Array[Any](user.id.bytes))
      } yield queryResult.rows.get.map {
        item: RowData => TaskResponse(item)
      }
    }.recover {
      case exception => throw exception
    }

    /**
     * List all users latest response revisions to a particular task.
     *
     * @param task the [[Task]] to list responses for
     * @return an array of [[TaskResponse]] objects representing each response
     */
    override def list(task: Task)(implicit conn: Connection): Future[IndexedSeq[TaskResponse]] = {
      for {
        queryResult <- conn.sendPreparedStatement(SelectByTaskId, Array[Any](task.id.bytes))
      }
      yield queryResult.rows.get.map {
        item: RowData => TaskResponse(item)
      }
    }.recover {
      case exception => throw exception
    }

    /**
     * Insert a new TaskResponse. Used to create new responses, and to insert new
     * revisions to existing responses. Note that the primary key comprises the user's ID,
     * the task's ID, and the revision number, so each revision is a separate entry in
     * the database.
     *
     * @param taskResponse the [[TaskResponse]] object to be inserted.
     * @return the newly created [[TaskResponse]]
     */
    override def insert(taskResponse: TaskResponse)(implicit conn: Connection): Future[TaskResponse] = {
      conn.sendPreparedStatement(Insert, Array(
        taskResponse.userId.bytes,
        taskResponse.taskId.bytes,
        taskResponse.revision,
        new DateTime,
        new DateTime,
        taskResponse.content,
        taskResponse.isComplete
      )).map {
        result => TaskResponse(result.rows.get.head)
      }
    }.recover {
      case exception => throw exception
    }

    /**
     * Update an existing [[TaskResponse]] revision. This always updates a specific
     * revision, since the primary key comprises user ID, task ID, and revision number.
     * Each revision has its own versioning w.r.t. optimistic offline lock.
     *
     * @param taskResponse the [[TaskResponse]] object to be inserted.
     * @return the newly created [[TaskResponse]]
     */
    override def update(taskResponse: TaskResponse)(implicit conn: Connection): Future[TaskResponse] = {
      conn.sendPreparedStatement(Update, Array(
        taskResponse.content,
        taskResponse.isComplete,
        (taskResponse.version + 1),
        new DateTime,
        taskResponse.userId.bytes,
        taskResponse.taskId.bytes,
        taskResponse.revision,
        taskResponse.version
      )).map {
        result => TaskResponse(result.rows.get.head)
      }
    }.recover {
      case exception => throw exception
    }

    /**
     * Deletes all revisions of a task response for a particular task.
     *
     * @param user the user whose task response will be deleted
     * @param task the task to delete the response for
     * @return
     */
    override def delete(taskResponse: TaskResponse)(implicit conn: Connection): Future[Boolean] = {
      for {
        queryResult <- conn.sendPreparedStatement(Purge, Array(
          taskResponse.userId.bytes,
          taskResponse.taskId.bytes,
          taskResponse.version
        ))
      }
      yield { queryResult.rowsAffected > 0 }
    }.recover {
      case exception => throw exception
    }

    /**
     * Deletes all revisions of a task response for a particular task.
     *
     * @param user the user whose task response will be deleted
     * @param task the task to delete the response for
     * @return
     */
    override def delete(task: Task)(implicit conn: Connection): Future[Boolean] = {
      for {
        queryResult <- conn.sendPreparedStatement(DeleteByTask, Array[Any](task.id.bytes))
      }
      yield { queryResult.rowsAffected > 0 }
    }.recover {
      case exception => throw exception
    }
  }
}
