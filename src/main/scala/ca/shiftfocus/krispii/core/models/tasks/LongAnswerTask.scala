package ca.shiftfocus.krispii.core.models.tasks

import ca.shiftfocus.krispii.core.lib.UUID
import ca.shiftfocus.krispii.core.models.Part
import com.github.mauricio.async.db.RowData
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._

/**
 * A long answer task is one in which the student is expected to write more than
 * a few words in response. This task type gives them free reign to enter as much
 * text as they desire.
 *
 * @param id The task's [[UUID]].
 * @param partId The [[Part]] to which this task belongs.
 * @param position The order in the part in which this task falls.
 * @param version The version of the task entity, for offline locking.
 * @param settings An object containing common settings for tasks.
 * @param createdAt When the entity was created.
 * @param updatedAt When the entity was last updated.
 */
case class LongAnswerTask(
  // Primary Key
  id: UUID,
  // Combination must be unique
  partId: UUID,
  position: Int,
  // Additional data
  version: Long = 0,
  settings: CommonTaskSettings = CommonTaskSettings(),
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
) extends Task

object LongAnswerTask {

  /**
   * Create a LongAnswerTask from a row returned by the database.
   *
   * @param row a [[RowData]] object returned from the db.
   * @return a [[LongAnswerTask]] object
   */
  def apply(row: RowData): LongAnswerTask = {
    LongAnswerTask(
      // Primary Key
      id = UUID(row("id").asInstanceOf[Array[Byte]]),
      partId = UUID(row("part_id").asInstanceOf[Array[Byte]]),
      position = row("position").asInstanceOf[Int],

      // Additional data
      version = row("version").asInstanceOf[Long],
      settings = CommonTaskSettings(row),
      createdAt = Some(row("created_at").asInstanceOf[DateTime]),
      updatedAt = Some(row("updated_at").asInstanceOf[DateTime])
    )
  }

  /**
   * Unserialize a [[LongAnswerTask]] from JSON.
   */
  implicit val taskReads: Reads[LongAnswerTask] = (
    (__ \ "id").read[UUID] and
      (__ \ "partId").read[UUID] and
      (__ \ "position").read[Int] and
      (__ \ "version").read[Long] and
      (__ \ "settings").read[CommonTaskSettings] and
      (__ \ "createdAt").readNullable[DateTime] and
      (__ \ "updatedAt").readNullable[DateTime]
  )(LongAnswerTask.apply(_: UUID, _: UUID, _: Int, _: Long, _: CommonTaskSettings, _: Option[DateTime], _: Option[DateTime]))

  /**
   * Serialize a [[LongAnswerTask]] to JSON.
   */
  implicit val taskWrites: Writes[LongAnswerTask] = (
    (__ \ "id").write[UUID] and
      (__ \ "partId").write[UUID] and
      (__ \ "position").write[Int] and
      (__ \ "version").write[Long] and
      (__ \ "settings").write[CommonTaskSettings] and
      (__ \ "createdAt").writeNullable[DateTime] and
      (__ \ "updatedAt").writeNullable[DateTime]
  )(unlift(LongAnswerTask.unapply))
}
