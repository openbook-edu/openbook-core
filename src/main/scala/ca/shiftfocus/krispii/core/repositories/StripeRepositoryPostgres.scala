package ca.shiftfocus.krispii.core.repositories

import java.io.{PrintWriter, StringWriter}
import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.{Connection, RowData}
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json.{JsValue, Json}

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
    """
      |SELECT subscription as data
      |FROM users_subscriptions
      |WHERE user_id = ?
     """.stripMargin

  val InsertSubscription =
    """
       |INSERT INTO users_subscriptions (user_id, subscription)
       |VALUES (?, ?)
       |RETURNING subscription as data
     """.stripMargin

  val UpdateSubscription =
    """
       |UPDATE users_subscriptions
       |SET subscription = ?
       |WHERE user_id = ?
       |  AND subscription::jsonb->>'id' = ?
       |RETURNING subscription as data
     """.stripMargin

  val MoveSubscriptions =
    """
       |UPDATE users_subscriptions
       |SET user_id = ?
       |WHERE user_id = ?
       |RETURNING subscription as data
     """.stripMargin

  val DeleteSubscription =
    """
       |DELETE FROM users_subscriptions
       |WHERE user_id = ?
       |  AND subscription::jsonb->>'id' = ?
       |RETURNING subscription as data
     """.stripMargin

  //------ EVENTS ------------------------------------------------------------------------------------------------------

  val GetEvent =
    """
       |SELECT event as data
       |FROM stripe_events
       |WHERE id = ?
     """.stripMargin

  val InsertEvent =
    """
       |INSERT INTO stripe_events (id, type, event, created_at)
       |VALUES (?, ?, ?, ?)
       |RETURNING event as data
     """.stripMargin

  //--------------------------------------------------------------------------------------------------------------------

  // SUBSCRIPTIONS

  def listSubscriptions(userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[JsValue]]] = {
    Logger.debug("in StripeRepositoryPostgres listSubscriptions")
    val sw = new StringWriter
    val st = new RuntimeException
    st.printStackTrace(new PrintWriter(sw))
    Logger.debug(sw.toString)
    // val st = new RuntimeException().getStackTrace.mkString("\n")
    // st.take(10).foreach {Logger.debug)
    // Logger.debug(st)
    queryList(ListSubscriptionsByUser, Seq[Any](userId))
  }

  def createSubscription(userId: UUID, subscription: JsValue)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JsValue]] = {
    queryOne(InsertSubscription, Seq[Any](userId, subscription))
  }

  def updateSubscription(userId: UUID, subscriptionId: String, subscription: JsValue)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JsValue]] = {
    queryOne(UpdateSubscription, Seq[Any](subscription, userId, subscriptionId))
  }

  def moveSubscriptions(oldUserId: UUID, newUserId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[JsValue]]] = {
    queryList(MoveSubscriptions, Seq[Any](newUserId, oldUserId))
  }

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
