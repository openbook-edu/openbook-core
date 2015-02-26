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
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime
)

object TaskFeedback {

  implicit val taskFeedbackReads: Reads[TaskFeedback] = (
    (__ \ "studentId").read[UUID] and
      (__ \ "taskId").read[UUID] and
      (__ \ "version").read[Long] and
      (__ \ "documentId").read[UUID] and
      (__ \ "createdAt").readNullable[DateTime] and
      (__ \ "updatedAt").readNullable[DateTime]
  )(TaskFeedback.apply _)

  implicit val taskFeedbackWrites: Writes[TaskFeedback] = (
    (__ \ "studentId").write[UUID] and
      (__ \ "taskId").write[UUID] and
      (__ \ "version").write[Long] and
      (__ \ "documentId").write[UUID] and
      (__ \ "createdAt").writeNullable[DateTime] and
      (__ \ "updatedAt").writeNullable[DateTime]
  )(unlift(TaskFeedback.unapply))

}
