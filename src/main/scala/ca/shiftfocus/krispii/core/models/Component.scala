package ca.shiftfocus.krispii.core.models

import com.github.mauricio.async.db.RowData
import ca.shiftfocus.uuid.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._

abstract class Component {
  val id: UUID
  val version: Long
  val ownerId: UUID
  val title: String
  val questions: String
  val thingsToThinkAbout: String
  val createdAt: DateTime
  val updatedAt: DateTime
}

object Component {

  implicit val componentReads = new Reads[Component] {
    def reads(js: JsValue) = {
      JsSuccess((js \ "type").as[String] match {
        case "audio" => AudioComponent(
          id = (js \ "id").as[UUID],
          version = (js \ "version").as[Long],
          ownerId = (js \ "ownerId").as[UUID],
          title = (js \ "title").as[String],
          questions = (js \ "questions").as[String],
          thingsToThinkAbout = (js \ "thingsToThinkAbout").as[String],
          soundcloudId = (js \ "soundcloudId").as[String],
          createdAt = (js \ "createdAt").as[DateTime],
          updatedAt = (js \ "updatedAt").as[DateTime]
        )
        case "text" => TextComponent(
          id = (js \ "id").as[UUID],
          version = (js \ "version").as[Long],
          ownerId = (js \ "ownerId").as[UUID],
          title = (js \ "title").as[String],
          questions = (js \ "questions").as[String],
          thingsToThinkAbout = (js \ "thingsToThinkAbout").as[String],
          content = (js \ "content").as[String],
          createdAt = (js \ "createdAt").as[DateTime],
          updatedAt = (js \ "updatedAt").as[DateTime]
        )
        case "video" => VideoComponent(
          id = (js \ "id").as[UUID],
          version = (js \ "version").as[Long],
          ownerId = (js \ "ownerId").as[UUID],
          title = (js \ "title").as[String],
          questions = (js \ "questions").as[String],
          thingsToThinkAbout = (js \ "thingsToThinkAbout").as[String],
          vimeoId = (js \ "vimeoId").as[String],
          width = (js \ "width").as[Int],
          height = (js \ "height").as[Int],
          createdAt = (js \ "createdAt").as[DateTime],
          updatedAt = (js \ "updatedAt").as[DateTime]
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
