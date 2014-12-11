package ca.shiftfocus.krispii.core.models

import collection.immutable.StringOps
import com.github.mauricio.async.db.RowData
import ca.shiftfocus.uuid.UUID
import java.awt.Color
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._

case class Class(
  id: UUID = UUID.random,
  version: Long = 0,
  teacherId: Option[UUID],
  name: String,
  color: Color,
  projects: Option[IndexedSeq[Project]] = None,
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
)

object Class {

  def apply(row: RowData): Class = {
    Class(
      UUID(row("id").asInstanceOf[Array[Byte]]),
      row("version").asInstanceOf[Long],
      Option(row("teacher_id").asInstanceOf[Array[Byte]]) match {
        case Some(bytes) => Some(UUID(bytes))
        case None => None
      },
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

  implicit val sectionReads: Reads[Class] = (
    (__ \ "id").read[UUID] and
    (__ \ "version").read[Long] and
    (__ \ "teacherId").readNullable[UUID] and
    (__ \ "name").read[String] and
    (__ \ "color").read[Color] and
    (__ \ "projects").readNullable[IndexedSeq[Project]] and
    (__ \ "createdAt").readNullable[DateTime] and
    (__ \ "updatedAt").readNullable[DateTime]
  )(Class.apply(_: UUID, _: Long, _: Option[UUID], _: String, _: Color, _: Option[IndexedSeq[Project]], _: Option[DateTime], _: Option[DateTime]))

  implicit val sectionWrites: Writes[Class] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "teacherId").writeNullable[UUID] and
    (__ \ "name").write[String] and
    (__ \ "color").write[Color] and
    (__ \ "projects").writeNullable[IndexedSeq[Project]] and
    (__ \ "createdAt").writeNullable[DateTime] and
    (__ \ "updatedAt").writeNullable[DateTime]
  )(unlift(Class.unapply))

}

case class SectionPost(
  teacherId: Option[UUID],
  projectIds: Option[IndexedSeq[UUID]],
  name: String
)
object SectionPost {
  implicit val coursePutReads = (
    (__ \ "teacherId").readNullable[UUID] and
    (__ \ "projectIds").readNullable[IndexedSeq[UUID]] and
    (__ \ "name").read[String]
  )(SectionPost.apply _)
}

case class SectionPut(
  version: Long,
  teacherId: Option[UUID],
  projectIds: Option[IndexedSeq[UUID]],
  name: String
)
object SectionPut {
  implicit val coursePutReads = (
    (__ \ "version").read[Long] and
    (__ \ "teacherId").readNullable[UUID] and
    (__ \ "projectIds").readNullable[IndexedSeq[UUID]] and
    (__ \ "name").read[String]
  )(SectionPut.apply _)
}
