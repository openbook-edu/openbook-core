package ca.shiftfocus.krispii.core.models.work

import ca.shiftfocus.uuid.UUID
import ca.shiftfocus.krispii.core.models.tasks.Task._
import ca.shiftfocus.krispii.core.models.tasks.MatchingTask.Match
import com.github.mauricio.async.db.RowData
import play.api.libs.functional.syntax._
import play.api.libs.json._
import org.joda.time.DateTime

/**
 * The "work" trait is the supertype for work that students have done.
 */
trait Work {
  val id: UUID
  val studentId: UUID
  val taskId: UUID
  val version: Long = 1L
  val answer: AnyRef
  val isComplete: Boolean
  val createdAt: DateTime
  val updatedAt: DateTime
}

object Work {

  implicit val jsonWrites = new Writes[Work] {
    def writes(work: Work): JsValue = {
      val jsVal = Json.obj(
        "studentId" -> work.studentId,
        "taskId" -> work.taskId,
        "version" -> work.version,
        "answer" -> {
          work match {
            case specific: LongAnswerWork => specific.answer
            case specific: ShortAnswerWork => specific.answer
            case specific: MultipleChoiceWork => specific.answer
            case specific: OrderingWork => specific.answer
            case specific: MatchingWork => specific.answer
            case _ => throw new Exception("Tried to serialize a work type that, somehow, doesn't exist.")
          }
        },
        "isComplete" -> work.isComplete,
        "createdAt" -> work.createdAt,
        "updatedAt" -> work.updatedAt
      )
      work match {
        case laWork: LongAnswerWork => jsVal + ("documentId" -> Json.toJson(laWork.documentId))
        case saWork: ShortAnswerWork => jsVal + ("documentId" -> Json.toJson(saWork.documentId))
        case _ => jsVal
      }
    }
  }

}
