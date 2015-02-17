package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.repositories.error.RepositoryError
import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks.Task
import ca.shiftfocus.krispii.core.models.work.Work
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future
import scalaz.\/

trait WorkRepositoryComponent {
  val workRepository: WorkRepository

  trait WorkRepository {
    def list(user: User, course: Course, project: Project): Future[\/[RepositoryError, IndexedSeq[Work]]]
    def list(user: User, task: Task, course: Course): Future[\/[RepositoryError, IndexedSeq[Work]]]

    def find(workId: UUID): Future[Option[Work]]
    def find(user: User, task: Task, course: Course): Future[\/[RepositoryError, Work]]
    def find(user: User, task: Task, course: Course, revision: Long): Future[\/[RepositoryError, Work]]

    def insert(work: Work)(implicit conn: Connection): Future[\/[RepositoryError, Work]]
    def update(work: Work)(implicit conn: Connection): Future[\/[RepositoryError, Work]]
    def update(work: Work, newRevision: Boolean)(implicit conn: Connection): Future[\/[RepositoryError, Work]]

    def delete(work: Work, thisRevisionOnly: Boolean = false)(implicit conn: Connection): Future[\/[RepositoryError, Work]]
    def delete(task: Task)(implicit conn: Connection): Future[\/[RepositoryError, Work]]
    //def forceComplete(task: Task, course: Course)(implicit conn: Connection): Future[Boolean]
  }
}
