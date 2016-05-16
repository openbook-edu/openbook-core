package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.{ Connection, RowData }
import play.api.libs.json.{ JsValue, Json }

import scala.concurrent.Future
import scalaz.\/

class CustomerRepositoryPostgres extends CustomerRepository with PostgresRepository[JsValue] {
  override val entityName = "Customer"
  override def constructor(row: RowData): JsValue = {
    Json.parse(row("customer").asInstanceOf[String])
  }

  val Select =
    s"""
       |SELECT customer
       |FROM users_customers
       |WHERE user_id = ?
     """.stripMargin

  val Insert =
    s"""
       |INSERT INTO users_customers (user_id, customer)
       |VALUES (?, ?)
       |RETURNING customer
     """.stripMargin

  val Update =
    s"""
       |UPDATE users_customers
       |SET customer = ?
       |WHERE user_id = ?
       |RETURNING customer
     """.stripMargin

  def getCustomer(userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JsValue]] = {
    queryOne(Select, Seq[Any](userId))
  }

  def createCustomer(userId: UUID, customer: JsValue)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JsValue]] = {
    queryOne(Insert, Seq[Any](userId, customer))
  }

  def updateCustomer(userId: UUID, customer: JsValue)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JsValue]] = {
    queryOne(Update, Seq[Any](customer, userId))
  }
}
