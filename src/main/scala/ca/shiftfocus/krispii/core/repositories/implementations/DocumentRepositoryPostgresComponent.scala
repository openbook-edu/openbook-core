package ca.shiftfocus.krispii.core.repositories

import java.util.NoSuchElementException

import ca.shiftfocus.krispii.core.models.document.Revision
import ca.shiftfocus.krispii.core.models.document.Document
import ca.shiftfocus.krispii.core.models.User
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import org.joda.time.DateTime
import play.api.Logger
import ws.kahn.ot.Operation

import scala.collection.immutable.HashMap
import scala.concurrent.Future


trait DocumentRepositoryPostgresComponent extends DocumentRepositoryComponent {
  self: UserRepositoryComponent with PostgresDB =>

  override val documentRepository: DocumentRepository = new DocumentRepositoryPostgres

  private class DocumentRepositoryPostgres extends DocumentRepository {

    val SelectDocument =
      s"""
         |SELECT id, version, title, latest_text, owner_id, editor_ids, created_at, updated_at
       """.stripMargin

    val FromDocuments =
      s"""
         |FROM documents
       """.stripMargin

    val ReturningDocument =
      s"""
         |RETURNING id, version, title, latest_text, owner_id, editor_ids, created_at, updated_at
       """.stripMargin

    val SelectRevision =
      s"""
         |SELECT document_id, version, author_id, operation, created_at
       """.stripMargin

    val FromRevisions =
      s"""
         |FROM document_revisions
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
         |INSERT INTO documents (id, version, title, latest_text, owner_id, created_at, updated_at)
         |VALUES (?, 0, ?, ?, ?, ?, ?)
         |$ReturningDocument
       """.stripMargin

    val UpdateDocument =
      s"""
         |UPDATE documents
         |SET version = ?, title = ?, latest_text = ?, owner_id = ?, editor_ids = ?, updated_at = ?
         |WHERE id = ?
         |  AND version = ?
         |$ReturningDocument
       """.stripMargin

    // ----

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
         |INSERT INTO document_revisions (document_id, version, author_id, operation, created_at)
         |VALUES (?, ?, ?, ?, ?)
         |RETURNING document_id, version, author_id, operation, created_at
       """.stripMargin

    // ----

    /**
     * Find an individual document.
     *
     * @param id
     * @return
     */
    override def find(id: UUID): Future[Option[Document]] = {
      for {
        result <- db.pool.sendPreparedStatement(FindDocument, Seq[Any](id.bytes)).map(_.rows.get.headOption.get).map({ result =>
          Logger.debug("got document")
          result
        })
        owner <- userRepository.find(UUID(result("owner_id").asInstanceOf[Array[Byte]])).map(_.get)
        editors <- Future successful IndexedSeq.empty[User]
      }
      yield Some(Document(result)(owner, editors))
    }.recover {
      case exception: NoSuchElementException => None
      case exception: NullPointerException => None
      case exception => {
        Logger.error("Database error while finding document.")
        throw exception
      }
    }

    /**
     * Create a new empty document.
     *
     */
    override def insert(document: Document)(implicit conn: Connection): Future[Document] = {
      conn.sendPreparedStatement(CreateDocument, Seq[Any](
        document.id.bytes, document.title, "", document.owner.id.bytes, new DateTime, new DateTime
      )).map { result =>
        Document(result.rows.get.head)(document.owner, document.editors)
      }.recover {
        case exception => {
          Logger.error(exception.getMessage())
          Logger.error("Error inserting document.")
          throw exception
        }
      }
    }

    /**
     * Update an existing document.
     *
     * Do not call this function to update the document's text unless you know what you're doing! The latest_text
     * field stores exactly that... a snapshot of the latest document text. That snapshot should be constructed
     * from the revision history stored in the revisions table.
     */
    override def update(document: Document)(implicit conn: Connection): Future[Document] = {
      conn.sendPreparedStatement(UpdateDocument, Seq[Any](
        document.version + 1, document.title, document.content, document.owner.id.bytes, document.editors.map(_.id.bytes),
        new DateTime, document.id.bytes, document.version
      )).map { result =>
        Document(result.rows.get.head)(document.owner, document.editors)
      }.recover {
        case exception => throw exception
      }
    }

    /**
     * List revisions for a document.
     *
     * Should make 2 db queries: one for the list of revisions, and a second for the list of authors.
     *
     * @param document
     * @param version
     * @return
     */
    override def list(document: Document, version: Long = 0): Future[IndexedSeq[Revision]] = {
      for {
        rows <- db.pool.sendPreparedStatement(ListRecentRevisions, Seq[Any](document.id.bytes, version)).map(_.rows.get)
        authorIds <- Future successful rows.map { row =>
          UUID(row("author_ID").asInstanceOf[Array[Byte]])
        }.distinct
        users <- userRepository.list(authorIds).map({ users => HashMap(users.map({ user => (user.id, user) }): _*) })
        revisions <- Future successful rows.map { row =>
          val authorId = UUID(row("author_ID").asInstanceOf[Array[Byte]])
          Revision(row)(users(authorId))
        }
      }
      yield revisions
    }.recover {
      case exception => throw exception
    }

    /**
     * Insert a revision into the database.
     *
     * @param revision
     * @param conn
     * @return
     */
    override def insert(revision: Revision)(implicit conn: Connection): Future[Revision] = {
      conn.sendPreparedStatement(PushRevision, Seq[Any](
        revision.documentId.bytes,
        revision.version,
        revision.author.id.bytes,
        Operation.writes.writes(revision.operation).toString(),
        new DateTime
      )).map { result =>
        revision
      }.recover {
        case exception => {
          Logger.error("Error inserting revision")
          throw exception
        }
      }
    }

  }
}
