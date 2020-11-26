package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.Customer
import com.github.mauricio.async.db.{Connection, RowData}
import play.api.Logger
import scalaz.\/

import scala.concurrent.Future

class CustomerRepositoryPostgres(
    val cacheRepository: CacheRepository
) extends CustomerRepository with PostgresRepository[Customer] {
  override val entityName = "Customer"
  override def constructor(row: RowData): Customer = {
    Customer(
      row("id").asInstanceOf[String],
      row("version").asInstanceOf[Long],
      row("account_id").asInstanceOf[UUID],
      row("email").asInstanceOf[String],
      row("givenname").asInstanceOf[String],
      row("surname").asInstanceOf[String],
      row("exp_month").asInstanceOf[String],
      row("exp_year").asInstanceOf[String],
      row("brand").asInstanceOf[String],
      row("last4").asInstanceOf[String]
    )
  }

  val Table: String = "customers"
  val Fields: String = "id, version, account_id, email, givenname, surname, exp_month, exp_year, brand, last4"
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
       |SET version = ?, account_id = ?, email = ?, givenname = ?, surname = ?, exp_month = ?, exp_year = ?, brand = ?, last4 = ?
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
   * Get stripe customer information by stripe ID string; we don't cache the information.
   * @param id String
   * @param conn implicit database connection
   * @return a Customer or an error
   */
  def get(id: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Customer]] =
    queryOne(Select, Seq[Any](id))

  /**
   * Get stripe customer information by krispii account ID. Theoretically a list, which will
   * be either empty or a single Customer
   * @param accountId unique ID of krispii account
   * @param conn implicit database connection
   * @return indexed sequence of Customer (zero or one elements), or an error
   */
  def listByAccountId(accountId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Customer]]] =
    queryList(SelectByAccountId, Seq[Any](accountId))
    
  def insert(customer: Customer)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Customer]] = {
    val params = Seq[Any](
      customer.id, 1, customer.accountId, customer.email, customer.givenname, customer.surname, customer.exp_month,
      customer.exp_year, customer.brand, customer.last4
    )

    lift(queryOne(Insert, params))
  }

  def update(customer: Customer)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Customer]] = {
    val params = Seq[Any](
      1, customer.accountId, customer.email, customer.givenname, customer.surname, customer.exp_month,
      customer.exp_year, customer.brand, customer.last4, customer.id
    )

    Logger.debug(s"Doing UPDATE on customer: $Update\nwith parameters $params")
    for {
      current <- lift(queryOne(Select, Seq[Any](customer.id)))
      _ = Logger.debug(s"Customer before update: $current")
      updated <- lift(queryOne(Update, params))
      _ = Logger.debug(s"Updated customer: $updated")
    } yield updated
  }

  def delete(id: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Customer]] = {
    for {
      deleted <- lift(queryOne(Delete, Seq[Any](id)))
    } yield deleted
  }
}

