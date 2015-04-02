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
import scalaz.{\/, -\/, \/-}


class TaskFeedbackRepositoryPostgres extends TaskFeedbackRepository with PostgresRepository[TaskFeedback] {

  def constructor(row: RowData): TaskFeedback = {
    TaskFeedback(
      studentId  = UUID(row("student_id").asInstanceOf[Array[Byte]]),
      taskId     = UUID(row("task_id").asInstanceOf[Array[Byte]]),
      version    = row("version").asInstanceOf[Long],
      documentId = UUID(row("document_id").asInstanceOf[Array[Byte]]),
      createdAt  = row("created_at").asInstanceOf[DateTime],
      updatedAt  = row("updated_at").asInstanceOf[DateTime]
    )
  }

  val Fields = "student_id, task_id, version, document_id, created_at, updated_at"
  val QMarks = "?, ?, ?, ?, ?, ?"
  val Table = "task_feedbacks"

  val Insert =
    s"""
       |INSERT INTO $Table ($Fields)
       |VALUES (QMarks)
       |RETURNING $Fields
    """.stripMargin

  val Update =
    s"""
       |UPDATE $Table
       |SET version = ?, document_id = ?, updated_at = ?
       |WHERE student_id = ?
       |  AND task_id = ?
       |  AND version = ?
       |RETURNING $Fields
    """.stripMargin

  val SelectAllForStudentAndProject =
    s"""
       |SELECT $Fields
       |FROM $Table as tf, projects, parts, tasks
       |WHERE student_id = ?
       |  AND projects.id = ?
       |  AND projects.id = parts.project_id
       |  AND parts.id = tasks.part_id
       |  AND tasks.id = tf.task_id
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
    DELETE FROM $Table
    WHERE student_id = ?
      AND task_id = ?
      AND version = ?
  """

  val DeleteAllForTask = s"""
    DELETE FROM $Table
    WHERE task_id = ?
  """

  /**
   * List all feedbacks in a project for one student.
   *
   * @param student
   * @param project
   * @return
   */
  def list(student: User, project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[TaskFeedback]]] = {
    queryList(SelectAllForStudentAndProject, Seq[Any](student.id.bytes, project.id.bytes, student.id.bytes))
  }

  /**
   * List all feedbacks for a given task
   *
   * @param task the task to list feedbacks for
   * @return
   */
  def list(task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[TaskFeedback]]] = {
    queryList(SelectAllForTask, Seq[Any](task.id.bytes))
  }

  /**
   * Find a single feedback for one task, teacher and student.
   *
   * @param student
   * @param task
   * @return
   */
  def find(student: User, task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, TaskFeedback]] = {
    queryOne(SelectOneById, Array[Any](student.id.bytes, task.id.bytes))
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
    queryOne(Insert, Array[Any](
      feedback.studentId.bytes,
      feedback.taskId.bytes,
      1L,
      feedback.documentId.bytes,
      new DateTime,
      new DateTime
    ))
  }

  /**
   * Update an existing feedback.
   *
   * @param feedback
   * @param conn an implicit connection is required, which can be used to
   *             run this operation in a transaction.
   * @return
   */
  def update(feedback: TaskFeedback)(implicit conn: Connection): Future[\/[RepositoryError.Fail, TaskFeedback]] = {
    queryOne(Insert, Array[Any](
      feedback.version + 1,
      feedback.documentId,
      new DateTime,
      feedback.studentId.bytes,
      feedback.taskId.bytes,
      feedback.version
    ))
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
    queryOne(Delete, Array[Any](
      feedback.studentId,
      feedback.taskId,
      feedback.version
    ))
  }

  /**
   * Delete a feedback.
   *
   * @param task the task for which feedbacks should be deleted
   * @param conn an implicit connection is required, which can be used to
   *             run this operation in a transaction.
   * @return
   */
  def delete(task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[TaskFeedback]]] = {
    (for {
      feedbackList <- lift(list(task))
      deletedList <- lift(queryList(DeleteAllForTask, Array[Any](task.id.bytes)))
    } yield deletedList).run
  }
}
