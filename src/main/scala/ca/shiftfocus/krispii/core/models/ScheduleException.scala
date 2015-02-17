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


case class CourseScheduleException(
  id: UUID = UUID.random,
  userId: UUID,
  courseId: UUID,
  version: Long = 0,
  day: LocalDate,
  startTime: LocalTime,
  endTime: LocalTime,
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
)

object CourseScheduleException extends LocalDateTimeJson {

  def apply(row: RowData): CourseScheduleException = {
    val day = row("day").asInstanceOf[DateTime]
    val startTime = row("start_time").asInstanceOf[DateTime]
    val endTime = row("end_time").asInstanceOf[DateTime]

    CourseScheduleException(
      UUID(row("id").asInstanceOf[Array[Byte]]),
      UUID(row("user_id").asInstanceOf[Array[Byte]]),
      UUID(row("course_id").asInstanceOf[Array[Byte]]),
      row("version").asInstanceOf[Long],
      day.toLocalDate(),
      new DateTime(day.getYear(), day.getMonthOfYear(), day.getDayOfMonth(), startTime.getHourOfDay(), startTime.getMinuteOfHour, startTime.getSecondOfMinute()).toLocalTime(),
      new DateTime(day.getYear(), day.getMonthOfYear(), day.getDayOfMonth(), endTime.getHourOfDay(), endTime.getMinuteOfHour, endTime.getSecondOfMinute()).toLocalTime(),
      Some(row("created_at").asInstanceOf[DateTime]),
      Some(row("updated_at").asInstanceOf[DateTime])
    )
  }

  implicit val courseScheduleReads: Reads[CourseScheduleException] = (
    (__ \ "id").read[UUID] and
      (__ \ "userId").read[UUID] and
      (__ \ "courseId").read[UUID] and
      (__ \ "version").read[Long] and
      (__ \ "day").read[LocalDate] and
      (__ \ "startTime").read[LocalTime] and
      (__ \ "endTime").read[LocalTime] and
      (__ \ "createdAt").readNullable[DateTime] and
      (__ \ "updatedAt").readNullable[DateTime]
    )(CourseScheduleException.apply(_: UUID, _: UUID, _: UUID, _: Long, _: LocalDate, _: LocalTime, _: LocalTime, _: Option[DateTime], _: Option[DateTime]))

  implicit val courseScheduleWrites: Writes[CourseScheduleException] = (
    (__ \ "id").write[UUID] and
      (__ \ "userId").write[UUID] and
      (__ \ "courseId").write[UUID] and
      (__ \ "version").write[Long] and
      (__ \ "day").write[LocalDate] and
      (__ \ "startTime").write[LocalTime] and
      (__ \ "endTime").write[LocalTime] and
      (__ \ "createdAt").writeNullable[DateTime] and
      (__ \ "updatedAt").writeNullable[DateTime]
    )(unlift(CourseScheduleException.unapply))
}
