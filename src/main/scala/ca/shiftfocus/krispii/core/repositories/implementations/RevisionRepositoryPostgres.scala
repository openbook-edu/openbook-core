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

  val ListRecentRevisions =
    s"""
       |$SelectRevision
        |$FromRevisions
        |WHERE document_id = ?
        |  AND version > ?
        |ORDER BY version ASC
     """.stripMargin

  val PushRevision =
    s"""
       |INSERT INTO document_revisions (document_id, version, author_id, delta, created_at)
       |VALUES (?, ?, ?, ?, ?)
       |RETURNING document_id, version, author_id, delta, created_at
     """.stripMargin

  /**
   * List revisions for a document.
   *
   * Should make 2 db queries: one for the list of revisions, and a second for the list of authors.
   *
   * @param document
   * @param version
   * @return
   */
  override def list(document: Document, version: Long = 0)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Revision]]] = {
    queryList(ListRecentRevisions, Seq[Any](document.id.bytes, version))
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