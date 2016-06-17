package ca.shiftfocus.krispii.core.models

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class Tag(
  id: UUID,
  name: String
) {}

object Tag {
  implicit val tagWrites = new Writes[Tag] {
    def writes(tag: Tag): JsValue = {
      Json.obj(
        "name" -> tag.name
      )
    }
  }
}

