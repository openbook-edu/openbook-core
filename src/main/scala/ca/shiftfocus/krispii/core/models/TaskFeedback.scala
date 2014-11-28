package ca.shiftfocus.krispii.core.models

import com.github.mauricio.async.db.RowData
import ca.shiftfocus.uuid.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._


case class TaskFeedback(
  teacherId: UUID,
  studentId: UUID,
  taskId: UUID,
  revision: Long = 1,
  version: Long = 0,
  content: String,
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
)

object TaskFeedback {

  def apply(row: RowData): TaskFeedback = {
    TaskFeedback(
      teacherId = UUID(row("teacher_id").asInstanceOf[Array[Byte]]),
      studentId = UUID(row("student_id").asInstanceOf[Array[Byte]]),
      taskId    = UUID(row("task_id").asInstanceOf[Array[Byte]]),
      revision  = row("revision").asInstanceOf[Long],
      version   = row("version").asInstanceOf[Long],
      content   = row("content").asInstanceOf[String],
      createdAt = Some(row("created_at").asInstanceOf[DateTime]),
      updatedAt = Some(row("updated_at").asInstanceOf[DateTime])
    )
  }

  implicit val taskFeedbackReads: Reads[TaskFeedback] = (
    (__ \ "teacherId").read[UUID] and
      (__ \ "studentId").read[UUID] and
      (__ \ "taskId").read[UUID] and
      (__ \ "revision").read[Long] and
      (__ \ "version").read[Long] and
      (__ \ "content").read[String] and
      (__ \ "createdAt").readNullable[DateTime] and
      (__ \ "updatedAt").readNullable[DateTime]
    )(TaskFeedback.apply(_: UUID, _: UUID, _: UUID, _: Long, _: Long, _: String, _: Option[DateTime], _: Option[DateTime]))

  implicit val taskFeedbackWrites: Writes[TaskFeedback] = (
    (__ \ "teacherId").write[UUID] and
      (__ \ "studentId").write[UUID] and
      (__ \ "taskId").write[UUID] and
      (__ \ "revision").write[Long] and
      (__ \ "version").write[Long] and
      (__ \ "content").write[String] and
      (__ \ "createdAt").writeNullable[DateTime] and
      (__ \ "updatedAt").writeNullable[DateTime]
    )(unlift(TaskFeedback.unapply))

}
