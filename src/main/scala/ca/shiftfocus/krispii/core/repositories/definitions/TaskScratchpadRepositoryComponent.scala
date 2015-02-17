package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.repositories.error.RepositoryError
import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks.Task
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future
import scalaz.{\/, EitherT}

trait TaskScratchpadRepositoryComponent {
  self: UserRepositoryComponent with
        ProjectRepositoryComponent with
        TaskRepositoryComponent =>

  val taskScratchpadRepository: TaskScratchpadRepository

  trait TaskScratchpadRepository {
    /**
     * The usual CRUD functions for the projects table.
     */
    def list(task: Task)(implicit conn: Connection): Future[\/[RepositoryError, IndexedSeq[TaskScratchpad]]]
    def list(user: User)(implicit conn: Connection): Future[\/[RepositoryError, IndexedSeq[TaskScratchpad]]]
    def list(user: User, project: Project)(implicit conn: Connection): Future[\/[RepositoryError, IndexedSeq[TaskScratchpad]]]
    def list(user: User, task: Task)(implicit conn: Connection): Future[\/[RepositoryError, IndexedSeq[TaskScratchpad]]]

    def find(user: User, task: Task)(implicit conn: Connection): Future[\/[RepositoryError, TaskScratchpad]]
    def find(user: User, task: Task, revision: Long)(implicit conn: Connection): Future[\/[RepositoryError, TaskScratchpad]]

    def insert(taskResponse: TaskScratchpad)(implicit conn: Connection): Future[\/[RepositoryError, TaskScratchpad]]
    def update(taskResponse: TaskScratchpad)(implicit conn: Connection): Future[\/[RepositoryError, TaskScratchpad]]
    def delete(taskResponse: TaskScratchpad)(implicit conn: Connection): Future[\/[RepositoryError, TaskScratchpad]]
    def delete(task: Task)(implicit conn: Connection): Future[\/[RepositoryError, TaskScratchpad]]

    protected def lift = EitherT.eitherT[Future, RepositoryError, TaskScratchpad] _
    protected def liftList = EitherT.eitherT[Future, RepositoryError, IndexedSeq[TaskScratchpad]] _
  }
}
