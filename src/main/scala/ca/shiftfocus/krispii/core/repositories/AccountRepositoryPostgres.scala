package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.{ Account, AccountStatus }
import com.github.mauricio.async.db.{ Connection, RowData }
import org.joda.time.DateTime
import play.api.libs.json.{ JsValue, Json }

import scala.concurrent.Future
import scalaz.\/

class AccountRepositoryPostgres extends AccountRepository with PostgresRepository[Account] {
  override val entityName = "Account"
  override def constructor(row: RowData): Account = {
    Account(
      row("id").asInstanceOf[UUID],
      row("version").asInstanceOf[Long],
      row("user_id").asInstanceOf[UUID],
      row("status").asInstanceOf[String],
      Option(row("customer")).map(customer => Json.parse(customer.asInstanceOf[String])),
      IndexedSeq.empty[JsValue],
      Option(row("active_until")).map(_.asInstanceOf[DateTime]),
      Option(row("overdue_started_at")).map(_.asInstanceOf[DateTime]),
      Option(row("overdue_ended_at")).map(_.asInstanceOf[DateTime]),
      Option(row("overdue_plan_id")).map(_.asInstanceOf[String])
    )
  }

  val Table = "accounts"
  val Fields = "id, version, user_id, status, customer, active_until, overdue_started_at, overdue_ended_at, overdue_plan_id"
  val QMarks = "?, ?, ?, ?, ?, ?, ?, ?, ?"

  val Select =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE id = ?
     """.stripMargin

  val SelectByUserId =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE user_id = ?
     """.stripMargin

  val SelectByCustomerId =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE customer::jsonb->>'id' = ?
     """.stripMargin

  val Insert =
    s"""
       |INSERT INTO $Table ($Fields)
       |VALUES ($QMarks)
       |RETURNING $Fields
     """.stripMargin

  val Update =
    s"""
       |UPDATE $Table
       |SET version = ?, user_id = ?, status = ?, customer = ?, active_until = ?, overdue_started_at = ?, overdue_ended_at = ?, overdue_plan_id = ?
       |WHERE id = ?
       |RETURNING $Fields
     """.stripMargin

  val Delete =
    s"""
       |DELETE FROM $Table
       |WHERE user_id = ?
       |RETURNING $Fields
     """.stripMargin

  // TODO - add cache
  def get(accountId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Account]] = {
    queryOne(Select, Seq[Any](accountId))
  }

  // TODO - add cache
  def getByUserId(userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Account]] = {
    queryOne(SelectByUserId, Seq[Any](userId))
  }

  // TODO - add cache
  def getByCustomerId(customerId: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Account]] = {
    queryOne(SelectByCustomerId, Seq[Any](customerId))
  }

  // TODO - add cache
  def insert(account: Account)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Account]] = {
    val params = Seq[Any](
      account.id, 1, account.userId, account.status, account.customer, account.activeUntil, account.overdueStartedAt, account.overdueEndedAt, account.overduePlanId
    )

    queryOne(Insert, params)
  }

  // TODO - add cache
  def update(account: Account)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Account]] = {
    val params = Seq[Any](
      account.version + 1, account.userId, account.status, account.customer, account.activeUntil, account.overdueStartedAt, account.overdueEndedAt, account.overduePlanId, account.id
    )

    queryOne(Update, params)
  }

  // TODO - add cache
  def delete(userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Account]] = {
    queryOne(Delete, Seq[Any](userId))
  }
}

