package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.Connection
import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks.Task
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future
import scalaz.{\/, EitherT}

trait TaskFeedbackRepository extends Repository {

  /**
   * List all feedbacks in a project for one student.
   * @param student
   * @param project
   * @return
   */
  def list(student: User, project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[TaskFeedback]]]

  /**
   * List all feedbacks in a project for one student.
   * @param task
   * @return
   */
  def list(task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[TaskFeedback]]]

  /**
   * Find a single feedback for one task, teacher and student.
   *
   * @param student
   * @param task
   * @return
   */
  def find(student: User, task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, TaskFeedback]]

  /**
   * Create a new feedback for a task.
   *
   * @param feedback
   * @param conn an implicit connection is required, which can be used to
   *             run this operation in a transaction.
   * @return
   */
  def insert(feedback: TaskFeedback)(implicit conn: Connection): Future[\/[RepositoryError.Fail, TaskFeedback]]

  /**
   * Delete a feedback.
   *
   * @param feedback
   * @param conn an implicit connection is required, which can be used to
   *             run this operation in a transaction.
   * @return
   */
  def delete(feedback: TaskFeedback)(implicit conn: Connection): Future[\/[RepositoryError.Fail, TaskFeedback]]

  /**
   * Delete all feedbacks associated with a task.
   *
   * @param task
   * @param conn
   * @return
   */
  def delete(task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[TaskFeedback]]]
}
