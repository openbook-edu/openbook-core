package ca.shiftfocus.krispii.core.models

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json.JodaReads._
import play.api.libs.json.JodaWrites._
import play.api.libs.json._
import play.api.libs.json.Writes._

/**
 * Represents the authentication token assign to a new user when signing up or to a user which requests a password
 */
case class UserToken(
  userId: UUID,
  token: String,
  tokenType: String,
  createdAt: DateTime = new DateTime
)

object UserToken {
  implicit val userTokenReads: Reads[UserToken] = (
    (__ \ "userId").read[UUID] and
    (__ \ "token").read[String] and
    (__ \ "tokenType").read[String] and
    (__ \ "createdAt").read[DateTime]
  )(UserToken.apply _)

  implicit val userTokenWrites: Writes[UserToken] = (
    (__ \ "userId").write[UUID] and
    (__ \ "token").write[String] and
    (__ \ "tokenType").write[String] and
    (__ \ "createdAt").write[DateTime]
  )(unlift(UserToken.unapply))
}
