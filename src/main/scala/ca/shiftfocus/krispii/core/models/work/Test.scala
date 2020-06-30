package ca.shiftfocus.krispii.core.models.work

import java.util.UUID

import ca.shiftfocus.krispii.core.models.User
import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.libs.json.JodaWrites._

case class Test(
    id: UUID = UUID.randomUUID,
    examId: UUID,
    teamId: Option[UUID] = None,
    name: String,
    version: Long = 1L,
    grade: String = "", // initially empty, in contrast with Work
    origResponse: UUID, // PDF component
    scorers: Option[IndexedSeq[User]] = None,
    scores: Option[IndexedSeq[Score]] = None,
    createdAt: DateTime = new DateTime(),
    updatedAt: DateTime = new DateTime()
) extends Evaluation {
  override def responseToString: String =
    s"${name}: ${grade}"
}

object Test {
  implicit val testWrites = new Writes[Test] {
    def writes(test: Test): JsValue = {
      Json.obj(
        "id" -> test.id,
        "examId" -> test.examId,
        "teamId" -> test.teamId,
        "name" -> test.name,
        "version" -> test.version,
        "grade" -> test.grade,
        "orig_response" -> test.origResponse,
        "scorers" -> test.scorers,
        "scores" -> test.scores,
        "createdAt" -> test.createdAt,
        "updatedAt" -> test.updatedAt
      )
    }
  }
}
