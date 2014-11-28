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
  revision: Long = 0,
  version: Long = 0,
  content: String,
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
      row("revision").asInstanceOf[Long],
      row("version").asInstanceOf[Long],
      row("notes").asInstanceOf[String],
      Some(row("created_at").asInstanceOf[DateTime]),
      Some(row("updated_at").asInstanceOf[DateTime])
    )
  }

  implicit val taskReads: Reads[TaskScratchpad] = (
    (__ \ "userId").read[UUID] and
    (__ \ "taskId").read[UUID] and
    (__ \ "revision").read[Long] and
    (__ \ "version").read[Long] and
    (__ \ "content").read[String] and
    (__ \ "createdAt").readNullable[DateTime] and
    (__ \ "updatedAt").readNullable[DateTime]
  )(TaskScratchpad.apply(_: UUID, _: UUID, _: Long, _: Long, _: String, _: Option[DateTime], _: Option[DateTime]))

  implicit val taskWrites: Writes[TaskScratchpad] = (
    (__ \ "userId").write[UUID] and
    (__ \ "taskId").write[UUID] and
    (__ \ "revision").write[Long] and
    (__ \ "version").write[Long] and
    (__ \ "content").write[String] and
    (__ \ "createdAt").writeNullable[DateTime] and
    (__ \ "updatedAt").writeNullable[DateTime]
   )(unlift(TaskScratchpad.unapply _))

}
