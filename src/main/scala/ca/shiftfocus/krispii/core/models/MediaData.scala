package ca.shiftfocus.krispii.core.models

import play.api.libs.json._

case class MediaData(
    host: Option[String] = None,
    data: Option[String] = None,
    dataType: Option[String] = None,
    serverFileName: Option[String] = None,
    hash: Option[String] = None,
    // We store the size of a file in Bytes
    size: Option[Long] = None,
    isPublic: Option[Boolean] = None,
    thumbName: Option[String] = None,
    thumbSize: Option[Long] = None
) {
  override def equals(anotherObject: Any): Boolean = {
    anotherObject match {
      case anotherMediaData: MediaData => {
        this.host == anotherMediaData.host &&
          this.data == anotherMediaData.data &&
          this.dataType == anotherMediaData.dataType &&
          this.serverFileName == anotherMediaData.serverFileName &&
          this.hash == anotherMediaData.hash &&
          this.size == anotherMediaData.size &&
          this.isPublic == anotherMediaData.isPublic &&
          this.thumbName == anotherMediaData.thumbName &&
          this.thumbSize == anotherMediaData.thumbSize
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
          (json \ "serverFileName").asOpt[String],
          (json \ "hash").asOpt[String],
          (json \ "size").asOpt[Long],
          (json \ "isPublic").asOpt[Boolean],
          (json \ "thumbName").asOpt[String],
          (json \ "thumbSize").asOpt[Long]
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
        "serverFileName" -> mediaData.serverFileName,
        "hash" -> mediaData.hash,
        "size" -> mediaData.size,
        "isPublic" -> mediaData.isPublic,
        "thumbName" -> mediaData.thumbName,
        "thumbSize" -> mediaData.thumbSize
      )
    }
  }
}

