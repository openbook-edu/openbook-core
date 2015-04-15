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

class DocumentRepositoryPostgres (val revisionRepository: RevisionRepository)
  extends DocumentRepository with PostgresRepository[Document] {

  /**
   * Instantiate a Document given a row result from the database. Must be provided
   * with the owner and users.
   *
   * @param row
   * @return
   */
  def constructor(row: RowData): Document = {
    Document(
      id        = UUID(row("id").asInstanceOf[Array[Byte]]),
      version   = row("version").asInstanceOf[Long],
      title     = row("title").asInstanceOf[String],
      delta     = Json.parse(row("delta").asInstanceOf[String]).as[Delta],
      ownerId   = UUID(row("owner_id").asInstanceOf[Array[Byte]]),
      createdAt = row("created_at").asInstanceOf[DateTime],
      updatedAt = row("created_at").asInstanceOf[DateTime]
    )
  }

  val SelectDocument =
    s"""
       |SELECT id, version, title, delta, owner_id, created_at, updated_at
     """.stripMargin

  val FromDocuments =
    s"""
       |FROM documents
     """.stripMargin

  val ReturningDocument =
    s"""
       |RETURNING id, version, title, delta, owner_id, created_at, updated_at
     """.stripMargin

  // ----

  val FindDocument =
    s"""
       |$SelectDocument

        |$FromDocuments

        |WHERE documents.id = ?
     """.stripMargin

  val CreateDocument =
    s"""
       |INSERT INTO documents (id, version, title, delta, owner_id, created_at, updated_at)
       |VALUES (?, 0, ?, ?, ?, ?, ?)
       |$ReturningDocument
     """.stripMargin

  val UpdateDocument =
    s"""
       |UPDATE documents
       |SET version = ?, title = ?, delta = ?, owner_id = ?, updated_at = ?
       |WHERE id = ?
       |  AND version = ?
       |$ReturningDocument
     """.stripMargin

  // ----

  /**
   * Find an individual document.
   *
   * @param id
   * @param version Optional, if not indicated, the latest revision will be found. Otherwise, will return computed Delta
   *                starting from first version till indicated one.
   * @param conn
   * @return
   */
  override def find(id: UUID, version: Long = 0)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Document]] = {
    queryOne(FindDocument, Seq[Any](id.bytes)).flatMap {
      case \/-(document) => version match {
        case 0 => Future successful \/-(document)
        case _ => {
          (for {
            revisions <- lift(revisionRepository.list(document, toVersion = version))
            result = {
              // If there were more recent revisions
              if (revisions.nonEmpty) {
                val deltas = revisions.map(_.delta)
                val computedDelta =
                  if (deltas.length == 1)
                    deltas.head
                  else deltas.tail.foldRight(deltas(0)) {
                    (left: Delta, right: Delta) => {
                      left o right
                    }
                  }
                document.copy(delta = computedDelta)
              }
              // If there were no more recent revisions
              else {
                document
              }
            }
          } yield result).run
        }
      }
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Create a new empty document.
   *
   */
  override def insert(document: Document)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Document]] = {
    queryOne(CreateDocument, Seq[Any](
      document.id.bytes, document.title, Json.toJson(document.delta).toString(), document.ownerId.bytes, new DateTime, new DateTime
    ))
  }

  /**
   * Update an existing document.
   *
   * Do not call this function to update the document's text unless you know what you're doing! The latest_text
   * field stores exactly that... a snapshot of the latest document text. That snapshot should be constructed
   * from the revision history stored in the revisions table.
   */
  override def update(document: Document)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Document]] = {
    queryOne(UpdateDocument, Seq[Any](
      document.version + 1, document.title, Json.toJson(document.delta).toString(), document.ownerId.bytes,
      new DateTime, document.id.bytes, document.version
    ))
  }
}
