package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks.Task
import java.util.UUID

import ca.shiftfocus.krispii.core.models.user.User
import com.github.mauricio.async.db.{Connection, RowData}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.\/

class TaskFeedbackRepositoryPostgres(val documentRepository: DocumentRepository)
    extends TaskFeedbackRepository with PostgresRepository[TaskFeedback] {

  override val entityName = "TaskFeedback"

  def constructor(row: RowData): TaskFeedback = {
    TaskFeedback(
      studentId = row("student_id").asInstanceOf[UUID],
      taskId = row("task_id").asInstanceOf[UUID],
      documentId = row("document_id").asInstanceOf[UUID]
    )
  }

  val Fields = "student_id, task_id, document_id"
  val QMarks = "?, ?, ?"
  val Table = "task_feedbacks"

  val Insert =
    s"""
       |INSERT INTO $Table ($Fields)
       |VALUES ($QMarks)
       |RETURNING $Fields
    """.stripMargin

  val SelectAllForStudentAndProject =
    s"""
       |SELECT $Fields
       |FROM $Table, projects, parts, tasks
       |WHERE student_id = ?
       |  AND projects.id = ?
       |  AND projects.id = parts.project_id
       |  AND parts.id = tasks.part_id
       |  AND tasks.id = $Table.task_id
     """.stripMargin

  val SelectAllForTask =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE task_id = ?
     """.stripMargin

  val SelectOneById =
    s"""
      |SELECT $Fields
      |FROM $Table
      |WHERE student_id = ?
      |  AND task_id = ?
    """.stripMargin

  val Delete = s"""
    |DELETE FROM $Table
    |WHERE student_id = ?
    |  AND task_id = ?
    |RETURNING $Fields
  """.stripMargin

  val DeleteAllForTask = s"""
    |DELETE FROM $Table
    |WHERE task_id = ?
    |RETURNING $Fields
  """.stripMargin

  /**
   * List all feedbacks in a project for one student.
   *
   * @param student
   * @param project
   * @return
   */
  def list(student: User, project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[TaskFeedback]]] = {
    (for {
      taskFeedbackList <- lift(queryList(SelectAllForStudentAndProject, Seq[Any](student.id, project.id)))
      result <- liftSeq(taskFeedbackList.map(taskFeedback =>
        (for {
          document <- lift(documentRepository.find(taskFeedback.documentId))
          result = taskFeedback.copy(
            version = document.version,
            createdAt = document.createdAt,
            updatedAt = document.updatedAt
          )
        } yield result).run))
    } yield result).run
  }

  /**
   * List all feedbacks for a given task
   *
   * @param task the task to list feedbacks for
   * @return
   */
  def list(task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[TaskFeedback]]] = {
    (for {
      taskFeedbackList <- lift(queryList(SelectAllForTask, Seq[Any](task.id)))
      result <- liftSeq(taskFeedbackList.map(taskFeedback =>
        (for {
          document <- lift(documentRepository.find(taskFeedback.documentId))
          result = taskFeedback.copy(
            version = document.version,
            createdAt = document.createdAt,
            updatedAt = document.updatedAt
          )
        } yield result).run))
    } yield result).run
  }

  /**
   * Find a single feedback for one task and student.
   *
   * @param student
   * @param task
   * @return
   */
  def find(student: User, task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, TaskFeedback]] = {
    (for {
      taskFeedback <- lift(queryOne(SelectOneById, Array[Any](student.id, task.id)))
      document <- lift(documentRepository.find(taskFeedback.documentId))
    } yield taskFeedback.copy(
      version = document.version,
      createdAt = document.createdAt,
      updatedAt = document.updatedAt
    )).run
  }

  /**
   * Create a new feedback for a task.
   *
   * @param feedback
   * @param conn an implicit connection is required, which can be used to
   *             run this operation in a transaction.
   * @return
   */
  def insert(feedback: TaskFeedback)(implicit conn: Connection): Future[\/[RepositoryError.Fail, TaskFeedback]] = {
    (for {
      taskFeedback <- lift(queryOne(Insert, Array[Any](
        feedback.studentId,
        feedback.taskId,
        feedback.documentId
      )))
      document <- lift(documentRepository.find(taskFeedback.documentId))
    } yield taskFeedback.copy(
      version = document.version,
      createdAt = document.createdAt,
      updatedAt = document.updatedAt
    )).run
  }

  /**
   * Delete a feedback.
   *
   * @param feedback the entity to be deleted
   * @param conn an implicit connection is required, which can be used to
   *             run this operation in a transaction.
   * @return
   */
  def delete(feedback: TaskFeedback)(implicit conn: Connection): Future[\/[RepositoryError.Fail, TaskFeedback]] = {
    (for {
      taskFeedback <- lift(queryOne(Delete, Array[Any](
        feedback.studentId,
        feedback.taskId
      )))
      document <- lift(documentRepository.find(taskFeedback.documentId))
    } yield taskFeedback.copy(
      version = document.version,
      createdAt = document.createdAt,
      updatedAt = document.updatedAt
    )).run
  }

  /**
   * Delete all feedbacks for a task.
   *
   * @param task the task for which feedbacks should be deleted
   * @param conn an implicit connection is required, which can be used to
   *             run this operation in a transaction.
   * @return
   */
  def delete(task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[TaskFeedback]]] = {
    (for {
      feedbackList <- lift(list(task))
      _ <- lift(queryList(DeleteAllForTask, Array[Any](task.id)))
    } yield feedbackList).run
  }
}
