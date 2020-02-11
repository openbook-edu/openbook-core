package ca.shiftfocus.krispii.core.models

import java.util.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.JodaReads._

abstract class Component {
  val id: UUID
  val version: Long
  val ownerId: UUID
  val title: String
  val questions: String
  val thingsToThinkAbout: String
  val order: Int
  val isPrivate: Boolean
  val description: String
  val parentId: Option[UUID]
  val parentVersion: Option[Long]
  val createdAt: DateTime
  val updatedAt: DateTime
}

object Component {

  val Audio = "audio"
  val Image = "image"
  val Google = "google"
  val Microsoft = "microsoft"
  val Text = "text"
  val Video = "video"
  val GenericHTML = "generic_html"
  val Rubric = "rubric"
  val Book = "book"

  implicit val componentReads = new Reads[Component] {
    def reads(js: JsValue) = {
      JsSuccess((js \ "type").as[String] match {
        case Component.Audio => AudioComponent(
          id = (js \ "id").as[UUID],
          version = (js \ "version").as[Long],
          ownerId = (js \ "ownerId").as[UUID],
          title = (js \ "title").as[String],
          questions = (js \ "questions").as[String],
          thingsToThinkAbout = (js \ "thingsToThinkAbout").as[String],
          mediaData = (js \ "audio_data").as[MediaData],
          order = (js \ "order").as[Int],
          isPrivate = (js \ "isPrivate").as[Boolean],
          description = (js \ "description").as[String],
          parentId = Option((js \ "parentId").as[UUID]),
          parentVersion = Option((js \ "parentVersion").as[Long]),
          createdAt = (js \ "createdAt").as[DateTime],
          updatedAt = (js \ "updatedAt").as[DateTime]
        )
        case Component.Google => GoogleComponent(
          id = (js \ "id").as[UUID],
          version = (js \ "version").as[Long],
          ownerId = (js \ "ownerId").as[UUID],
          title = (js \ "title").as[String],
          questions = (js \ "questions").as[String],
          thingsToThinkAbout = (js \ "thingsToThinkAbout").as[String],
          mediaData = (js \ "google_data").as[MediaData],
          order = (js \ "order").as[Int],
          isPrivate = (js \ "isPrivate").as[Boolean],
          description = (js \ "description").as[String],
          parentId = Option((js \ "parentId").as[UUID]),
          parentVersion = Option((js \ "parentVersion").as[Long]),
          createdAt = (js \ "createdAt").as[DateTime],
          updatedAt = (js \ "updatedAt").as[DateTime]
        )
        case Component.Microsoft => MicrosoftComponent(
          id = (js \ "id").as[UUID],
          version = (js \ "version").as[Long],
          ownerId = (js \ "ownerId").as[UUID],
          title = (js \ "title").as[String],
          questions = (js \ "questions").as[String],
          thingsToThinkAbout = (js \ "thingsToThinkAbout").as[String],
          mediaData = (js \ "microsoft_data").as[MediaData],
          order = (js \ "order").as[Int],
          isPrivate = (js \ "isPrivate").as[Boolean],
          description = (js \ "description").as[String],
          parentId = Option((js \ "parentId").as[UUID]),
          parentVersion = Option((js \ "parentVersion").as[Long]),
          createdAt = (js \ "createdAt").as[DateTime],
          updatedAt = (js \ "updatedAt").as[DateTime]
        )
        case Component.Image => ImageComponent(
          id = (js \ "id").as[UUID],
          version = (js \ "version").as[Long],
          ownerId = (js \ "ownerId").as[UUID],
          title = (js \ "title").as[String],
          questions = (js \ "questions").as[String],
          thingsToThinkAbout = (js \ "thingsToThinkAbout").as[String],
          mediaData = (js \ "image_data").as[MediaData],
          order = (js \ "order").as[Int],
          isPrivate = (js \ "isPrivate").as[Boolean],
          description = (js \ "description").as[String],
          parentId = Option((js \ "parentId").as[UUID]),
          parentVersion = Option((js \ "parentVersion").as[Long]),
          createdAt = (js \ "createdAt").as[DateTime],
          updatedAt = (js \ "updatedAt").as[DateTime]
        )
        case Component.Text => TextComponent(
          id = (js \ "id").as[UUID],
          version = (js \ "version").as[Long],
          ownerId = (js \ "ownerId").as[UUID],
          title = (js \ "title").as[String],
          questions = (js \ "questions").as[String],
          thingsToThinkAbout = (js \ "thingsToThinkAbout").as[String],
          content = (js \ "content").as[String],
          order = (js \ "order").as[Int],
          isPrivate = (js \ "isPrivate").as[Boolean],
          description = (js \ "description").as[String],
          parentId = Option((js \ "parentId").as[UUID]),
          parentVersion = Option((js \ "parentVersion").as[Long]),
          createdAt = (js \ "createdAt").as[DateTime],
          updatedAt = (js \ "updatedAt").as[DateTime]
        )
        case Component.GenericHTML => GenericHTMLComponent(
          id = (js \ "id").as[UUID],
          version = (js \ "version").as[Long],
          ownerId = (js \ "ownerId").as[UUID],
          title = (js \ "title").as[String],
          questions = (js \ "questions").as[String],
          thingsToThinkAbout = (js \ "thingsToThinkAbout").as[String],
          htmlContent = (js \ "content").as[String],
          order = (js \ "order").as[Int],
          isPrivate = (js \ "isPrivate").as[Boolean],
          description = (js \ "description").as[String],
          parentId = Option((js \ "parentId").as[UUID]),
          parentVersion = Option((js \ "parentVersion").as[Long]),
          createdAt = (js \ "createdAt").as[DateTime],
          updatedAt = (js \ "updatedAt").as[DateTime]
        )
        case Component.Rubric => RubricComponent(
          id = (js \ "id").as[UUID],
          version = (js \ "version").as[Long],
          ownerId = (js \ "ownerId").as[UUID],
          title = (js \ "title").as[String],
          questions = (js \ "questions").as[String],
          thingsToThinkAbout = (js \ "thingsToThinkAbout").as[String],
          rubricContent = (js \ "content").as[String],
          order = (js \ "order").as[Int],
          isPrivate = (js \ "isPrivate").as[Boolean],
          description = (js \ "description").as[String],
          parentId = Option((js \ "parentId").as[UUID]),
          parentVersion = Option((js \ "parentVersion").as[Long]),
          createdAt = (js \ "createdAt").as[DateTime],
          updatedAt = (js \ "updatedAt").as[DateTime]
        )
        case Component.Book => BookComponent(
          id = (js \ "id").as[UUID],
          version = (js \ "version").as[Long],
          ownerId = (js \ "ownerId").as[UUID],
          title = (js \ "title").as[String],
          questions = (js \ "questions").as[String],
          thingsToThinkAbout = (js \ "thingsToThinkAbout").as[String],
          mediaData = (js \ "file_data").as[MediaData],
          order = (js \ "order").as[Int],
          isPrivate = (js \ "isPrivate").as[Boolean],
          description = (js \ "description").as[String],
          parentId = Option((js \ "parentId").as[UUID]),
          parentVersion = Option((js \ "parentVersion").as[Long]),
          createdAt = (js \ "createdAt").as[DateTime],
          updatedAt = (js \ "updatedAt").as[DateTime]
        )
        case Component.Video => VideoComponent(
          id = (js \ "id").as[UUID],
          version = (js \ "version").as[Long],
          ownerId = (js \ "ownerId").as[UUID],
          title = (js \ "title").as[String],
          questions = (js \ "questions").as[String],
          thingsToThinkAbout = (js \ "thingsToThinkAbout").as[String],
          mediaData = (js \ "video_data").as[MediaData],
          width = (js \ "width").as[Int],
          height = (js \ "height").as[Int],
          order = (js \ "order").as[Int],
          isPrivate = (js \ "isPrivate").as[Boolean],
          description = (js \ "description").as[String],
          parentId = Option((js \ "parentId").as[UUID]),
          parentVersion = Option((js \ "parentVersion").as[Long]),
          createdAt = (js \ "createdAt").as[DateTime],
          updatedAt = (js \ "updatedAt").as[DateTime]
        )
      })
    }
  }

  implicit val componentWrites = new Writes[Component] {
    def writes(component: Component): JsValue = component match {
      case component: GoogleComponent => Json.toJson(component).as[JsObject].deepMerge(Json.obj(
        "type" -> Component.Google
      ))
      case component: MicrosoftComponent => Json.toJson(component).as[JsObject].deepMerge(Json.obj(
        "type" -> Component.Microsoft
      ))
      case component: VideoComponent => Json.toJson(component).as[JsObject].deepMerge(Json.obj(
        "type" -> Component.Video
      ))
      case component: AudioComponent => Json.toJson(component).as[JsObject].deepMerge(Json.obj(
        "type" -> Component.Audio
      ))
      case component: ImageComponent => Json.toJson(component).as[JsObject].deepMerge(Json.obj(
        "type" -> Component.Image
      ))
      case component: TextComponent => Json.toJson(component).as[JsObject].deepMerge(Json.obj(
        "type" -> Component.Text
      ))
      case component: GenericHTMLComponent => Json.toJson(component).as[JsObject].deepMerge(Json.obj(
        "type" -> Component.GenericHTML
      ))
      case component: RubricComponent => Json.toJson(component).as[JsObject].deepMerge(Json.obj(
        "type" -> Component.Rubric
      ))
      case component: BookComponent => Json.toJson(component).as[JsObject].deepMerge(Json.obj(
        "type" -> Component.Book
      ))
    }
  }
}
