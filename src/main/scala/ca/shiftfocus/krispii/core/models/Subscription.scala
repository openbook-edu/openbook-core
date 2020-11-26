package ca.shiftfocus.krispii.core.models

import java.util.UUID

import play.api.libs.json.{Reads, Writes, __}
import play.api.libs.functional.syntax._

// Information on each Stripe plan a customer has signed up to
case class Subscription(
 id: String, // furnished by stripe - we can use this instead of a UUID
 version: Long = 1L, // is a lock useful? one krispii customer can only log in to one connection at a time
 accountId: UUID, // we only need to link to our krispii Account, not to the stripe customer
 planId: UUID,
 currentPeriodEnd: Long,
 cancelAtPeriodEnd: Boolean
)

object Subscription {
  implicit val subscriptionWrites: Writes[Subscription] = (
    (__ \ "id").write[String] and
    (__ \ "version").write[Long] and
    (__ \ "accountId").write[UUID] and
    (__ \ "planId").write[UUID] and
    (__ \ "currentPeriodEnd").write[Long] and
    (__ \ "cancelAtPeriodEnd").write[Boolean]
  )(unlift(Subscription.unapply))

  implicit val subscriptionReads: Reads[Subscription] = (
    (__ \ "id").read[String] and
      (__ \ "version").read[Long] and
      (__ \ "accountId").read[UUID] and
      (__ \ "planId").read[UUID] and
      (__ \ "currentPeriodEnd").read[Long] and
      (__ \ "cancelAtPeriodEnd").read[Boolean]
    )(Subscription.apply _)
}
