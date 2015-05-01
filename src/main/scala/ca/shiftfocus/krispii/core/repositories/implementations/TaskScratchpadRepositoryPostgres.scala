package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks.Task
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.{ResultSet, RowData, Connection}
import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.DateTime
import scala.concurrent.Future
import scalaz.{\/, \/-, -\/}

class TaskScratchpadRepositoryPostgres extends TaskScratchpadRepository with PostgresRepository[TaskScratchpad] {

  override def constructor(row: RowData): TaskScratchpad = {
    TaskScratchpad(
      userId = UUID(row("user_id").asInstanceOf[Array[Byte]]),
      taskId = UUID(row("task_id").asInstanceOf[Array[Byte]]),
      documentId = UUID(row("document_id").asInstanceOf[Array[Byte]])
    )
  }

  val Table = "task_notes"
  val Fields = "user_id, task_id, document_id"
  val QMarks = "?, ?, ?"

  val Insert = {
    s"""
       |INSERT INTO $Table ($Fields)
       |VALUES ($QMarks)
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
  override def list(user: User, task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[TaskScratchpad]]] = {
    queryList(SelectAllForUserAndTask, Seq[Any](user.id.bytes, task.id.bytes))
  }

  /**
   * List a user's latest revisions for each task in a project.
   *
   * @param user the [[User]] whose scratchpad it is
   * @param project the [[Project]] this scratchpad is for
   * @return an array of [[TaskScratchpad]] objects representing each scratchpad
   */
  override def list(user: User, project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[TaskScratchpad]]] = {
    queryList(SelectAllForProject, Seq[Any](user.id.bytes, project.id.bytes))
  }

  /**
   * List a user's latest revisions for all task scratchpads for all projects.
   *
   * @param user the [[User]] whose scratchpad it is
   * @return an array of [[TaskScratchpad]] objects representing each scratchpad
   */
  override def list(user: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[TaskScratchpad]]] = {
    queryList(SelectAllForUser, Seq[Any](user.id.bytes))
  }

  /**
   * List all users latest scratchpad revisions to a particular task.
   *
   * @param task the [[Task]] to list scratchpads for
   * @return an array of [[TaskScratchpad]] objects representing each scratchpad
   */
  override def list(task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[TaskScratchpad]]] = {
    queryList(SelectAllForTask, Seq[Any](task.id.bytes))
  }

  /**
   * Find the latest revision of a task scratchpad.
   *
   * @param user the [[User]] whose scratchpad it is
   * @param task the [[Task]] this scratchpad is for
   * @return an optional [[TaskScratchpad]] object
   */
  override def find(user: User, task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, TaskScratchpad]] = {
    queryOne(SelectOne, Seq[Any](user.id.bytes, task.id.bytes))
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
  override def insert(taskScratchpad: TaskScratchpad)(implicit conn: Connection): Future[\/[RepositoryError.Fail, TaskScratchpad]] = {
    queryOne(Insert, Seq(
      taskScratchpad.userId.bytes,
      taskScratchpad.taskId.bytes,
      taskScratchpad.documentId.bytes
    ))
  }

  /**
   * Deletes a task scratchpad.
   *
   * @param taskScratchpad
   * @return
   */
  override def delete(taskScratchpad: TaskScratchpad)(implicit conn: Connection): Future[\/[RepositoryError.Fail, TaskScratchpad]] = {
    queryOne(DeleteOne, Seq(
      taskScratchpad.userId.bytes,
      taskScratchpad.taskId.bytes
    ))
  }

  /**
   * Deletes all revisions of a task response for a particular task.
   *
   * @param task the task to delete the response for
   * @return
   */
  override def delete(task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[TaskScratchpad]]] = {
    (for {
      currentList <- lift(list(task))
      deletedList <- lift(queryList(DeleteAllForTask, Seq[Any](task.id.bytes)))
    } yield deletedList).run
  }
}
