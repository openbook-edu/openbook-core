package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.UserToken
import com.github.mauricio.async.db.{Connection}
import scala.concurrent.Future
import scalaz.\/
/**
 * Handles the CRUD operations related to the token of a new user when signing up or resetting the password.
 * Created by ryanez on 11/02/16.
 */
trait UserTokenRepository extends Repository {
  def find(userId: UUID, tokenType: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, UserToken]]
  def findTokenByNonce(nonce: String, tokenType: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, UserToken]]
  def insert(userId: UUID, nonce: String, tokenType: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, UserToken]]
  def update(userId: UUID, nonce: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, UserToken]]
  def delete(userId: UUID, tokenType: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, UserToken]]
}
