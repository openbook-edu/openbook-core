package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models.document.Document
import java.util.UUID
import com.github.mauricio.async.db.{RowData, Connection}
import play.api.libs.json.Json
import ca.shiftfocus.otlib.exceptions.IncompatibleDeltasException
import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.DateTime
import ca.shiftfocus.otlib.{InsertText, Delta}
import scala.concurrent.Future
import scalaz.{\/, -\/, \/-}

class DocumentRepositoryPostgres(val revisionRepository: RevisionRepository)
    extends DocumentRepository with PostgresRepository[Document] {

  override val entityName = "Document"

  /**
   * Instantiate a Document given a row result from the database. Must be provided
   * with the owner and users.
   *
   * @param row
   * @return
   */
  def constructor(row: RowData): Document = {
    Document(
      id = row("id").asInstanceOf[UUID],
      version = row("version").asInstanceOf[Long],
      title = row("title").asInstanceOf[String],
      delta = Json.parse(row("delta").asInstanceOf[String]).as[Delta],
      ownerId = row("owner_id").asInstanceOf[UUID],
      createdAt = row("created_at").asInstanceOf[DateTime],
      updatedAt = row("updated_at").asInstanceOf[DateTime]
    )
  }

  val Table = "documents"
  val Fields = "id, version, title, delta, owner_id, created_at, updated_at"
  val QMarks = "?, ?, ?, ?, ?, ?, ?"

  val FindDocument =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE id = ?
     """.stripMargin

  val CreateDocument =
    s"""
       |INSERT INTO $Table ($Fields)
       |VALUES ($QMarks)
       |RETURNING $Fields
     """.stripMargin

  val UpdateDocument =
    s"""
       |UPDATE $Table
       |SET version = ?, title = ?, delta = ?, owner_id = ?, updated_at = ?
       |WHERE id = ?
       |  AND version = ?
       |RETURNING $Fields
     """.stripMargin

  val DeleteDocument =
    s"""
       |DELETE FROM $Table
       |WHERE id = ?
       |RETURNING $Fields
     """.stripMargin

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
    queryOne(FindDocument, Seq[Any](id)).flatMap {
      case \/-(document) => version match {
        case 0 => Future successful \/-(document)
        case _ =>
          (for {
            revisions <- lift(revisionRepository.list(document, toVersion = version))
            result = if (revisions.nonEmpty) {
              val deltas = revisions.map(_.delta)
              val computedDelta = if (deltas.length == 1) { // scalastyle:ignore
                deltas.head
              }
              else deltas.foldLeft(Delta(IndexedSeq(InsertText("")))) {
                (left: Delta, right: Delta) =>
                  {
                    left o right
                  }
              }
              document.copy(version = version, delta = computedDelta)
            }
            // We don't have revision for document version 1, as it is empty document
            else if (version == 1L) {
              document.copy(version = version, delta = Delta(IndexedSeq()))
            }
            else {
              document
            }
          } yield result).recover {
            case ex: IncompatibleDeltasException =>
              -\/(RepositoryError.DatabaseError("Incompatible deltas.", Some(ex)))

            case ex: Exception => -\/(RepositoryError.DatabaseError("Unexpected failure", Some(ex)))
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
      document.id, 1, document.title, Json.toJson(document.delta).toString(), document.ownerId, new DateTime, new DateTime
    ))
  }

  /**
   * Update an existing document.
   *
   * Do not call this function to update the document's delta unless you know what you're doing! The delta
   * field stores exactly that... a snapshot of the latest document text. That snapshot should be constructed
   * from the revision history stored in the revisions table.
   */
  override def update(document: Document)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Document]] = {
    queryOne(UpdateDocument, Seq[Any](
      document.version + 1, document.title, Json.toJson(document.delta).toString(), document.ownerId,
      new DateTime, document.id, document.version
    ))
  }

  override def delete(docId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Document]] = {
    queryOne(DeleteDocument, Seq[Any](docId))
  }
}
