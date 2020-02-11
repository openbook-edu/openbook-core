package ca.shiftfocus.krispii.core.models

import java.util.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._

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
    (__ \ "createdAt").read[DateTime] and
    (__ \ "updatedAt").read[DateTime]
  )(TaskFeedback.apply _)

  implicit val taskFeedbackWrites: Writes[TaskFeedback] = (
    (__ \ "studentId").write[UUID] and
    (__ \ "taskId").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "documentId").write[UUID] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(TaskFeedback.unapply))

}
