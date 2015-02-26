package ca.shiftfocus.krispii.core.models.tasks

import ca.shiftfocus.uuid.UUID
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
  id: UUID = UUID.random,
  // Combination must be unique
  partId: UUID,
  position: Int,
  // Additional data
  version: Long = 0,
  settings: CommonTaskSettings = CommonTaskSettings(),
  maxLength: Int = 50,
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime
) extends Task {

  /**
   * Which type of task this is. Hard-coded value per class!
   */
  override val taskType: Int = Task.ShortAnswer

  override def equals(other: Any): Boolean = {
    other match {
      case otherShortAnswerTask: ShortAnswerTask => {
        this.id == otherShortAnswerTask.id
      }
      case _ => false
    }
  }
}

object ShortAnswerTask {

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
      (__ \ "createdAt").write[DateTime] and
      (__ \ "updatedAt").write[DateTime]
    )(unlift(ShortAnswerTask.unapply))

}