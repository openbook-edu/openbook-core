package ca.shiftfocus.krispii.core.models

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.JodaWrites._

case class ProjectToken(
  projectId: UUID,
  email: String,
  token: String = UUID.randomUUID.toString,
  createdAt: DateTime = new DateTime
) {}

object ProjectToken {
  implicit val projectTokenWrites: Writes[ProjectToken] = (
    (__ \ "projectId").write[UUID] and
    (__ \ "email").write[String] and
    (__ \ "token").write[String] and
    (__ \ "createdAt").write[DateTime]
  )(unlift(ProjectToken.unapply))
}
