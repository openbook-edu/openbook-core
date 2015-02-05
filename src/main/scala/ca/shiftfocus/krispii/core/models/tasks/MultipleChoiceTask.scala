package ca.shiftfocus.krispii.core.models.tasks

import ca.shiftfocus.uuid.UUID
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
 * @param id The task's [[UUID]].
 * @param partId The [[Part]] to which this task belongs.
 * @param position The order in the part in which this task falls.
 * @param version The version of the task entity, for offline locking. Default = 0.
 * @param settings An object containing common settings for tasks.
 * @param choices A vector of possible choices a student can select from.
 * @param answer A vector of indeces indicating which choices are "correct".
 * @param allowMultiple Whether the student is allowed to select more than one answer.
 * @param randomizeChoices Whether the choices should be presented randomly, or in the
 *                            order in which they are defined.
 * @param createdAt When the entity was created. Default = None.
 * @param updatedAt When the entity was last updated. Default = None.
 */
case class MultipleChoiceTask(
  // Primary Key
  id: UUID = UUID.random,
  // Combination must be unique
  partId: UUID,
  position: Int,
  // Additional data
  version: Long = 0,
  settings: CommonTaskSettings = CommonTaskSettings(),
  choices: IndexedSeq[String] = IndexedSeq(),
  answer: IndexedSeq[Int] = IndexedSeq(),
  allowMultiple: Boolean = false,
  randomizeChoices: Boolean = true,
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
) extends Task {

  /**
   * Which type of task this is. Hard-coded value per class!
   */
  override val taskType: Int = Task.MultipleChoice

  override def equals(other: Any): Boolean = {
    other match {
      case otherMultipleChoiceTask: MultipleChoiceTask => {
        this.id == otherMultipleChoiceTask.id
      }
      case _ => false
    }
  }
}

object MultipleChoiceTask {

  /**
   * Create a MultipleChoiceTask from a row returned by the database.
   *
   * @param row a [[RowData]] object returned from the db.
   * @return a [[MultipleChoiceTask]] object
   */
  def apply(row: RowData): MultipleChoiceTask = {
    MultipleChoiceTask(
      // Primary Key
      id = UUID(row("id").asInstanceOf[Array[Byte]]),

      // Unique combination
      partId = UUID(row("part_id").asInstanceOf[Array[Byte]]),
      position = row("position").asInstanceOf[Int],

      // Additional data
      version = row("version").asInstanceOf[Long],
      settings = CommonTaskSettings(row),

      // Specific to this type
      choices = Option(row("choices").asInstanceOf[IndexedSeq[String]]).getOrElse(IndexedSeq.empty[String]),
      answer  = Option(row("answers").asInstanceOf[IndexedSeq[Int]]).getOrElse(IndexedSeq.empty[Int]),
      allowMultiple = row("allow_multiple").asInstanceOf[Boolean],
      randomizeChoices = row("randomize").asInstanceOf[Boolean],

      // All entities have these
      createdAt = Some(row("created_at").asInstanceOf[DateTime]),
      updatedAt = Some(row("updated_at").asInstanceOf[DateTime])
    )
  }

  /**
   * Unserialize a [[LongAnswerTask]] from JSON.
   */
  implicit val jsonReads = new Reads[MultipleChoiceTask] {
    def reads(js: JsValue) = {
      JsSuccess(MultipleChoiceTask(
        id       = (js \ "id").as[UUID],
        partId   = (js \ "partId").as[UUID],
        position = (js \ "position").as[Int],
        version  = (js \ "version").as[Long],
        settings = (js \ "settings").as[CommonTaskSettings],
        choices  = (js \ "elements").as[IndexedSeq[String]],
        answer   = (js \ "answer").as[IndexedSeq[Int]],
        allowMultiple = (js \ "allowMultiple").as[Boolean],
        randomizeChoices = (js \ "randomizeChoices").as[Boolean],
        createdAt = (js \ "createdAt").as[Option[DateTime]],
        updatedAt = (js \ "updatedAt").as[Option[DateTime]]
      ))
    }
  }

  /**
   * Unserialize a [[MultipleChoiceTask]] from JSON.
   */
//  implicit val taskReads: Reads[MultipleChoiceTask] = (
//    (__ \ "id").read[UUID] and
//      (__ \ "partId").read[UUID] and
//      (__ \ "position").read[Int] and
//      (__ \ "version").read[Long] and
//      (__ \ "settings").read[CommonTaskSettings] and
//      (__ \ "choices").read[IndexedSeq[String]] and
//      (__ \ "answer").read[IndexedSeq[Int]] and
//      (__ \ "allowMultiple").read[Boolean] and
//      (__ \ "randomizeChoices").read[Boolean] and
//      (__ \ "createdAt").readNullable[DateTime] and
//      (__ \ "updatedAt").readNullable[DateTime]
//    )(MultipleChoiceTask.apply(_: UUID, _: UUID, _: Int, _: Long, _: CommonTaskSettings, _: IndexedSeq[String], _: IndexedSeq[Int], _: Boolean, _: Boolean, _: Option[DateTime], _: Option[DateTime]))

  /**
   * Serialize a [[MultipleChoiceTask]] to JSON.
   */
  implicit val taskWrites: Writes[MultipleChoiceTask] = (
    (__ \ "id").write[UUID] and
      (__ \ "partId").write[UUID] and
      (__ \ "position").write[Int] and
      (__ \ "version").write[Long] and
      (__ \ "settings").write[CommonTaskSettings] and
      (__ \ "choices").write[IndexedSeq[String]] and
      (__ \ "answer").write[IndexedSeq[Int]] and
      (__ \ "allowMultiple").write[Boolean] and
      (__ \ "randomizeChoices").write[Boolean] and
      (__ \ "createdAt").writeNullable[DateTime] and
      (__ \ "updatedAt").writeNullable[DateTime]
    )(unlift(MultipleChoiceTask.unapply))
  
}