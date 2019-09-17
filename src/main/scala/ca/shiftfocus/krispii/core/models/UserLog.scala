package ca.shiftfocus.krispii.core.models

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.libs.json.JodaWrites._

case class UserLog(
  id: UUID = UUID.randomUUID(),
  userId: UUID,
  logType: String,
  data: Option[String],
  createdAt: DateTime = new DateTime()
)

object UserLog {
  implicit val usegLogWrites = new Writes[UserLog] {
    def writes(userLog: UserLog): JsValue = {
      Json.obj(
        "id" -> userLog.id,
        "userId" -> userLog.userId,
        "logType" -> userLog.logType,
        "data" -> userLog.data,
        "createdAt" -> userLog.createdAt
      )
    }
  }
}

object UserLogType {
  val login = "login"
}

