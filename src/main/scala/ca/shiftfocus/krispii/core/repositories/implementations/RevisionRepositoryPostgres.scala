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
import play.api.Logger
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

  val SelectRevision =
    s"""
       |SELECT document_id, version, author_id, delta, created_at
     """.stripMargin

  val FromRevisions =
    s"""
      |FROM document_revisions
     """.stripMargin

  val SelectOneRevision =
    s"""
       |$SelectRevision
       |$FromRevisions
       |WHERE document_id = ?
       |  AND version = ?
     """.stripMargin

  val ListRevisionsFrom =
    s"""
      |$SelectRevision
      |$FromRevisions
      |WHERE document_id = ?
      |  AND version > ?
      |ORDER BY version ASC
     """.stripMargin

  val ListRevisionsTo =
    s"""
      |$SelectRevision
      |$FromRevisions
      |WHERE document_id = ?
      |  AND version < ?
      |ORDER BY version ASC
     """.stripMargin

  val ListRevisionsBetween =
    s"""
      |$SelectRevision
      |$FromRevisions
      |WHERE document_id = ?
      | AND created_at
      | BETWEEN ? and ?
      |ORDER BY version ASC
     """.stripMargin

  val ListAllRevisions =
    s"""
      |$SelectRevision
      |$FromRevisions
      |WHERE document_id = ?
      |ORDER BY version ASC
     """.stripMargin

  val PushRevision =
    s"""
       |INSERT INTO document_revisions (document_id, version, author_id, delta, created_at)
       |VALUES (?, ?, ?, ?, ?)
       |RETURNING document_id, version, author_id, delta, created_at
     """.stripMargin


  // TODO - Should make 2 db queries: one for the list of revisions, and a second for the list of authors.
  /**
   * List revisions for a document.
   *
   * @param document
   * @param fromVersion
   * @param toVersion
   * @param conn
   * @return
   */
  override def list(document: Document, fromVersion: Long = 0, toVersion: Long = 0)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Revision]]] = {
    (fromVersion, toVersion) match {
      case (0, 0) => queryList(ListAllRevisions, Seq[Any](document.id.bytes))
      case (_, 0) => queryList(ListRevisionsFrom, Seq[Any](document.id.bytes, fromVersion))
      case (0, _) => queryList(ListRevisionsTo, Seq[Any](document.id.bytes, toVersion))
      case (_, _) => queryList(ListRevisionsBetween, Seq[Any](document.id.bytes, fromVersion, toVersion))
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