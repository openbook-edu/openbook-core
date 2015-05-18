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
  version: Long = 1L,
  day: LocalDate,
  startTime: LocalTime,
  endTime: LocalTime,
  reason: String,
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime
)

object CourseScheduleException extends LocalDateTimeJson {
  implicit val courseScheduleWrites: Writes[CourseScheduleException] = (
    (__ \ "id").write[UUID] and
      (__ \ "userId").write[UUID] and
      (__ \ "courseId").write[UUID] and
      (__ \ "version").write[Long] and
      (__ \ "day").write[LocalDate] and
      (__ \ "startTime").write[LocalTime] and
      (__ \ "endTime").write[LocalTime] and
      (__ \ "reason").write[String] and
      (__ \ "createdAt").write[DateTime] and
      (__ \ "updatedAt").write[DateTime]
    )(unlift(CourseScheduleException.unapply))
}
