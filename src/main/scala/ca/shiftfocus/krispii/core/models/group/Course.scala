package ca.shiftfocus.krispii.core.models.group

import java.awt.Color
import java.util.UUID

import ca.shiftfocus.krispii.core.models.Project
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.JodaWrites._

case class Course(
  id: UUID = UUID.randomUUID,
  version: Long = 1L,
  ownerId: UUID,
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
) extends Group

object Course {

  implicit val colorWrites = ColorBox.colorWrites

  implicit val courseWrites: Writes[Course] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "ownerId").write[UUID] and
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
  implicit val colorReads = ColorBox.colorReads
  implicit val coursePutReads = (
    (__ \ "ownerId").read[UUID] and
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
  implicit val colorReads = ColorBox.colorReads
  implicit val coursePutReads = (
    (__ \ "version").read[Long] and
    (__ \ "ownerId").readNullable[UUID] and
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
