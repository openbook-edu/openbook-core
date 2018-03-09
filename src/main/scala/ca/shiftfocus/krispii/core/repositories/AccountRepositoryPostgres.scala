package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.lib.{ ScalaCacheConfig }
import ca.shiftfocus.krispii.core.models.{ Account }
import com.github.mauricio.async.db.{ Connection, RowData }
import org.joda.time.DateTime
import play.api.libs.json.{ JsValue, Json }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.{ -\/, \/, \/- }

class AccountRepositoryPostgres(val scalaCacheConfig: ScalaCacheConfig) extends AccountRepository with PostgresRepository[Account] with CacheRepository {
  override val entityName = "Account"
  override def constructor(row: RowData): Account = {
    Account(
      row("id").asInstanceOf[UUID],
      row("version").asInstanceOf[Long],
      row("user_id").asInstanceOf[UUID],
      row("status").asInstanceOf[String],
      Option(row("customer")).map(customer => Json.parse(customer.asInstanceOf[String])),
      IndexedSeq.empty[JsValue],
      Option(row("trial_started_at")).map(_.asInstanceOf[DateTime]),
      Option(row("active_until")).map(_.asInstanceOf[DateTime]),
      Option(row("overdue_started_at")).map(_.asInstanceOf[DateTime]),
      Option(row("overdue_ended_at")).map(_.asInstanceOf[DateTime]),
      Option(row("overdue_plan_id")).map(_.asInstanceOf[String])
    )
  }

  val Table = "accounts"
  val Fields = "id, version, user_id, status, customer, trial_started_at, active_until, overdue_started_at, overdue_ended_at, overdue_plan_id"
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
    cache[Account].getCached(cacheAccountKey(accountId)).flatMap {
      case \/-(account) => Future successful \/-(account)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          account <- lift(queryOne(Select, Seq[Any](accountId)))
          _ <- lift(cache[Account].putCache(cacheAccountKey(account.id))(account, ttl))
          _ <- lift(cache[Account].putCache(cacheAccountUserKey(account.userId))(account, ttl))
        } yield account
      case -\/(error) => Future successful -\/(error)
    }
  }

  def getByUserId(userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Account]] = {
    cache[Account].getCached(cacheAccountUserKey(userId)).flatMap {
      case \/-(account) => Future successful \/-(account)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          account <- lift(queryOne(SelectByUserId, Seq[Any](userId)))
          _ <- lift(cache[Account].putCache(cacheAccountKey(account.id))(account, ttl))
          _ <- lift(cache[Account].putCache(cacheAccountUserKey(account.userId))(account, ttl))
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
      _ <- lift(cache[Account].removeCached(cacheAccountKey(inserted.id)))
      _ <- lift(cache[Account].removeCached(cacheAccountUserKey(inserted.userId)))
    } yield inserted
  }

  def update(account: Account)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Account]] = {
    val params = Seq[Any](
      account.version + 1, account.userId, account.status, account.customer, account.trialStartedAt,
      account.activeUntil, account.overdueStartedAt, account.overdueEndedAt, account.overduePlanId, account.id
    )

    for {
      updated <- lift(queryOne(Update, params))
      _ <- lift(cache[Account].removeCached(cacheAccountKey(updated.id)))
      _ <- lift(cache[Account].removeCached(cacheAccountUserKey(updated.userId)))
    } yield updated
  }

  def delete(userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Account]] = {
    for {
      deleted <- lift(queryOne(Delete, Seq[Any](userId)))
      _ <- lift(cache[Account].removeCached(cacheAccountKey(deleted.id)))
      _ <- lift(cache[Account].removeCached(cacheAccountUserKey(deleted.userId)))
    } yield deleted
  }
}

