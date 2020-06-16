package ca.shiftfocus.krispii.core.models.course

import java.awt.Color
import java.util.UUID

import ca.shiftfocus.krispii.core.models.Team
import ca.shiftfocus.krispii.core.models.work.Test
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json.JodaWrites._
import play.api.libs.json._

case class Exam(
  id: UUID = UUID.randomUUID,
  version: Long = 1L,
  ownerId: UUID,
  name: String,
  color: Color,
  slug: String,
  origRubricId: Option[UUID] = None, // might be the ID of a rubricComponent or an imageComponent
  enabled: Boolean = true,
  archived: Boolean = false,
  deleted: Boolean = false,
  teams: Option[IndexedSeq[Team]] = None,
  tests: Option[IndexedSeq[Test]] = None,
  lastTeamId: Option[UUID] = None, // necessary?
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime
) extends Group

object Exam {

  implicit val colorWrites = ColorBox.colorWrites

  implicit val examWrites: Writes[Exam] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "ownerId").write[UUID] and
    (__ \ "name").write[String] and
    (__ \ "color").write[Color] and
    (__ \ "slug").write[String] and
    (__ \ "origRubricId").writeNullable[UUID] and
    (__ \ "enabled").write[Boolean] and
    (__ \ "archived").write[Boolean] and
    (__ \ "deleted").write[Boolean] and
    (__ \ "teams").writeNullable[IndexedSeq[Team]] and
    (__ \ "tests").writeNullable[IndexedSeq[Test]] and
    (__ \ "lastTeamId").writeNullable[UUID] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(Exam.unapply))

}
