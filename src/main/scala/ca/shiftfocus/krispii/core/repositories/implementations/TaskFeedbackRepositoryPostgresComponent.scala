package ca.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.{RowData, Connection}
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib.ExceptionWriter
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks.Task
import ca.shiftfocus.uuid.UUID
import play.api.Play.current

import play.api.Logger
import scala.concurrent.Future
import org.joda.time.DateTime
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB

trait TaskFeedbackRepositoryPostgresComponent extends TaskFeedbackRepositoryComponent {
  self: PostgresDB =>

  val taskFeedbackRepository: TaskFeedbackRepository = new TaskFeedbackRepositoryPSQL

  private class TaskFeedbackRepositoryPSQL extends TaskFeedbackRepository {
    val table = "task_feedbacks"
    val pkeyFields = List("teacher_id", "student_id", "task_id", "revision")
    val pkeyText = pkeyFields.mkString(", ")
    val pkeyQs = pkeyFields.map(_ => "?").mkString(", ")

    val dataFields = List("content")
    val fieldsText = dataFields.mkString(", ")
    val dataQs = dataFields.map(_ => "?").mkString(", ")

    /*
     * SQL queries
     */

    val Insert =
      s"""
         |INSERT INTO $table ($pkeyText, version, status, created_at, updated_at, $fieldsText)
         |VALUES ($pkeyQs, 1, 1, ?, ?, $dataQs)
         |RETURNING $pkeyText, version, created_at, updated_at, $fieldsText
      """.stripMargin


    val Update =
      s"""
         |UPDATE $table
         |SET content = ? , version = ?, updated_at = ?
         |WHERE teacher_id = ?
         |  AND student_id = ?
         |  AND task_id = ?
         |  AND revision = ?
         |  AND version = ?
         |  AND status = 1
         |RETURNING $pkeyText, version, created_at, updated_at, $fieldsText
      """.stripMargin


    val ListRevisionsById = s"""
      SELECT teacher_id, student_id, task_id, revision, version, created_at, updated_at, content
      FROM $table
      WHERE teacher_id = ?
        AND student_id = ?
        AND task_id = ?
        AND status = 1
      ORDER BY revision DESC
    """

    val ListLatestForStudentAndProject =
      s"""
         |SELECT $table.teacher_id, $table.student_id, $table.task_id, $table.revision,
         |       $table.version, $table.created_at, $table.updated_at, $table.content
         |FROM $table as tf, projects, parts, tasks
         |WHERE student_id = ?
         |  AND projects.id = ?
         |  AND projects.id = parts.project_id
         |  AND parts.id = tasks.part_id
         |  AND tasks.id = $table.task_id
         |  AND status = 1
         |  AND revision = (SELECT MAX(revision) FROM $table as tf2 WHERE tf2.student_id = ? AND tf2.teacher_id = tf.teacher_id AND task_id = tasks.id)
       """.stripMargin

    val ListLatestForStudentAndTeacherAndProject =
      s"""
         |SELECT $table.teacher_id, $table.student_id, $table.task_id, $table.revision,
         |       $table.version, $table.created_at, $table.updated_at, $table.content
         |FROM $table as tf, projects, parts, tasks
         |WHERE student_id = ?
         |  AND teacher_id = ?
         |  AND projects.id = ?
         |  AND projects.id = parts.project_id
         |  AND parts.id = tasks.part_id
         |  AND tasks.id = $table.task_id
         |  AND status = 1
         |  AND revision = (SELECT MAX(revision) FROM $table as tf2 WHERE tf2.student_id = ? AND tf2.teacher_id = ? AND task_id = tasks.id)
       """.stripMargin

    val SelectLatestById =
      s"""
        |SELECT teacher_id, student_id, task_id, revision, version, created_at, updated_at, content
        |FROM $table
        |WHERE teacher_id = ?
        |  AND student_id = ?
        |  AND task_id = ?
        |  AND status = 1
        |ORDER BY revision DESC
        |LIMIT 1
      """.stripMargin

    val SelectRevisionById =
      s"""
         |SELECT teacher_id, student_id, task_id, revision, version, created_at, updated_at, content
         |FROM $table
         |WHERE teacher_id = ?
         |  AND student_id = ?
         |  AND task_id = ?
         |  AND revision = ?
         |  AND status = 1
       """.stripMargin

    val Delete = s"""
      DELETE FROM $table
      WHERE teacher_id = ?
        AND student_id = ?
        AND task_id = ?
    """

    val DeleteByTask = s"""
      DELETE FROM $table
      WHERE task_id = ?
    """

    /**
     * List all feedbacks in a project for one student.
     *
     * @param student
     * @param project
     * @return
     */
    def list(student: User, project: Project): Future[IndexedSeq[TaskFeedback]] = {
      db.pool.sendPreparedStatement(
        ListLatestForStudentAndProject,
        Array[Any](student.id.bytes, project.id.bytes, student.id.bytes)
      ).map {
        result => result.rows.get.map {
          item: RowData => TaskFeedback(item)
        }
      }.recover {
        case exception => throw exception
      }
    }

    /**
     * List all feedbacks in a project for one student, for one teacher.
     *
     * @param teacher
     * @param student
     * @param project
     * @return
     */
    def list(teacher: User, student: User, project: Project): Future[IndexedSeq[TaskFeedback]] = {
      db.pool.sendPreparedStatement(
        ListLatestForStudentAndTeacherAndProject,
        Array[Any](student.id.bytes, teacher.id.bytes, project.id.bytes, student.id.bytes, teacher.id.bytes)
      ).map {
        result => result.rows.get.map {
          item: RowData => TaskFeedback(item)
        }
      }.recover {
        case exception => throw exception
      }
    }

    /**
     * Find a single feedback for one task, teacher and student.
     *
     * @param teacher
     * @param student
     * @param task
     * @return
     */
    def find(teacher: User, student: User, task: Task): Future[Option[TaskFeedback]] = {
      db.pool.sendPreparedStatement(SelectLatestById, Array[Any](teacher.id.bytes, student.id.bytes, task.id.bytes)).map {
        result => result.rows.get.headOption match {
          case Some(rowData) => Some(TaskFeedback(rowData))
          case None => None
        }
      }.recover {
        case exception => throw exception
      }
    }

    /**
     * Find a specific revision of a feedback.
     *
     * @param teacher
     * @param student
     * @param task
     * @param revision
     * @return
     */
    def find(teacher: User, student: User, task: Task, revision: Long): Future[Option[TaskFeedback]] = {
      db.pool.sendPreparedStatement(SelectRevisionById, Array[Any](teacher.id.bytes, student.id.bytes, task.id.bytes, revision)).map {
        result => result.rows.get.headOption match {
          case Some(rowData) => Some(TaskFeedback(rowData))
          case None => None
        }
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
    def insert(feedback: TaskFeedback)(implicit conn: Connection): Future[TaskFeedback] = {
      conn.sendPreparedStatement(Insert, Array[Any](
        feedback.teacherId.bytes,
        feedback.studentId.bytes,
        feedback.taskId.bytes,
        feedback.revision,
        new DateTime,
        new DateTime,
        feedback.content
      )).map {
        result => TaskFeedback(result.rows.get.head)
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
    def update(feedback: TaskFeedback)(implicit conn: Connection): Future[TaskFeedback] = {
      conn.sendPreparedStatement(Insert, Array[Any](
        feedback.content,
        (feedback.version + 1),
        new DateTime,
        feedback.teacherId.bytes,
        feedback.studentId.bytes,
        feedback.taskId.bytes,
        feedback.revision,
        feedback.version
      )).map {
        result => TaskFeedback(result.rows.get.head)
      }.recover {
        case exception => throw exception
      }
    }

    /**
     * Delete a feedback.
     *
     * @param feedback
     * @param conn an implicit connection is required, which can be used to
     *             run this operation in a transaction.
     * @return
     */
    def delete(feedback: TaskFeedback)(implicit conn: Connection): Future[Boolean] = {
      conn.sendPreparedStatement(Delete, Array[Any](
        feedback.teacherId,
        feedback.studentId,
        feedback.taskId
      )).map {
        result => (result.rowsAffected > 0)
      }.recover {
        case exception => throw exception
      }
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
