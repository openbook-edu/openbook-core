package ca.shiftfocus.krispii.core.models

import java.util.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._
import play.api.libs.json.JodaWrites._

case class Score(
  workId: UUID,
  scorerId: UUID,
  version: Long = 1L,
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime,
  grade: String = ""
)

object Score {
  implicit val writes: Writes[Score] = (
    (__ \ "workId").write[UUID] and
    (__ \ "scorerId").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime] and
    (__ \ "grade").write[String]
  )(unlift(Score.unapply))
}
