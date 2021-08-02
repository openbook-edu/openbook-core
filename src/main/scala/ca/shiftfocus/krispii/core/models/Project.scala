package ca.shiftfocus.krispii.core.models

import java.util.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._
import play.api.libs.json.JodaWrites._

case class Project(
    id: UUID = UUID.randomUUID,
    courseId: UUID,
    parentId: Option[UUID] = None,
    parentVersion: Option[Long] = None,
    isMaster: Boolean = false,
    version: Long = 1L,
    name: String,
    slug: String,
    description: String,
    longDescription: String,
    availability: String = Project.Availability.AnyTime,
    enabled: Boolean = false,
    projectType: String,
    parts: IndexedSeq[Part],
    tags: IndexedSeq[Tag] = IndexedSeq(),
    status: Option[String] = None,
    lastTaskId: Option[UUID] = None,
    createdAt: DateTime = new DateTime,
    updatedAt: DateTime = new DateTime
) {
  override def equals(other: Any): Boolean = {
    other match {
      case otherProject: Project => {
        this.id.equals(otherProject.id) &&
          this.courseId.equals(otherProject.courseId) &&
          this.isMaster.equals(otherProject.isMaster) &&
          this.version == otherProject.version &&
          this.name == otherProject.name &&
          this.slug == otherProject.slug &&
          this.description == otherProject.description &&
          this.longDescription == otherProject.longDescription &&
          this.availability == otherProject.availability &&
          this.projectType == otherProject.projectType &&
          this.parts == otherProject.parts &&
          this.status == otherProject.status
      }
      case _ => false
    }
  }

  /**
   * Check if project object contains at least one task
   *
   * @return
   */
  def isProjectEmpty: Boolean = {
    this.parts.map { part =>
      if (part.tasks.nonEmpty) return false;
    }

    return true;
  }
}

object Project {

  object Availability {
    val AnyTime = "any"
    val FreeTime = "free"
    val CourseTime = "group"
  }

  object Type {
    val DefaultProject = "default_project"
    val SaS = "SaS"
    val KrispiiBites = "krispii_bites"
  }

  implicit val projectWrites: Writes[Project] = (
    (__ \ "id").write[UUID] and
    (__ \ "courseId").write[UUID] and
    (__ \ "parentId").writeNullable[UUID] and
    (__ \ "parentVersion").writeNullable[Long] and
    (__ \ "isMaster").write[Boolean] and
    (__ \ "version").write[Long] and
    (__ \ "name").write[String] and
    (__ \ "slug").write[String] and
    (__ \ "description").write[String] and
    (__ \ "longDescription").write[String] and
    (__ \ "availability").write[String] and
    (__ \ "enabled").write[Boolean] and
    (__ \ "projectType").write[String] and
    (__ \ "parts").write[IndexedSeq[Part]] and
    (__ \ "tags").write[IndexedSeq[Tag]] and
    (__ \ "status").write[Option[String]] and
    (__ \ "lastTaskId").write[Option[UUID]] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(Project.unapply))
}
