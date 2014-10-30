package ca.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks.Task
import scala.concurrent.Future

trait TaskScratchpadRepositoryComponent {

  val taskScratchpadRepository: TaskScratchpadRepository

  trait TaskScratchpadRepository {
    /**
     * The usual CRUD functions for the projects table.
     */
    def list(task: Task)(implicit conn: Connection): Future[IndexedSeq[TaskScratchpad]]
    def list(user: User)(implicit conn: Connection): Future[IndexedSeq[TaskScratchpad]]
    def list(user: User, project: Project)(implicit conn: Connection): Future[IndexedSeq[TaskScratchpad]]
    def list(user: User, task: Task)(implicit conn: Connection): Future[IndexedSeq[TaskScratchpad]]
    def find(user: User, task: Task)(implicit conn: Connection): Future[Option[TaskScratchpad]]
    def find(user: User, task: Task, revision: Long)(implicit conn: Connection): Future[Option[TaskScratchpad]]
    def insert(taskResponse: TaskScratchpad)(implicit conn: Connection): Future[TaskScratchpad]
    def update(taskResponse: TaskScratchpad)(implicit conn: Connection): Future[TaskScratchpad]
    def delete(taskResponse: TaskScratchpad)(implicit conn: Connection): Future[Boolean]
    def delete(task: Task)(implicit conn: Connection): Future[Boolean]
  }
}
