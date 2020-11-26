package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.{Account, Customer}
import com.github.mauricio.async.db.{Connection, RowData}
import org.joda.time.DateTime
import play.api.Logger
import scalaz.{-\/, \/, \/-}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AccountRepositoryPostgres(
    val cacheRepository: CacheRepository
) extends AccountRepository with PostgresRepository[Account] {
  override val entityName = "Account"
  override def constructor(row: RowData): Account = {
    Account(
      row("id").asInstanceOf[UUID],
      row("version").asInstanceOf[Long],
      row("user_id").asInstanceOf[UUID],
      row("status").asInstanceOf[String],
      Option(row("customer").asInstanceOf[Customer]),
      IndexedSeq.empty[UUID], // subscriptions are initialized empty!
      Option(row("trial_started_at").asInstanceOf[DateTime]),
      Option(row("active_until").asInstanceOf[DateTime]),
      Option(row("overdue_started_at").asInstanceOf[DateTime]),
      Option(row("overdue_ended_at").asInstanceOf[DateTime]),
      Option(row("overdue_plan_id").asInstanceOf[String])
    )
  }

  val Table = "accounts"
  // the database table "accounts" does not contain a subscriptions field
  val Fields = "id, version, user_id, status, customer, trial_started_at, active_until, overdue_started_at, overdue_ended_at, overdue_plan_id"
  val FieldsWithTable = Fields.split(", ").map({ field => s"${Table}." + field }).mkString(", ")
  val QMarks = Fields.split(", ").map({ field => "?" }).mkString(", ")

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
       |SELECT $FieldsWithTable
       |FROM $Table
       |WHERE $Fields.id = customers.account_id
       |  AND customers.id = ?
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
       |SET version = ?, user_id = ?, status = ?, customer = ?, trial_started_at = ?, active_until = ?, overdue_started_at = ?, overdue_ended_at = ?, overdue_plan_id = ?
       |WHERE id = ?
       |RETURNING $Fields
     """.stripMargin

  val Delete =
    s"""
       |DELETE FROM $Table
       |WHERE user_id = ?
       |RETURNING $Fields
     """.stripMargin

  def get(accountId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Account]] = {
    cacheRepository.cacheAccount.getCached(cacheAccountKey(accountId)).flatMap {
      case \/-(account) => Future successful \/-(account)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          account <- lift(queryOne(Select, Seq[Any](accountId)))
          _ <- lift(cacheRepository.cacheAccount.putCache(cacheAccountKey(account.id))(account, ttl))
          _ <- lift(cacheRepository.cacheAccount.putCache(cacheAccountUserKey(account.userId))(account, ttl))
        } yield account
      case -\/(error) => Future successful -\/(error)
    }
  }

  def getByUserId(userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Account]] = {
    cacheRepository.cacheAccount.getCached(cacheAccountUserKey(userId)).flatMap {
      case \/-(account) => Future successful \/-(account)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          account <- lift(queryOne(SelectByUserId, Seq[Any](userId)))
          _ <- lift(cacheRepository.cacheAccount.putCache(cacheAccountKey(account.id))(account, ttl))
          _ <- lift(cacheRepository.cacheAccount.putCache(cacheAccountUserKey(account.userId))(account, ttl))
        } yield account
      case -\/(error) => Future successful -\/(error)
    }
  }

  // Can't use cache here for now
  def getByCustomerId(customerId: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Account]] = {
    queryOne(SelectByCustomerId, Seq[Any](customerId))
  }

  def insert(account: Account)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Account]] = {
    val params = Seq[Any](
      account.id, 1, account.userId, account.status, account.customer, account.trialStartedAt, account.activeUntil,
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
      account.version + 1, account.userId, account.status, account.customer, account.trialStartedAt,
      account.activeUntil, account.overdueStartedAt, account.overdueEndedAt, account.overduePlanId, account.id
    )

    Logger.debug(s"Doing UPDATE on account: ${Update}\nwith parameters ${params}")
    for {
      current <- lift(queryOne(Select, Seq[Any](account.id)))
      _ = Logger.debug(s"Account before update: ${current}")
      updated <- lift(queryOne(Update, params))
      _ = Logger.debug(s"Updated account: ${updated}")
      _ <- lift(cacheRepository.cacheAccount.removeCached(cacheAccountKey(updated.id)))
      _ <- lift(cacheRepository.cacheAccount.removeCached(cacheAccountUserKey(updated.userId)))
    } yield updated
  }

  def delete(userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Account]] = {
    for {
      deleted <- lift(queryOne(Delete, Seq[Any](userId)))
      _ <- lift(cacheRepository.cacheAccount.removeCached(cacheAccountKey(deleted.id)))
      _ <- lift(cacheRepository.cacheAccount.removeCached(cacheAccountUserKey(deleted.userId)))
    } yield deleted
  }
}

