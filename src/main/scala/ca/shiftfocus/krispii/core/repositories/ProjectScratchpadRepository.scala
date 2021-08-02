package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.user.User
import ca.shiftfocus.krispii.core.models.{Project, _}
import com.github.mauricio.async.db.Connection

import scala.concurrent.Future
import scalaz.\/

trait ProjectScratchpadRepository extends Repository {

  val documentRepository: DocumentRepository

  def list(project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[ProjectScratchpad]]]
  def list(user: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[ProjectScratchpad]]]

  def find(user: User, project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, ProjectScratchpad]]

  def insert(projectScratchpad: ProjectScratchpad)(implicit conn: Connection): Future[\/[RepositoryError.Fail, ProjectScratchpad]]

  def delete(projectScratchpad: ProjectScratchpad)(implicit conn: Connection): Future[\/[RepositoryError.Fail, ProjectScratchpad]]
  def delete(project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[ProjectScratchpad]]]
}
