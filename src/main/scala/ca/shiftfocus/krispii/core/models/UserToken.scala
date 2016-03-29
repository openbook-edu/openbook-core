package ca.shiftfocus.krispii.core.models

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

/**
 * Represents the authentication token assign to a new user when signing up or to a user which requests a password
 */
case class UserToken(
  userId: UUID,
  token: String,
  tokenType: String,
  createdAt: DateTime = new DateTime
) {}

object UserToken {
  //  implicit val activationReads: Reads[UserToken] = (
  //      (JsPath \ "id").read[UUID] and (JsPath \ "token").read[String]
  //    ) (UserToken.apply _)
  //
  //  implicit val activationWrites: Writes[UserToken] = (
  //      (JsPath \ "id").write[UUID] and (JsPath \ "token").write[String]
  //    ) (unlift(UserToken.unapply))
}
