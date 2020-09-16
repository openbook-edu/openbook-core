package ca.shiftfocus.krispii.core.models

import java.awt.Color
import java.util.UUID

import ca.shiftfocus.krispii.core.models.course.ColorBox
import ca.shiftfocus.krispii.core.models.work.Test
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json.Writes._
import play.api.libs.json._
import play.api.libs.json.JodaWrites._

case class Team(
  id: UUID = UUID.randomUUID,
  examId: UUID,
  version: Long = 1L,
  color: Option[Color] = None,
  enabled: Boolean = true,
  chatEnabled: Boolean = true,
  scorers: IndexedSeq[User] = IndexedSeq.empty[User],
  tests: IndexedSeq[Test] = IndexedSeq.empty[Test],
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime
)

object Team {

  implicit val colorWrites = ColorBox.colorWrites

  implicit val teamWrites: Writes[Team] = (
    (__ \ "id").write[UUID] and
    (__ \ "examId").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "color").writeNullable[Color] and
    (__ \ "enabled").write[Boolean] and
    (__ \ "chatEnabled").write[Boolean] and
    (__ \ "scorers").write[IndexedSeq[User]] and
    (__ \ "tests").write[IndexedSeq[Test]] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(Team.unapply))
}
