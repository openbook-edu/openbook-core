package ca.shiftfocus.krispii.core.models

import java.util.UUID
import java.awt.Color
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._
import play.api.libs.json.JodaWrites._

case class Course(
  id: UUID = UUID.randomUUID,
  version: Long = 1L,
  teacherId: UUID,
  name: String,
  color: Color,
  slug: String,
  enabled: Boolean = true,
  archived: Boolean = false,
  deleted: Boolean = false,
  chatEnabled: Boolean = true,
  schedulingEnabled: Boolean = false,
  projects: Option[IndexedSeq[Project]] = None,
  theaterMode: Boolean = false,
  lastProjectId: Option[UUID] = None,
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime
)

object Course {

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

  implicit val sectionWrites: Writes[Course] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "teacherId").write[UUID] and
    (__ \ "name").write[String] and
    (__ \ "color").write[Color] and
    (__ \ "slug").write[String] and
    (__ \ "enabled").write[Boolean] and
    (__ \ "archived").write[Boolean] and
    (__ \ "deleted").write[Boolean] and
    (__ \ "chatEnabled").write[Boolean] and
    (__ \ "schedulingEnabled").write[Boolean] and
    (__ \ "projects").writeNullable[IndexedSeq[Project]] and
    (__ \ "theaterMode").write[Boolean] and
    (__ \ "lastProjectId").writeNullable[UUID] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(Course.unapply))

}

case class CoursePost(
  teacherId: UUID,
  name: String,
  color: Color,
  slug: String
)
object CoursePost {
  implicit val colorReads = Course.colorReads
  implicit val coursePutReads = (
    (__ \ "teacherId").read[UUID] and
    (__ \ "name").read[String] and
    (__ \ "color").read[Color] and
    (__ \ "slug").read[String]
  )(CoursePost.apply _)
}

case class CoursePut(
  version: Long,
  teacherId: Option[UUID],
  name: Option[String],
  color: Option[Color],
  slug: Option[String],
  enabled: Option[Boolean],
  archived: Option[Boolean],
  deleted: Option[Boolean],
  chatEnabled: Option[Boolean],
  schedulingEnabled: Option[Boolean]
)
object CoursePut {
  implicit val colorReads = Course.colorReads
  implicit val coursePutReads = (
    (__ \ "version").read[Long] and
    (__ \ "teacherId").readNullable[UUID] and
    (__ \ "name").readNullable[String] and
    (__ \ "color").readNullable[Color] and
    (__ \ "slug").readNullable[String] and
    (__ \ "enabled").readNullable[Boolean] and
    (__ \ "archived").readNullable[Boolean] and
    (__ \ "deleted").readNullable[Boolean] and
    (__ \ "chatEnabled").readNullable[Boolean] and
    (__ \ "schedulingEnabled").readNullable[Boolean]
  )(CoursePut.apply _)
}
