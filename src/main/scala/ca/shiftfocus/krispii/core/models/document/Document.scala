package ca.shiftfocus.krispii.core.models.document

import java.security.MessageDigest

import ca.shiftfocus.krispii.core.models.User
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.RowData
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._
import ws.kahn.ot._

case class Document(
  id: UUID = UUID.random,
  version: Long = 1L,
  title: String,
  plaintext: String,
  delta: Delta,
  owner: User,
  editors: IndexedSeq[User],
  revisions: IndexedSeq[Operation] = IndexedSeq.empty[Operation],
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
) {

  /**
   * Checksum is computed from the content string.
   */
  val checksum: Array[Byte] = Document.md5(this.plaintext)

}

object Document {

  /**
   * Instantiate a Document given a row result from the database. Must be provided
   * with the owner and users.
   *
   * @param row
   * @param owner
   * @param editors
   * @return
   */
  def apply(row: RowData)(owner: User, editors: IndexedSeq[User]): Document = {
    Document(
      id = UUID(row("id").asInstanceOf[Array[Byte]]),
      version = row("version").asInstanceOf[Long],
      title = row("title").asInstanceOf[String],
      plaintext = row("plaintext").asInstanceOf[String],
      delta = Json.parse(row("delta").asInstanceOf[String]).as[Delta],
      owner = owner,
      editors = editors,
      createdAt = Option(row("created_at").asInstanceOf[DateTime]),
      updatedAt = Option(row("created_at").asInstanceOf[DateTime])
    )
  }

  implicit val writes: Writes[Document] = (
    (__ \ "id").write[UUID] and
      (__ \ "version").write[Long] and
      (__ \ "title").write[String] and
      (__ \ "plaintext").write[String] and
      (__ \ "delta").write[Delta] and
      (__ \ "ownerId").write[User] and
      (__ \ "editorIds").write[IndexedSeq[User]] and
      (__ \ "revisions").write[IndexedSeq[Operation]] and
      (__ \ "createdAt").writeNullable[DateTime] and
      (__ \ "updatedAt").writeNullable[DateTime]
    )(unlift(Document.unapply))

  def md5(text: String): Array[Byte] = {
    MessageDigest.getInstance("MD5").digest(text.getBytes)
  }

}
