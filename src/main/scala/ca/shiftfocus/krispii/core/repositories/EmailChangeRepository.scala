package ca.shiftfocus.krispii.core.repositories

import java.util.UUID
import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.EmailChangeRequest
import com.github.mauricio.async.db.Connection
import scala.concurrent.Future
import scalaz.\/

trait EmailChangeRepository extends Repository {

  def find(userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, EmailChangeRequest]]
  def find(email: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, EmailChangeRequest]]
  def insert(ecr: EmailChangeRequest)(implicit conn: Connection): Future[\/[RepositoryError.Fail, EmailChangeRequest]]
  def delete(ecr: EmailChangeRequest)(implicit conn: Connection): Future[\/[RepositoryError.Fail, EmailChangeRequest]]
}