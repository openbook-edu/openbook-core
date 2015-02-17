package ca.shiftfocus.krispii.core.models

import com.github.mauricio.async.db.RowData
import ca.shiftfocus.uuid.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._


case class Project(
  id: UUID = UUID.random,
  courseId: UUID,
  version: Long = 0,
  name: String,
  slug: String,
  description: String,
  availability: String = Project.Availability.AnyTime,
  parts: IndexedSeq[Part],
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
) {
  override def equals(other: Any): Boolean = {
    other match {
      case otherProject: Project => {
        this.id == otherProject.id &&
        this.courseId == otherProject.courseId &&
        this.version == otherProject.version &&
        this.name == otherProject.name &&
        this.slug == otherProject.slug &&
        this.description == otherProject.description &&
        this.availability == otherProject.availability &&
        this.parts == otherProject.parts
//          {(this.createdAt, otherProject.createdAt) match {
//          case (Some(thisDate), Some(thatDate)) => thisDate.equals(thatDate)
//          case _ => false
//        }} &&
//        {(this.updatedAt, otherProject.updatedAt) match {
//          case (Some(thisDate), Some(thatDate)) => thisDate.equals(thatDate)
//          case _ => false
//        }}
      }
      case _ => false
    }
  }
}

object Project {

  object Availability {
    val AnyTime = "any"
    val FreeTime = "free"
    val CourseTime = "class"
  }

  def apply(row: RowData): Project = {
    Project(
      UUID(row("id").asInstanceOf[Array[Byte]]),
      UUID(row("course_id").asInstanceOf[Array[Byte]]),
      row("version").asInstanceOf[Long],
      row("name").asInstanceOf[String],
      row("slug").asInstanceOf[String],
      row("description").asInstanceOf[String],
      row("availability").asInstanceOf[String],
      IndexedSeq[Part](),
      Some(row("created_at").asInstanceOf[DateTime]),
      Some(row("updated_at").asInstanceOf[DateTime])
    )
  }

  implicit val projectReads: Reads[Project] = (
    (__ \ "id").read[UUID] and
    (__ \ "courseId").read[UUID] and
    (__ \ "version").read[Long] and
    (__ \ "name").read[String] and
    (__ \ "slug").read[String] and
    (__ \ "description").read[String] and
    (__ \ "availability").read[String] and
    (__ \ "parts").read[IndexedSeq[Part]] and
    (__ \ "createdAt").readNullable[DateTime] and
    (__ \ "updatedAt").readNullable[DateTime]
  )(Project.apply(_: UUID, _: UUID, _: Long, _: String, _: String, _: String, _: String, _: IndexedSeq[Part], _: Option[DateTime], _: Option[DateTime]))

  implicit val projectWrites: Writes[Project] = (
    (__ \ "id").write[UUID] and
    (__ \ "courseId").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "name").write[String] and
    (__ \ "slug").write[String] and
    (__ \ "description").write[String] and
    (__ \ "availability").write[String] and
    (__ \ "parts").write[IndexedSeq[Part]] and
    (__ \ "createdAt").writeNullable[DateTime] and
    (__ \ "updatedAt").writeNullable[DateTime]
  )(unlift(Project.unapply))

}

case class ProjectPost(
  name: String,
  slug: String,
  description: String
)
object ProjectPost {
  implicit val projectPostReads = (
    (__ \ "name").read[String] and
    (__ \ "slug").read[String] and
    (__ \ "description").read[String]
  )(ProjectPost.apply _)
}

case class ProjectPut(
  version: Long,
  name: String,
  slug: String,
  description: String
)
object ProjectPut {
  implicit val projectPutReads = (
    (__ \ "version").read[Long] and
    (__ \ "name").read[String] and
    (__ \ "slug").read[String] and
    (__ \ "description").read[String]
  )(ProjectPut.apply _)
}
