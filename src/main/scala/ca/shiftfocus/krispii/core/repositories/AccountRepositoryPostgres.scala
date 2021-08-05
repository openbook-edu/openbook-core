package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.Account
import ca.shiftfocus.krispii.core.models.stripe.StripeSubscription
import com.github.mauricio.async.db.{Connection, RowData}
import org.joda.time.DateTime
import play.api.Logger
import scalaz.{-\/, \/, \/-}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AccountRepositoryPostgres(
    val cacheRepository: CacheRepository,
    val creditCardRepository: CreditCardRepository,
    val stripeSubscriptionRepository: StripeSubscriptionRepository
) extends AccountRepository with PostgresRepository[Account] {
  override val entityName = "Account"
  override def constructor(row: RowData): Account = {
    Account(
      row("id").asInstanceOf[UUID],
      row("version").asInstanceOf[Long],
      row("user_id").asInstanceOf[UUID],
      row("status").asInstanceOf[String],
      None, // creditCard is initialized empty!
      IndexedSeq.empty[StripeSubscription], // subscriptions are initialized empty!
      Option(row("trial_started_at").asInstanceOf[DateTime]),
      Option(row("active_until").asInstanceOf[DateTime]),
      Option(row("overdue_started_at").asInstanceOf[DateTime]),
      Option(row("overdue_ended_at").asInstanceOf[DateTime]),
      // wouldn't it make more sense to register overdue for each subscription?
      Option(row("overdue_plan_id").asInstanceOf[String])
    )
  }

  val Table: String = "accounts"
  // we will ignore the customer field in the accounts table from now on!
  val Fields: String = "id, version, user_id, status, trial_started_at, active_until, overdue_started_at, overdue_ended_at, overdue_plan_id"
  val FieldsWithTable: String = Fields.split(", ").map({ field => s"$Table." + field }).mkString(", ")
  val QMarks: String = Fields.split(", ").map({ _ => "?" }).mkString(", ")

  val Select: String =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE id = ?
     """.stripMargin

  val SelectByUserId: String =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE user_id = ?
     """.stripMargin

  val SelectByCustomerId: String =
    s"""
       |SELECT $FieldsWithTable
       |FROM $Table, creditcards c
       |WHERE $Table.id = c.account_id
       |  AND c.customer_id = ?
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
       |SET version = ?, user_id = ?, status = ?, trial_started_at = ?, active_until = ?, overdue_started_at = ?, overdue_ended_at = ?, overdue_plan_id = ?
       |WHERE id = ?
       |RETURNING $Fields
     """.stripMargin

  val Delete: String =
    s"""
       |DELETE FROM $Table
       |WHERE user_id = ?
       |RETURNING $Fields
     """.stripMargin

  /**
   * Add the first (and only) credit card and all subscriptions to the account.
   * @param raw Account as present in SQL accounts table
   * @param conn Connection
   * @return Future containing either the enriched account, or an error
   */
  def enrichAccount(raw: Account)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Account]] = {
    for {
      cardList <- lift(creditCardRepository.listByAccountId(raw.id))
      subscrList <- lift(stripeSubscriptionRepository.listByAccountId(raw.id))
      result = raw.copy(creditCard = cardList.headOption, subscriptions = subscrList)
      _ = Logger.debug(s"enrichAccount: after adding stripe info, account is $result")
    } yield result
  }

  def get(accountId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Account]] = {
    cacheRepository.cacheAccount.getCached(cacheAccountKey(accountId)).flatMap {
      case \/-(account) => Future successful \/-(account)
      case -\/(_: RepositoryError.NoResults) =>
        for {
          raw <- lift(queryOne(Select, Seq[Any](accountId)))
          account <- lift(enrichAccount(raw))
          _ <- lift(cacheRepository.cacheAccount.putCache(cacheAccountKey(account.id))(account, ttl))
          _ <- lift(cacheRepository.cacheAccount.putCache(cacheAccountUserKey(account.userId))(account, ttl))
        } yield account
      case -\/(error) => Future successful -\/(error)
    }
  }

  def getByUserId(userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Account]] = {
    cacheRepository.cacheAccount.getCached(cacheAccountUserKey(userId)).flatMap {
      case \/-(account) => Future successful \/-(account)
      case -\/(_: RepositoryError.NoResults) =>
        for {
          raw <- lift(queryOne(SelectByUserId, Seq[Any](userId)))
          account <- lift(enrichAccount(raw))
          _ <- lift(cacheRepository.cacheAccount.putCache(cacheAccountKey(account.id))(account, ttl))
          _ <- lift(cacheRepository.cacheAccount.putCache(cacheAccountUserKey(account.userId))(account, ttl))
        } yield account
      case -\/(error) => Future successful -\/(error)
    }
  }

  // Can't use cache here for now
  def getByCustomerId(customerId: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Account]] =
    for {
      raw <- lift(queryOne(SelectByCustomerId, Seq[Any](customerId)))
      account <- lift(enrichAccount(raw))
    } yield account

  def insert(account: Account)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Account]] = {
    val params = Seq[Any](
      account.id, 1, account.userId, account.status, account.trialStartedAt, account.activeUntil,
      account.overdueStartedAt, account.overdueEndedAt, account.overduePlanId
    )

    for {
      inserted <- lift(queryOne(Insert, params))
      _ <- lift(cacheRepository.cacheAccount.removeCached(cacheAccountKey(inserted.id)))
      _ <- lift(cacheRepository.cacheAccount.removeCached(cacheAccountUserKey(inserted.userId)))
    } yield inserted
  }

  def update(account: Account)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Account]] = {
    val params = Seq[Any](
      account.version + 1, account.userId, account.status, account.trialStartedAt,
      account.activeUntil, account.overdueStartedAt, account.overdueEndedAt, account.overduePlanId, account.id
    )

    Logger.debug(s"Doing UPDATE on account: $Update\nwith parameters $params")
    for {
      current <- lift(queryOne(Select, Seq[Any](account.id)))
      _ = Logger.debug(s"Account before update: $current")
      raw <- lift(queryOne(Update, params))
      _ = Logger.debug(s"Updated raw account: $raw")
      _ <- lift(cacheRepository.cacheAccount.removeCached(cacheAccountKey(raw.id)))
      _ <- lift(cacheRepository.cacheAccount.removeCached(cacheAccountUserKey(raw.userId)))
      updated <- lift(enrichAccount(raw))
      _ = Logger.debug(s"Updated enriched account: $account")
    } yield updated
  }

  def delete(userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Account]] =
    for {
      deleted <- lift(queryOne(Delete, Seq[Any](userId)))
      _ <- lift(cacheRepository.cacheAccount.removeCached(cacheAccountKey(deleted.id)))
      _ <- lift(cacheRepository.cacheAccount.removeCached(cacheAccountUserKey(deleted.userId)))
    } yield deleted
}

