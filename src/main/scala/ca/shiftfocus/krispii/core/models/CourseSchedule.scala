package ca.shiftfocus.krispii.core.models

import com.github.mauricio.async.db.RowData
import ca.shiftfocus.krispii.core.lib.{ LocalDateTimeJson }
import java.util.UUID
import org.joda.time.DateTime
import org.joda.time.LocalTime
import org.joda.time.LocalDate
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._
import org.joda.time.LocalDate
import org.joda.time.LocalTime

case class CourseSchedule(
  id: UUID = UUID.randomUUID,
  version: Long = 1L,
  courseId: UUID,
  day: LocalDate,
  startTime: LocalTime,
  endTime: LocalTime,
  description: String,
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime
) extends Schedule

trait Schedule {
  def day: LocalDate
  def startTime: LocalTime
  def endTime: LocalTime
}

object CourseSchedule extends LocalDateTimeJson {

  implicit val sectionScheduleReads: Reads[CourseSchedule] = (
    (__ \ "id").read[UUID] and
    (__ \ "version").read[Long] and
    (__ \ "courseId").read[UUID] and
    (__ \ "day").read[LocalDate] and
    (__ \ "startTime").read[LocalTime] and
    (__ \ "endTime").read[LocalTime] and
    (__ \ "description").read[String] and
    (__ \ "createdAt").read[DateTime] and
    (__ \ "updatedAt").read[DateTime]
  )(CourseSchedule.apply _)

  implicit val sectionScheduleWrites: Writes[CourseSchedule] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "courseId").write[UUID] and
    (__ \ "day").write[LocalDate] and
    (__ \ "startTime").write[LocalTime] and
    (__ \ "endTime").write[LocalTime] and
    (__ \ "description").write[String] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
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
