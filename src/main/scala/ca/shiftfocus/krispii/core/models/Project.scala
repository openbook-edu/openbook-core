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
  version: Long = 1L,
  name: String,
  slug: String,
  description: String,
  availability: String = Project.Availability.AnyTime,
  parts: IndexedSeq[Part],
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime
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
      }
      case _ => false
    }
  }
}

object Project {

  object Availability {
    val AnyTime = "any"
    val FreeTime = "free"
    val CourseTime = "course"
  }

  implicit val projectWrites: Writes[Project] = (
    (__ \ "id").write[UUID] and
    (__ \ "courseId").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "name").write[String] and
    (__ \ "slug").write[String] and
    (__ \ "description").write[String] and
    (__ \ "availability").write[String] and
    (__ \ "parts").write[IndexedSeq[Part]] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
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
