package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.models.{DocumentRevision, Document}
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import ca.shiftfocus.ot.Operation
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.Connection
import org.joda.time.DateTime

import scala.concurrent.Future

trait DocumentRepositoryPostgresComponent extends DocumentRepositoryComponent {
  self: PostgresDB =>

  override val documentRepository: DocumentRepository = new DocumentRepositoryPostgres

  private class DocumentRepositoryPostgres extends DocumentRepository {

    // -- query building bits --------

    val SelectDocument =
      s"""
         |SELECT documents.id as id, documents.version as version, documents.content as content, documents.checksum as checksum,
         |       documents.created_at as created_at, documents.updated_at as updated_at
       """.stripMargin

    val FromDocuments =
      s"""
         |FROM documents
       """.stripMargin


    val SelectRevision =
      s"""
         |SELECT document_revisions.document_id as document_id, document_revisions.version as version,
         |       document_revisions.revision as revision, document_revisions.created_at as created_at
       """.stripMargin

    val FromRevisions =
      s"""
         |FROM document_revisions
       """.stripMargin

    // -- sql queries --------

    val ListDocuments =
      s"""
         |$SelectDocument
         |$FromDocuments
       """.stripMargin


    val ListRevisions =
      s"""
         |$SelectRevision
         |$FromRevisions
         |WHERE document_id = ?
         |ORDER BY version ASC
       """.stripMargin

    val ListRevisionsMinVer =
      s"""
         |$SelectRevision
         |$FromRevisions
         |WHERE document_id = ?
         |  AND version >= ?
         |ORDER BY version ASC
       """.stripMargin

    val ListRevisionsMaxVer =
      s"""
         |$SelectRevision
         |$FromRevisions
         |WHERE document_id = ?
         |  AND version <= ?
         |ORDER BY version ASC
       """.stripMargin

    val ListRevisionsRangeVer =
      s"""
         |$SelectRevision
         |$FromRevisions
         |WHERE document_id = ?
         |  AND version >= ?
         |  AND version <= ?
         |ORDER BY version ASC
       """.stripMargin

    val FindDocument =
      s"""
         |$SelectDocument
         |$FromDocuments
         |WHERE id = ?
       """.stripMargin

    val CreateDocument =
      s"""
         |INSERT INTO documents (id, version, contents, checksum, created_at, updated_at)
         |VALUES (?, ?, ?, ?, ?, ?)
       """.stripMargin

    // Fetch a group of documents, optionally with their revision histories.
    override def list(documentIds: IndexedSeq[UUID]): Future[IndexedSeq[Document]] = {
      val arrayString = documentIds.map { documentId =>
        val cleanId = documentId.string filterNot ("-" contains _)
        s"decode('$cleanId', 'hex')"
      }.mkString("ARRAY[", ",", "]")
      val query = s"""${ListDocuments} WHERE ARRAY[document_revisions.document_id] <@ $arrayString"""

      db.pool.sendQuery(query).map { result =>
        result.rows.get.map { row => Document(row) }
      }.recover {
        case exception => throw exception
      }
    }

    // Fetch (optionally just a slice of) a document's revision history.
    override def revisions(documentId: UUID, maybeFromVer: Option[Long] = None, maybeToVer: Option[Long] = None): Future[IndexedSeq[DocumentRevision]] = {

      // Build query and data arguments based on method input.
      val (query, data) = (maybeFromVer, maybeToVer) match {
        case (Some(fromVer), Some(toVer)) => {
          (ListRevisionsRangeVer, Seq[Any](documentId.bytes, fromVer, toVer))
        }
        case (Some(fromVer), None) => {
          (ListRevisionsMinVer, Seq[Any](documentId.bytes, fromVer))
        }
        case (None, Some(toVer)) => {
          (ListRevisionsMaxVer, Seq[Any](documentId.bytes, toVer))
        }
        case (None, None) => {
          (ListRevisions, Seq[Any](documentId.bytes))
        }
      }

      // Send the actual query and return results, recovering exceptions.
      db.pool.sendPreparedStatement(query, data).map { result =>
        result.rows.get.map { row => DocumentRevision(row) }
      }.recover {
        case exception => throw exception
      }

    }

    // Fetch a single document, optionally with its revision history.
    override def find(documentId: UUID, withHistory: Boolean = false, fromVer: Option[Long] = None, toVer: Option[Long] = None): Future[Option[Document]] = {
      for {
        history <- if (withHistory) { revisions(documentId, fromVer, toVer) } else { Future successful IndexedSeq.empty[DocumentRevision] }
        document <- db.pool.sendPreparedStatement(FindDocument, Seq[Any](documentId.bytes)).map { result =>
          result.rows.get.headOption match {
            case Some(row) => Some(Document(row, history))
            case None => None
          }
        }
      } yield document
    }.recover {
      case exception => throw exception
    }

    // Create a new document. Creates a new, empty, document with the given ID.
    override def create(documentId: UUID)(implicit conn: Connection): Future[Document] = {
      conn.sendPreparedStatement(CreateDocument, Seq[Any](
        documentId.bytes, 0, "", Array.empty[Byte], new DateTime, new DateTime
      )).map { result =>
         Document(result.rows.get.head)
      }.recover {
        case exception => throw exception
      }
    }

    /**
     * Push an update onto a document. This is where the magic happens!
     *
     * Given a document ID, and a version to apply against, and a chain of operations called a "revision", this will
     * push the new revision onto the document's revision history, update the latest copy of the document, and bump the
     * version number.
     *
     * If the revision has been pushed with an "outdated" version, then that revision will be "transformed" against the
     * more recent revisions and applied. We should also transform the more recent revisions against the appended one,
     * and relay them back to the caller.
     *
     * This is "operational transformation". The more recent operations are considered the "server" operation, and
     * the operation to be pushed is considered the "client" operation. They are transformed against each other and the
     * server applies one transformation onto the history stack, and sends the other transformation back to the client.
     *
     * @param documentId
     * @param version
     * @param revisions
     * @param connection
     * @return
     */
    override def push(documentId: UUID, version: Long, revisions: IndexedSeq[Operation])(implicit connection: Connection): Future[Option[(DocumentRevision, Array[Byte])]] = {

    }

    // Delete a document and its revision history.
    override def delete(documentId: UUID, version: Long)(implicit connection: Connection): Future[Boolean] = {

    }

  }
}