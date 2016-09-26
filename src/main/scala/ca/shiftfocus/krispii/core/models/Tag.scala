package ca.shiftfocus.krispii.core.models

import java.util.UUID

import ca.shiftfocus.krispii.core.helpers.NaturalOrderComparator
import org.joda.time.DateTime
import play.Logger
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class Tag(
    name: String,
    lang: String,
    category: String,
    frequency: Int
) extends Ordered[Tag] {
  /**
    * Natural order tag name comparation
    * @param that
    * @return
    */
  def compare(that: Tag): Int = {
    val comp = new NaturalOrderComparator
    comp.compare(this.name, that.name)
  }
}

object Tag {
  implicit val tagWrites = new Writes[Tag] {
    def writes(tag: Tag): JsValue = {
      Json.obj(
        "name" -> tag.name,
        "lang" -> tag.lang,
        "category" -> tag.category,
        "frequency" -> tag.frequency
      )
    }
  }
}

