package ca.shiftfocus.krispii.core.models

import com.github.mauricio.async.db.RowData
import java.util.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._

case class Project(
    id: UUID = UUID.randomUUID,
    courseId: UUID,
    version: Long = 1L,
    name: String,
    slug: String,
    description: String,
    availability: String = Project.Availability.AnyTime,
    enabled: Boolean = false,
    parts: IndexedSeq[Part],
    createdAt: DateTime = new DateTime,
    updatedAt: DateTime = new DateTime
) {
  override def equals(other: Any): Boolean = {
    other match {
      case otherProject: Project => {
        this.id.equals(otherProject.id) &&
          this.courseId.equals(otherProject.courseId) &&
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
    (__ \ "enabled").write[Boolean] and
    (__ \ "parts").write[IndexedSeq[Part]] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(Project.unapply))

}
