package ca.shiftfocus.krispii.core.models

import java.util.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._

case class ComponentScratchpadAlreadyExistsException(msg: String) extends Exception
case class ComponentScratchpadOutOfDateException(msg: String) extends Exception

case class ComponentScratchpad(
  userId: UUID,
  componentId: UUID,
  version: Long = 1L,
  documentId: UUID,
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime
)

object ComponentScratchpad {

  implicit val taskReads: Reads[ComponentScratchpad] = (
    (__ \ "userId").read[UUID] and
    (__ \ "componentId").read[UUID] and
    (__ \ "version").read[Long] and
    (__ \ "document_id").read[UUID] and
    (__ \ "createdAt").read[DateTime] and
    (__ \ "updatedAt").read[DateTime]
  )(ComponentScratchpad.apply _)

  implicit val taskWrites: Writes[ComponentScratchpad] = (
    (__ \ "userId").write[UUID] and
    (__ \ "componentId").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "document_id").write[UUID] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(ComponentScratchpad.unapply))

}
