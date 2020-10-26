package ca.shiftfocus.krispii.core.models.group

import java.awt.Color
import java.util.UUID

import ca.shiftfocus.krispii.core.models.User
import ca.shiftfocus.krispii.core.models.work.Test
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
  archived: Boolean = false,
  deleted: Boolean = false,
  chatEnabled: Boolean = true,
  scorers: IndexedSeq[User] = IndexedSeq.empty[User],
  tests: IndexedSeq[Test] = IndexedSeq.empty[Test],
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
    (__ \ "archived").write[Boolean] and
    (__ \ "deleted").write[Boolean] and
    (__ \ "chatEnabled").write[Boolean] and
    (__ \ "scorers").write[IndexedSeq[User]] and
    (__ \ "tests").write[IndexedSeq[Test]] and
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
    (__ \ "archived").read[Boolean] and
    (__ \ "deleted").read[Boolean] and
    (__ \ "chatEnabled").read[Boolean] and
    (__ \ "scorers").read[IndexedSeq[User]] and
    (__ \ "tests").read[IndexedSeq[Test]] and
    (__ \ "createdAt").read[DateTime] and
    (__ \ "updatedAt").read[DateTime]
  )(Team.apply _)
}
