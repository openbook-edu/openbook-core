package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.ProjectToken
import com.github.mauricio.async.db.Connection

import scala.concurrent.Future
import scalaz.\/

trait ProjectTokenRepository extends Repository {
  def get(token: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, ProjectToken]]
  def insert(projectToken: ProjectToken)(implicit conn: Connection): Future[\/[RepositoryError.Fail, ProjectToken]]
  def delete(projectToken: ProjectToken)(implicit conn: Connection): Future[\/[RepositoryError.Fail, ProjectToken]]
}
