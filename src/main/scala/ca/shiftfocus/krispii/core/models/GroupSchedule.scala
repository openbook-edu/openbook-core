package ca.shiftfocus.krispii.core.models

import ca.shiftfocus.krispii.core.lib.{LocalDateTimeJson}
import java.util.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._

case class GroupSchedule(
    id: UUID = UUID.randomUUID,
    version: Long = 1L,
    groupId: UUID,
    startDay: LocalDate,
    endDay: LocalDate,
    startTime: LocalTime,
    endTime: LocalTime,
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
          this.startDay.toString == anotherCS.startDay.toString &&
          this.endDay.toString == anotherCS.endDay.toString &&
          this.startTime.toString == anotherCS.startTime.toString &&
          this.endTime.toString == anotherCS.endTime.toString &&
          this.description == anotherCS.description
      case _ => false
    }
  }
}

trait Schedule {
  def startDay: LocalDate
  def endDay: LocalDate
  def startTime: LocalTime
  def endTime: LocalTime
}

object GroupSchedule extends LocalDateTimeJson {

  implicit val groupScheduleReads: Reads[GroupSchedule] = (
    (__ \ "id").read[UUID] and
    (__ \ "version").read[Long] and
    (__ \ "groupId").read[UUID] and
    (__ \ "startDay").read[LocalDate] and
    (__ \ "endDay").read[LocalDate] and
    (__ \ "startTime").read[LocalTime] and
    (__ \ "endTime").read[LocalTime] and
    (__ \ "description").read[String] and
    (__ \ "createdAt").read[DateTime] and
    (__ \ "updatedAt").read[DateTime]
  )(GroupSchedule.apply _)

  implicit val groupScheduleWrites: Writes[GroupSchedule] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "groupId").write[UUID] and
    (__ \ "startDay").write[LocalDate] and
    (__ \ "endDay").write[LocalDate] and
    (__ \ "startTime").write[LocalTime] and
    (__ \ "endTime").write[LocalTime] and
    (__ \ "description").write[String] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(GroupSchedule.unapply))
}

/*
 * Case-classes for FORM definitions!
 */
case class GroupSchedulePost(
  startDay: LocalDate,
  endDay: LocalDate,
  startTime: LocalTime,
  endTime: LocalTime,
  description: String
)
object GroupSchedulePost extends LocalDateTimeJson {
  implicit val sectionScheduleReads: Reads[GroupSchedulePost] = (
    (__ \ "startDay").read[LocalDate] and
    (__ \ "endDay").read[LocalDate] and
    (__ \ "startTime").read[LocalTime] and
    (__ \ "endTime").read[LocalTime] and
    (__ \ "description").read[String]
  )(GroupSchedulePost.apply _)
}

case class GroupSchedulePut(
  version: Long,
  groupId: Option[UUID],
  startDay: Option[LocalDate],
  endDay: Option[LocalDate],
  startTime: Option[LocalTime],
  endTime: Option[LocalTime],
  description: Option[String]
)
object GroupSchedulePut extends LocalDateTimeJson {
  implicit val sectionScheduleReads: Reads[GroupSchedulePut] = (
    (__ \ "version").read[Long] and
    (__ \ "groupId").readNullable[UUID] and
    (__ \ "startDay").readNullable[LocalDate] and
    (__ \ "endDay").readNullable[LocalDate] and
    (__ \ "startTime").readNullable[LocalTime] and
    (__ \ "endTime").readNullable[LocalTime] and
    (__ \ "description").readNullable[String]
  )(GroupSchedulePut.apply _)
}
