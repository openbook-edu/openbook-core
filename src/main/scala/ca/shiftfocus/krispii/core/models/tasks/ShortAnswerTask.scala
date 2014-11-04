package ca.shiftfocus.krispii.core.models.tasks

import ca.shiftfocus.krispii.core.lib.UUID
import ca.shiftfocus.krispii.core.models.Part
import com.github.mauricio.async.db.RowData
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._

/**
 * A short answer task is one for which the student is expected to write only a
 * couple words.
 *
 * @param id The task's [[UUID]].
 * @param partId The [[Part]] to which this task belongs.
 * @param position The order in the part in which this task falls.
 * @param version The version of the task entity, for offline locking. Default = 0.
 * @param settings An object containing common settings for tasks.
 * @param maxLength The maximum length of answer accepted, as a number of characters. Default = 50.
 * @param createdAt When the entity was created. Default = None.
 * @param updatedAt When the entity was last updated. Default = None.
 */
case class ShortAnswerTask(
  // Primary Key
  id: UUID,
  // Combination must be unique
  partId: UUID,
  position: Int,
  // Additional data
  version: Long = 0,
  settings: CommonTaskSettings = CommonTaskSettings(),
  maxLength: Int = 50,
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
) extends Task {

  /**
   * Which type of task this is. Hard-coded value per class!
   */
  override val taskType: String = "shortAnswer"

}

object ShortAnswerTask {
  /**
   * Create a ShortAnswerTask from a row returned by the database.
   *
   * @param row a [[RowData]] object returned from the db.
   * @return a [[ShortAnswerTask]] object
   */
  def apply(row: RowData): ShortAnswerTask = {
    ShortAnswerTask(
      // Primary Key
      id = UUID(row("id").asInstanceOf[Array[Byte]]),
      partId = UUID(row("part_id").asInstanceOf[Array[Byte]]),
      position = row("position").asInstanceOf[Int],

      // Additional data
      version = row("version").asInstanceOf[Long],
      settings = CommonTaskSettings(row),
      maxLength = row("max_length").asInstanceOf[Int],
      createdAt = Some(row("created_at").asInstanceOf[DateTime]),
      updatedAt = Some(row("updated_at").asInstanceOf[DateTime])
    )
  }

  /**
   * Unserialize a [[ShortAnswerTask]] from JSON.
   */
  implicit val taskReads: Reads[ShortAnswerTask] = (
    (__ \ "id").read[UUID] and
      (__ \ "partId").read[UUID] and
      (__ \ "position").read[Int] and
      (__ \ "version").read[Long] and
      (__ \ "settings").read[CommonTaskSettings] and
      (__ \ "maxLength").read[Int] and
      (__ \ "createdAt").readNullable[DateTime] and
      (__ \ "updatedAt").readNullable[DateTime]
    )(ShortAnswerTask.apply(_: UUID, _: UUID, _: Int, _: Long, _: CommonTaskSettings, _: Int, _: Option[DateTime], _: Option[DateTime]))

  /**
   * Serialize a [[ShortAnswerTask]] to JSON.
   */
  implicit val taskWrites: Writes[ShortAnswerTask] = (
    (__ \ "id").write[UUID] and
      (__ \ "partId").write[UUID] and
      (__ \ "position").write[Int] and
      (__ \ "version").write[Long] and
      (__ \ "settings").write[CommonTaskSettings] and
      (__ \ "maxLength").write[Int] and
      (__ \ "createdAt").writeNullable[DateTime] and
      (__ \ "updatedAt").writeNullable[DateTime]
    )(unlift(ShortAnswerTask.unapply))
}