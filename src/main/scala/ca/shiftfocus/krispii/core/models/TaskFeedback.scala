package ca.shiftfocus.krispii.core.models

import com.github.mauricio.async.db.RowData
import ca.shiftfocus.uuid.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._


case class TaskFeedback(
  studentId: UUID,
  taskId: UUID,
  version: Long = 1,
  documentId: UUID,
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
)

object TaskFeedback {

  def apply(row: RowData): TaskFeedback = {
    TaskFeedback(
      studentId  = UUID(row("student_id").asInstanceOf[Array[Byte]]),
      taskId     = UUID(row("task_id").asInstanceOf[Array[Byte]]),
      version    = row("version").asInstanceOf[Long],
      documentId = UUID(row("document_id").asInstanceOf[Array[Byte]]),
      createdAt  = Some(row("created_at").asInstanceOf[DateTime]),
      updatedAt  = Some(row("updated_at").asInstanceOf[DateTime])
    )
  }

  implicit val taskFeedbackReads: Reads[TaskFeedback] = (
    (__ \ "studentId").read[UUID] and
      (__ \ "taskId").read[UUID] and
      (__ \ "version").read[Long] and
      (__ \ "documentId").read[UUID] and
      (__ \ "createdAt").readNullable[DateTime] and
      (__ \ "updatedAt").readNullable[DateTime]
  )(TaskFeedback.apply(_: UUID, _: UUID, _: Long, _: UUID, _: Option[DateTime], _: Option[DateTime]))

  implicit val taskFeedbackWrites: Writes[TaskFeedback] = (
    (__ \ "studentId").write[UUID] and
      (__ \ "taskId").write[UUID] and
      (__ \ "version").write[Long] and
      (__ \ "documentId").write[UUID] and
      (__ \ "createdAt").writeNullable[DateTime] and
      (__ \ "updatedAt").writeNullable[DateTime]
  )(unlift(TaskFeedback.unapply))

}
