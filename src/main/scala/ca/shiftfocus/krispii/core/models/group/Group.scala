package ca.shiftfocus.krispii.core.models.group

import java.awt.Color
import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.json.Writes._
import play.api.libs.json._

trait Group {
  val id: UUID
  val version: Long
  val ownerId: UUID
  val name: String
  val slug: String
  val color: Color
  val enabled: Boolean
  val archived: Boolean
  val deleted: Boolean
  val createdAt: DateTime
  val updatedAt: DateTime
}

object ColorBox {
  implicit val colorReads = new Reads[Color] {
    def reads(json: JsValue) = {
      val mr = (json \ "r").asOpt[Int]
      val mg = (json \ "g").asOpt[Int]
      val mb = (json \ "b").asOpt[Int]

      (mr, mg, mb) match {
        case (Some(r), Some(g), Some(b)) => JsSuccess(new Color(r, g, b))
        case _ => JsError("Invalid format: color must include r, g, and b values as integers.")
      }
    }
  }

  implicit val colorWrites = new Writes[Color] {
    def writes(color: Color): JsValue = {
      Json.obj(
        "r" -> color.getRed,
        "g" -> color.getGreen,
        "b" -> color.getBlue
      )
    }
  }

}
