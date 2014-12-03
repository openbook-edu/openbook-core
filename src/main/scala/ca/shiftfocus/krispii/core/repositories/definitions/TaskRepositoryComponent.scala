package ca.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks.Task
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future

trait TaskRepositoryComponent {
  /**
   * Value storing an instance of the repository. Should be overridden with
   * a concrete implementation to be used via dependency injection.
   */
  val taskRepository: TaskRepository

  trait TaskRepository {
    /**
     * The usual CRUD functions for the parts table.
     */
    def list: Future[IndexedSeq[Task]]
    def list(project: Project): Future[IndexedSeq[Task]]
    def list(part: Part): Future[IndexedSeq[Task]]
    def list(project: Project, partNum: Int): Future[IndexedSeq[Task]]
    //def list(section: Section): Future[IndexedSeq[Task]]
    def find(id: UUID): Future[Option[Task]]
    def find(project: Project, partNum: Int, taskNum: Int): Future[Option[Task]]
    //def find(slug: String): Future[Option[Task]]
    def insert(task: Task)(implicit conn: Connection): Future[Task]
    def update(task: Task)(implicit conn: Connection): Future[Task]
    def delete(task: Task)(implicit conn: Connection): Future[Boolean]
    def delete(part: Part)(implicit conn: Connection): Future[Boolean]
    //def clearPartCache(part: Part)
  }
}
