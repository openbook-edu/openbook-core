package ca.shiftfocus.krispii.core.models

import com.github.mauricio.async.db.RowData
import ca.shiftfocus.uuid.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._



case class Section(
  id: UUID = UUID.random,
  version: Long = 0,
  courseId: UUID,
  teacherId: Option[UUID],
  name: String,
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
)

object Section {

  def apply(row: RowData): Section = {
    Section(
      UUID(row("id").asInstanceOf[Array[Byte]]),
      row("version").asInstanceOf[Long],
      UUID(row("course_id").asInstanceOf[Array[Byte]]),
      Option(row("teacher_id").asInstanceOf[Array[Byte]]) match {
        case Some(bytes) => Some(UUID(bytes))
        case None => None
      },
      row("name").asInstanceOf[String],
      Some(row("created_at").asInstanceOf[DateTime]),
      Some(row("updated_at").asInstanceOf[DateTime])
    )
  }

  implicit val sectionReads: Reads[Section] = (
    (__ \ "id").read[UUID] and
    (__ \ "version").read[Long] and
    (__ \ "courseId").read[UUID] and
    (__ \ "teacherId").readNullable[UUID] and
    (__ \ "name").read[String] and
    (__ \ "createdAt").readNullable[DateTime] and
    (__ \ "updatedAt").readNullable[DateTime]
  )(Section.apply(_: UUID, _: Long, _: UUID, _: Option[UUID], _: String, _: Option[DateTime], _: Option[DateTime]))

  implicit val sectionWrites: Writes[Section] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "courseId").write[UUID] and
    (__ \ "teacherId").writeNullable[UUID] and
    (__ \ "name").write[String] and
    (__ \ "createdAt").writeNullable[DateTime] and
    (__ \ "updatedAt").writeNullable[DateTime]
  )(unlift(Section.unapply))

}

case class SectionPost(
  courseId: UUID,
  teacherId: Option[UUID],
  projectIds: Option[IndexedSeq[UUID]],
  name: String
)
object SectionPost {
  implicit val coursePutReads = (
    (__ \ "courseId").read[UUID] and
    (__ \ "teacherId").readNullable[UUID] and
    (__ \ "projectIds").readNullable[IndexedSeq[UUID]] and
    (__ \ "name").read[String]
  )(SectionPost.apply _)
}

case class SectionPut(
  version: Long,
  courseId: UUID,
  teacherId: Option[UUID],
  projectIds: Option[IndexedSeq[UUID]],
  name: String
)
object SectionPut {
  implicit val coursePutReads = (
    (__ \ "version").read[Long] and
    (__ \ "courseId").read[UUID] and
    (__ \ "teacherId").readNullable[UUID] and
    (__ \ "projectIds").readNullable[IndexedSeq[UUID]] and
    (__ \ "name").read[String]
  )(SectionPut.apply _)
}
