package ca.shiftfocus.krispii.core.models

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Created by ryanez on 13/04/16.
 */
case class GenericHTMLComponent(
  id: UUID = UUID.randomUUID,
  version: Long = 1L,
  ownerId: UUID,
  title: String,
  questions: String,
  thingsToThinkAbout: String,
  order: Int,
  isPrivate: Boolean = false,
  htmlContent: String,
  description: String = "",
  parentId: Option[UUID] = None,
  parentVersion: Option[Long] = None,
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime
) extends Component

object GenericHTMLComponent {

  implicit val genericHTMLComponentWrites: Writes[GenericHTMLComponent] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "ownerId").write[UUID] and
    (__ \ "title").write[String] and
    (__ \ "questions").write[String] and
    (__ \ "thingsToThinkAbout").write[String] and
    (__ \ "order").write[Int] and
    (__ \ "isPrivate").write[Boolean] and
    (__ \ "htmlContent").write[String] and
    (__ \ "description").write[String] and
    (__ \ "parentId").writeNullable[UUID] and
    (__ \ "parentVersion").writeNullable[Long] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(GenericHTMLComponent.unapply))

}

case class GenericHTMLComponentPost(
  ownerId: UUID,
  title: String,
  questions: Option[String],
  thingsToThinkAbout: Option[String],
  order: Int,
  isPrivate: Boolean,
  htmlContent: String,
  description: String,
  parentId: Option[UUID],
  parentVersion: Option[Long]
)

object GenericHTMLComponentPost {
  implicit val projectPostReads = (
    (__ \ "ownerId").read[UUID] and
    (__ \ "title").read[String] and
    (__ \ "questions").readNullable[String] and
    (__ \ "thingsToThinkAbout").readNullable[String] and
    (__ \ "order").read[Int] and
    (__ \ "isPrivate").read[Boolean] and
    (__ \ "htmlContent").read[String] and
    (__ \ "description").read[String] and
    (__ \ "parentId").readNullable[UUID] and
    (__ \ "parentVersion").readNullable[Long]
  )(GenericHTMLComponentPost.apply _)
}

case class GenericHTMLComponentPut(
  version: Long,
  title: String,
  questions: String,
  thingsToThinkAbout: String,
  order: Int,
  isPrivate: Boolean,
  htmlContent: String,
  description: String,
  parentId: Option[UUID],
  parentVersion: Option[Long]
)

object GenericHTMLComponentPut {
  implicit val projectPutReads = (
    (__ \ "version").read[Long] and
    (__ \ "title").read[String] and
    (__ \ "questions").read[String] and
    (__ \ "thingsToThinkAbout").read[String] and
    (__ \ "order").read[Int] and
    (__ \ "isPrivate").read[Boolean] and
    (__ \ "htmlContent").read[String] and
    (__ \ "description").read[String] and
    (__ \ "parentId").readNullable[UUID] and
    (__ \ "parentVersion").readNullable[Long]
  )(GenericHTMLComponentPut.apply _)
}