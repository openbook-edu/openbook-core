package ca.shiftfocus.krispii.core.models

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json.{ JsValue, _ }
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._

case class PaymentLog(
  id: UUID = UUID.randomUUID(),
  logType: String,
  description: String,
  data: String,
  userId: Option[UUID] = None,
  createdAt: DateTime = new DateTime()
)

object PaymentLogType {
  val info = "info"
  val warning = "warning"
  val error = "error"
}

object PaymentLog {
  implicit val writes: Writes[PaymentLog] = (
    (__ \ "id").write[UUID] and
    (__ \ "logType").write[String] and
    (__ \ "description").write[String] and
    (__ \ "data").write[String] and
    (__ \ "userId").writeNullable[UUID] and
    (__ \ "createdAt").write[DateTime]
  )(unlift(PaymentLog.unapply))
}