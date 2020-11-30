package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.stripe.StripeSubscription
import com.github.mauricio.async.db.{Connection, RowData}
import play.api.Logger
import scalaz.\/

import scala.concurrent.Future

class StripeSubscriptionRepositoryPostgres(
    val cacheRepository: CacheRepository
) extends StripeSubscriptionRepository with PostgresRepository[StripeSubscription] {
  override val entityName = "StripeSubscription"
  override def constructor(row: RowData): StripeSubscription = {
    StripeSubscription(
      row("customer_id").asInstanceOf[String],
      row("version").asInstanceOf[Long],
      row("account_id").asInstanceOf[UUID],
      row("plan_id").asInstanceOf[String],
      row("current_period_end").asInstanceOf[Long],
      row("cancel_at_period_end").asInstanceOf[Boolean]
    )
  }

  val Table: String = "stripe_subscriptions"
  val Fields: String = "customer_id, version, account_id, plan_id, current_period_end, cancel_at_period_end"
  val FieldsWithTable: String = Fields.split(", ").map({ field => s"$Table." + field }).mkString(", ")
  val QMarks: String = Fields.split(", ").map({ _ => "?" }).mkString(", ")

  val Select: String =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE customer_id = ?
     """.stripMargin

  val SelectByAccountId: String =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE account_id = ?
     """.stripMargin

  val Insert: String =
    s"""
       |INSERT INTO $Table ($Fields)
       |VALUES ($QMarks)
       |RETURNING $Fields
     """.stripMargin

  val Update: String =
    s"""
       |UPDATE $Table
       |SET version = ?, account_id = ?, plan_id = ?, current_period_end = ?, cancel_at_period_end = ?
       |WHERE customer_id = ?
       |RETURNING $Fields
     """.stripMargin

  val Delete: String =
    s"""
       |DELETE FROM $Table
       |WHERE customer_id = ?
       |RETURNING $Fields
     """.stripMargin

  /**
   * Get stripe plan subscription information by stripe ID string; we don't cache the information.
   * @param id string furnished by stripe
   * @param conn implicit database connection
   * @return a StripeSubscription or an error
   */
  def get(id: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, StripeSubscription]] =
    queryOne(Select, Seq[Any](id))

  /**
   * Get all stripe plan subscriptions associated with a krispii account
   * @param accountId krispii account unique ID
   * @param conn implicit database connection
   * @return an indexed sequence of StripeSubscriptions or an error
   */
  def listByAccountId(accountId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[StripeSubscription]]] =
    queryList(SelectByAccountId, Seq[Any](accountId))

  def insert(subscription: StripeSubscription)(implicit conn: Connection): Future[\/[RepositoryError.Fail, StripeSubscription]] = {
    val params = Seq[Any](
      subscription.customerId, 1, subscription.accountId, subscription.planId, subscription.currentPeriodEnd, subscription.cancelAtPeriodEnd
    )

    lift(queryOne(Insert, params))
  }

  def update(subscription: StripeSubscription)(implicit conn: Connection): Future[\/[RepositoryError.Fail, StripeSubscription]] = {
    val params = Seq[Any](
      1, subscription.accountId, subscription.planId, subscription.currentPeriodEnd, subscription.cancelAtPeriodEnd, subscription.customerId
    )

    Logger.debug(s"Doing UPDATE on subscription: $Update\nwith parameters $params")
    for {
      current <- lift(queryOne(Select, Seq[Any](subscription.customerId)))
      _ = Logger.debug(s"StripeSubscription before update: $current")
      updated <- lift(queryOne(Update, params))
      _ = Logger.debug(s"Updated subscription: $updated")
    } yield updated
  }

  def delete(id: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, StripeSubscription]] = {
    for {
      deleted <- lift(queryOne(Delete, Seq[Any](id)))
    } yield deleted
  }
}

