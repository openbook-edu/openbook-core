package ca.shiftfocus.krispii.core.models

import collection.immutable.StringOps
import com.github.mauricio.async.db.RowData
import ca.shiftfocus.uuid.UUID
import java.awt.Color
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._

case class Course(
  id: UUID = UUID.random,
  version: Long = 0,
  teacherId: UUID,
  name: String,
  color: Color,
  projects: Option[IndexedSeq[Project]] = None,
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
)

object Course {

  def apply(row: RowData): Course = {
    Course(
      UUID(row("id").asInstanceOf[Array[Byte]]),
      row("version").asInstanceOf[Long],
      UUID(row("teacher_id").asInstanceOf[Array[Byte]]),
      row("name").asInstanceOf[String],
      new Color(Option(row("color").asInstanceOf[Int]).getOrElse(0)),
      None,
      Some(row("created_at").asInstanceOf[DateTime]),
      Some(row("updated_at").asInstanceOf[DateTime])
    )
  }

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

  implicit val sectionReads: Reads[Course] = (
    (__ \ "id").read[UUID] and
    (__ \ "version").read[Long] and
    (__ \ "teacherId").read[UUID] and
    (__ \ "name").read[String] and
    (__ \ "color").read[Color] and
    (__ \ "projects").readNullable[IndexedSeq[Project]] and
    (__ \ "createdAt").readNullable[DateTime] and
    (__ \ "updatedAt").readNullable[DateTime]
  )(Course.apply(_: UUID, _: Long, _: UUID, _: String, _: Color, _: Option[IndexedSeq[Project]], _: Option[DateTime], _: Option[DateTime]))

  implicit val sectionWrites: Writes[Course] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "teacherId").write[UUID] and
    (__ \ "name").write[String] and
    (__ \ "color").write[Color] and
    (__ \ "projects").writeNullable[IndexedSeq[Project]] and
    (__ \ "createdAt").writeNullable[DateTime] and
    (__ \ "updatedAt").writeNullable[DateTime]
  )(unlift(Course.unapply))

}

case class CoursePost(
  teacherId: Option[UUID],
  projectIds: Option[IndexedSeq[UUID]],
  name: String
)
object CoursePost {
  implicit val coursePutReads = (
    (__ \ "teacherId").readNullable[UUID] and
    (__ \ "projectIds").readNullable[IndexedSeq[UUID]] and
    (__ \ "name").read[String]
  )(CoursePost.apply _)
}

case class CoursePut(
  version: Long,
  teacherId: Option[UUID],
  projectIds: Option[IndexedSeq[UUID]],
  name: String
)
object CoursePut {
  implicit val coursePutReads = (
    (__ \ "version").read[Long] and
    (__ \ "teacherId").readNullable[UUID] and
    (__ \ "projectIds").readNullable[IndexedSeq[UUID]] and
    (__ \ "name").read[String]
  )(CoursePut.apply _)
}
