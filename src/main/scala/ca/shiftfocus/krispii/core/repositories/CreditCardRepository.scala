package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.CreditCard
import com.github.mauricio.async.db.Connection

import scala.concurrent.Future
import scalaz.\/

trait CreditCardRepository extends Repository {
  def get(customerId: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, CreditCard]]
  def listByAccountId(accountId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[CreditCard]]]
  def insert(card: CreditCard)(implicit conn: Connection): Future[\/[RepositoryError.Fail, CreditCard]]
  def update(card: CreditCard)(implicit conn: Connection): Future[\/[RepositoryError.Fail, CreditCard]]
  def delete(customerId: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, CreditCard]]
}
