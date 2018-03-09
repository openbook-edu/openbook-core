package ca.shiftfocus.krispii.core.models

import java.util.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._
import play.api.libs.json.JodaWrites._

case class TextComponent(
  id: UUID = UUID.randomUUID,
  version: Long = 1L,
  ownerId: UUID,
  title: String,
  questions: String,
  thingsToThinkAbout: String,
  content: String,
  order: Int,
  isPrivate: Boolean = false,
  description: String = "",
  parentId: Option[UUID] = None,
  parentVersion: Option[Long] = None,
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime
) extends Component

object TextComponent {

  implicit val textComponentWrites: Writes[TextComponent] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "ownerId").write[UUID] and
    (__ \ "title").write[String] and
    (__ \ "questions").write[String] and
    (__ \ "thingsToThinkAbout").write[String] and
    (__ \ "content").write[String] and
    (__ \ "order").write[Int] and
    (__ \ "isPrivate").write[Boolean] and
    (__ \ "description").write[String] and
    (__ \ "parentId").writeNullable[UUID] and
    (__ \ "parentVersion").writeNullable[Long] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(TextComponent.unapply))

}

case class TextComponentPost(
  ownerId: UUID,
  title: String,
  questions: Option[String],
  thingsToThinkAbout: Option[String],
  content: String,
  order: Int,
  isPrivate: Boolean,
  description: String,
  parentId: Option[UUID],
  parentVersion: Option[Long]
)
object TextComponentPost {
  implicit val projectPostReads = (
    (__ \ "ownerId").read[UUID] and
    (__ \ "title").read[String] and
    (__ \ "questions").readNullable[String] and
    (__ \ "thingsToThinkAbout").readNullable[String] and
    (__ \ "content").read[String] and
    (__ \ "order").read[Int] and
    (__ \ "isPrivate").read[Boolean] and
    (__ \ "description").read[String] and
    (__ \ "parentId").readNullable[UUID] and
    (__ \ "parentVersion").readNullable[Long]
  )(TextComponentPost.apply _)
}

case class TextComponentPut(
  version: Long,
  title: String,
  questions: String,
  thingsToThinkAbout: String,
  content: String,
  order: Int,
  isPrivate: Boolean,
  description: String,
  parentId: Option[UUID],
  parentVersion: Option[Long]
)
object TextComponentPut {
  implicit val projectPutReads = (
    (__ \ "version").read[Long] and
    (__ \ "title").read[String] and
    (__ \ "questions").read[String] and
    (__ \ "thingsToThinkAbout").read[String] and
    (__ \ "content").read[String] and
    (__ \ "order").read[Int] and
    (__ \ "isPrivate").read[Boolean] and
    (__ \ "description").read[String] and
    (__ \ "parentId").readNullable[UUID] and
    (__ \ "parentVersion").readNullable[Long]
  )(TextComponentPut.apply _)
}
