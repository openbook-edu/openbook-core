package ca.shiftfocus.krispii.core.models

import ca.shiftfocus.krispii.core.models.document.Document
import java.util.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._
import play.api.libs.json.JodaWrites._

case class TaskScratchpadOutOfDateException(msg: String) extends Exception
case class TaskScratchpadAlreadyExistsException(msg: String) extends Exception
case class TaskScratchpadDisabledException(msg: String) extends Exception

case class TaskScratchpad(
  userId: UUID,
  taskId: UUID,
  version: Long = 1L,
  documentId: UUID,
  document: Option[Document] = None,
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime
)

object TaskScratchpad {

  implicit val taskWrites: Writes[TaskScratchpad] = (
    (__ \ "userId").write[UUID] and
    (__ \ "taskId").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "documentId").write[UUID] and
    (__ \ "document").writeNullable[Document] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(TaskScratchpad.unapply _))

}
