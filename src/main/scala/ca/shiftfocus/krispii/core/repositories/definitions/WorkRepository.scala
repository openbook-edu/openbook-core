package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks.Task
import ca.shiftfocus.krispii.core.models.work.Work
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future
import scalaz.\/

trait WorkRepository extends Repository {
  def list(user: User, project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Work]]]
  def list(user: User, task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Work]]]
  def list(task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Work]]]

  def find(workId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Work]]
  def find(user: User, task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Work]]
  def find(user: User, task: Task, version: Long)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Work]]

  def insert(work: Work)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Work]]
  def update(work: Work)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Work]]
  def update(work: Work, newRevision: Boolean)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Work]]

  def delete(work: Work, thisRevisionOnly: Boolean = false)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Work]]
  def delete(task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Work]]]
}