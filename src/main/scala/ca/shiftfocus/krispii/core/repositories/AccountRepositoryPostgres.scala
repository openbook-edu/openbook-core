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
      Option(row("active_until")).map(_.asInstanceOf[DateTime])
    )
  }

  val Table = "accounts"
  val Fields = "id, version, user_id, status, customer, active_until"
  val QMarks = "?, ?, ?, ?, ?, ?"

  val Select =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE id = ?
     """.stripMargin

  val SelectByUser =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE user_id = ?
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
       |SET version = ?, status = ?, customer = ?, active_until = ?
       |WHERE id = ?
       |RETURNING $Fields
     """.stripMargin

  def get(accountId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Account]] = {
    queryOne(Select, Seq[Any](accountId))
  }

  def getByUserId(userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Account]] = {
    queryOne(SelectByUser, Seq[Any](userId))
  }

  def insert(account: Account)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Account]] = {
    val params = Seq[Any](
      account.id, 1, account.userId, account.status, account.customer, account.activeUntil
    )

    queryOne(Insert, params)
  }

  def update(account: Account)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Account]] = {
    val params = Seq[Any](
      account.version + 1, account.status, account.customer, account.activeUntil, account.id
    )

    queryOne(Update, params)
  }
}

