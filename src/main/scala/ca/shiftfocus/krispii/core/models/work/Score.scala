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
    grade: String, // initially empty, in contrast with Work
    isVisible: Boolean = false,
    examFile: MediaAnswer = MediaAnswer(),
    rubricFile: MediaAnswer = MediaAnswer(),
    origComments: String,
    addComments: String,
    createdAt: DateTime = new DateTime(),
    updatedAt: DateTime = new DateTime()
) extends Evaluation {
  override def responseToString: String = {
    scorerId.toString()
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
        "exam_file" -> MediaAnswer.writes.writes(score.examFile),
        "rubric_file" -> MediaAnswer.writes.writes(score.rubricFile),
        "orig_comments" -> score.origComments,
        "add_comments" -> score.addComments,
        "createdAt" -> score.createdAt,
        "updatedAt" -> score.updatedAt
      )
    }
  }
}
