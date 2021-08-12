package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.{Connection, RowData}
import scalaz.{-\/, \/, \/-}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CopiesCountRepositoryPostgres extends CopiesCountRepository with PostgresRepository[BigInt] {
  override val entityName = "CopiesCount"
  override def constructor(row: RowData): BigInt = {
    row("count").asInstanceOf[BigInt]
  }

  private def Get(entityType: String): String =
    s"""
       |SELECT ${entityType}_copies_count.count FROM ${entityType}_copies_count, ${entityType}s
       |  WHERE ${entityType}_copies_count.${entityType}_id = ${entityType}s.id
       |""".stripMargin

  private def Insert(entityType: String): String =
    s"""
       |INSERT INTO ${entityType}_copies_count
       |VALUES (?, ?)
       |RETURNING count
       |""".stripMargin

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

  override def get(entityType: String, entityId: UUID)(implicit conn: Connection): Future[RepositoryError.Fail \/ BigInt] =
    queryOne(Get(entityType), Seq[UUID](entityId))

  override def inc(entityType: String, entityId: UUID, n: Int = 1)(implicit conn: Connection): Future[RepositoryError.Fail \/ BigInt] =
    get(entityType, entityId).flatMap {
      case \/-(oldCount) => queryOne(Update(entityType), Seq[Any](entityId, oldCount + n))
      case -\/(_: RepositoryError.NoResults) =>
        queryOne(Insert(entityType), Seq[Any](entityId, n))
      case -\/(error) => Future successful -\/(error)
    }

  override def dec(entityType: String, entityId: UUID, n: Int = 1)(implicit conn: Connection): Future[RepositoryError.Fail \/ BigInt] =
    get(entityType, entityId).flatMap {
      case \/-(oldCount) => queryOne(Update(entityType), Seq[Any](entityId, oldCount - n))
      case -\/(_: RepositoryError.NoResults) =>
        queryOne(Insert(entityType), Seq[Any](entityId, n))
      case -\/(error) => Future successful -\/(error)
    }

  override def delete(entityType: String, entityId: UUID)(implicit conn: Connection): Future[RepositoryError.Fail \/ BigInt] =
    queryOne(Delete(entityType), Seq[UUID](entityId))
}
