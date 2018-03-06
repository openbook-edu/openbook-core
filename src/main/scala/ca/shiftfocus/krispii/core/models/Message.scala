package ca.shiftfocus.krispii.core.models

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.json.{ JsValue, Json, Writes }
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._

case class Message(
  id: UUID = UUID.randomUUID(),
  conversationId: UUID,
  userId: UUID,
  content: String,
  revisionId: Option[String] = None,
  revisionType: Option[String] = None,
  revisionVersion: Option[String] = None,
  createdAt: DateTime = new DateTime()
)

object Message {
  implicit val writes = new Writes[Message] {
    def writes(message: Message): JsValue = {
      Json.obj(
        "id" -> message.id.toString,
        "ownerId" -> message.conversationId.toString,
        "userId" -> message.userId.toString,
        "content" -> message.content,
        "revisionId" -> message.revisionId,
        "revisionType" -> message.revisionType,
        "revisionVersion" -> message.revisionVersion,
        "createdAt" -> message.createdAt
      )
    }
  }
}