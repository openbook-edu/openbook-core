package ca.shiftfocus.krispii.core.models

import java.util.UUID

import play.api.libs.json._

case class TagCategory(
  id: UUID = UUID.randomUUID(),
  version: Long = 1L,
  name: String,
  lang: String
)

object TagCategory {
  implicit val tagCategoryWrites = new Writes[TagCategory] {
    def writes(tagCategory: TagCategory): JsValue = {
      Json.obj(
        "id" -> tagCategory.id,
        "version" -> tagCategory.version,
        "name" -> tagCategory.name,
        "lang" -> tagCategory.lang
      )
    }
  }
}
