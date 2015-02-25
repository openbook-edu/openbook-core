package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.fail._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks.Task
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import com.github.mauricio.async.db.{ResultSet, RowData, Connection}
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import org.joda.time.DateTime
import scala.concurrent.Future
import scalaz.{\/, \/-, -\/}

trait TaskScratchpadRepositoryPostgresComponent extends TaskScratchpadRepositoryComponent {
  self: UserRepositoryComponent with
        ProjectRepositoryComponent with
        TaskRepositoryComponent with
        PostgresDB =>

  override val taskScratchpadRepository: TaskScratchpadRepository = new TaskScratchpadRepositoryPSQL

  private class TaskScratchpadRepositoryPSQL extends TaskScratchpadRepository {
    val Table = "task_feedbacks"
    val Fields = "user_id, task_id, version, document_id, created_at, updated_at"
    val QMarks = "?, ?, ?, ?, ?, ?"

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
         |SET document_id = ?, version = ?, updated_at = ?
         |WHERE user_id = ?
         |  AND task_id = ?
         |  AND version = ?
         |RETURNING $Fields
       """.stripMargin
    }

    val SelectAll =
      s"""
         |SELECT $Fields
         |FROM $Table
       """.stripMargin

    val SelectOne =
      s"""
         |SELECT $Fields
         |FROM $Table
         |WHERE user_id = ?
         |  AND task_id = ?
         |  AND revision = ?
         |LIMIT 1
       """.stripMargin

    val SelectAllForUserAndTask =
      s"""
         |SELECT $Fields
         |FROM $Table
         |WHERE user_id = ?
         |  AND task_id = ?
       """.stripMargin

    val SelectAllForProject = s"""
      |SELECT $Fields
      |FROM $Table, parts, projects, tasks
      |WHERE user_id = ?
      |  AND projects.id = ?
      |  AND parts.id = tasks.part_id
      |  AND projects.id = parts.project_id
      |  AND task_notes.task_id = tasks.id
    """.stripMargin

    val SelectAllForUser =
      s"""
         |SELECT $Fields
         |FROM $Table
         |WHERE user_id = ?
       """.stripMargin

    val SelectAllForTask =
      s"""
         |SELECT $Fields
         |FROM $Table
         |WHERE task_id = ?
       """.stripMargin

    val DeleteOne =
      s"""
         |DELETE FROM $Table
         |WHERE user_id = ?
         |  AND task_id = ?
         |  AND version = ?
       """.stripMargin

    val DeleteAllForTask =
      s"""
         |DELETE FROM $Table
         |WHERE task_id = ?
       """.stripMargin

    /**
     * List all revisions of a task scratchpad.
     *
     * @param user the [[User]] whose scratchpad it is
     * @param task the [[Task]] this scratchpad is for
     * @return an array of [[TaskScratchpad]] objects representing each revision
     */
    override def list(user: User, task: Task)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[TaskScratchpad]]] = {
      conn.sendPreparedStatement(
        SelectAllForUserAndTask,
        Array[Any](user.id.bytes, task.id.bytes)
      ).map {
        result => buildTaskScratchpadList(result.rows)
      }.recover {
        case exception => -\/(ExceptionalFail("Uncaught exception", exception))
      }
    }

    /**
     * List a user's latest revisions for each task in a project.
     *
     * @param user the [[User]] whose scratchpad it is
     * @param project the [[Project]] this scratchpad is for
     * @return an array of [[TaskScratchpad]] objects representing each scratchpad
     */
    override def list(user: User, project: Project)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[TaskScratchpad]]] = {
      conn.sendPreparedStatement(
        SelectAllForProject,
        Array[Any](user.id.bytes, project.id.bytes)
      ).map {
        result => buildTaskScratchpadList(result.rows)
      }.recover {
        case exception => -\/(ExceptionalFail("Uncaught exception", exception))
      }
    }

    /**
     * List a user's latest revisions for all task scratchpads for all projects.
     *
     * @param user the [[User]] whose scratchpad it is
     * @return an array of [[TaskScratchpad]] objects representing each scratchpad
     */
    override def list(user: User)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[TaskScratchpad]]] = {
      conn.sendPreparedStatement(
        SelectAllForUser,
        Array[Any](user.id.bytes)
      ).map {
        result => buildTaskScratchpadList(result.rows)
      }.recover {
        case exception => -\/(ExceptionalFail("Uncaught exception", exception))
      }
    }

    /**
     * List all users latest scratchpad revisions to a particular task.
     *
     * @param task the [[Task]] to list scratchpads for
     * @return an array of [[TaskScratchpad]] objects representing each scratchpad
     */
    override def list(task: Task)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[TaskScratchpad]]] = {
      conn.sendPreparedStatement(
        SelectAllForTask,
        Array[Any](task.id.bytes)
      ).map {
        result => buildTaskScratchpadList(result.rows)
      }.recover {
        case exception => -\/(ExceptionalFail("Uncaught exception", exception))
      }
    }

    /**
     * Find the latest revision of a task scratchpad.
     *
     * @param user the [[User]] whose scratchpad it is
     * @param task the [[Task]] this scratchpad is for
     * @return an optional [[TaskScratchpad]] object
     */
    override def find(user: User, task: Task)(implicit conn: Connection): Future[\/[Fail, TaskScratchpad]] = {
      conn.sendPreparedStatement(SelectOne, Array[Any](user.id.bytes, task.id.bytes)).map {
        result => buildTaskScratchpad(result.rows)
      }.recover {
        case exception => -\/(ExceptionalFail("Uncaught exception", exception))
      }
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
    override def insert(taskScratchpad: TaskScratchpad)(implicit conn: Connection): Future[\/[Fail, TaskScratchpad]] = {
      conn.sendPreparedStatement(Insert, Array(
        taskScratchpad.userId.bytes,
        taskScratchpad.taskId.bytes,
        1L,
        new DateTime,
        new DateTime,
        taskScratchpad.documentId
      )).map {
        result => buildTaskScratchpad(result.rows)
      }.recover {
        case exception => -\/(ExceptionalFail("Uncaught exception", exception))
      }
    }

    /**
     * Update an existing [[TaskScratchpad]] revision. This always updates a specific
     * revision, since the primary key comprises user ID, task ID, and revision number.
     * Each revision has its own versioning w.r.t. optimistic offline lock.
     *
     * @param taskScratchpad the [[TaskScratchpad]] object to be inserted.
     * @return the newly created [[TaskScratchpad]]
     */
    override def update(taskScratchpad: TaskScratchpad)(implicit conn: Connection): Future[\/[Fail, TaskScratchpad]] = {
      conn.sendPreparedStatement(Update, Array(
          taskScratchpad.documentId,
          taskScratchpad.version + 1,
          new DateTime,
          taskScratchpad.userId.bytes,
          taskScratchpad.taskId.bytes,
          taskScratchpad.version
        )).map {
        result => buildTaskScratchpad(result.rows)
      }.recover {
        case exception => -\/(ExceptionalFail("Uncaught exception", exception))
      }

    }

    /**
     * Deletes a task scratchpad.
     *
     * @param taskScratchpad
     * @return
     */
    override def delete(taskScratchpad: TaskScratchpad)(implicit conn: Connection): Future[\/[Fail, TaskScratchpad]] = {
      conn.sendPreparedStatement(DeleteOne, Array(
        taskScratchpad.userId.bytes,
        taskScratchpad.taskId.bytes,
        taskScratchpad.version
      )).map {
        result =>
          if (result.rowsAffected == 1) \/-(taskScratchpad)
          else -\/(GenericFail("The query returned no errors, but the TaskFeedback was not deleted."))
      }.recover {
        case exception => -\/(ExceptionalFail("Uncaught exception", exception))
      }

    }

    /**
     * Deletes all revisions of a task response for a particular task.
     *
     * @param task the task to delete the response for
     * @return
     */
    override def delete(task: Task)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[TaskScratchpad]]] = {
      val result = for {
        currentList <- lift(list(task))
        deletedList <- lift(conn.sendPreparedStatement(DeleteAllForTask, Array[Any](task.id.bytes)).map {
          result =>
            if (result.rowsAffected == 1) \/-(currentList)
            else -\/(GenericFail("The query returned no errors, but the TaskFeedback was not deleted."))
        })
      } yield deletedList

      result.run.recover {
        case exception => throw exception
      }
    }

    /**
     * Build a TaskFeedback object from a database result.
     *
     * @param maybeResultSet the [[ResultSet]] from the database to use
     * @return
     */
    private def buildTaskScratchpad(maybeResultSet: Option[ResultSet]): \/[Fail, TaskScratchpad] = {
      try {
        maybeResultSet match {
          case Some(resultSet) => resultSet.headOption match {
            case Some(firstRow) => \/-(TaskScratchpad(firstRow))
            case None => -\/(NoResults("The query was successful but ResultSet was empty."))
          }
          case None => -\/(NoResults("The query was successful but no ResultSet was returned."))
        }
      }
      catch {
        case exception: Throwable => -\/(ExceptionalFail("Uncaught exception", exception))
      }
    }

    /**
     * Converts an optional result set into works list
     *
     * @param maybeResultSet the [[ResultSet]] from the database to use
     * @return
     */
    private def buildTaskScratchpadList(maybeResultSet: Option[ResultSet]): \/[Fail, IndexedSeq[TaskScratchpad]] = {
      try {
        maybeResultSet match {
          case Some(resultSet) => \/-(resultSet.map(TaskScratchpad.apply))
          case None => -\/(NoResults("The query was successful but no ResultSet was returned."))
        }
      }
      catch {
        case exception: NoSuchElementException => -\/(ExceptionalFail(s"Invalid data: could not build a TaskScratchpad List from the rows returned.", exception))
      }
    }
  }
}
