package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.{ Connection, RowData }
import play.api.libs.json.{ JsValue, Json }
import scala.concurrent.Future
import scalaz.\/

class SubscriptionRepositoryPostgres extends SubscriptionRepository with PostgresRepository[JsValue] {
  override val entityName = "Subscription"
  override def constructor(row: RowData): JsValue = {
    Json.parse(row("subscription").asInstanceOf[String])
  }

  val ListByUser =
    s"""
      |SELECT subscription
      |FROM users_subscriptions
      |WHERE user_id = ?
     """.stripMargin

  val Insert =
    s"""
       |INSERT INTO users_subscriptions (user_id, subscription)
       |VALUES (?, ?)
       |RETURNING subscription
     """.stripMargin

  def listSubscriptions(userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[JsValue]]] = {
    queryList(ListByUser, Seq[Any](userId))
  }

  def createSubscription(userId: UUID, subscription: JsValue)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JsValue]] = {
    queryOne(Insert, Seq[Any](userId, subscription))
  }
}
