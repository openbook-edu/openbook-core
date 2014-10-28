package com.shiftfocus.krispii.core.models.tasks

/**
 * The supertype for tasks.
 */
case class LongAnswerTask(
  // Primary Key
  id: UUID,
  partId: UUID,
  position: Int,
  // Additional data
  version: Long = 0,
  settings: TaskSettings = TaskSettings(),
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
) extends Task

object LongAnswerTask {
  def apply(row: RowData): LongAnswerTask = {
    LongAnswerTask(
      // Primary Key
      partId = UUID(row("part_id").asInstanceOf[Array[Byte]]),
      position = row("position").asInstanceOf[Int],

      // Additional data
      row("version").asInstanceOf[Long],
      Option(row("dependency_id").asInstanceOf[Array[Byte]]) match {
        case Some(bytes) => Some(UUID(bytes))
        case _ => None
      },
      row("name").asInstanceOf[String],
      row("description").asInstanceOf[String],
      row("notes_allowed").asInstanceOf[Boolean],
      Some(row("created_at").asInstanceOf[DateTime]),
      Some(row("updated_at").asInstanceOf[DateTime])
    )
  }

  implicit val taskReads: Reads[LongAnswerTask] = (
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
  )(LongAnswerTask.apply(_: UUID, _: Long, _: UUID, _: Option[UUID], _: String, _: String, _: Int, _: Boolean, _: Option[DateTime], _: Option[DateTime]))

  implicit val taskWrites: Writes[LongAnswerTask] = (
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
  )(unlift(LongAnswerTask.unapply))
}
