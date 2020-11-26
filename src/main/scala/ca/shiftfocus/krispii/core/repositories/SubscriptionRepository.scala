package ca.shiftfocus.krispii.core.repositories

import java.util.UUID
import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.Subscription
import com.github.mauricio.async.db.Connection
import scala.concurrent.Future
import scalaz.\/

trait SubscriptionRepository extends Repository {
  def get(id: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Subscription]]
  def listByAccountId(accountId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Subscription]]]
  def insert(subscription: Subscription)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Subscription]]
  def update(subscription: Subscription)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Subscription]]
  def delete(id: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Subscription]]
}
