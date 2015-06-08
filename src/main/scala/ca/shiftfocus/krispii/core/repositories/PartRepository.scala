package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.Connection
import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import java.util.UUID
import scala.concurrent.Future
import scalacache.ScalaCache
import scalaz.{ EitherT, \/ }

trait PartRepository extends Repository {
  val taskRepository: TaskRepository

  def list(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[Part]]]
  def list(project: Project)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[Part]]]
  def list(project: Project, fetchTasks: Boolean)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[Part]]]
  def list(component: Component)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[Part]]]
  def find(id: UUID)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Part]]
  def find(project: Project, position: Int)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Part]]
  def find(id: UUID, fetchTasks: Boolean)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Part]]
  def find(project: Project, position: Int, fetchTasks: Boolean)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Part]]
  def insert(part: Part)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Part]]
  def update(part: Part)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Part]]
  def delete(part: Part)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Part]]
  def delete(project: Project)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[Part]]]
}
