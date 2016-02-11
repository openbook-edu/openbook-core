package ca.shiftfocus.krispii.core.models

import java.util.UUID

import play.api.libs.json.{Writes, Reads}

/**
  * Represents the authentication token assign to a new user when signing up.
  * Created by ryanez on 11/02/16.
  */
case class UserToken (
  id: UUID,
  token: String
                     ) {}

object UserToken {
  implicit val activationReads: Reads[UserToken] = (
      (__ \ "id").read[UUID] and (__ \ "token").read[String]
    ) (UserToken.apply _)

  implicit val activationWrites: Writes[UserToken] = (
      (__ \ "id").write[UUID] and (__ \ "token").write[String]
    ) (unlift(UserToken.unapply()))
}
