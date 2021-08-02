package ca.shiftfocus.krispii.core.models

import ca.shiftfocus.krispii.core.lib.LocalDateTimeJson
import java.util.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._
import play.api.libs.json.JodaWrites._

case class GroupScheduleException(
    id: UUID = UUID.randomUUID,
    userId: UUID,
    groupId: UUID,
    version: Long = 1L,
    startDate: DateTime,
    endDate: DateTime,
    reason: String,
    block: Boolean = false,
    createdAt: DateTime = new DateTime,
    updatedAt: DateTime = new DateTime
) extends Schedule {
  override def equals(anotherObject: Any): Boolean = {
    anotherObject match {
      case anotherCSE: GroupScheduleException =>
        this.id == anotherCSE.id &&
          this.userId == anotherCSE.userId &&
          this.groupId == anotherCSE.groupId &&
          this.version == anotherCSE.version &&
          this.startDate.toString == anotherCSE.startDate.toString &&
          this.endDate.toString == anotherCSE.endDate.toString
        this.reason == anotherCSE.reason
      case _ => false
    }
  }
}

object GroupScheduleException extends LocalDateTimeJson {
  implicit val courseScheduleWrites: Writes[GroupScheduleException] = (
    (__ \ "id").write[UUID] and
    (__ \ "userId").write[UUID] and
    (__ \ "groupId").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "startDate").write[DateTime] and
    (__ \ "endDate").write[DateTime] and
    (__ \ "reason").write[String] and
    (__ \ "block").write[Boolean] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(GroupScheduleException.unapply))
}
