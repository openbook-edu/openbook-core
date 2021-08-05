package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.Connection
import play.api.libs.json.JsValue
import scalaz.\/

import scala.concurrent.Future

trait StripeEventRepository extends Repository {
  // SUBSCRIPTIONS are now handled by StripeSubscriptionRepository
  /*def listSubscriptionsByUser(userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Subscription]]]
  def listSubscriptionsByAccount(accountId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Subscription]]]
  def createSubscription(userId: UUID, subscription: JsValue)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Subscription]]
  def updateSubscription(userId: UUID, subscriptionId: String, subscription: Subscription)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JsValue]]
  def moveSubscriptions(oldUserId: UUID, newUserId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[JsValue]]]
  def deleteSubscription(userId: UUID, subscriptionId: Subscription)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JsValue]] */

  // EVENTS
  def getEvent(eventId: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JsValue]]
  def createEvent(eventId: String, eventType: String, event: JsValue)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JsValue]]
}
