package ca.shiftfocus.krispii.core.models

import com.github.mauricio.async.db.RowData
import java.util.UUID
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
  val order: Int
  val createdAt: DateTime
  val updatedAt: DateTime
}

object Component {

  val Audio = "audio"
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
          createdAt = (js \ "createdAt").as[DateTime],
          updatedAt = (js \ "updatedAt").as[DateTime]
        )
      })
    }
  }

  implicit val componentWrites = new Writes[Component] {
    def writes(component: Component): JsValue = component match {
      case component: VideoComponent => Json.toJson(component).as[JsObject].deepMerge(Json.obj(
        "type" -> Component.Video
      ))
      case component: AudioComponent => Json.toJson(component).as[JsObject].deepMerge(Json.obj(
        "type" -> Component.Audio
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

case class MediaData(
    host: Option[String] = None,
    data: Option[String] = None,
    dataType: Option[String] = None,
    // We store the size of a file in Bytes
    size: Option[Long] = None
) {
  override def equals(anotherObject: Any): Boolean = {
    anotherObject match {
      case anotherMediaData: MediaData => {
        this.host == anotherMediaData.host &&
          this.data == anotherMediaData.data &&
          this.dataType == anotherMediaData.dataType &&
          this.size == anotherMediaData.size
      }
      case _ => false
    }
  }
}

object MediaData {
  implicit val reads = new Reads[MediaData] {
    def reads(json: JsValue) = {
      JsSuccess(
        MediaData(
          (json \ "host").asOpt[String],
          (json \ "data").asOpt[String],
          (json \ "dataType").asOpt[String],
          (json \ "size").asOpt[Long]
        )
      )
    }
  }
  implicit val writes = new Writes[MediaData] {
    def writes(mediaData: MediaData): JsValue = {
      Json.obj(
        "host" -> mediaData.host,
        "data" -> mediaData.data,
        "dataType" -> mediaData.dataType,
        "size" -> mediaData.size
      )
    }
  }
}

