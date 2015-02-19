package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.fail.Fail
import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks.Task
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future
import scalaz.{EitherT, \/}

trait TaskRepositoryComponent extends FutureMonad {
  self: ProjectRepositoryComponent with
        PartRepositoryComponent =>

  val taskRepository: TaskRepository

  trait TaskRepository {
    def list: Future[\/[Fail, IndexedSeq[Task]]]
    def list(project: Project): Future[\/[Fail, IndexedSeq[Task]]]
    def list(part: Part): Future[\/[Fail, IndexedSeq[Task]]]
    def list(project: Project, partNum: Int): Future[\/[Fail, IndexedSeq[Task]]]

    def find(id: UUID): Future[\/[Fail, Task]]
    def findNow(student: User, project: Project): Future[\/[Fail, Task]]
    def find(project: Project, partNum: Int, taskNum: Int): Future[\/[Fail, Task]]

    def insert(task: Task)(implicit conn: Connection): Future[\/[Fail, Task]]
    def update(task: Task)(implicit conn: Connection): Future[\/[Fail, Task]]
    def delete(task: Task)(implicit conn: Connection): Future[\/[Fail, Task]]
    def delete(part: Part)(implicit conn: Connection): Future[\/[Fail, Task]]
  }
}
