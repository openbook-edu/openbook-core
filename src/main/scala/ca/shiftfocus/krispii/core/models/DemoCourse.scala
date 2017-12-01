package ca.shiftfocus.krispii.core.models

import java.awt.Color
import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class DemoCourse(
  id: UUID = UUID.randomUUID,
  version: Long = 1L,
  name: String,
  lang: String,
  // Dark green
  color: Color = new Color(0, 100, 0),
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime
)

object DemoCourse {

  implicit val colorReads = new Reads[Color] {
    def reads(json: JsValue) = {
      val mr = (json \ "r").asOpt[Int]
      val mg = (json \ "g").asOpt[Int]
      val mb = (json \ "b").asOpt[Int]

      (mr, mg, mb) match {
        case (Some(r), Some(g), Some(b)) => JsSuccess(new Color(r, g, b))
        case _ => JsError("Invalid format: color must include r, g, and b values as integers.")
      }
    }
  }

  implicit val colorWrites = new Writes[Color] {
    def writes(color: Color): JsValue = {
      Json.obj(
        "r" -> color.getRed,
        "g" -> color.getGreen,
        "b" -> color.getBlue
      )
    }
  }

  implicit val sectionWrites: Writes[DemoCourse] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "name").write[String] and
    (__ \ "lang").write[String] and
    (__ \ "color").write[Color] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(DemoCourse.unapply))
}

case class DemoCoursePost(
  name: String,
  lang: String,
  color: Color
)
object DemoCoursePost {
  implicit val colorReads = DemoCourse.colorReads
  implicit val demoCoursePutReads = (
    (__ \ "name").read[String] and
    (__ \ "lang").read[String] and
    (__ \ "color").read[Color]
  )(DemoCoursePost.apply _)
}

case class DemoCoursePut(
  version: Long,
  name: Option[String],
  color: Option[Color]
)
object DemoCoursePut {
  implicit val colorReads = DemoCourse.colorReads
  implicit val demoCoursePutReads = (
    (__ \ "version").read[Long] and
    (__ \ "name").readNullable[String] and
    (__ \ "color").readNullable[Color]
  )(DemoCoursePut.apply _)
}
