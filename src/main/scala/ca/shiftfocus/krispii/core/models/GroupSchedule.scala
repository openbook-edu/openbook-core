package ca.shiftfocus.krispii.core.models

import ca.shiftfocus.krispii.core.lib.LocalDateTimeJson
import java.util.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._

trait Schedule {
  val startDate: DateTime
  val endDate: DateTime
}

case class GroupSchedule(
    id: UUID = UUID.randomUUID,
    version: Long = 1L,
    groupId: UUID,
    startDate: DateTime,
    endDate: DateTime,
    description: String,
    createdAt: DateTime = new DateTime,
    updatedAt: DateTime = new DateTime
) extends Schedule {
  override def equals(anotherObject: Any): Boolean = {
    anotherObject match {
      case anotherCS: GroupSchedule =>
        this.id == anotherCS.id &&
          this.version == anotherCS.version &&
          this.groupId == anotherCS.groupId &&
          this.startDate.toString == anotherCS.startDate.toString &&
          this.endDate.toString == anotherCS.endDate.toString
        this.description == anotherCS.description
      case _ => false
    }
  }
}

object GroupSchedule extends LocalDateTimeJson {

  implicit val groupScheduleReads: Reads[GroupSchedule] = (
    (__ \ "id").read[UUID] and
    (__ \ "version").read[Long] and
    (__ \ "groupId").read[UUID] and
    (__ \ "startDate").read[DateTime] and
    (__ \ "endDate").read[DateTime] and
    (__ \ "description").read[String] and
    (__ \ "createdAt").read[DateTime] and
    (__ \ "updatedAt").read[DateTime]
  )(GroupSchedule.apply _)

  implicit val groupScheduleWrites: Writes[GroupSchedule] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "groupId").write[UUID] and
    (__ \ "startDate").write[DateTime] and
    (__ \ "endDate").write[DateTime] and
    (__ \ "description").write[String] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(GroupSchedule.unapply))
}
