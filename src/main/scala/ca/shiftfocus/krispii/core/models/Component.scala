package ca.shiftfocus.krispii.core.models

import com.github.mauricio.async.db.RowData
import ca.shiftfocus.uuid.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._

abstract class Component {
  val id: UUID
  val version: Long
  val title: String
  val questions: String
  val thingsToThinkAbout: String
}

object Component {

  def apply(row: RowData): Component = {
    row("type").asInstanceOf[String] match {
      case "audio" => AudioComponent(row)
      case "text" => TextComponent(row)
      case "video" => VideoComponent(row)
    }
  }

  implicit val componentReads = new Reads[Component] {
    def reads(js: JsValue) = {
      JsSuccess((js \ "type").as[String] match {
        case "audio" => AudioComponent(
          id = (js \ "id").as[UUID],
          version = (js \ "version").as[Long],
          title = (js \ "title").as[String],
          questions = (js \ "questions").as[String],
          thingsToThinkAbout = (js \ "thingsToThinkAbout").as[String],
          soundcloudId = (js \ "soundcloudId").as[String],
          createdAt = (js \ "createdAt").asOpt[DateTime],
          updatedAt = (js \ "updatedAt").asOpt[DateTime]
        )
        case "text" => TextComponent(
          id = (js \ "id").as[UUID],
          version = (js \ "version").as[Long],
          title = (js \ "title").as[String],
          questions = (js \ "questions").as[String],
          thingsToThinkAbout = (js \ "thingsToThinkAbout").as[String],
          content = (js \ "content").as[String],
          createdAt = (js \ "createdAt").asOpt[DateTime],
          updatedAt = (js \ "updatedAt").asOpt[DateTime]
        )
        case "video" => VideoComponent(
          id = (js \ "id").as[UUID],
          version = (js \ "version").as[Long],
          title = (js \ "title").as[String],
          questions = (js \ "questions").as[String],
          thingsToThinkAbout = (js \ "thingsToThinkAbout").as[String],
          vimeoId = (js \ "vimeoId").as[String],
          width = (js \ "width").as[Int],
          height = (js \ "height").as[Int],
          createdAt = (js \ "createdAt").asOpt[DateTime],
          updatedAt = (js \ "updatedAt").asOpt[DateTime]
        )
      })
    }
  }

  implicit val componentWrites = new Writes[Component] {
    def writes(component: Component): JsValue = component match {
      case component: VideoComponent => Json.toJson(component).as[JsObject].deepMerge(Json.obj(
        "type" -> "video"
      ))
      case component: AudioComponent => Json.toJson(component).as[JsObject].deepMerge(Json.obj(
        "type" -> "audio"
      ))
      case component: TextComponent  => Json.toJson(component).as[JsObject].deepMerge(Json.obj(
        "type" -> "text"
      ))
    }
  }
}
