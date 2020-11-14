package ca.shiftfocus.krispii.core.models.user

import java.util.UUID

import ca.shiftfocus.krispii.core.models.{Role, Tag, UserToken}
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json.JodaReads._
import play.api.libs.json.JodaWrites._
import play.api.libs.json.Reads._
import play.api.libs.json.Writes._
import play.api.libs.json._

case class Scorer(
  id: UUID = UUID.randomUUID,
  version: Long = 1L,
  username: String,
  email: String,
  hash: Option[String] = None,
  givenname: String,
  surname: String,
  alias: Option[String] = None,
  roles: IndexedSeq[Role] = IndexedSeq.empty[Role],
  tags: IndexedSeq[Tag] = IndexedSeq.empty[Tag],
  token: Option[UserToken] = None,
  accountType: String,
  leader: Boolean = false, // derived from teams_scorers.leader
  isDeleted: Boolean = false, // derived from teams_scorers.deleted
  isArchived: Boolean = false, // derived from teams_scorers.archived
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime,
  var includedAt: DateTime = new DateTime // derived from teams_scorers.created_at
) extends UserTrait

/**
 * Scorer companion object. Fills the role of input/output mappers in the
 * layered architecture. This object should handle all interaction with the
 * technical services layer for Scorers.
 *
 * For individual scorer lookups, this mapper will check the in-memory cache
 * first, followed by the database.
 */
object Scorer {
  implicit val scorerWrites = new Writes[Scorer] {
    /* hard to write this in a functional way because we don't want to write
       hash nor token*/
    def writes(scorer: Scorer): JsValue = {
      Json.obj(
        "id" -> scorer.id.toString,
        "version" -> scorer.version,
        "username" -> scorer.username,
        "email" -> scorer.email,
        "givenname" -> scorer.givenname,
        "surname" -> scorer.surname,
        "alias" -> scorer.alias,
        "roles" -> scorer.roles,
        "tags" -> scorer.tags,
        "accountType" -> scorer.accountType,
        "leader" -> scorer.leader,
        "isDeleted" -> scorer.isDeleted,
        "isArchived" -> scorer.isArchived,
        "createdAt" -> scorer.createdAt,
        "updatedAt" -> scorer.updatedAt,
        "includedAt" -> scorer.includedAt
      )
    }
  }

  implicit val scorerReads: Reads[Scorer] = (
    (__ \ "id").read[UUID] and
    (__ \ "version").read[Long] and
    (__ \ "username").read[String] and
    (__ \ "email").read[String] and
    (__ \ "hash").readNullable[String] and
    (__ \ "givenname").read[String] and
    (__ \ "surname").read[String] and
    (__ \ "alias").readNullable[String] and
    (__ \ "roles").read[IndexedSeq[Role]] and
    (__ \ "tags").read[IndexedSeq[Tag]] and
    (__ \ "token").readNullable[UserToken] and
    (__ \ "accountType").read[String] and
    (__ \ "leader").read[Boolean] and
    (__ \ "isDeleted").read[Boolean] and
    (__ \ "isArchived").read[Boolean] and
    (__ \ "createdAt").read[DateTime] and
    (__ \ "updatedAt").read[DateTime] and
    (__ \ "includedAt").read[DateTime]
  )(Scorer.apply _)
}

