package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks.Task
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import java.util.UUID
import com.github.mauricio.async.db.{ResultSet, RowData, Connection}
import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.DateTime
import scala.concurrent.Future
import scalaz.{\/, \/-, -\/}

class TaskScratchpadRepositoryPostgres(val documentRepository: DocumentRepository)
  extends TaskScratchpadRepository with PostgresRepository[TaskScratchpad] {

  override val entityName = "TaskScratchpad"

  override def constructor(row: RowData): TaskScratchpad = {
    TaskScratchpad(
      userId = row("user_id").asInstanceOf[UUID],
      taskId = row("task_id").asInstanceOf[UUID],
      documentId = row("document_id").asInstanceOf[UUID]
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

  val SelectAllForProject = s"""
    |SELECT $Fields
    |FROM $Table, parts, projects, tasks
    |WHERE user_id = ?
    |  AND projects.id = ?
    |  AND parts.id = tasks.part_id
    |  AND projects.id = parts.project_id
    |  AND $Table.task_id = tasks.id
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
       |RETURNING $Fields
     """.stripMargin

  val DeleteAllForTask =
    s"""
       |DELETE FROM $Table
       |WHERE task_id = ?
       |RETURNING $Fields
     """.stripMargin


  /**
   * List a user's latest revisions for each task in a project.
   *
   * @param user the user whose scratchpad it is
   * @param project the Project this scratchpad is for
   * @return an array of TaskScratchpad objects representing each scratchpad
   */
  override def list(user: User, project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[TaskScratchpad]]] = {
    (for {
      taskScratchpadList <- lift(queryList(SelectAllForProject, Seq[Any](user.id, project.id)))
      result <- liftSeq(taskScratchpadList.map( taskScratchpad =>
        (for {
          document <- lift(documentRepository.find(taskScratchpad.documentId))
          result   = taskScratchpad.copy(
            version   = document.version,
            createdAt = document.createdAt,
            updatedAt = document.updatedAt
          )
        } yield result).run
      ))
    } yield result).run
  }

  /**
   * List a user's latest revisions for all task scratchpads for all projects.
   *
   * @param user the user whose scratchpad it is
   * @return an array of TaskScratchpad objects representing each scratchpad
   */
  override def list(user: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[TaskScratchpad]]] = {
    (for {
      taskScratchpadList <- lift(queryList(SelectAllForUser, Seq[Any](user.id)))
      result <- liftSeq(taskScratchpadList.map( taskScratchpad =>
        (for {
          document <- lift(documentRepository.find(taskScratchpad.documentId))
          result   = taskScratchpad.copy(
            version   = document.version,
            createdAt = document.createdAt,
            updatedAt = document.updatedAt
          )
        } yield result).run
      ))
    } yield result).run
  }

  /**
   * List all users latest scratchpad revisions to a particular task.
   *
   * @param task the task to list scratchpads for
   * @return an array of TaskScratchpad objects representing each scratchpad
   */
  override def list(task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[TaskScratchpad]]] = {
    (for {
      taskScratchpadList <- lift(queryList(SelectAllForTask, Seq[Any](task.id)))
      result <- liftSeq(taskScratchpadList.map( taskScratchpad =>
        (for {
          document <- lift(documentRepository.find(taskScratchpad.documentId))
          result   = taskScratchpad.copy(
            version   = document.version,
            createdAt = document.createdAt,
            updatedAt = document.updatedAt
          )
        } yield result).run
      ))
    } yield result).run
  }

  /**
   * Find the latest revision of a task scratchpad.
   *
   * @param user the user whose scratchpad it is
   * @param task the task this scratchpad is for
   * @return an optional TaskScratchpad object
   */
  override def find(user: User, task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, TaskScratchpad]] = {
    (for {
      taskScratchpad <- lift(queryOne(SelectOne, Seq[Any](user.id, task.id)))
      document     <- lift(documentRepository.find(taskScratchpad.documentId))
    } yield taskScratchpad.copy(
        version   = document.version,
        createdAt = document.createdAt,
        updatedAt = document.updatedAt,
        document = Some(document)
      )).run
  }

  /**
   * Insert a new TaskScratchpad. Used to create new scratchpads, and to insert new
   * revisions to existing pads. Note that the primary key comprises the user's ID,
   * the task's ID, and the revision number, so each revision is a separate entry in
   * the database.
   *
   * @param scratchpad the TaskScratchpad object to be inserted.
   * @return the newly created TaskScratchpad
   */
  override def insert(scratchpad: TaskScratchpad)(implicit conn: Connection): Future[\/[RepositoryError.Fail, TaskScratchpad]] = {
    (for {
      taskScratchpad <- lift(queryOne(Insert, Array[Any](
        scratchpad.userId,
        scratchpad.taskId,
        scratchpad.documentId
      )))
      document <- lift(documentRepository.find(taskScratchpad.documentId))
    } yield taskScratchpad.copy(
        version   = document.version,
        createdAt = document.createdAt,
        updatedAt = document.updatedAt
      )).run
  }

  /**
   * Deletes a task scratchpad.
   *
   * @param taskScratchpad
   * @return
   */
  override def delete(taskScratchpad: TaskScratchpad)(implicit conn: Connection): Future[\/[RepositoryError.Fail, TaskScratchpad]] = {
    (for {
      taskScratchpad <- lift(queryOne(DeleteOne, Seq(
        taskScratchpad.userId,
        taskScratchpad.taskId
      )))
      document <- lift(documentRepository.find(taskScratchpad.documentId))
    } yield taskScratchpad.copy(
        version   = document.version,
        createdAt = document.createdAt,
        updatedAt = document.updatedAt
      )).run
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
      _ <- lift(queryList(DeleteAllForTask, Seq[Any](task.id)))
    } yield currentList).run
  }
}
