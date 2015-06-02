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
import ws.kahn.ot.exceptions.IncompatibleDeltasException
import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.DateTime

import ws.kahn.ot.{InsertText, Delta}

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
      updatedAt = row("updated_at").asInstanceOf[DateTime]
    )
  }

  val Table  = "documents"
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
        case _ =>
          (for {
            revisions <- lift(revisionRepository.list(document, toVersion = version))
            result =
              if (revisions.nonEmpty) {
                val deltas = revisions.map(_.delta)
                val computedDelta =
                  if (deltas.length == 1)
                    deltas.head
                  else deltas.foldLeft(Delta(IndexedSeq(InsertText("")))) {
                    (left: Delta, right: Delta) => {
                      left o right
                    }
                  }
                document.copy(version = version, delta = computedDelta)
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
      document.id.bytes, 1, document.title, Json.toJson(document.delta).toString(), document.ownerId.bytes, new DateTime, new DateTime
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
      document.version + 1, document.title, Json.toJson(document.delta).toString(), document.ownerId.bytes,
      new DateTime, document.id.bytes, document.version
    ))
  }
}
