package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.{Connection, RowData}
import scalaz.{-\/, \/, \/-}

import scala.concurrent.Future

class CopiesCountRepositoryPostgres extends CopiesCountRepository with PostgresRepository[BigInt] {
  override val entityName = "CopiesCount"
  override def constructor(row: RowData): BigInt = {
    row("count").asInstanceOf[BigInt]
  }

  private val OrgTable: String = s"organization_copies_count"
  private val Fields: String = "user_id, entity_id, entity_type, last_seen"

  // Organization CRUD operations

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

  def get(entityType: String, entityId: String)(implicit conn: Connection): Future[RepositoryError.Fail \/ BigInt] =
    queryOne(Get(entityType), Seq[String](entityId))

  override def inc(entityType: String, entityId: String, n: Int)(implicit conn: Connection): Future[RepositoryError.Fail \/ BigInt] =
    get(entityType, entityId).flatMap {
      case \/-(oldCount) => queryOne(Update(entityType), Seq[BigInt](oldCount + n))
      case -\/(_: RepositoryError.NoResults) =>
        queryOne(Insert(entityType), Seq[Int](n))
      case -\/(error) => Future successful -\/(error)
    }

  override def dec(entityType: String, entityId: String, n: Int)(implicit conn: Connection): Future[RepositoryError.Fail \/ BigInt] = ???

  override def delete(entityType: String, entityId: String)(implicit conn: Connection): Future[RepositoryError.Fail \/ Unit] = ???
}
