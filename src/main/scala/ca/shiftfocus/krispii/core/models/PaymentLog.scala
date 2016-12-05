package ca.shiftfocus.krispii.core.models

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsValue, _}


case class PaymentLog(
  userId: UUID,
  logType: String,
  description: String,
  data: String,
  createdAt: DateTime = new DateTime()
)

object PaymentLogType {
  val info = "info"
  val warning = "warning"
  val error = "error"
}

object PaymentLog {
  implicit val writes: Writes[PaymentLog] = (
    (__ \ "userId").write[UUID] and
    (__ \ "logType").write[String] and
    (__ \ "description").write[String] and
    (__ \ "data").write[String] and
    (__ \ "createdAt").write[DateTime]
  )(unlift(PaymentLog.unapply))
}