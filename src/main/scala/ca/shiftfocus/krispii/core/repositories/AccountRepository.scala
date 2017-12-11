package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.krispii.core.models.Account
import com.github.mauricio.async.db.Connection

import scala.concurrent.Future
import scalaz.\/

trait AccountRepository extends Repository {
  def get(accountId: UUID)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Account]]
  def getByUserId(userId: UUID)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Account]]
  def getByCustomerId(customerId: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Account]]
  def insert(account: Account)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Account]]
  def update(account: Account)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Account]]
  def delete(userId: UUID)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Account]]
}
