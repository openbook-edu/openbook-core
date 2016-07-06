package ca.shiftfocus.krispii.core.models

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class TagCategory(
  name: String,
  lang: String
) {}

object TagCategory {
  implicit val tagCategoryWrites = new Writes[TagCategory] {
    def writes(tagCategory: TagCategory): JsValue = {
      Json.obj(
        "name" -> tagCategory.name
      )
    }
  }
}
