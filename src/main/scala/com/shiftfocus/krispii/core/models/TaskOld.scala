package com.shiftfocus.krispii.core.models

import com.github.mauricio.async.db.RowData
import com.shiftfocus.krispii.core.lib.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._

case class TaskGroup(
  part: Part,
  status: Int,
  tasks: IndexedSeq[TaskGroupItem] = IndexedSeq()
)
case class TaskGroupItem(
  status: Int,
  task: Task,
  responseOption: Option[TaskResponse] = None
)

case class Task(
  id: UUID = UUID.random,
  version: Long = 0,
  partId: UUID,
  dependencyId: Option[UUID] = None,
  name: String,
  description: String = "",
  position: Int = 0,
  notesAllowed: Boolean = true,
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
) {
  override def toString() = {
    s"Task(id: ${id.string}, version: ${version}, position: ${position.toString()}, name: '${name}')"
  }
}

object Task {

  def apply(row: RowData): Task = {
    Task(
      UUID(row("id").asInstanceOf[Array[Byte]]),
      row("version").asInstanceOf[Long],
      UUID(row("part_id").asInstanceOf[Array[Byte]]),
      Option(row("dependency_id").asInstanceOf[Array[Byte]]) match {
        case Some(bytes) => Some(UUID(bytes))
        case _ => None
      },
      row("name").asInstanceOf[String],
      row("description").asInstanceOf[String],
      row("position").asInstanceOf[Int],
      row("notes_allowed").asInstanceOf[Boolean],
      Some(row("created_at").asInstanceOf[DateTime]),
      Some(row("updated_at").asInstanceOf[DateTime])
    )
  }

  implicit val taskReads: Reads[Task] = (
    (__ \ "id").read[UUID] and
    (__ \ "version").read[Long] and
    (__ \ "partId").read[UUID] and
    (__ \ "dependencyId").readNullable[UUID] and
    (__ \ "name").read[String] and
    (__ \ "description").read[String] and
    (__ \ "position").read[Int] and
    (__ \ "notesAllowed").read[Boolean] and
    (__ \ "createdAt").readNullable[DateTime] and
    (__ \ "updatedAt").readNullable[DateTime]
  )(Task.apply(_: UUID, _: Long, _: UUID, _: Option[UUID], _: String, _: String, _: Int, _: Boolean, _: Option[DateTime], _: Option[DateTime]))

  implicit val taskWrites: Writes[Task] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "partId").write[UUID] and
    (__ \ "dependencyId").writeNullable[UUID] and
    (__ \ "name").write[String] and
    (__ \ "description").write[String] and
    (__ \ "position").write[Int] and
    (__ \ "notesAllowed").write[Boolean] and
    (__ \ "createdAt").writeNullable[DateTime] and
    (__ \ "updatedAt").writeNullable[DateTime]
  )(unlift(Task.unapply))

  val NotStarted = 0
  val Incomplete = 1
  val Complete = 2

  /**
   * Find the "now" for a user on a specific project.
   * @type {[type]}
   */
  def findNow(project: Project, enabledParts: IndexedSeq[Part], tasks: IndexedSeq[Task], responses: IndexedSeq[TaskResponse]): Option[Task] = {
    val enabledPartsIds = enabledParts.map(_.id)
    val enabledTasks = tasks.filter({task => enabledPartsIds.contains(task.partId)})
    val candidates = enabledTasks.filter { task =>
      if (responses.filter({response => (response.taskId == task.id && response.isComplete == true)}).isEmpty) true
      else false
    }
    candidates.headOption
  }

}

case class TaskPost(
  partId: UUID,
  dependencyId: Option[UUID],
  name: String,
  description: String,
  position: Int,
  notesAllowed: Boolean
)
object TaskPost {
  implicit val coursePutReads = (
    (__ \ "partId").read[UUID] and
      (__ \ "dependencyId").readNullable[UUID] and
      (__ \ "name").read[String] and
      (__ \ "description").read[String] and
      (__ \ "position").read[Int] and
      (__ \ "notesAllowed").read[Boolean]
  )(TaskPost.apply _)
}

case class TaskPut(
  version: Long,
  partId: UUID,
  dependencyId: Option[UUID],
  name: String,
  description: String,
  position: Int,
  notesAllowed: Boolean
)
object TaskPut {
  implicit val coursePutReads = (
    (__ \ "version").read[Long] and
      (__ \ "partId").read[UUID] and
      (__ \ "dependencyId").readNullable[UUID] and
      (__ \ "name").read[String] and
      (__ \ "description").read[String] and
      (__ \ "position").read[Int] and
      (__ \ "notesAllowed").read[Boolean]
  )(TaskPut.apply _)
}
