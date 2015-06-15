package ca.shiftfocus.krispii.core.models.tasks

import java.util.UUID
import ca.shiftfocus.krispii.core.models.Part
import com.github.mauricio.async.db.RowData
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._

/**
 * A multiple-choice task is one in which the student must select one
 * or more from a list of possible answers.
 *
 * @param id The task's UUID.
 * @param partId The part to which this task belongs.
 * @param position The order in the part in which this task falls.
 * @param version The version of the task entity, for offline locking. Default = 0.
 * @param settings An object containing common settings for tasks.
 * @param choices A vector of possible choices a student can select from.
 * @param answers A vector of indeces indicating which choices are "correct".
 * @param allowMultiple Whether the student is allowed to select more than one answer.
 * @param randomizeChoices Whether the choices should be presented randomly, or in the
 *                            order in which they are defined.
 * @param createdAt When the entity was created. Default = None.
 * @param updatedAt When the entity was last updated. Default = None.
 */
case class MultipleChoiceTask(
    // Primary Key
    id: UUID = UUID.randomUUID,
    // Combination must be unique
    partId: UUID,
    position: Int,
    // Additional data
    version: Long = 1L,
    settings: CommonTaskSettings = CommonTaskSettings(),
    choices: IndexedSeq[String] = IndexedSeq(),
    answers: IndexedSeq[Int] = IndexedSeq(),
    allowMultiple: Boolean = false,
    randomizeChoices: Boolean = true,
    createdAt: DateTime = new DateTime,
    updatedAt: DateTime = new DateTime
) extends Task {

  /**
   * Which type of task this is. Hard-coded value per class!
   */
  override val taskType: Int = Task.MultipleChoice

  override def equals(other: Any): Boolean = {
    other match {
      case otherMultipleChoiceTask: MultipleChoiceTask => {
        this.id == otherMultipleChoiceTask.id &&
          this.partId == otherMultipleChoiceTask.partId &&
          this.position == otherMultipleChoiceTask.position &&
          this.version == otherMultipleChoiceTask.version &&
          this.settings.toString == otherMultipleChoiceTask.settings.toString &&
          this.choices.toString == otherMultipleChoiceTask.choices.toString &&
          this.answers.toString == otherMultipleChoiceTask.answers.toString &&
          this.allowMultiple == otherMultipleChoiceTask.allowMultiple &&
          this.randomizeChoices == otherMultipleChoiceTask.randomizeChoices
      }
      case _ => false
    }
  }
  override def hashCode: Int = 41 * this.id.hashCode
}

object MultipleChoiceTask {

  /**
   * Serialize a MultipleChoiceTask to JSON.
   */
  implicit val taskWrites: Writes[MultipleChoiceTask] = (
    (__ \ "id").write[UUID] and
    (__ \ "partId").write[UUID] and
    (__ \ "position").write[Int] and
    (__ \ "version").write[Long] and
    (__ \ "settings").write[CommonTaskSettings] and
    (__ \ "choices").write[IndexedSeq[String]] and
    (__ \ "answers").write[IndexedSeq[Int]] and
    (__ \ "allowMultiple").write[Boolean] and
    (__ \ "randomizeChoices").write[Boolean] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(MultipleChoiceTask.unapply))

}
