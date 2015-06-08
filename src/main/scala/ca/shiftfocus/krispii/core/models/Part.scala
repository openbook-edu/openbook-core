package ca.shiftfocus.krispii.core.models

import java.util.UUID
import ca.shiftfocus.krispii.core.models.tasks.Task
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._

case class Part(
    id: UUID = UUID.randomUUID,
    version: Long = 1L,
    projectId: UUID,
    name: String,
    position: Int = 0,
    enabled: Boolean = true,
    tasks: IndexedSeq[Task] = IndexedSeq(),
    createdAt: DateTime = new DateTime,
    updatedAt: DateTime = new DateTime
) {
  override def toString: String = {
    s"Part(id: '${id.toString}', position: '${position.toString}', name: '$name')"
  }
  override def equals(other: Any): Boolean = {
    other match {
      case otherPart: Part =>
        this.id == otherPart.id &&
          this.version == otherPart.version &&
          this.projectId == otherPart.projectId &&
          this.name == otherPart.name &&
          this.position == otherPart.position &&
          this.enabled == otherPart.enabled &&
          this.tasks == otherPart.tasks
      case _ => false
    }
  }
}

object Part {

  implicit val partWrites: Writes[Part] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "projectId").write[UUID] and
    (__ \ "name").write[String] and
    (__ \ "position").write[Int] and
    (__ \ "enabled").write[Boolean] and
    (__ \ "tasks").write[IndexedSeq[Task]] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(Part.unapply))

  val Locked = 0
  val Unlocked = 1

}

case class PartPost(
  projectId: UUID,
  name: String,
  position: Int,
  enabled: Boolean
)
object PartPost {
  implicit val projectPostReads = (
    (__ \ "projectId").read[UUID] and
    (__ \ "name").read[String] and
    (__ \ "position").read[Int] and
    (__ \ "enabled").read[Boolean]
  )(PartPost.apply _)
}

case class PartPut(
  version: Long,
  projectId: UUID,
  name: String,
  position: Int,
  enabled: Boolean
)
object PartPut {
  implicit val projectPutReads = (
    (__ \ "version").read[Long] and
    (__ \ "projectId").read[UUID] and
    (__ \ "name").read[String] and
    (__ \ "position").read[Int] and
    (__ \ "enabled").read[Boolean]
  )(PartPut.apply _)
}
