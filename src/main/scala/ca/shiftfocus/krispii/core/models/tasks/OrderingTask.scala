package ca.shiftfocus.krispii.core.models.tasks

import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.RowData
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * An ordering task is one for which the student must place a given
 * set of elements in the correct sequence, or order.
 *
 * @param id
 * @param partId
 * @param position
 * @param version
 * @param settings
 * @param elements
 * @param answer
 * @param allowMultiple
 * @param randomizeChoices
 * @param createdAt
 * @param updatedAt
 */
case class OrderingTask(
  // Primary Key
  id: UUID = UUID.random,
  // Combination must be unique
  partId: UUID,
  position: Int,
  // Additional data
  version: Long = 0,
  settings: CommonTaskSettings = CommonTaskSettings(),
  elements: IndexedSeq[String] = IndexedSeq(),
  answer: IndexedSeq[Int] = IndexedSeq(),
  randomizeChoices: Boolean = true,
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
) extends Task {

  /**
   * Which type of task this is. Hard-coded value per class!
   */
  override val taskType: Int = Task.Ordering

  override def equals(other: Any): Boolean = {
    other match {
      case otherOrderingTask: OrderingTask => {
        this.id == otherOrderingTask.id
      }
      case _ => false
    }
  }
}

object OrderingTask {
  /**
   * Unserialize a [[LongAnswerTask]] from JSON.
   */
  implicit val jsonReads = new Reads[OrderingTask] {
    def reads(js: JsValue) = {
      JsSuccess(OrderingTask(
        id       = (js \ "id").as[UUID],
        partId   = (js \ "partId").as[UUID],
        position = (js \ "position").as[Int],
        version  = (js \ "version").as[Long], // CommonTaskSettings
        settings = (js \ "settings").as[CommonTaskSettings],
        elements = (js \ "elements").as[IndexedSeq[String]],
        answer   = (js \ "answers").as[IndexedSeq[Int]],
        randomizeChoices = (js \ "randomizeChoices").as[Boolean],
        createdAt = (js \ "createdAt").as[Option[DateTime]],
        updatedAt = (js \ "updatedAt").as[Option[DateTime]]
      ))
    }
  }

  /**
   * Serialize a [[OrderingTask]] to JSON.
   */
  implicit val taskWrites: Writes[OrderingTask] = (
    (__ \ "id").write[UUID] and
      (__ \ "partId").write[UUID] and
      (__ \ "position").write[Int] and
      (__ \ "version").write[Long] and
      (__ \ "settings").write[CommonTaskSettings] and
      (__ \ "elements").write[IndexedSeq[String]] and
      (__ \ "answer").write[IndexedSeq[Int]] and
      (__ \ "randomizeChoices").write[Boolean] and
      (__ \ "createdAt").writeNullable[DateTime] and
      (__ \ "updatedAt").writeNullable[DateTime]
    )(unlift(OrderingTask.unapply))

}