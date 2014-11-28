package ca.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks.Task
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future

trait TaskFeedbackRepositoryComponent {

  val taskFeedbackRepository: TaskFeedbackRepository

  trait TaskFeedbackRepository {

    /**
     * List all feedbacks in a project for one student.
     * @param student
     * @param project
     * @return
     */
    def list(student: User, project: Project): Future[IndexedSeq[TaskFeedback]]

    /**
     * List all feedbacks in a project for one student, for one teacher.
     *
     * @param teacher
     * @param student
     * @param project
     * @return
     */
    def list(teacher: User, student: User, project: Project): Future[IndexedSeq[TaskFeedback]]

    /**
     * Find a single feedback for one task, teacher and student.
     *
     * @param teacher
     * @param student
     * @param task
     * @return
     */
    def find(teacher: User, student: User, task: Task): Future[Option[TaskFeedback]]

    /**
     * Find a specific revision of a feedback.
     *
     * @param teacher
     * @param student
     * @param task
     * @param revision
     * @return
     */
    def find(teacher: User, student: User, task: Task, revision: Long): Future[Option[TaskFeedback]]

    /**
     * Create a new feedback for a task.
     *
     * @param feedback
     * @param conn an implicit connection is required, which can be used to
     *             run this operation in a transaction.
     * @return
     */
    def insert(feedback: TaskFeedback)(implicit conn: Connection): Future[TaskFeedback]

    /**
     * Update an existing feedback.
     *
     * @param feedback
     * @param conn an implicit connection is required, which can be used to
     *             run this operation in a transaction.
     * @return
     */
    def update(feedback: TaskFeedback)(implicit conn: Connection): Future[TaskFeedback]

    /**
     * Delete a feedback.
     *
     * @param feedback
     * @param conn an implicit connection is required, which can be used to
     *             run this operation in a transaction.
     * @return
     */
    def delete(feedback: TaskFeedback)(implicit conn: Connection): Future[Boolean]

    /**
     * Delete all feedbacks associated with a task.
     *
     * @param task
     * @param conn
     * @return
     */
    def delete(task: Task)(implicit conn: Connection): Future[Boolean]
  }

}
