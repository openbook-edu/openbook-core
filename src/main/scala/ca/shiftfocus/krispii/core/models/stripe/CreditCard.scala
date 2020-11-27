package ca.shiftfocus.krispii.core.models.stripe

import java.util.UUID

import play.api.libs.functional.syntax._
import play.api.libs.json._

// this is a subset of the information in stripe's Card class
case class CreditCard(
  customerId: String, // furnished by stripe - we can use this instead of a UUID
  version: Long = 1L, // is a lock useful? one krispii client can only log in to one connection at a time
  accountId: UUID,
  email: String, // do we really need to store email and name separately from User?
  givenname: String,
  surname: String,
  exp_month: Option[Int] = None,
  exp_year: Option[Int] = None,
  brand: String = "",
  last4: String = ""
/*currency: String,
  livemode: Boolean,
  delinquent: Boolean,
  description: String = "",
  balance: Long = 0 // account_balance */
)

object CreditCard {
  implicit val creditCardWrites: Writes[CreditCard] = (
    (__ \ "customerId").write[String] and
    (__ \ "version").write[Long] and
    (__ \ "accountId").write[UUID] and
    (__ \ "email").write[String] and
    (__ \ "givenname").write[String] and
    (__ \ "surname").write[String] and
    (__ \ "exp_month").writeNullable[Int] and
    (__ \ "exp_year").writeNullable[Int] and
    (__ \ "brand").write[String] and
    (__ \ "last4").write[String]
  /*(__ \ "currency").write[String] and
    (__ \ "livemode").write[Boolean] and
    (__ \ "delinquent").write[Boolean] and
    (__ \ "description").write[String] and
    (__ \ "balance").write[Long] */
  )(unlift(CreditCard.unapply))

  implicit val creditCardReads: Reads[CreditCard] = (
    (__ \ "customerId").read[String] and
    (__ \ "version").read[Long] and
    (__ \ "accountId").read[UUID] and
    (__ \ "email").read[String] and
    (__ \ "givenname").read[String] and
    (__ \ "surname").read[String] and
    (__ \ "exp_month").readNullable[Int] and
    (__ \ "exp_year").readNullable[Int] and
    (__ \ "brand").read[String] and
    (__ \ "last4").read[String]
  )(CreditCard.apply _)
}
