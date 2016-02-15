package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.krispii.core.models.UserToken
import com.github.mauricio.async.db.{ RowData, Connection }

import scala.concurrent.Future
import scalaz.\/

/**
 * Handles the CRUD operations related to the token of a new user when signing up.
 * Created by ryanez on 11/02/16.
 */
trait ActivationRepository extends Repository {
  def constructor(row: RowData): UserToken
  def find(userId: UUID)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, UserToken]]
  def find(email: String)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, UserToken]]
  def insert(userId: UUID, nonce: String)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, UserToken]]
  def update(userId: UUID, nonce: String)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, UserToken]]
  def delete(userId: UUID)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, UserToken]]
}
