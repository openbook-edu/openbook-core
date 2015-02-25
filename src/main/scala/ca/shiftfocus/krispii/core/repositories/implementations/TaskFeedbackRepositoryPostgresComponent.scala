package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.fail._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks.Task
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import com.github.mauricio.async.db.{ResultSet, RowData, Connection}
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import org.joda.time.DateTime
import scala.concurrent.Future
import scalaz.{\/, -\/, \/-}

trait TaskFeedbackRepositoryPostgresComponent extends TaskFeedbackRepositoryComponent with PostgresRepository {
  self: UserRepositoryComponent with
        ProjectRepositoryComponent with
        TaskRepositoryComponent with
        PostgresDB =>

  val taskFeedbackRepository: TaskFeedbackRepository = new TaskFeedbackRepositoryPSQL

  private class TaskFeedbackRepositoryPSQL extends TaskFeedbackRepository {
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
    def list(student: User, project: Project): Future[\/[Fail, IndexedSeq[TaskFeedback]]] = {
      db.pool.sendPreparedStatement(
        SelectAllForStudentAndProject,
        Array[Any](student.id.bytes, project.id.bytes, student.id.bytes)
      ).map {
        result => buildEntityList(result.rows, TaskFeedback.apply)
      }.recover {
        case exception => throw exception
      }
    }

    /**
     * List all feedbacks for a given task
     *
     * @param task the task to list feedbacks for
     * @return
     */
    def list(task: Task): Future[\/[Fail, IndexedSeq[TaskFeedback]]] = {
      db.pool.sendPreparedStatement(
        SelectAllForTask,
        Array[Any](task.id.bytes)
      ).map {
        result => buildEntityList(result.rows, TaskFeedback.apply)
      }.recover {
        case exception => throw exception
      }
    }

    /**
     * Find a single feedback for one task, teacher and student.
     *
     * @param student
     * @param task
     * @return
     */
    def find(student: User, task: Task): Future[\/[Fail, TaskFeedback]] = {
      db.pool.sendPreparedStatement(SelectOneById, Array[Any](student.id.bytes, task.id.bytes)).map {
        result => buildEntity(result.rows, TaskFeedback.apply)
      }.recover {
        case exception => throw exception
      }
    }

    /**
     * Create a new feedback for a task.
     *
     * @param feedback
     * @param conn an implicit connection is required, which can be used to
     *             run this operation in a transaction.
     * @return
     */
    def insert(feedback: TaskFeedback)(implicit conn: Connection): Future[\/[Fail, TaskFeedback]] = {
      conn.sendPreparedStatement(Insert, Array[Any](
        feedback.studentId.bytes,
        feedback.taskId.bytes,
        feedback.documentId.bytes,
        new DateTime,
        new DateTime
      )).map {
        result => buildEntity(result.rows, TaskFeedback.apply)
      }.recover {
        case exception => throw exception
      }
    }

    /**
     * Update an existing feedback.
     *
     * @param feedback
     * @param conn an implicit connection is required, which can be used to
     *             run this operation in a transaction.
     * @return
     */
    def update(feedback: TaskFeedback)(implicit conn: Connection): Future[\/[Fail, TaskFeedback]] = {
      conn.sendPreparedStatement(Insert, Array[Any](
        feedback.version + 1,
        feedback.documentId,
        new DateTime,
        feedback.studentId.bytes,
        feedback.taskId.bytes,
        feedback.version
      )).map {
        result => buildEntity(result.rows, TaskFeedback.apply)
      }.recover {
        case exception => throw exception
      }
    }

    /**
     * Delete a feedback.
     *
     * @param feedback the entity to be deleted
     * @param conn an implicit connection is required, which can be used to
     *             run this operation in a transaction.
     * @return
     */
    def delete(feedback: TaskFeedback)(implicit conn: Connection): Future[\/[Fail, TaskFeedback]] = {
      conn.sendPreparedStatement(Delete, Array[Any](
        feedback.studentId,
        feedback.taskId,
        feedback.version
      )).map {
        result =>
          if (result.rowsAffected == 1) \/-(feedback)
          else -\/(GenericFail("The query returned no errors, but the TaskFeedback was not deleted."))
      }.recover {
        case exception => throw exception
      }
    }

    /**
     * Delete a feedback.
     *
     * @param task the task for which feedbacks should be deleted
     * @param conn an implicit connection is required, which can be used to
     *             run this operation in a transaction.
     * @return
     */
    def delete(task: Task)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[TaskFeedback]]] = {
      val result = for {
        feedbackList <- lift(list(task))
        deletedList <- lift(conn.sendPreparedStatement(DeleteAllForTask, Array[Any](task.id.bytes)).map {
          result =>
            if (result.rowsAffected == feedbackList.length) \/-(feedbackList)
            else -\/(GenericFail("The query returned no errors, but the TaskFeedback was not deleted."))
        })
      } yield deletedList

      result.run.recover {
        case exception => throw exception
      }
    }
  }
}
