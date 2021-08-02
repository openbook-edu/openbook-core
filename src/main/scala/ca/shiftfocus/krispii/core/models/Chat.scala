package ca.shiftfocus.krispii.core.models

import java.util.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._

case class Chat(
  courseId: UUID,
  messageNum: Long = 0L,
  userId: UUID,
  message: String,
  seen: Boolean = false, // derived from table messages_seen
  hidden: Boolean = false,
  shouting: Boolean = false,
  createdAt: DateTime = new DateTime
)

object Chat {
  implicit val chatReads: Reads[Chat] = (
    (__ \ "groupId").read[UUID] and
    (__ \ "messageNum").read[Long] and
    (__ \ "userId").read[UUID] and
    (__ \ "message").read[String] and
    (__ \ "seen").read[Boolean] and
    (__ \ "hidden").read[Boolean] and
    (__ \ "shouting").read[Boolean] and
    (__ \ "createdAt").read[DateTime]
  )(Chat.apply _)

  implicit val chatWrites: Writes[Chat] = (
    (__ \ "groupId").write[UUID] and
    (__ \ "messageNum").write[Long] and
    (__ \ "userId").write[UUID] and
    (__ \ "message").write[String] and
    (__ \ "seen").write[Boolean] and
    (__ \ "hidden").write[Boolean] and
    (__ \ "shouting").write[Boolean] and
    (__ \ "createdAt").write[DateTime]
  )(unlift(Chat.unapply))
}
