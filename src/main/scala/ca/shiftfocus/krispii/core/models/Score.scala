package ca.shiftfocus.krispii.core.models

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.json.JodaWrites._
import play.api.libs.json.Writes._
import play.api.libs.json._

case class Score(
  work_id: UUID,
  scorer_id: UUID,
  version: Long = 1L,
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime,
  grade: String = ""
) {}

object Score {
  implicit val jsonWrites = new Writes[Score] {
    def writes(score: Score): JsValue = {
      Json.obj(
        "work_id" -> score.work_id,
        "scorer_id" -> score.scorer_id,
        "version" -> score.version,
        "createdAt" -> score.createdAt,
        "updatedAt" -> score.updatedAt,
        "grade" -> score.grade
      )
    }
  }
}
