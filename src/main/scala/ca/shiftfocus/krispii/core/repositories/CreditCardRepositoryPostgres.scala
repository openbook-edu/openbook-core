package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.stripe.CreditCard
import com.github.mauricio.async.db.{Connection, RowData}
import play.api.Logger
import scalaz.\/

import scala.concurrent.Future

class CreditCardRepositoryPostgres(
    val cacheRepository: CacheRepository
) extends CreditCardRepository with PostgresRepository[CreditCard] {
  override val entityName = "CreditCard"
  override def constructor(row: RowData): CreditCard = {
    CreditCard(
      row("customer_id").asInstanceOf[String],
      row("version").asInstanceOf[Long],
      row("account_id").asInstanceOf[UUID],
      row("email").asInstanceOf[String],
      row("givenname").asInstanceOf[String],
      row("surname").asInstanceOf[String],
      Option(row("exp_month").asInstanceOf[Long]),
      Option(row("exp_year").asInstanceOf[Long]),
      row("brand").asInstanceOf[String],
      row("last4").asInstanceOf[String]
    )
  }

  val Table: String = "creditcards"
  val Fields: String = "customer_id, version, account_id, email, givenname, surname, exp_month, exp_year, brand, last4"
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
       |SET version = ?, account_id = ?, email = ?, givenname = ?, surname = ?, exp_month = ?, exp_year = ?, brand = ?, last4 = ?
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
   * Get credit card information by stripe customer ID string; we don't cache the information.
   * @param customerId String
   * @param conn implicit database connection
   * @return a CreditCard or an error
   */
  def get(customerId: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, CreditCard]] =
    queryOne(Select, Seq[Any](customerId))

  /**
   * Get credit card information by krispii account ID. Theoretically a list, which will
   * be either empty or a single CreditCard
   * @param accountId unique ID of krispii account
   * @param conn implicit database connection
   * @return indexed sequence of CreditCard (zero or one elements), or an error
   */
  def listByAccountId(accountId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[CreditCard]]] =
    queryList(SelectByAccountId, Seq[Any](accountId))

  def insert(card: CreditCard)(implicit conn: Connection): Future[\/[RepositoryError.Fail, CreditCard]] = {
    val params = Seq[Any](
      card.customerId, 1, card.accountId, card.email, card.givenname, card.surname, card.expMonth,
      card.expYear, card.brand, card.last4
    )

    lift(queryOne(Insert, params))
  }

  def update(card: CreditCard)(implicit conn: Connection): Future[\/[RepositoryError.Fail, CreditCard]] = {
    val params = Seq[Any](
      1, card.accountId, card.email, card.givenname, card.surname, card.expMonth,
      card.expYear, card.brand, card.last4, card.customerId
    )

    Logger.debug(s"Doing UPDATE on card: $Update\nwith parameters $params")
    for {
      current <- lift(queryOne(Select, Seq[Any](card.customerId)))
      _ = Logger.debug(s"CreditCard before update: $current")
      updated <- lift(queryOne(Update, params))
      _ = Logger.debug(s"Updated card: $updated")
    } yield updated
  }

  def delete(customer_id: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, CreditCard]] = {
    for {
      deleted <- lift(queryOne(Delete, Seq[Any](customer_id)))
    } yield deleted
  }
}

