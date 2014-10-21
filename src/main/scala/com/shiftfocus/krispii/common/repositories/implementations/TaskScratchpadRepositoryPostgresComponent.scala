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

trait TaskScratchpadRepositoryPostgresComponent extends TaskScratchpadRepositoryComponent {
  self: PostgresDB =>

  override val taskScratchpadRepository: TaskScratchpadRepository = new TaskScratchpadRepositoryPSQL

  private class TaskScratchpadRepositoryPSQL extends TaskScratchpadRepository {
    def fields = Seq("notes")
    def table = "task_notes"
    def orderBy = "surname ASC, givenname ASC"
    val fieldsText = fields.mkString(", ")
    val questions = fields.map(_ => "?").mkString(", ")

    val Insert = {
      val extraFields = fields.mkString(",")
      val questions = fields.map(_ => "?").mkString(",")
      s"""
        INSERT INTO $table (user_id, task_id, revision, version, status, created_at, updated_at, $extraFields)
        VALUES (?, ?, ?, 1, 1, ?, ?, $questions)
        RETURNING user_id, task_id, revision, version, status, created_at, updated_at, $extraFields
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
        RETURNING user_id, task_id, revision, version, status, created_at, updated_at, $extraFields
      """
    }

    val SelectAll = s"""
      SELECT user_id, task_id, revision, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE status = 1
      ORDER BY id asc
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
      SELECT user_id, task_id, revision, task_notes.version, task_notes.created_at, task_notes.updated_at, $fieldsText
      FROM $table, parts, projects, tasks
      WHERE user_id = ?
        AND projects.id = ?
        AND parts.id = tasks.part_id
        AND projects.id = parts.project_id
        AND task_notes.task_id = tasks.id
        AND task_notes.status = 1
        AND revision = (SELECT MAX(revision) FROM task_notes WHERE user_id= ? AND task_id=tasks.id)
    """

    val SelectByUserId = s"""
      SELECT sr1.user_id, sr1.task_id, sr1.revision, sr1.version, sr1.created_at, sr1.updated_at, sr1.notes
      FROM task_notes sr1 LEFT JOIN task_notes sr2
        ON (sr1.user_id = sr2.user_id AND sr1.task_id < sr2.task_id)
      WHERE user_id = ?
        AND sr1.task_id IS NULL
        AND status = 1
      ORDER BY revision DESC
    """

    val SelectByTaskId = s"""
      SELECT sr1.user_id, sr1.task_id, sr1.revision, sr1.version, sr1.created_at, sr1.updated_at, sr1.notes
      FROM task_notes sr1 LEFT JOIN task_notes sr2
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
        AND version = ?
    """

    /**
     * Find a specific revision of a task scratchpad.
     *
     * @param user the [[User]] whose scratchpad it is
     * @param task the [[Task]] this scratchpad is for
     * @param revision the revision of the scratchpad to fetch
     * @return an optional [[TaskScratchpad]] object
     */
    override def find(user: User, task: Task, revision: Long)(implicit conn: Connection): Future[Option[TaskScratchpad]] = {
      for {
        result <- conn.sendPreparedStatement(SelectOne, Array[Any](user.id.bytes, task.id.bytes, revision))
      }
      yield result.rows.get.headOption match {
        case Some(rowData) => Some(TaskScratchpad(rowData))
        case None => None
      }
    }.recover {
      case exception => throw exception
    }

    /**
     * Find the latest revision of a task scratchpad.
     *
     * @param user the [[User]] whose scratchpad it is
     * @param task the [[Task]] this scratchpad is for
     * @return an optional [[TaskScratchpad]] object
     */
    override def find(user: User, task: Task)(implicit conn: Connection): Future[Option[TaskScratchpad]] = {
      for {
        result <- conn.sendPreparedStatement(SelectLatest, Array[Any](user.id.bytes, task.id.bytes))
      }
      yield result.rows.get.headOption match {
        case Some(rowData) => Some(TaskScratchpad(rowData))
        case None => None
      }
    }.recover {
      case exception => throw exception
    }

    /**
     * List all revisions of a task scratchpad.
     *
     * @param user the [[User]] whose scratchpad it is
     * @param task the [[Task]] this scratchpad is for
     * @return an array of [[TaskScratchpad]] objects representing each revision
     */
    override def list(user: User, task: Task)(implicit conn: Connection): Future[IndexedSeq[TaskScratchpad]] = {
      for {
        queryResult <- conn.sendPreparedStatement(SelectRevisionsById, Array[Any](user.id.bytes, task.id.bytes))
      }
      yield queryResult.rows.get.map {
        item: RowData => TaskScratchpad(item)
      }
    }.recover {
      case exception => throw exception
    }

    /**
     * List a user's latest revisions for each task in a project.
     *
     * @param user the [[User]] whose scratchpad it is
     * @param project the [[Project]] this scratchpad is for
     * @return an array of [[TaskScratchpad]] objects representing each scratchpad
     */
    override def list(user: User, project: Project)(implicit conn: Connection): Future[IndexedSeq[TaskScratchpad]] = {
      for {
        queryResult <- conn.sendPreparedStatement(SelectLatestByProject, Array[Any](user.id.bytes, project.id.bytes, user.id.bytes))
      }
      yield queryResult.rows.get.map {
        item: RowData => TaskScratchpad(item)
      }
    }.recover {
      case exception => throw exception
    }

    /**
     * List a user's latest revisions for all task scratchpads for all projects.
     *
     * @param user the [[User]] whose scratchpad it is
     * @return an array of [[TaskScratchpad]] objects representing each scratchpad
     */
    override def list(user: User)(implicit conn: Connection): Future[IndexedSeq[TaskScratchpad]] = {
      for {
        queryResult <- conn.sendPreparedStatement(SelectByUserId, Array[Any](user.id.bytes))
      } yield queryResult.rows.get.map {
        item: RowData => TaskScratchpad(item)
      }
    }.recover {
      case exception => throw exception
    }

    /**
     * List all users latest scratchpad revisions to a particular task.
     *
     * @param task the [[Task]] to list scratchpads for
     * @return an array of [[TaskScratchpad]] objects representing each scratchpad
     */
    override def list(task: Task)(implicit conn: Connection): Future[IndexedSeq[TaskScratchpad]] = {
      for {
        queryResult <- conn.sendPreparedStatement(SelectByTaskId, Array[Any](task.id.bytes))
      }
      yield queryResult.rows.get.map {
        item: RowData => TaskScratchpad(item)
      }
    }.recover {
      case exception => throw exception
    }

    /**
     * Insert a new TaskScratchpad. Used to create new scratchpads, and to insert new
     * revisions to existing pads. Note that the primary key comprises the user's ID,
     * the task's ID, and the revision number, so each revision is a separate entry in
     * the database.
     *
     * @param taskScratchpad the [[TaskScratchpad]] object to be inserted.
     * @return the newly created [[TaskScratchpad]]
     */
    override def insert(taskScratchpad: TaskScratchpad)(implicit conn: Connection): Future[TaskScratchpad] = {
      for {
        result <- conn.sendPreparedStatement(Insert, Array(
          taskScratchpad.userId.bytes,
          taskScratchpad.taskId.bytes,
          taskScratchpad.revision,
          new DateTime,
          new DateTime,
          taskScratchpad.content
        ))
      }
      yield TaskScratchpad(result.rows.get.head)
    }.recover {
      case exception => throw exception
    }

    /**
     * Update an existing [[TaskScratchpad]] revision. This always updates a specific
     * revision, since the primary key comprises user ID, task ID, and revision number.
     * Each revision has its own versioning w.r.t. optimistic offline lock.
     *
     * @param taskScratchpad the [[TaskScratchpad]] object to be inserted.
     * @return the newly created [[TaskScratchpad]]
     */
    override def update(taskScratchpad: TaskScratchpad)(implicit conn: Connection): Future[TaskScratchpad] = {
      for {
        result <- conn.sendPreparedStatement(Update, Array(
          taskScratchpad.content,
          (taskScratchpad.version + 1),
          new DateTime,
          taskScratchpad.userId.bytes,
          taskScratchpad.taskId.bytes,
          taskScratchpad.revision,
          taskScratchpad.version
        ))
      }
      yield TaskScratchpad(result.rows.get.head)
    }.recover {
      case exception => throw exception
    }

    /**
     * Deletes a task scratchpad.
     *
     * @param id
     * @return
     */
    override def delete(taskScratchpad: TaskScratchpad)(implicit conn: Connection): Future[Boolean] = {
      for {
        queryResult <- conn.sendPreparedStatement(Purge, Array(
          taskScratchpad.userId.bytes,
          taskScratchpad.taskId.bytes,
          taskScratchpad.version
        ))
      }
      yield { queryResult.rowsAffected > 0 }
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
