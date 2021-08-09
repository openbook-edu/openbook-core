package ca.shiftfocus.krispii.core.models.stripe

import java.util.UUID

import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, Writes, __}

// This is a subset of the information in the stripe class Subscription
case class StripeSubscription(
  subscriptionId: String, // furnished by Stripe - we can use this instead of a UUID
  version: Long = 1L, // is a lock useful? one krispii customer can only log in to one connection at a time
  customerId: String, // furnished by Stripe
  accountId: UUID, // krispii Account
  planId: String, // stripe ID string, which is constrained to be unique in the table stripe_plans
  currentPeriodStart: Long,
  currentPeriodEnd: Long,
  cancelAtPeriodEnd: Boolean
)

object StripeSubscription {
  implicit val subscriptionWrites: Writes[StripeSubscription] = (
    (__ \ "subscriptionId").write[String] and
    (__ \ "version").write[Long] and
    (__ \ "customerId").write[String] and
    (__ \ "accountId").write[UUID] and
    (__ \ "planId").write[String] and
    (__ \ "currentPeriodStart").write[Long] and
    (__ \ "currentPeriodEnd").write[Long] and
    (__ \ "cancelAtPeriodEnd").write[Boolean]
  )(unlift(StripeSubscription.unapply))

  implicit val subscriptionReads: Reads[StripeSubscription] = (
    (__ \ "subscriptionId").read[String] and
    (__ \ "version").read[Long] and
    (__ \ "customerId").read[String] and
    (__ \ "accountId").read[UUID] and
    (__ \ "planId").read[String] and
    (__ \ "currentPeriodStart").read[Long] and
    (__ \ "currentPeriodEnd").read[Long] and
    (__ \ "cancelAtPeriodEnd").read[Boolean]
  )(StripeSubscription.apply _)
}
