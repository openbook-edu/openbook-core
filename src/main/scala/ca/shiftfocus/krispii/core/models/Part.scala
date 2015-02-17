package ca.shiftfocus.krispii.core.models

import com.github.mauricio.async.db.RowData
import ca.shiftfocus.uuid.UUID
import ca.shiftfocus.krispii.core.models.tasks.Task
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._


case class Part(
  id: UUID = UUID.random,
  version: Long = 0,
  projectId: UUID,
  name: String,
//TODO remove descritpion everywhere
//  description: String = "",
  position: Int = 0,
  enabled: Boolean = true,
  tasks: IndexedSeq[Task] = IndexedSeq(),
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
) {
  override def toString() = {
    s"Part(id: '${id.string}', position: '${position.toString()}', name: '${name}')"
  }
  override def equals(other: Any): Boolean = {
    other match {
      case otherPart: Part => {
        this.id == otherPart.id &&
        this.tasks == otherPart.tasks
      }
      case _ => false
    }
  }
}

object Part {

  def apply(row: RowData): Part = {
    Part(
      id          = UUID(row("id").asInstanceOf[Array[Byte]]),
      version     = row("version").asInstanceOf[Long],
      projectId   = UUID(row("project_id").asInstanceOf[Array[Byte]]),
      name        = row("name").asInstanceOf[String],
//      description = row("description").asInstanceOf[String],
      position    = row("position").asInstanceOf[Int],
      enabled     = row("enabled").asInstanceOf[Boolean],
      createdAt   = Some(row("created_at").asInstanceOf[DateTime]),
      updatedAt   = Some(row("updated_at").asInstanceOf[DateTime])
    )
  }

  implicit val partReads: Reads[Part] = (
    (__ \ "id").read[UUID] and
    (__ \ "version").read[Long] and
    (__ \ "projectId").read[UUID] and
    (__ \ "name").read[String] and
//    (__ \ "description").read[String] and
    (__ \ "position").read[Int] and
    (__ \ "enabled").read[Boolean] and
    (__ \ "tasks").read[IndexedSeq[Task]] and
    (__ \ "createdAt").readNullable[DateTime] and
    (__ \ "updatedAt").readNullable[DateTime]
  )(Part.apply(_: UUID, _: Long, _: UUID, _: String, _: Int, _: Boolean, _: IndexedSeq[Task], _: Option[DateTime], _: Option[DateTime]))
//  )(Part.apply(_: UUID, _: Long, _: UUID, _: String, _: String, _: Int, _: Boolean, _: IndexedSeq[Task], _: Option[DateTime], _: Option[DateTime]))

  implicit val partWrites: Writes[Part] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "projectId").write[UUID] and
    (__ \ "name").write[String] and
//    (__ \ "description").write[String] and
    (__ \ "position").write[Int] and
    (__ \ "enabled").write[Boolean] and
    (__ \ "tasks").write[IndexedSeq[Task]] and
    (__ \ "createdAt").writeNullable[DateTime] and
    (__ \ "updatedAt").writeNullable[DateTime]
  )(unlift(Part.unapply))

  val Locked = 0
  val Unlocked = 1

}


case class PartPost(
  projectId: UUID,
  name: String,
//  description: String,
  position: Int,
  enabled: Boolean
)
object PartPost {
  implicit val projectPostReads = (
    (__ \ "projectId").read[UUID] and
    (__ \ "name").read[String] and
//    (__ \ "description").read[String] and
    (__ \ "position").read[Int] and
    (__ \ "enabled").read[Boolean]
  )(PartPost.apply _)
}

case class PartPut(
  version: Long,
  projectId: UUID,
  name: String,
//  description: String,
  position: Int,
  enabled: Boolean
)
object PartPut {
  implicit val projectPutReads = (
    (__ \ "version").read[Long] and
    (__ \ "projectId").read[UUID] and
    (__ \ "name").read[String] and
//    (__ \ "description").read[String] and
    (__ \ "position").read[Int] and
    (__ \ "enabled").read[Boolean]
  )(PartPut.apply _)
}
