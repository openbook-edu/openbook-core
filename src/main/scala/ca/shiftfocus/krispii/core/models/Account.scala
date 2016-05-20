package ca.shiftfocus.krispii.core.models

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class Account(
  id: UUID = UUID.randomUUID(),
  version: Long = 1L,
  userId: UUID,
  status: String = AccountStatus.inactive,
  customer: Option[JsValue] = None,
  subscriptions: IndexedSeq[JsValue] = IndexedSeq.empty[JsValue],
  activeUntil: Option[DateTime] = None
)

object AccountStatus {
  val inactive = "inactive"
  val free = "free"
  val trial = "trial"
  val paid = "paid"
  val onhold = "onhold"
}

object Account {
  implicit val sectionWrites: Writes[Account] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "userId").write[UUID] and
    (__ \ "status").write[String] and
    (__ \ "customer").writeNullable[JsValue] and
    (__ \ "subscriptions").write[IndexedSeq[JsValue]] and
    (__ \ "activeUntil").writeNullable[DateTime]
  )(unlift(Account.unapply))
}

