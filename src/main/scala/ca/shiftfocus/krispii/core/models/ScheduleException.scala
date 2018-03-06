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
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._

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
      case anotherCSE: CourseScheduleException =>
        this.id == anotherCSE.id &&
          this.userId == anotherCSE.userId &&
          this.courseId == anotherCSE.courseId &&
          this.version == anotherCSE.version &&
          this.day.toString == anotherCSE.day.toString &&
          this.startTime.toString == anotherCSE.startTime.toString &&
          this.endTime.toString == anotherCSE.endTime.toString &&
          this.reason == anotherCSE.reason
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
