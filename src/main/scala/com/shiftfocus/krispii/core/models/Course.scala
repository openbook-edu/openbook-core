package com.shiftfocus.krispii.core.models

import com.github.mauricio.async.db.RowData
import com.shiftfocus.krispii.core.lib.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._

case class Course(
  id: UUID = UUID.random,
  version: Long = 0,
  name: String,
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
)

object Course {

  def apply(row: RowData): Course = {
    Course(
      UUID(row("id").asInstanceOf[Array[Byte]]),
      row("version").asInstanceOf[Long],
      row("name").asInstanceOf[String],
      Some(row("created_at").asInstanceOf[DateTime]),
      Some(row("updated_at").asInstanceOf[DateTime])
    )
  }

  implicit val courseReads: Reads[Course] = (
    (__ \ "id").read[UUID] and
    (__ \ "version").read[Long] and
    (__ \ "name").read[String] and
    (__ \ "createdAt").readNullable[DateTime] and
    (__ \ "updatedAt").readNullable[DateTime]
  )(Course.apply(_: UUID, _: Long, _: String, _: Option[DateTime], _: Option[DateTime]))

  implicit val courseWrites: Writes[Course] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "name").write[String] and
    (__ \ "createdAt").writeNullable[DateTime] and
    (__ \ "updatedAt").writeNullable[DateTime]
  )(unlift(Course.unapply))

}

case class CoursePut(
  version: Long,
  name: String
)
object CoursePut {
  implicit val coursePutReads = (
    (__ \ "version").read[Long] and
    (__ \ "name").read[String]
  )(CoursePut.apply _)
}
