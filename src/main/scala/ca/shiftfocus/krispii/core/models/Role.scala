package ca.shiftfocus.krispii.core.models

import java.util.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._

case class Role(
    id: UUID = UUID.randomUUID,
    version: Long = 1L,
    name: String,
    createdAt: DateTime = new DateTime,
    updatedAt: DateTime = new DateTime
) {
  override def equals(that: Any): Boolean = {
    that match {
      case thatRole: Role => this.id == thatRole.id
      case thatString: String => this.name == thatString
      case _ => false
    }
  }
}

object Role {
  implicit val roleWrites: Writes[Role] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "name").write[String] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(Role.unapply))
}
