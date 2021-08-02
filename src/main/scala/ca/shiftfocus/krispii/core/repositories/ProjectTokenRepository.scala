package ca.shiftfocus.krispii.core.repositories

import java.util.UUID
import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.ProjectToken
import com.github.mauricio.async.db.Connection
import scala.concurrent.Future
import scalaz.\/

trait ProjectTokenRepository extends Repository {
  def find(token: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, ProjectToken]]
  def list(projectId: UUID, email: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[ProjectToken]]]
  def listByProject(projectId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[ProjectToken]]]
  def listByEmail(email: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[ProjectToken]]]
  def insert(projectToken: ProjectToken)(implicit conn: Connection): Future[\/[RepositoryError.Fail, ProjectToken]]
  def delete(projectToken: ProjectToken)(implicit conn: Connection): Future[\/[RepositoryError.Fail, ProjectToken]]
}
