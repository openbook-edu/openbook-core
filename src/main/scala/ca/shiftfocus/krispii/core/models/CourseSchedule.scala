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
import org.joda.time.LocalDate
import org.joda.time.LocalTime

case class CourseSchedule(
  id: UUID = UUID.random,
  version: Long = 0,
  courseId: UUID,
  day: LocalDate,
  startTime: LocalTime,
  endTime: LocalTime,
  description: String,
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
)

object CourseSchedule extends LocalDateTimeJson {

  def apply(row: RowData): CourseSchedule = {
    val day = row("day").asInstanceOf[DateTime]
    val startTime = row("start_time").asInstanceOf[DateTime]
    val endTime = row("end_time").asInstanceOf[DateTime]

    CourseSchedule(
      UUID(row("id").asInstanceOf[Array[Byte]]),
      row("version").asInstanceOf[Long],
      UUID(row("course_id").asInstanceOf[Array[Byte]]),
      day.toLocalDate(),
      new DateTime(day.getYear(), day.getMonthOfYear(), day.getDayOfMonth(), startTime.getHourOfDay(), startTime.getMinuteOfHour, startTime.getSecondOfMinute()).toLocalTime(),
      new DateTime(day.getYear(), day.getMonthOfYear(), day.getDayOfMonth(), endTime.getHourOfDay(), endTime.getMinuteOfHour, endTime.getSecondOfMinute()).toLocalTime(),
      row("description").asInstanceOf[String],
      Some(row("created_at").asInstanceOf[DateTime]),
      Some(row("updated_at").asInstanceOf[DateTime])
    )
  }

  implicit val sectionScheduleReads: Reads[CourseSchedule] = (
    (__ \ "id").read[UUID] and
    (__ \ "version").read[Long] and
    (__ \ "courseId").read[UUID] and
    (__ \ "day").read[LocalDate] and
    (__ \ "startTime").read[LocalTime] and
    (__ \ "endTime").read[LocalTime] and
    (__ \ "description").read[String] and
    (__ \ "createdAt").readNullable[DateTime] and
    (__ \ "updatedAt").readNullable[DateTime]
  )(CourseSchedule.apply(_: UUID, _: Long, _: UUID, _: LocalDate, _: LocalTime, _: LocalTime, _: String, _: Option[DateTime], _: Option[DateTime]))

  implicit val sectionScheduleWrites: Writes[CourseSchedule] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "courseId").write[UUID] and
    (__ \ "day").write[LocalDate] and
    (__ \ "startTime").write[LocalTime] and
    (__ \ "endTime").write[LocalTime] and
    (__ \ "description").write[String] and
    (__ \ "createdAt").writeNullable[DateTime] and
    (__ \ "updatedAt").writeNullable[DateTime]
  )(unlift(CourseSchedule.unapply))
}

/*
 * Case-classes for FORM definitions!
 */
case class CourseSchedulePost(
  day: LocalDate,
  startTime: LocalTime,
  endTime: LocalTime,
  description: String
)
object CourseSchedulePost extends LocalDateTimeJson {
  implicit val sectionScheduleReads: Reads[CourseSchedulePost] = (
    (__ \ "day").read[LocalDate] and
      (__ \ "startTime").read[LocalTime] and
      (__ \ "endTime").read[LocalTime] and
      (__ \ "description").read[String]
    )(CourseSchedulePost.apply _)
}

case class CourseSchedulePut(
  version: Long,
  courseId: Option[UUID],
  day: Option[LocalDate],
  startTime: Option[LocalTime],
  endTime: Option[LocalTime],
  description: Option[String]
)
object CourseSchedulePut extends LocalDateTimeJson {
  implicit val sectionScheduleReads: Reads[CourseSchedulePut] = (
    (__ \ "version").read[Long] and
      (__ \ "courseId").readNullable[UUID] and
      (__ \ "day").readNullable[LocalDate] and
      (__ \ "startTime").readNullable[LocalTime] and
      (__ \ "endTime").readNullable[LocalTime] and
      (__ \ "description").readNullable[String]
    )(CourseSchedulePut.apply _)
}
