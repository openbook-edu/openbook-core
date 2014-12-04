package ca.shiftfocus.krispii.core.models

import com.github.mauricio.async.db.RowData
import ca.shiftfocus.krispii.core.lib.{LocalDateTimeJson}
import ca.shiftfocus.uuid.UUID
import org.joda.time.DateTime
import org.joda.time.LocalTime
import org.joda.time.LocalDate
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._


case class SectionScheduleException(
  id: UUID = UUID.random,
  userId: UUID,
  classId: UUID,
  version: Long = 0,
  day: LocalDate,
  startTime: LocalTime,
  endTime: LocalTime,
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
)

object SectionScheduleException extends LocalDateTimeJson {

  def apply(row: RowData): SectionScheduleException = {
    val day = row("day").asInstanceOf[DateTime]
    val startTime = row("start_time").asInstanceOf[DateTime]
    val endTime = row("end_time").asInstanceOf[DateTime]

    SectionScheduleException(
      UUID(row("id").asInstanceOf[Array[Byte]]),
      UUID(row("user_id").asInstanceOf[Array[Byte]]),
      UUID(row("class_id").asInstanceOf[Array[Byte]]),
      row("version").asInstanceOf[Long],
      day.toLocalDate(),
      new DateTime(day.getYear(), day.getMonthOfYear(), day.getDayOfMonth(), startTime.getHourOfDay(), startTime.getMinuteOfHour, startTime.getSecondOfMinute()).toLocalTime(),
      new DateTime(day.getYear(), day.getMonthOfYear(), day.getDayOfMonth(), endTime.getHourOfDay(), endTime.getMinuteOfHour, endTime.getSecondOfMinute()).toLocalTime(),
      Some(row("created_at").asInstanceOf[DateTime]),
      Some(row("updated_at").asInstanceOf[DateTime])
    )
  }

  implicit val sectionScheduleReads: Reads[SectionScheduleException] = (
    (__ \ "id").read[UUID] and
      (__ \ "userId").read[UUID] and
      (__ \ "classId").read[UUID] and
      (__ \ "version").read[Long] and
      (__ \ "day").read[LocalDate] and
      (__ \ "startTime").read[LocalTime] and
      (__ \ "endTime").read[LocalTime] and
      (__ \ "createdAt").readNullable[DateTime] and
      (__ \ "updatedAt").readNullable[DateTime]
    )(SectionScheduleException.apply(_: UUID, _: UUID, _: UUID, _: Long, _: LocalDate, _: LocalTime, _: LocalTime, _: Option[DateTime], _: Option[DateTime]))

  implicit val sectionScheduleWrites: Writes[SectionScheduleException] = (
    (__ \ "id").write[UUID] and
      (__ \ "userId").write[UUID] and
      (__ \ "classId").write[UUID] and
      (__ \ "version").write[Long] and
      (__ \ "day").write[LocalDate] and
      (__ \ "startTime").write[LocalTime] and
      (__ \ "endTime").write[LocalTime] and
      (__ \ "createdAt").writeNullable[DateTime] and
      (__ \ "updatedAt").writeNullable[DateTime]
    )(unlift(SectionScheduleException.unapply))
}
