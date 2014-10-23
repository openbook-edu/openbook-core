package com.shiftfocus.krispii.core.models

import com.github.mauricio.async.db.RowData
import com.shiftfocus.krispii.core.lib.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._


case class TaskResponseAlreadyExistsException(msg: String) extends Exception
case class TaskResponseOutOfDateException(msg: String) extends Exception

case class TaskResponse(
  userId: UUID,
  taskId: UUID,
  revision: Long = 1,
  version: Long = 0,
  content: String,
  isComplete: Boolean,
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
)

object TaskResponse {

  def apply(row: RowData): TaskResponse = {
    TaskResponse(
      UUID(row("user_id").asInstanceOf[Array[Byte]]),
      UUID(row("task_id").asInstanceOf[Array[Byte]]),
      row("revision").asInstanceOf[Long],
      row("version").asInstanceOf[Long],
      row("response").asInstanceOf[String],
      row("is_complete").asInstanceOf[Boolean],
      Some(row("created_at").asInstanceOf[DateTime]),
      Some(row("updated_at").asInstanceOf[DateTime])
    )
  }

  implicit val taskReads: Reads[TaskResponse] = (
    (__ \ "userId").read[UUID] and
    (__ \ "taskId").read[UUID] and
    (__ \ "revision").read[Long] and
    (__ \ "version").read[Long] and
    (__ \ "content").read[String] and
    (__ \ "isComplete").read[Boolean] and
    (__ \ "createdAt").readNullable[DateTime] and
    (__ \ "updatedAt").readNullable[DateTime]
  )(TaskResponse.apply(_: UUID, _: UUID, _: Long, _: Long, _: String, _: Boolean, _: Option[DateTime], _: Option[DateTime]))

  implicit val taskWrites: Writes[TaskResponse] = (
    (__ \ "userId").write[UUID] and
    (__ \ "taskId").write[UUID] and
    (__ \ "revision").write[Long] and
    (__ \ "version").write[Long] and
    (__ \ "content").write[String] and
    (__ \ "isComplete").write[Boolean] and
    (__ \ "createdAt").writeNullable[DateTime] and
    (__ \ "updatedAt").writeNullable[DateTime]
  )(unlift(TaskResponse.unapply))
}

case class TaskResponsePut(
  userId: UUID,
  taskId: UUID,
  revision: Long,
  version: Long = 0,
  content: String,
  isComplete: Boolean,
  newRevision: Option[Boolean],
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
)
object TaskResponsePut {
  implicit val taskReads: Reads[TaskResponsePut] = (
    (__ \ "userId").read[UUID] and
    (__ \ "taskId").read[UUID] and
    (__ \ "revision").read[Long] and
    (__ \ "version").read[Long] and
    (__ \ "content").read[String] and
    (__ \ "isComplete").read[Boolean] and
    (__ \ "newRevision").readNullable[Boolean] and
    (__ \ "createdAt").readNullable[DateTime] and
    (__ \ "updatedAt").readNullable[DateTime]
  )(TaskResponsePut.apply _)
}
