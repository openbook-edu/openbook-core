package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.Subscription
import com.github.mauricio.async.db.{Connection, RowData}
import play.api.Logger
import scalaz.\/

import scala.concurrent.Future

class SubscriptionRepositoryPostgres(
    val cacheRepository: CacheRepository
) extends SubscriptionRepository with PostgresRepository[Subscription] {
  override val entityName = "Subscription"
  override def constructor(row: RowData): Subscription = {
    Subscription(
      row("id").asInstanceOf[String],
      row("version").asInstanceOf[Long],
      row("account_id").asInstanceOf[UUID],
      row("planId").asInstanceOf[UUID],
      row("currentPeriodEnd").asInstanceOf[Long],
      row("cancelAtPeriodEnd").asInstanceOf[Boolean]
    )
  }

  val Table: String = "subscriptions"
  val Fields: String = "id, version, account_id, planId, currentPeriodEnd, cancelAtPeriodEnd"
  val FieldsWithTable: String = Fields.split(", ").map({ field => s"$Table." + field }).mkString(", ")
  val QMarks: String = Fields.split(", ").map({ _ => "?" }).mkString(", ")

  val Select: String =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE id = ?
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
       |SET version = ?, account_id = ?, planId = ?, currentPeriodEnd = ?, cancelAtPeriodEnd = ?
       |WHERE id = ?
       |RETURNING $Fields
     """.stripMargin

  val Delete: String =
    s"""
       |DELETE FROM $Table
       |WHERE id = ?
       |RETURNING $Fields
     """.stripMargin

  /**
   * Get stripe plan subscription information by stripe ID string; we don't cache the information.
   * @param id String
   * @param conn implicit database connection
   * @return a Subscription or an error
   */
  def get(id: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Subscription]] =
    queryOne(Select, Seq[Any](id))
  
  def listByAccountId(accountId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Subscription]]] =
    queryList(SelectByAccountId, Seq[Any](accountId))
    
  def insert(subscription: Subscription)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Subscription]] = {
    val params = Seq[Any](
      subscription.id, 1, subscription.accountId, subscription.planId, subscription.currentPeriodEnd, subscription.cancelAtPeriodEnd
    )

    lift(queryOne(Insert, params))
  }

  def update(subscription: Subscription)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Subscription]] = {
    val params = Seq[Any](
      1, subscription.accountId, subscription.planId, subscription.currentPeriodEnd, subscription.cancelAtPeriodEnd, subscription.id
    )

    Logger.debug(s"Doing UPDATE on subscription: $Update\nwith parameters $params")
    for {
      current <- lift(queryOne(Select, Seq[Any](subscription.id)))
      _ = Logger.debug(s"Subscription before update: $current")
      updated <- lift(queryOne(Update, params))
      _ = Logger.debug(s"Updated subscription: $updated")
    } yield updated
  }

  def delete(id: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Subscription]] = {
    for {
      deleted <- lift(queryOne(Delete, Seq[Any](id)))
    } yield deleted
  }
}

