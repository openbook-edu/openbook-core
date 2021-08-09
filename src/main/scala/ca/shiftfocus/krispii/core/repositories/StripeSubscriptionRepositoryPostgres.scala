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
      row("subscription_id").asInstanceOf[String],
      row("version").asInstanceOf[Long],
      row("customer_id").asInstanceOf[String],
      row("account_id").asInstanceOf[UUID],
      row("plan_id").asInstanceOf[String],
      row("current_period_start").asInstanceOf[Long],
      row("current_period_end").asInstanceOf[Long],
      row("cancel_at_period_end").asInstanceOf[Boolean]
    )
  }

  private val Table = "stripe_subscriptions"
  private val Fields = "subscription_id, version, customer_id, account_id, plan_id, current_period_start, current_period_end, cancel_at_period_end"

  // not used
  // private val FieldsWithTable = Fields.split(", ").map({ field => s"$Table." + field }).mkString(", ")
  private val QMarks = Fields.split(", ").map({ _ => "?" }).mkString(", ")

  private val Select =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE subscription_id = ?
     """.stripMargin

  private val SelectByCustomerId =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE customer_id = ?
     """.stripMargin

  private val SelectByAccountId =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE account_id = ?
     """.stripMargin

  private val Insert =
    s"""
       |INSERT INTO $Table ($Fields)
       |VALUES ($QMarks)
       |RETURNING $Fields
     """.stripMargin

  private val Update =
    s"""
       |UPDATE $Table
       |SET version = ?, customer_id = ?, account_id = ?, plan_id = ?, current_period_start = ?, current_period_end = ?, cancel_at_period_end = ?
       |WHERE subscription_id = ?
       |RETURNING $Fields
     """.stripMargin

  private val Delete =
    s"""
       |DELETE FROM $Table
       |WHERE subscription_id = ?
       |RETURNING $Fields
     """.stripMargin

  /**
   * Get stripe plan subscription information by Stripe subscription ID string; we don't cache the information.
   * @param subscription_id string furnished by stripe
   * @param conn implicit database connection
   * @return a StripeSubscription or an error
   */
  def get(subscription_id: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, StripeSubscription]] =
    queryOne(Select, Seq[Any](subscription_id))

  /**
   * Get all stripe plan subscriptions associated with a krispii account
   * @param customerId Stripe customer ID string
   * @param conn implicit database connection
   * @return an indexed sequence of StripeSubscriptions or an error
   */
  def listByCustomerId(customerId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[StripeSubscription]]] =
    queryList(SelectByCustomerId, Seq[Any](customerId))

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
      subscription.subscriptionId, 1, subscription.customerId, subscription.accountId, subscription.planId,
      subscription.currentPeriodStart, subscription.currentPeriodEnd, subscription.cancelAtPeriodEnd
    )
    lift(queryOne(Insert, params))
  }

  def update(subscription: StripeSubscription)(implicit conn: Connection): Future[\/[RepositoryError.Fail, StripeSubscription]] = {
    val params = Seq[Any](
      1, subscription.customerId, subscription.accountId, subscription.planId,
      subscription.currentPeriodStart, subscription.currentPeriodEnd, subscription.cancelAtPeriodEnd, subscription.subscriptionId
    )

    Logger.debug(s"Doing UPDATE on subscription: $Update\nwith parameters $params")
    for {
      current <- lift(queryOne(Select, Seq[Any](subscription.customerId)))
      _ = Logger.debug(s"StripeSubscription before update: $current")
      updated <- lift(queryOne(Update, params))
      _ = Logger.debug(s"Updated subscription: $updated")
    } yield updated
  }

  def delete(subscriptionId: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, StripeSubscription]] = {
    for {
      deleted <- lift(queryOne(Delete, Seq[Any](subscriptionId)))
    } yield deleted
  }
}

