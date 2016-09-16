package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.{ Connection, RowData }
import org.joda.time.DateTime
import play.api.libs.json.{ JsValue, Json }

import scala.concurrent.Future
import scalaz.\/

/**
 * Work with database tables: users_subscriptions, stripe_events
 */
class StripeRepositoryPostgres extends StripeRepository with PostgresRepository[JsValue] {
  override val entityName = "Stripe"
  override def constructor(row: RowData): JsValue = {
    Json.parse(row("data").asInstanceOf[String])
  }

  //------ SUBSCRIPTIONS -----------------------------------------------------------------------------------------------

  val ListSubscriptionsByUser =
    s"""
      |SELECT subscription as data
      |FROM users_subscriptions
      |WHERE user_id = ?
     """.stripMargin

  val InsertSubscription =
    s"""
       |INSERT INTO users_subscriptions (user_id, subscription)
       |VALUES (?, ?)
       |RETURNING subscription as data
     """.stripMargin

  val UpdateSubscription =
    s"""
       |UPDATE users_subscriptions
       |SET subscription = ?
       |WHERE user_id = ?
       |  AND subscription::jsonb->>'id' = ?
       |RETURNING subscription as data
     """.stripMargin

  val DeleteSubscription =
    s"""
       |DELETE FROM users_subscriptions
       |WHERE user_id = ?
       |  AND subscription::jsonb->>'id' = ?
       |RETURNING subscription as data
     """.stripMargin

  //------ EVENTS ------------------------------------------------------------------------------------------------------

  val GetEvent =
    s"""
       |SELECT event as data
       |FROM stripe_events
       |WHERE id = ?
     """.stripMargin

  val InsertEvent =
    s"""
       |INSERT INTO stripe_events (id, type, event, created_at)
       |VALUES (?, ?, ?, ?)
       |RETURNING event as data
     """.stripMargin

  //--------------------------------------------------------------------------------------------------------------------

  // SUBSCRIPTIONS

  def listSubscriptions(userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[JsValue]]] = {
    queryList(ListSubscriptionsByUser, Seq[Any](userId))
  }

  def createSubscription(userId: UUID, subscription: JsValue)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JsValue]] = {
    queryOne(InsertSubscription, Seq[Any](userId, subscription))
  }

  // TODO - update where subscription id
  def updateSubscription(userId: UUID, subscriptionId: String, subscription: JsValue)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JsValue]] = {
    queryOne(UpdateSubscription, Seq[Any](subscription, userId, subscriptionId))
  }

  // TODO - delete where subscription id
  def deleteSubscription(userId: UUID, subscriptionId: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JsValue]] = {
    queryOne(DeleteSubscription, Seq[Any](userId, subscriptionId))
  }

  // EVENTS

  def getEvent(eventId: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JsValue]] = {
    queryOne(GetEvent, Seq[Any](eventId))
  }

  def createEvent(eventId: String, eventType: String, event: JsValue)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JsValue]] = {
    queryOne(InsertEvent, Seq[Any](eventId, eventType, event, new DateTime()))
  }
}
