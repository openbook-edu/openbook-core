package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.Connection
import ca.shiftfocus.krispii.core.models._
import java.util.UUID
import scala.concurrent.Future
import scalaz.{ \/ }

trait PartRepository extends Repository {
  val taskRepository: TaskRepository

  def list(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Part]]]
  def list(project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Part]]]
  def list(project: Project, fetchTasks: Boolean, fetchComponents: Boolean = false)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Part]]]
  def list(component: Component)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Part]]]
  def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Part]]
  def find(project: Project, position: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Part]]
  def find(id: UUID, fetchTasks: Boolean)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Part]]
  def find(project: Project, position: Int, fetchTasks: Boolean)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Part]]
  def insert(part: Part)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Part]]
  def update(part: Part)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Part]]
  def delete(part: Part)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Part]]
  def delete(project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Part]]]
}
