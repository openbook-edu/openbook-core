package ca.shiftfocus.krispii.core.models

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._

case class Account(
  id: UUID = UUID.randomUUID(),
  version: Long = 1L,
  userId: UUID,
  status: String = AccountStatus.inactive,
  creditCard: Option[CreditCard] = None,
  subscriptions: IndexedSeq[Subscription] = IndexedSeq.empty[Subscription],
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
  implicit val accountWrites: Writes[Account] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "userId").write[UUID] and
    (__ \ "status").write[String] and
    (__ \ "creditCard").writeNullable[CreditCard] and
    (__ \ "subscriptions").write[IndexedSeq[Subscription]] and
    (__ \ "trialStartedAt").writeNullable[DateTime] and
    (__ \ "activeUntil").writeNullable[DateTime] and
    (__ \ "overdueStartedAt").writeNullable[DateTime] and
    (__ \ "overdueEndedAt").writeNullable[DateTime] and
    (__ \ "overduePlanId").writeNullable[String]
  )(unlift(Account.unapply))

  implicit val accountReads: Reads[Account] = (
    (__ \ "id").read[UUID] and
    (__ \ "version").read[Long] and
    (__ \ "userId").read[UUID] and
    (__ \ "status").read[String] and
    (__ \ "creditCard").readNullable[CreditCard] and
    (__ \ "subscriptions").read[IndexedSeq[Subscription]] and
    (__ \ "trialStartedAt").readNullable[DateTime] and
    (__ \ "activeUntil").readNullable[DateTime] and
    (__ \ "overdueStartedAt").readNullable[DateTime] and
    (__ \ "overdueEndedAt").readNullable[DateTime] and
    (__ \ "overduePlanId").readNullable[String]
  )(Account.apply _)
}
