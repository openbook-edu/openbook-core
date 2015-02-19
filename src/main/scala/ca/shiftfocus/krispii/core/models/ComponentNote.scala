package ca.shiftfocus.krispii.core.models

import com.github.mauricio.async.db.RowData
import ca.shiftfocus.uuid.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class ComponentScratchpadAlreadyExistsException(msg: String) extends Exception
case class ComponentScratchpadOutOfDateException(msg: String) extends Exception

case class ComponentScratchpad(
  userId: UUID,
  componentId: UUID,
  version: Long = 0,
  documentId: UUID,
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
)

object ComponentScratchpad {

  def apply(row: RowData): ComponentScratchpad = {
    ComponentScratchpad(
      UUID(row("user_id").asInstanceOf[Array[Byte]]),
      UUID(row("component_id").asInstanceOf[Array[Byte]]),
      row("version").asInstanceOf[Long],
      row("document_id").asInstanceOf[UUID],
      Some(row("created_at").asInstanceOf[DateTime]),
      Some(row("updated_at").asInstanceOf[DateTime])
    )
  }

  implicit val taskReads: Reads[ComponentScratchpad] = (
    (__ \ "userId").read[UUID] and
    (__ \ "componentId").read[UUID] and
    (__ \ "version").read[Long] and
    (__ \ "document_id").read[UUID] and
    (__ \ "createdAt").readNullable[DateTime] and
    (__ \ "updatedAt").readNullable[DateTime]
  )(ComponentScratchpad.apply(_: UUID, _: UUID, _: Long, _: UUID, _: Option[DateTime], _: Option[DateTime]))

  implicit val taskWrites: Writes[ComponentScratchpad] = (
    (__ \ "userId").write[UUID] and
    (__ \ "componentId").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "document_id").write[UUID] and
    (__ \ "createdAt").writeNullable[DateTime] and
    (__ \ "updatedAt").writeNullable[DateTime]
  )(unlift(ComponentScratchpad.unapply))

}
