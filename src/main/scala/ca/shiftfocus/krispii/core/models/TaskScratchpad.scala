package ca.shiftfocus.krispii.core.models

import com.github.mauricio.async.db.RowData
import ca.shiftfocus.uuid.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._

case class TaskScratchpadOutOfDateException(msg: String) extends Exception
case class TaskScratchpadAlreadyExistsException(msg: String) extends Exception
case class TaskScratchpadDisabledException(msg: String) extends Exception

case class TaskScratchpad(
  userId: UUID,
  taskId: UUID,
  version: Long = 0,
  documentId: UUID,
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
)

object TaskScratchpad {

  /**
   * Constructor that can build a new TaskNote from a RowData object.
   *
   * @param row a [[RowData]] object returned from the database layer
   * @return a new [[TaskNote]] object
   */
  def apply(row: RowData): TaskScratchpad = {
    TaskScratchpad(
      UUID(row("user_id").asInstanceOf[Array[Byte]]),
      UUID(row("task_id").asInstanceOf[Array[Byte]]),
      row("version").asInstanceOf[Long],
      UUID(row("document_id").asInstanceOf[Array[Byte]]),
      Some(row("created_at").asInstanceOf[DateTime]),
      Some(row("updated_at").asInstanceOf[DateTime])
    )
  }

  implicit val taskReads: Reads[TaskScratchpad] = (
    (__ \ "userId").read[UUID] and
    (__ \ "taskId").read[UUID] and
    (__ \ "version").read[Long] and
    (__ \ "documentId").read[UUID] and
    (__ \ "createdAt").readNullable[DateTime] and
    (__ \ "updatedAt").readNullable[DateTime]
  )(TaskScratchpad.apply(_: UUID, _: UUID, _: Long, _: UUID, _: Option[DateTime], _: Option[DateTime]))

  implicit val taskWrites: Writes[TaskScratchpad] = (
    (__ \ "userId").write[UUID] and
    (__ \ "taskId").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "documentId").write[UUID] and
    (__ \ "createdAt").writeNullable[DateTime] and
    (__ \ "updatedAt").writeNullable[DateTime]
   )(unlift(TaskScratchpad.unapply _))

}
