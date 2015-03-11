package ca.shiftfocus.krispii.core.models.tasks

import ca.shiftfocus.uuid.UUID
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
  id: UUID = UUID.random,
  // Combination must be unique
  partId: UUID,
  position: Int,
  // Additional data
  version: Long = 0,
  settings: CommonTaskSettings = CommonTaskSettings(),
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime
) extends Task {

  /**
   * Which type of task this is. Hard-coded value per class!
   */
  override val taskType: Int = Task.LongAnswer

  override def equals(other: Any): Boolean = {
    other match {
      case otherLongAnswerTask: LongAnswerTask => {
        this.id == otherLongAnswerTask.id
      }
      case _ => false
    }
  }
}

object LongAnswerTask {
  /**
   * Serialize a [[LongAnswerTask]] to JSON.
   */
  implicit val taskWrites: Writes[LongAnswerTask] = (
    (__ \ "id").write[UUID] and
      (__ \ "partId").write[UUID] and
      (__ \ "position").write[Int] and
      (__ \ "version").write[Long] and
      (__ \ "settings").write[CommonTaskSettings] and
      (__ \ "createdAt").write[DateTime] and
      (__ \ "updatedAt").write[DateTime]
  )(unlift(LongAnswerTask.unapply))
}
