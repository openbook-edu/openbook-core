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
  val inactive = "account.status.inactive"
  val free = "account.status.free"
  val trial = "account.status.trial"
  val paid = "account.status.paid"
  val onhold = "account.status.onhold"
  val error = "account.status.payment.error"
  val overdue = "account.status.overdue"
  val canceled = "account.status.canceled"

  def getAll: IndexedSeq[String] = {
    IndexedSeq(
      inactive,
      free,
      trial,
      paid,
      onhold,
      error,
      overdue,
      canceled
    )
  }
}

object Account {
  implicit val writes: Writes[Account] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "userId").write[UUID] and
    (__ \ "status").write[String] and
    (__ \ "customer").writeNullable[JsValue] and
    (__ \ "subscriptions").write[IndexedSeq[JsValue]] and
    (__ \ "activeUntil").writeNullable[DateTime]
  )(unlift(Account.unapply))
}

