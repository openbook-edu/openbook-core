package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.repositories.error.RepositoryError
import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks.Task
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future
import scalaz.{EitherT, \/}

trait TaskRepositoryComponent {
  self: ProjectRepositoryComponent with
        PartRepositoryComponent =>

  val taskRepository: TaskRepository

  trait TaskRepository {
    def list: Future[\/[RepositoryError, IndexedSeq[Task]]]
    def list(project: Project): Future[\/[RepositoryError, IndexedSeq[Task]]]
    def list(part: Part): Future[\/[RepositoryError, IndexedSeq[Task]]]
    def list(project: Project, partNum: Int): Future[\/[RepositoryError, IndexedSeq[Task]]]

    def find(id: UUID): Future[\/[RepositoryError, Task]]
    def findNow(student: User, project: Project): Future[\/[RepositoryError, Task]]
    def find(project: Project, partNum: Int, taskNum: Int): Future[\/[RepositoryError, Task]]

    def insert(task: Task)(implicit conn: Connection): Future[\/[RepositoryError, Task]]
    def update(task: Task)(implicit conn: Connection): Future[\/[RepositoryError, Task]]
    def delete(task: Task)(implicit conn: Connection): Future[\/[RepositoryError, Task]]
    // TODO - changed return from Task to IndexedSeq[Task], because we delete all tasks belonging to a part
    def delete(part: Part)(implicit conn: Connection): Future[\/[RepositoryError, IndexedSeq[Task]]]

    protected def lift = EitherT.eitherT[Future, RepositoryError, Task] _
    protected def liftList = EitherT.eitherT[Future, RepositoryError, IndexedSeq[Task]] _
  }
}
