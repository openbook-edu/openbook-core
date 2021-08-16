package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.{Connection, RowData}
import play.api.Logger
import scalaz.{-\/, \/, \/-}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CopiesCountRepositoryPostgres extends CopiesCountRepository with PostgresRepository[Long] {
  override val entityName = "CopiesCount"
  override def constructor(row: RowData): Long = {
    row("count").asInstanceOf[Long]
  }

  private def Get(entityType: String): String =
    s"""
       |SELECT count FROM ${entityType}_copies_count
       |  WHERE ${entityType}_id = ?
       |""".stripMargin

  private def Insert(entityType: String): String =
    s"""
       |INSERT INTO ${entityType}_copies_count
       |VALUES (?, ?)
       |RETURNING count
       |""".stripMargin

  private def Inc(entityType: String): String =
    s"""
       |UPDATE ${entityType}_copies_count
       |SET count = count + ?
       |WHERE ${entityType}_id = ?
       |RETURNING count
       |""".stripMargin

  private def Dec(entityType: String): String =
    s"""
       |UPDATE ${entityType}_copies_count
       |SET count = count - ?
       |WHERE ${entityType}_id = ?
       |RETURNING count
       |""".stripMargin

  // remove Update?
  private def Update(entityType: String): String =
    s"""
       |UPDATE ${entityType}_copies_count
       |SET count = ?
       |WHERE ${entityType}_id = ?
       |RETURNING count
       |""".stripMargin

  private def Delete(entityType: String): String =
    s"""
       |DELETE FROM ${entityType}_copies_count
       |WHERE ${entityType}_id = ?
       |RETURNING count
       |""".stripMargin

  /**
   * Get count of copies debited to the user or organization. If no entry exists, yet, create one and set it to zero.
   * @param entityType: String, either "user" or "organization"
   * @param entityId: UUID of the user or organization
   * @param conn: database connection
   * @return Future containing the count, or an error
   */
  override def get(entityType: String, entityId: UUID)(implicit conn: Connection): Future[RepositoryError.Fail \/ Long] =
    queryOne(Get(entityType), Seq[Any](entityId), debug = true).flatMap {
      case \/-(count) => Future successful \/-(count)
      case -\/(_: RepositoryError.NoResults) =>
        Logger.debug(s"$entityType $entityId had no copies yet, will now set to zero")
        queryOne(Insert(entityType), Seq[Any](entityId, 0), debug = true)
      case -\/(error) => Future successful -\/(error)
    }

  /**
   * Increment count of copies debited to the user or organization.
   * If no entry exists, yet, create one and set it to n.
   * @param entityType: String, either "user" or "organization"
   * @param entityId: UUID of the user or organization
   * @param n: Int - how many copies to increment; default 1
   * @param conn: database connection
   * @return Future containing the new count, or an error
   */
  override def inc(entityType: String, entityId: UUID, n: Int = 1)(implicit conn: Connection): Future[RepositoryError.Fail \/ Long] =
    queryOne(Inc(entityType), Seq[Any](n, entityId), debug = true).flatMap {
      case \/-(count) => Future successful \/-(count)
      case -\/(_: RepositoryError.NoResults) =>
        Logger.debug(s"$entityType $entityId had no copies yet, will now set to $n")
        queryOne(Insert(entityType), Seq[Any](entityId, n), debug = true)
      case -\/(error) => Future successful -\/(error)
    }

  /**
   * Decrement count of copies debited to the user or organization.
   * @param entityType: String, either "user" or "organization"
   * @param entityId: UUID of the user or organization
   * @param n: Int - how many copies to decrement; default 1
   * @param conn: database connection
   * @return Future containing the new count, or an error
   */
  override def dec(entityType: String, entityId: UUID, n: Int = 1)(implicit conn: Connection): Future[RepositoryError.Fail \/ Long] =
    queryOne(Dec(entityType), Seq[Any](n, entityId), debug = true)

  override def delete(entityType: String, entityId: UUID)(implicit conn: Connection): Future[RepositoryError.Fail \/ Long] =
    queryOne(Delete(entityType), Seq[Any](entityId))
}
