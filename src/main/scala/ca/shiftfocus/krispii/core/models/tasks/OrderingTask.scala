package ca.shiftfocus.krispii.core.models.tasks

import java.util.UUID
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
 * @param answers
 * @param randomizeChoices
 * @param createdAt
 * @param updatedAt
 */
case class OrderingTask(
  // Primary Key
  id: UUID = UUID.randomUUID,
  // Combination must be unique
  partId: UUID,
  position: Int,
  // Additional data
  version: Long = 1L,
  settings: CommonTaskSettings = CommonTaskSettings(),
  elements: IndexedSeq[String] = IndexedSeq(),
  answers: IndexedSeq[Int] = IndexedSeq(),
  randomizeChoices: Boolean = true,
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime
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
  override def hashCode: Int = 41 * this.id.hashCode
}

object OrderingTask {

  /**
   * Serialize a OrderingTask to JSON.
   */
  implicit val taskWrites: Writes[OrderingTask] = (
    (__ \ "id").write[UUID] and
      (__ \ "partId").write[UUID] and
      (__ \ "position").write[Int] and
      (__ \ "version").write[Long] and
      (__ \ "settings").write[CommonTaskSettings] and
      (__ \ "elements").write[IndexedSeq[String]] and
      (__ \ "answers").write[IndexedSeq[Int]] and
      (__ \ "randomizeChoices").write[Boolean] and
      (__ \ "createdAt").write[DateTime] and
      (__ \ "updatedAt").write[DateTime]
    )(unlift(OrderingTask.unapply))

}
