package ca.shiftfocus.krispii.core.models

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class Conversation(
  id: UUID = UUID.randomUUID(),
  ownerId: UUID,
  version: Long = 1L,
  title: String,
  shared: Boolean = false,
  entityId: Option[UUID] = None,
  entityType: Option[String] = None,
  members: IndexedSeq[User] = IndexedSeq.empty[User],
  createdAt: DateTime = new DateTime(),
  updatedAt: DateTime = new DateTime()
)

object Conversation {
  implicit val writes = new Writes[Conversation] {
    def writes(conversation: Conversation): JsValue = {
      Json.obj(
        "id" -> conversation.id.toString,
        "ownerId" -> conversation.ownerId.toString,
        "version" -> conversation.version,
        "title" -> conversation.title,
        "shared" -> conversation.shared,
        "entityId" -> conversation.entityId,
        "entityType" -> conversation.entityType,
        "members" -> conversation.members,
        "createdAt" -> conversation.createdAt,
        "updatedAt" -> conversation.updatedAt
      )
    }
  }
}

object ConversationEntityType {
  val Work = "work"
}

