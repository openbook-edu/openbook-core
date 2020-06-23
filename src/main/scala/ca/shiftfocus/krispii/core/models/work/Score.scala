package ca.shiftfocus.krispii.core.models.work

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.libs.json.JodaWrites._

case class Score(
    id: UUID = UUID.randomUUID,
    testId: UUID,
    scorerId: UUID,
    version: Long = 1L,
    grade: String = "", // initially empty, in contrast with Work
    isVisible: Boolean = false,
    /* will only be saved separate from the original versions in Test resp. Exam
       if the scorer has actually made changes to the PDFs etc. */
    examFile: Option[UUID] = None, // will usually be an annotated PDF component
    rubricFile: Option[UUID] = None, // could be a rubric, image or PDF component
    origComments: String = "",
    addComments: String = "",
    createdAt: DateTime = new DateTime(),
    updatedAt: DateTime = new DateTime()
) extends Evaluation {
  override def responseToString: String = {
    grade match {
      case "" => "None"
      case grade => grade
    }
  }
}

object Score {
  implicit val scoreWrites = new Writes[Score] {
    override def writes(score: Score): JsValue = {
      Json.obj(
        "id" -> score.id,
        "testId" -> score.testId,
        "scorerId" -> score.scorerId,
        "version" -> score.version,
        "grade" -> score.grade,
        "is_visible" -> score.isVisible,
        "exam_file" -> score.examFile,
        "rubric_file" -> score.rubricFile,
        "orig_comments" -> score.origComments,
        "add_comments" -> score.addComments,
        "createdAt" -> score.createdAt,
        "updatedAt" -> score.updatedAt
      )
    }
  }
}
