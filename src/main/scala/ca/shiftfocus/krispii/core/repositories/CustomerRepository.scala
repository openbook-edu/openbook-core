package ca.shiftfocus.krispii.core.repositories

import java.util.UUID
import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.Customer
import com.github.mauricio.async.db.Connection
import scala.concurrent.Future
import scalaz.\/

trait CustomerRepository extends Repository {
  def get(id: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Customer]]
  def listByAccountId(accountId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Customer]]]
  def insert(customer: Customer)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Customer]]
  def update(customer: Customer)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Customer]]
  def delete(id: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Customer]]
}
