package ca.shiftfocus.krispii.core.models.work

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.libs.json.JodaWrites._

case class Test(
    id: UUID = UUID.randomUUID,
    examId: UUID,
    teamId: UUID,
    name: String,
    version: Long = 1L,
    grade: String, // initially empty, in contrast with Work
    origResponse: MediaAnswer = MediaAnswer(),
    createdAt: DateTime = new DateTime(),
    updatedAt: DateTime = new DateTime()
) extends Evaluation {
  override def responseToString: String = {
    name
  }
}

object Test {
  implicit val testWrites = new Writes[Test] {
    def writes(test: Test): JsValue = {
      Json.obj(
        "id" -> test.id,
        "examId" -> test.examId,
        "teamId" -> test.teamId,
        "version" -> test.version,
        "orig_response" -> MediaAnswer.writes.writes(test.origResponse),
        "grade" -> test.grade,
        "createdAt" -> test.createdAt,
        "updatedAt" -> test.updatedAt
      )
    }
  }
}
