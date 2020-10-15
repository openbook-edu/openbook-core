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
  version: Long = 1L,
  /* an ownerId would make Team a descendant of Group and enable some redis caching, but
   * is redundant with examId and would lead to potential problems */
  examId: UUID,
  /* name and slug would make Team a descendant of Group;
   * they are perhaps not necessary, but wouldn't be a problem */
  color: Option[Color] = None, // would need to be non-optional
  enabled: Boolean = true,
  chatEnabled: Boolean = true,
  // archived and deleted seem useful
  scorers: IndexedSeq[User] = IndexedSeq.empty[User],
  tests: IndexedSeq[Test] = IndexedSeq.empty[Test],
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime
)

object Team {

  implicit val colorWrites = ColorBox.colorWrites

  implicit val teamWrites: Writes[Team] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "examId").write[UUID] and
    (__ \ "color").writeNullable[Color] and
    (__ \ "enabled").write[Boolean] and
    (__ \ "chatEnabled").write[Boolean] and
    (__ \ "scorers").write[IndexedSeq[User]] and
    (__ \ "tests").write[IndexedSeq[Test]] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(Team.unapply))
}
