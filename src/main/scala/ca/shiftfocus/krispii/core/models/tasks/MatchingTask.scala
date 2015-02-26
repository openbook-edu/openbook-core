package ca.shiftfocus.krispii.core.models.tasks

import ca.shiftfocus.uuid.UUID
import ca.shiftfocus.krispii.core.models.Part
import com.github.mauricio.async.db.RowData
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * A matching task is one in which the student is presented with two
 * lists of elements and is asked to match elements from one list with
 * their corresponding element in the other.
 *
 * @param id The task's [[UUID]].
 * @param partId The [[Part]] to which this task belongs.
 * @param position The order in the part in which this task falls.
 * @param version The version of the task entity, for offline locking. Default = 0.
 * @param settings An object containing common settings for tasks.
 * @param elementsLeft The left list of elements.
 * @param elementsRight The right list of elements.
 * @param answer The answers as a vector of Int -> Int tuples. Note that not every element
 *                needs to be accounted for in the answers.
 * @param randomizeChoices Whether the choices should be presented randomly, or in the
 *                            order in which they are defined.
 * @param createdAt When the entity was created. Default = None.
 * @param updatedAt When the entity was last updated. Default = None.
 */
case class MatchingTask(
  // Primary Key
  id: UUID = UUID.random,
  // Combination must be unique
  partId: UUID,
  position: Int,
  // Additional data
  version: Long = 0,
  settings: CommonTaskSettings = CommonTaskSettings(),
  elementsLeft: IndexedSeq[String] = IndexedSeq(),
  elementsRight: IndexedSeq[String] = IndexedSeq(),
  answer: IndexedSeq[MatchingTask.Match] = IndexedSeq(),
  randomizeChoices: Boolean = true,
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
) extends Task {

  /**
   * Which type of task this is. Hard-coded value per class!
   */
  override val taskType: Int = Task.Matching

  override def equals(other: Any): Boolean = {
    other match {
      case otherMatchingTask: MatchingTask => {
        this.id == otherMatchingTask.id
      }
      case _ => false
    }
  }
}

object MatchingTask {

  case class Match(left: Int, right: Int)
  object Match {
    implicit val jsonReads: Reads[Match] = (
      (__ \ "left").read[Int] and
        (__ \ "right").read[Int]
    )(Match.apply _)

    implicit val jsonWrites: Writes[Match] = (
      (__ \ "left").write[Int] and
        (__ \ "right").write[Int]
      )(unlift(Match.unapply))
  }

  /**
   * Unserialize a [[LongAnswerTask]] from JSON.
   */
  implicit val jsonReads = new Reads[MatchingTask] {
    def reads(js: JsValue) = {
      JsSuccess(MatchingTask(
        id       = (js \ "id").as[UUID],
        partId   = (js \ "partId").as[UUID],
        position = (js \ "position").as[Int],
        version  = (js \ "version").as[Long],
        settings = (js \ "settings").as[CommonTaskSettings],
        elementsLeft = (js \ "elementsLeft").as[IndexedSeq[String]],
        elementsRight = (js \ "elementsRight").as[IndexedSeq[String]],
        answer   = (js \ "answer").as[IndexedSeq[Match]],
        randomizeChoices = (js \ "randomizeChoices").as[Boolean],
        createdAt = (js \ "createdAt").as[Option[DateTime]],
        updatedAt = (js \ "updatedAt").as[Option[DateTime]]
      ))
    }
  }

  /**
   * Serialize a [[MatchingTask]] to JSON.
   */
  implicit val jsonWrites: Writes[MatchingTask] = (
    (__ \ "id").write[UUID] and
      (__ \ "partId").write[UUID] and
      (__ \ "position").write[Int] and
      (__ \ "version").write[Long] and
      (__ \ "settings").write[CommonTaskSettings] and
      (__ \ "elements_left").write[IndexedSeq[String]] and
      (__ \ "elements_right").write[IndexedSeq[String]] and
      (__ \ "answer").write[IndexedSeq[Match]] and
      (__ \ "randomizeChoices").write[Boolean] and
      (__ \ "createdAt").writeNullable[DateTime] and
      (__ \ "updatedAt").writeNullable[DateTime]
    )(unlift(MatchingTask.unapply))
  
}