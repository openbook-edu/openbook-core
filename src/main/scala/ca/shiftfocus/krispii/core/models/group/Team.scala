package ca.shiftfocus.krispii.core.models.group

import java.awt.Color
import java.util.UUID

import ca.shiftfocus.krispii.core.models.GroupSchedule
import ca.shiftfocus.krispii.core.models.user.Scorer
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._
import play.api.libs.json.Writes._
import play.api.libs.json.Reads._
import play.api.libs.json._

case class Team(
  id: UUID = UUID.randomUUID,
  version: Long = 1L,
  examId: UUID,
  ownerId: UUID, // redundant with examId, but necessary for descendants of Group
  name: String = "",
  slug: String = "",
  color: Color,
  enabled: Boolean = true,
  schedulingEnabled: Boolean = false,
  chatEnabled: Boolean = true,
  archived: Boolean = false,
  deleted: Boolean = false,
  scorers: IndexedSeq[Scorer] = IndexedSeq.empty[Scorer],
  schedules: IndexedSeq[GroupSchedule] = IndexedSeq.empty[GroupSchedule],
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime
) extends Group

object Team {

  implicit val colorWrites = ColorBox.colorWrites
  implicit val colorReads = ColorBox.colorReads

  implicit val teamWrites: Writes[Team] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "examId").write[UUID] and
    (__ \ "ownerId").write[UUID] and // it's ownerId in Scala
    (__ \ "name").write[String] and
    (__ \ "slug").write[String] and
    (__ \ "color").write[Color] and
    (__ \ "enabled").write[Boolean] and
    (__ \ "schedulingEnabled").write[Boolean] and
    (__ \ "chatEnabled").write[Boolean] and
    (__ \ "archived").write[Boolean] and
    (__ \ "deleted").write[Boolean] and
    (__ \ "scorers").write[IndexedSeq[Scorer]] and
    (__ \ "schedules").write[IndexedSeq[GroupSchedule]] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(Team.unapply))

  implicit val teamReads: Reads[Team] = (
    (__ \ "id").read[UUID] and
    (__ \ "version").read[Long] and
    (__ \ "examId").read[UUID] and
    (__ \ "ownerId").read[UUID] and
    (__ \ "name").read[String] and
    (__ \ "slug").read[String] and
    (__ \ "color").read[Color] and
    (__ \ "enabled").read[Boolean] and
    (__ \ "schedulingEnabled").read[Boolean] and
    (__ \ "chatEnabled").read[Boolean] and
    (__ \ "archived").read[Boolean] and
    (__ \ "deleted").read[Boolean] and
    (__ \ "scorers").read[IndexedSeq[Scorer]] and
    (__ \ "schedules").read[IndexedSeq[GroupSchedule]] and
    (__ \ "createdAt").read[DateTime] and
    (__ \ "updatedAt").read[DateTime]
  )(Team.apply _)
}
