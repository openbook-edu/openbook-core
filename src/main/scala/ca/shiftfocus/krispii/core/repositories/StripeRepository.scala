package ca.shiftfocus.krispii.core.repositories

import java.util.UUID
import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.Connection
import play.api.libs.json.JsValue
import scala.concurrent.Future
import scalaz.\/

trait StripeRepository extends Repository {
  // SUBSCRIPTIONS
  def listSubscriptions(userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[JsValue]]]
  def createSubscription(userId: UUID, subscription: JsValue)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JsValue]]
  def updateSubscription(userId: UUID, subscriptionId: String, subscription: JsValue)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JsValue]]
  def moveSubscriptions(oldUserId: UUID, newUserId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[JsValue]]]
  def deleteSubscription(userId: UUID, subscriptionId: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JsValue]]

  // EVENTS
  def getEvent(eventId: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JsValue]]
  def createEvent(eventId: String, eventType: String, event: JsValue)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JsValue]]
}
