package ca.shiftfocus.krispii.core.models

import ca.shiftfocus.uuid.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._

case class Chat(
  courseId: UUID,
  messageNum: Long,
  userId: UUID,
  message: String,
  hidden: Boolean = false,
  createdAt: DateTime = new DateTime
)

object Chat {
  implicit val chatReads: Reads[Chat] = (
    (__ \ "courseId").read[UUID] and
      (__ \ "messageNum").read[Long] and
      (__ \ "userId").read[UUID] and
      (__ \ "message").read[String] and
      (__ \ "hidden").read[Boolean] and
      (__ \ "createdAt").read[DateTime]
    )(Chat.apply _)

  implicit val chatWrites: Writes[Chat] = (
    (__ \ "courseId").write[UUID] and
      (__ \ "messageNum").write[Long] and
      (__ \ "userId").write[UUID] and
      (__ \ "message").write[String] and
      (__ \ "hidden").write[Boolean] and
      (__ \ "createdAt").write[DateTime]
    )(unlift(Chat.unapply))
}
