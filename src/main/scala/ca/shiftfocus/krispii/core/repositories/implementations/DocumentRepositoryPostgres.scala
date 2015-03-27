package ca.shiftfocus.krispii.core.repositories

import java.util.NoSuchElementException

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models.document.Revision
import ca.shiftfocus.krispii.core.models.document.Document
import ca.shiftfocus.krispii.core.models.User
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.{ResultSet, Connection}
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.DateTime
import play.api.Logger
import ws.kahn.ot.Delta

import scala.collection.immutable.HashMap
import scala.concurrent.Future
import scalaz.{\/, -\/, \/-}

class DocumentRepositoryPostgres(val userRepository: UserRepository) extends DocumentRepository {

  val SelectDocument =
    s"""
       |SELECT id, version, title, plaintext, delta, owner_id, created_at, updated_at
     """.stripMargin

  val FromDocuments =
    s"""
       |FROM documents
     """.stripMargin

  val ReturningDocument =
    s"""
       |RETURNING id, version, title, plaintext, delta, owner_id, created_at, updated_at
     """.stripMargin

  val SelectRevision =
    s"""
       |SELECT document_id, version, author_id, delta, created_at
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
       |INSERT INTO documents (id, version, title, plaintext, delta, owner_id, created_at, updated_at)
       |VALUES (?, 0, ?, ?, ?, ?, ?, ?)
       |$ReturningDocument
     """.stripMargin

  val UpdateDocument =
    s"""
       |UPDATE documents
       |SET version = ?, title = ?, plaintext = ?, delta = ?, owner_id = ?, updated_at = ?
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
       |INSERT INTO document_revisions (document_id, version, author_id, delta, created_at)
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
  override def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Document]] = {
    conn.sendPreparedStatement(FindDocument, Seq[Any](id.bytes)).flatMap { result =>
      val maybeRowData = result.rows match {
        case Some(rows) => rows.headOption
        case None => None
      }
      (for {
        owner <- lift(maybeRowData match {
          case Some(rowData) => userRepository.find(UUID(rowData("owner_id").asInstanceOf[Array[Byte]]))
          case None => Future successful -\/(RepositoryError.DatabaseError("Invalid data returned from db."))
        })
        document <- lift(Future.successful(buildDocument(result.rows)(owner, IndexedSeq.empty[User])))
      } yield document).run
    }.recover {
      case exception: NoSuchElementException => -\/(RepositoryError.DatabaseError("Invalid data returned from db."))
      case exception => throw exception
    }
  }

  /**
   * Create a new empty document.
   *
   */
  override def insert(document: Document)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Document]] = {
    conn.sendPreparedStatement(CreateDocument, Seq[Any](
      document.id.bytes, document.title, document.plaintext, Json.toJson(document.delta).toString(), document.owner.id.bytes, new DateTime, new DateTime
    )).map { result =>
      buildDocument(result.rows)(document.owner, document.editors)
    }.recover {
      case exception => throw exception
    }
  }

  /**
   * Update an existing document.
   *
   * Do not call this function to update the document's text unless you know what you're doing! The latest_text
   * field stores exactly that... a snapshot of the latest document text. That snapshot should be constructed
   * from the revision history stored in the revisions table.
   */
  override def update(document: Document)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Document]] = {
    conn.sendPreparedStatement(UpdateDocument, Seq[Any](
      document.version + 1, document.title, document.plaintext, Json.toJson(document.delta).toString(), document.owner.id.bytes,
      new DateTime, document.id.bytes, document.version
    )).map { result =>
      buildDocument(result.rows)(document.owner, document.editors)
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
  override def list(document: Document, version: Long = 0)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Revision]]] = {
    (for {
      rows <- lift(conn.sendPreparedStatement(ListRecentRevisions, Seq[Any](document.id.bytes, version)).map(_.rows match {
        case Some(rows) => \/-(rows)
        case None => -\/(RepositoryError.DatabaseError("No rows returned from db"))
      }))
      authorIds <- lift(Future successful {
        try {
          \/-(rows.map { row =>
            UUID(row("author_id").asInstanceOf[Array[Byte]])
          }.distinct)
        }
        catch {
          case exception: Throwable => -\/(RepositoryError.DatabaseError("Received invalid data from the db. Could not instantiate UUID from author_id field."))
        }
      })
      users <- lift(userRepository.list(authorIds))
      userMap = HashMap(users.map({ user => (user.id, user) }): _*)
      revisions <- lift(Future successful {
        try {
          \/-(rows.map { row =>
            val authorId = UUID(row("author_ID").asInstanceOf[Array[Byte]])
            Revision(row)(userMap(authorId))
          })
        }
        catch {
          case exception: Throwable => -\/(RepositoryError.DatabaseError("Received invalid data from the db. Could not instantiate UUID from author_id field."))
        }
      })
    }
    yield revisions).run.recover {
      case exception: NoSuchElementException => -\/(RepositoryError.DatabaseError("Invalid data returned from db."))
      case exception => throw exception
    }
  }

  /**
   * Insert a revision into the database.
   *
   * @param revision
   * @param conn
   * @return
   */
  override def insert(revision: Revision)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Revision]] = {
    conn.sendPreparedStatement(PushRevision, Seq[Any](
      revision.documentId.bytes,
      revision.version,
      revision.author.id.bytes,
      Json.toJson(revision.delta).toString(),
      new DateTime
    )).map { result =>
      \/-(revision)
    }.recover {
      case exception: NoSuchElementException => -\/(RepositoryError.DatabaseError("Invalid data returned from db."))
      case exception => throw exception
    }
  }

  private def buildDocument(maybeResultSet: Option[ResultSet])(user: User, owners: IndexedSeq[User]): \/[RepositoryError.Fail, Document] = {
    try {
      maybeResultSet match {
        case Some(resultSet) => resultSet.headOption match {
          case Some(firstRow) => \/-(Document(firstRow)(user, owners))
          case None => -\/(RepositoryError.NoResults)
        }
        case None => -\/(RepositoryError.NoResults)
      }
    }
    catch {
      case exception: NoSuchElementException => throw exception
    }
  }

  private def buildRevision(maybeResultSet: Option[ResultSet])(author: User): \/[RepositoryError.Fail, Revision] = {
    try {
      maybeResultSet match {
        case Some(resultSet) => resultSet.headOption match {
          case Some(firstRow) => \/-(Revision(firstRow)(author))
          case None => -\/(RepositoryError.NoResults)
        }
        case None => -\/(RepositoryError.NoResults)
      }
    }
    catch {
      case exception: NoSuchElementException => throw exception
    }
  }
}