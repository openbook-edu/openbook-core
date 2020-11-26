package ca.shiftfocus.krispii.core.models

import java.util.UUID

import play.api.libs.functional.syntax._
import play.api.libs.json._

// Stripe customer information - we need the stripe name, email and some credit card info,
// and we need to link this back to Account
case class Customer(
  id: String,  // furnished by stripe - we can use this instead of a UUID
  version: Long = 1L, // is a lock useful? one krispii customer can only log in to one connection at a time
  accountId: UUID,
  email: String,
  givenname: String,
  surname: String,
  exp_month: String,
  exp_year: String,
  brand: String,
  last4: String
  /*currency: String,
  livemode: Boolean,
  delinquent: Boolean,
  description: String = "",
  balance: Long = 0 // account_balance */
)

object Customer {
  implicit val customerWrites: Writes[Customer] = (
    (__ \ "id").write[String] and
    (__ \ "version").write[Long] and
    (__ \ "accountId").write[UUID] and
    (__ \ "email").write[String] and
    (__ \ "givenname").write[String] and
    (__ \ "surname").write[String] and
    (__ \ "exp_month").write[String] and
    (__ \ "exp_year").write[String] and
    (__ \ "brand").write[String] and
    (__ \ "last4").write[String]
    /*(__ \ "currency").write[String] and
    (__ \ "livemode").write[Boolean] and
    (__ \ "delinquent").write[Boolean] and
    (__ \ "description").write[String] and
    (__ \ "balance").write[Long] */
  )(unlift(Customer.unapply))

  implicit val customerReads: Reads[Customer] = (
    (__ \ "id").read[String] and
      (__ \ "version").read[Long] and
      (__ \ "accountId").read[UUID] and
      (__ \ "email").read[String] and
      (__ \ "givenname").read[String] and
      (__ \ "surname").read[String] and
      (__ \ "exp_month").read[String] and
      (__ \ "exp_year").read[String] and
      (__ \ "brand").read[String] and
      (__ \ "last4").read[String]
    )(Customer.apply _)
}
