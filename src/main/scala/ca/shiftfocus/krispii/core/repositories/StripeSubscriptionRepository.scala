package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.stripe.StripeSubscription
import com.github.mauricio.async.db.Connection

import scala.concurrent.Future
import scalaz.\/

trait StripeSubscriptionRepository extends Repository {
  def get(id: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, StripeSubscription]]
  def listByAccountId(accountId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[StripeSubscription]]]
  def insert(subscription: StripeSubscription)(implicit conn: Connection): Future[\/[RepositoryError.Fail, StripeSubscription]]
  def update(subscription: StripeSubscription)(implicit conn: Connection): Future[\/[RepositoryError.Fail, StripeSubscription]]
  def delete(id: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, StripeSubscription]]
}
