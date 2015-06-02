package ca.shiftfocus.krispii.core.repositories

import java.util.NoSuchElementException

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models.document.Revision
import ca.shiftfocus.krispii.core.models.document.Document
import ca.shiftfocus.krispii.core.models.User
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.{RowData, ResultSet, Connection}
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.DateTime

import ws.kahn.ot.Delta

import scala.collection.immutable.HashMap
import scala.concurrent.Future
import scalaz.{\/, -\/, \/-}

class RevisionRepositoryPostgres extends RevisionRepository with PostgresRepository[Revision] {

  /**
   *
   * @param row
   * @return
   */
  def constructor(row: RowData): Revision = {
    Revision(
      documentId = UUID(row("document_id").asInstanceOf[Array[Byte]]),
      version = row("version").asInstanceOf[Long],
      authorId = UUID(row("author_id").asInstanceOf[Array[Byte]]),
      delta = Json.parse(row("delta").asInstanceOf[String]).as[Delta],
      createdAt = row("created_at").asInstanceOf[DateTime]
    )
  }

  val Fields = "document_id, version, author_id, delta, created_at"
  val Table = "document_revisions"
  val QMarks = "?, ?, ?, ?, ?"
  val OrderBy = "version ASC"

  val SelectOneRevision =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE document_id = ?
       |  AND version = ?
     """.stripMargin

  val ListRevisionsAfter =
    s"""
      |SELECT $Fields
      |FROM $Table
      |WHERE document_id = ?
      |  AND version > ?
      |ORDER BY $OrderBy
     """.stripMargin

  val ListRevisionsTo =
    s"""
      |SELECT $Fields
      |FROM $Table
      |WHERE document_id = ?
      |  AND version <= ?
      |ORDER BY $OrderBy
     """.stripMargin

  val ListRevisionsBetween =
    s"""
      |SELECT $Fields
      |FROM $Table
      |WHERE document_id = ?
      | AND version
      | BETWEEN ? and ?
      |ORDER BY $OrderBy
     """.stripMargin

  val ListAllRevisions =
    s"""
      |SELECT $Fields
      |FROM $Table
      |WHERE document_id = ?
      |ORDER BY $OrderBy
     """.stripMargin

  val PushRevision =
    s"""
       |INSERT INTO $Table ($Fields)
       |VALUES ($QMarks)
       |RETURNING $Fields
     """.stripMargin


  // TODO - Should make 2 db queries: one for the list of revisions, and a second for the list of authors.
  /**
   * List revisions for a document.
   *
   * @param document
   * @param afterVersion
   * @param toVersion
   * @param conn
   * @return
   */
  override def list(document: Document, afterVersion: Long = 0, toVersion: Long = 0)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Revision]]] = {
    (afterVersion, toVersion) match {
      case (0, 0) => queryList(ListAllRevisions, Seq[Any](document.id.bytes))
      case (_, 0) => queryList(ListRevisionsAfter, Seq[Any](document.id.bytes, afterVersion))
      case (0, _) => queryList(ListRevisionsTo, Seq[Any](document.id.bytes, toVersion))
      case (_, _) => queryList(ListRevisionsBetween, Seq[Any](document.id.bytes, afterVersion, toVersion))
      case _      => Future.successful(\/-(IndexedSeq.empty[Revision]))
    }
  }

  /**
   * Find a single revision.
   *
   * @param document
   * @param version
   * @param conn
   * @return
   */
  override def find(document: Document, version: Long)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Revision]] = {
    queryOne(SelectOneRevision, Seq[Any](document.id.bytes, version))
  }

  /**
   * Insert a revision into the database.
   *
   * @param revision
   * @param conn
   * @return
   */
  override def insert(revision: Revision)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Revision]] = {
    queryOne(PushRevision, Seq[Any](
      revision.documentId.bytes,
      revision.version,
      revision.authorId.bytes,
      Json.toJson(revision.delta).toString(),
      new DateTime
    ))
  }
}