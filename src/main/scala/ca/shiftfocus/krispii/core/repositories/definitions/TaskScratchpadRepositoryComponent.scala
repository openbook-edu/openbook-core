package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.fail.Fail
import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks.Task
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future
import scalaz.{\/, EitherT}

trait TaskScratchpadRepositoryComponent extends FutureMonad {
  self: UserRepositoryComponent with
        ProjectRepositoryComponent with
        TaskRepositoryComponent =>

  val taskScratchpadRepository: TaskScratchpadRepository

  trait TaskScratchpadRepository {
    /**
     * The usual CRUD functions for the projects table.
     */
    def list(task: Task)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[TaskScratchpad]]]
    def list(user: User)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[TaskScratchpad]]]
    def list(user: User, project: Project)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[TaskScratchpad]]]
    def list(user: User, task: Task)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[TaskScratchpad]]]

    def find(user: User, task: Task)(implicit conn: Connection): Future[\/[Fail, TaskScratchpad]]

    def insert(taskScratchpad: TaskScratchpad)(implicit conn: Connection): Future[\/[Fail, TaskScratchpad]]
    def update(taskScratchpad: TaskScratchpad)(implicit conn: Connection): Future[\/[Fail, TaskScratchpad]]
    def delete(taskScratchpad: TaskScratchpad)(implicit conn: Connection): Future[\/[Fail, TaskScratchpad]]
    def delete(task: Task)(implicit conn: Connection): Future[\/[Fail, TaskScratchpad]]
  }
}
