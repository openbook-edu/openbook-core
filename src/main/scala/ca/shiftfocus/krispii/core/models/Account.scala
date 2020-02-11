package ca.shiftfocus.krispii.core.models

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.JodaWrites._

case class Account(
  id: UUID = UUID.randomUUID(),
  version: Long = 1L,
  userId: UUID,
  status: String = AccountStatus.inactive,
  customer: Option[JsValue] = None,
  subscriptions: IndexedSeq[JsValue] = IndexedSeq.empty[JsValue],
  trialStartedAt: Option[DateTime] = None,
  activeUntil: Option[DateTime] = None,
  overdueStartedAt: Option[DateTime] = None,
  overdueEndedAt: Option[DateTime] = None,
  overduePlanId: Option[String] = None
)

object AccountStatus {
  val inactive = "account.status.inactive"
  val limited = "account.status.limited"
  val free = "account.status.free"
  val trial = "account.status.trial"
  val paid = "account.status.paid"
  val group = "account.status.paid.group"
  val onhold = "account.status.onhold"
  val error = "account.status.payment.error"
  val overdue = "account.status.overdue"
  val canceled = "account.status.canceled"

  def getAll: IndexedSeq[String] = {
    IndexedSeq(
      inactive,
      limited,
      free,
      trial,
      paid,
      group,
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
    (__ \ "trialStartedAt").writeNullable[DateTime] and
    (__ \ "activeUntil").writeNullable[DateTime] and
    (__ \ "overdueStartedAt").writeNullable[DateTime] and
    (__ \ "overdueEndedAt").writeNullable[DateTime] and
    (__ \ "overduePlanId").writeNullable[String]
  )(unlift(Account.unapply))
}

