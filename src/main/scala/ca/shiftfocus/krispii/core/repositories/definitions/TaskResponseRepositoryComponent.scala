package ca.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks.Task
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future

trait TaskResponseRepositoryComponent {
  /**
   * Value storing an instance of the repository. Should be overridden with
   * a concrete implementation to be used via dependency injection.
   */
  val taskResponseRepository: TaskResponseRepository

  trait TaskResponseRepository {
    /**
     * The usual CRUD functions for the projects table.
     */
    //def list(implicit conn: Connection): Future[IndexedSeq[TaskResponse]]
    def list(task: Task)(implicit conn: Connection): Future[IndexedSeq[TaskResponse]]
    def list(user: User)(implicit conn: Connection): Future[IndexedSeq[TaskResponse]]
    def list(user: User, project: Project)(implicit conn: Connection): Future[IndexedSeq[TaskResponse]]
    def list(user: User, task: Task)(implicit conn: Connection): Future[IndexedSeq[TaskResponse]]
    def find(user: User, task: Task)(implicit conn: Connection): Future[Option[TaskResponse]]
    def find(user: User, task: Task, revision: Long)(implicit conn: Connection): Future[Option[TaskResponse]]
    def insert(taskResponse: TaskResponse)(implicit conn: Connection): Future[TaskResponse]
    def update(taskResponse: TaskResponse)(implicit conn: Connection): Future[TaskResponse]
    def delete(taskResponse: TaskResponse)(implicit conn: Connection): Future[Boolean]
    def delete(task: Task)(implicit conn: Connection): Future[Boolean]
    def forceComplete(task: Task, section: Course)(implicit conn: Connection): Future[Boolean]
  }
}
