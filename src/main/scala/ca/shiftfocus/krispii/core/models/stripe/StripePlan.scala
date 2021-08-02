package ca.shiftfocus.krispii.core.models.stripe

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.JodaWrites._

case class StripePlan(
  id: UUID = UUID.randomUUID(),
  version: Long = 1L,
  stripeId: String,
  title: String,
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime
)

object StripePlan {
  implicit val tagWrites = new Writes[StripePlan] {
    def writes(stripePlan: StripePlan): JsValue = {
      Json.obj(
        "id" -> stripePlan.id,
        "version" -> stripePlan.version,
        "stripeId" -> stripePlan.stripeId,
        "title" -> stripePlan.title,
        "createdAt" -> stripePlan.createdAt,
        "updatedAt" -> stripePlan.updatedAt
      )
    }
  }
}

