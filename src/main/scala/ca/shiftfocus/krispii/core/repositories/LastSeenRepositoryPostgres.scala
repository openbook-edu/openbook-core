package ca.shiftfocus.krispii.core.repositories

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.{Connection, RowData}
import org.joda.time.DateTime
import scalaz.{-\/, \/, \/-}

import scala.concurrent.Future

class LastSeenRepositoryPostgres extends LastSeenRepository with PostgresRepository[DateTime] {
  override val entityName = "LastSeen"
  override def constructor(row: RowData): DateTime = {
    row("last_seen").asInstanceOf[DateTime]
  }

  val Table: String = "last_seen"
  val Fields: String = "user_id, entity_id, entity_type, last_seen"

  // User CRUD operations
  val Peek: String =
    s"""
       |SELECT last_seen
       |FROM $Table
       |WHERE user_id = ?
       |  AND entity_id = ?
       |  AND entity_type = ?
     """.stripMargin

  val GetOldValueAndUpdate: String =
    s"""
       |UPDATE $Table
       |SET last_seen = now()
       |FROM (SELECT $Fields FROM $Table
       |      WHERE user_id = ?
       |        AND entity_id = ?
       |        AND entity_type = ?
       |      FOR UPDATE) old
       |WHERE $Table.user_id = old.user_id
       |  AND $Table.entity_id = old.entity_id
       |  AND $Table.entity_type = old.entity_type
       |RETURNING old.last_seen
       |""".stripMargin

  val Insert: String =
    s"""
       |INSERT INTO $Table
       |VALUES (?, ?, ?, ?)
       |RETURNING last_seen
       |""".stripMargin

  val Update: String =
    s"""
       |UPDATE $Table
       |SET last_seen = ?
       |WHERE user_id = ?
       |  AND entity_id = ?
       |  AND entity_type = ?
       |RETURNING last_seen
       |""".stripMargin

  val Delete: String =
    s"""
       |DELETE FROM $Table
       |WHERE user_id = ?
       |  AND entity_id = ?
       |  AND entity_type = ?
       |RETURNING last_seen
       |""".stripMargin

  override def find(readerId: UUID, entityId: UUID, entityType: String, peek: Boolean)(implicit conn: Connection): Future[RepositoryError.Fail \/ DateTime] = {
    val query = if (peek) Peek else GetOldValueAndUpdate
    queryOne(query, Seq[Any](readerId, entityId, entityType)).flatMap {
      case \/-(lastSeen) => Future successful \/-(lastSeen)
      case -\/(_: RepositoryError.NoResults) => queryOne(Insert, Seq[Any](readerId, entityId, entityType, new DateTime(0)))
      case -\/(error) => Future successful -\/(error)
    }
  }

  override def put(readerId: UUID, entityId: UUID, entityType: String, seen: DateTime)(implicit conn: Connection): Future[RepositoryError.Fail \/ DateTime] =
    queryOne(Update, Seq[Any](readerId, entityId, entityType, seen)).flatMap {
      case \/-(lastSeen) => Future successful \/-(lastSeen)
      case -\/(_: RepositoryError.NoResults) => queryOne(Insert, Seq[Any](readerId, entityId, entityType, seen))
      case -\/(error) => Future successful -\/(error)
    }

  override def delete(readerId: UUID, entityId: UUID, entityType: String)(implicit conn: Connection): Future[RepositoryError.Fail \/ DateTime] =
    queryOne(Delete, Seq[Any](readerId, entityId, entityType))
}
