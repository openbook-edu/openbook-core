package ca.shiftfocus.krispii.core.models

import java.util.UUID

import play.api.libs.functional.syntax._
import play.api.libs.json.{Writes, _}

case class EmailChangeRequest(
  userId: UUID,
  requestedEmail: String,
  token: String
)

object EmailChangeRequest {
  implicit val activationReads: Reads[EmailChangeRequest] = (
    (__ \ "userId").read[UUID] and
    (__ \ "requestedEmail").read[String] and
    (__ \ "token").read[String]
  )(EmailChangeRequest.apply _)

  implicit val activationWrites: Writes[EmailChangeRequest] = (
    (__ \ "userId").write[UUID] and
    (__ \ "requestedEmail").write[String] and
    (__ \ "token").write[String]
  )(unlift(EmailChangeRequest.unapply))
}

