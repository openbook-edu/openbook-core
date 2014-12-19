package ca.shiftfocus.krispii.core.models

import ca.shiftfocus.ot.Operation
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.RowData
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class DocumentRevision(
  documentId: UUID,
  version: Long,
  authorId: UUID,
  operations: IndexedSeq[Operation],
  createdAt: Option[DateTime]
)

object DocumentRevision {

  def apply(row: RowData): DocumentRevision = {
    DocumentRevision(
      documentId = UUID(row("document_id").asInstanceOf[Array[Byte]]),
      version = row("version").asInstanceOf[Long],
      authorId = UUID(row("author_id").asInstanceOf[Array[Byte]]),
      operations = row("revision").asInstanceOf[JsValue].as[IndexedSeq[Operation]],
      createdAt = row("created_at").asInstanceOf[Option[DateTime]]
    )
  }

  implicit val reads: Reads[DocumentRevision] = (
    (__ \ "documentId").read[UUID] and
      (__ \ "version").read[Long] and
      (__ \ "authorId").read[UUID] and
      (__ \ "operations").read[IndexedSeq[Operation]] and
      (__ \ "createdAt").readNullable[DateTime]
    )(DocumentRevision.apply(_: UUID, _: Long, _: UUID, _: IndexedSeq[Operation], _: Option[DateTime]))

  implicit val writes: Writes[DocumentRevision] = (
    (__ \ "documentId").write[UUID] and
      (__ \ "version").write[Long] and
      (__ \ "authorId").write[UUID] and
      (__ \ "operations").write[IndexedSeq[Operation]] and
      (__ \ "createdAt").writeNullable[DateTime]
    )(unlift(DocumentRevision.unapply))

}