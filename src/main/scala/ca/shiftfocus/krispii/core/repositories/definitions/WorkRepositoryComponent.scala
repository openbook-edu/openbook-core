package ca.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks.Task
import ca.shiftfocus.krispii.core.models.work.Work
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future

trait WorkRepositoryComponent {
  val workRepository: WorkRepository
  
  trait WorkRepository {
    def list(user: User, section: Class, project: Project): Future[IndexedSeq[Work]]
    def list(user: User, task: Task, section: Class): Future[IndexedSeq[Work]]

    def find(workId: UUID): Future[Option[Work]]
    def find(user: User, task: Task, section: Class): Future[Option[Work]]
    def find(user: User, task: Task, section: Class, revision: Long): Future[Option[Work]]

    def insert(work: Work)(implicit conn: Connection): Future[Work]
    def update(work: Work)(implicit conn: Connection): Future[Work]
    def update(work: Work, newRevision: Boolean)(implicit conn: Connection): Future[Work]

    def delete(work: Work, thisRevisionOnly: Boolean = false)(implicit conn: Connection): Future[Boolean]
    def delete(task: Task)(implicit conn: Connection): Future[Boolean]
    //def forceComplete(task: Task, section: Section)(implicit conn: Connection): Future[Boolean]
  }
}