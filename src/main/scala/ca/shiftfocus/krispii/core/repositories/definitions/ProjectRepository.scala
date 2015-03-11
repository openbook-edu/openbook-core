package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.Connection
import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future
import scalaz.{EitherT, \/}

trait ProjectRepository extends Repository {
  val partRepository: PartRepository

  def list(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Project]]]
  def list(course: Course)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Project]]]

  def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Project]]
  def find(projectId: UUID, user: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Project]]
  def find(slug: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Project]]

  def insert(project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Project]]
  def update(project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Project]]
  def delete(project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Project]]
}