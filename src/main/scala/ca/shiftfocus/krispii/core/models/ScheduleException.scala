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

case class CourseScheduleException(
    id: UUID = UUID.randomUUID,
    userId: UUID,
    courseId: UUID,
    version: Long = 1L,
    day: LocalDate,
    startTime: LocalTime,
    endTime: LocalTime,
    reason: String,
    block: Boolean = false,
    createdAt: DateTime = new DateTime,
    updatedAt: DateTime = new DateTime
) extends Schedule {
  override def equals(anotherObject: Any): Boolean = {
    anotherObject match {
      case anotherCourseScheduleException: CourseScheduleException =>
        this.id == anotherCourseScheduleException.id &&
          this.userId == anotherCourseScheduleException.userId &&
          this.courseId == anotherCourseScheduleException.courseId &&
          this.version == anotherCourseScheduleException.version &&
          this.day == anotherCourseScheduleException.day &&
          this.block == anotherCourseScheduleException.block &&
          this.startTime == anotherCourseScheduleException.startTime &&
          this.endTime == anotherCourseScheduleException.endTime &&
          this.reason == anotherCourseScheduleException.reason
      case _ => false

    }
  }
}

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
    (__ \ "block").write[Boolean] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(CourseScheduleException.unapply))
}
