package ca.shiftfocus.krispii.core.models

import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.RowData
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Document(
  id: UUID = UUID.random,
  version: Long = 0,
  content: String,
  checksum: Array[Byte],
  revisions: IndexedSeq[DocumentRevision] = IndexedSeq.empty[DocumentRevision],
  createdAt: Option[DateTime],
  updatedAt: Option[DateTime]
)

object Document {

  /**
   * Instantiate a document from a database row, optionally passing in its revision history.
   *
   * @param row
   * @param revisions
   * @return
   */
  def apply(row: RowData): Document = {
    Document.apply(row, IndexedSeq.empty[DocumentRevision])
  }

  def apply(row: RowData, revisions: IndexedSeq[DocumentRevision]) = {
    Document(
      id = UUID(row("id").asInstanceOf[Array[Byte]]),
      version = row("version").asInstanceOf[Long],
      content = row("content").asInstanceOf[String],
      checksum = row("checksum").asInstanceOf[Array[Byte]],
      revisions = revisions,
      createdAt = row("created_at").asInstanceOf[Option[DateTime]],
      updatedAt = row("updated_at").asInstanceOf[Option[DateTime]]
    )
  }

  implicit val reads: Reads[Document] = (
    (__ \ "id").read[UUID] and
    (__ \ "version").read[Long] and
    (__ \ "content").read[String] and
    (__ \ "checksum").read[Array[Byte]] and
    (__ \ "revisions").read[IndexedSeq[DocumentRevision]] and
    (__ \ "createdAt").readNullable[DateTime] and
    (__ \ "updatedAt").readNullable[DateTime]
  )(Document.apply(_: UUID, _: Long, _: String, _: Array[Byte], _: IndexedSeq[DocumentRevision], _: Option[DateTime], _: Option[DateTime]))

  implicit val writes: Writes[Document] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "content").write[String] and
    (__ \ "checksum").write[Array[Byte]] and
    (__ \ "revisions").write[IndexedSeq[DocumentRevision]] and
    (__ \ "createdAt").writeNullable[DateTime] and
    (__ \ "updatedAt").writeNullable[DateTime]
  )(unlift(Document.unapply))

}
