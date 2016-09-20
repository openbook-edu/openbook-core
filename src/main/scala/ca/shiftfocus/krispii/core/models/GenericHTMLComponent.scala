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
  htmlContent: String
)

object GenericHTMLComponentPost {
  implicit val projectPostReads = (
    (__ \ "ownerId").read[UUID] and
    (__ \ "title").read[String] and
    (__ \ "questions").readNullable[String] and
    (__ \ "thingsToThinkAbout").readNullable[String] and
    (__ \ "order").read[Int] and
    (__ \ "isPrivate").read[Boolean] and
    (__ \ "htmlContent").read[String]
  )(GenericHTMLComponentPost.apply _)
}

case class GenericHTMLComponentPut(
  version: Long,
  title: String,
  questions: String,
  thingsToThinkAbout: String,
  order: Int,
  isPrivate: Boolean,
  htmlContent: String
)

object GenericHTMLComponentPut {
  implicit val projectPutReads = (
    (__ \ "version").read[Long] and
    (__ \ "title").read[String] and
    (__ \ "questions").read[String] and
    (__ \ "thingsToThinkAbout").read[String] and
    (__ \ "order").read[Int] and
    (__ \ "isPrivate").read[Boolean] and
    (__ \ "htmlContent").read[String]
  )(GenericHTMLComponentPut.apply _)
}