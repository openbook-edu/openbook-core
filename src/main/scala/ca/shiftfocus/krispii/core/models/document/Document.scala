package ca.shiftfocus.krispii.core.models.document

import java.security.MessageDigest

import ca.shiftfocus.krispii.core.models.User
import java.util.UUID
import com.github.mauricio.async.db.RowData
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._
import ws.kahn.ot._
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._

case class Document(
    id: UUID = UUID.randomUUID,
    version: Long = 1L,
    title: String,
    delta: Delta, // represents the current state of the document, only inserts (text or codes)
    ownerId: UUID,
    revisions: IndexedSeq[Revision] = IndexedSeq.empty[Revision],
    createdAt: DateTime = new DateTime,
    updatedAt: DateTime = new DateTime
) {
  def plaintext: String = {
    delta.operations.map {
      case insert: InsertText => insert.chars
      case insert: InsertCode => insert.code match {
        case 0 => "\n"
        case _ => ""
      }
      case _ => ""
    }.mkString
  }

  override def equals(anotherObject: Any): Boolean = {
    anotherObject match {
      case anotherDocument: Document =>
        this.id == anotherDocument.id &&
          this.version == anotherDocument.version &&
          this.ownerId == anotherDocument.ownerId &&
          this.title == anotherDocument.title &&
          this.delta == anotherDocument.delta &&
          this.createdAt.toString == anotherDocument.createdAt.toString &&
          this.updatedAt.toString == anotherDocument.updatedAt.toString
      case _ => false
    }
  }
}

object Document {

  implicit val writes: Writes[Document] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "title").write[String] and
    (__ \ "delta").write[Delta] and
    (__ \ "ownerId").write[UUID] and
    (__ \ "revisions").write[IndexedSeq[Revision]] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(Document.unapply))

  def md5(text: String): Array[Byte] = {
    MessageDigest.getInstance("MD5").digest(text.getBytes)
  }

}
