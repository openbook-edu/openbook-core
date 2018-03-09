package ca.shiftfocus.krispii.core.models

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.JodaWrites._

case class Link(
  link: String,
  courseId: UUID,
  createdAt: DateTime = new DateTime
) {}

object Link {
  implicit val linkWrites: Writes[Link] = (
    (__ \ "link").write[String] and
    (__ \ "course_id").write[UUID] and
    (__ \ "createdAt").write[DateTime]
  )(unlift(Link.unapply))
}