package ca.shiftfocus.krispii.core.lib

import java.util.UUID

import play.api.libs.json._

trait UUIDOps {
  implicit val reads =  new Reads[UUID] {
    def reads(json: JsValue) = {
      try {
        JsSuccess(UUID.fromString(json.as[String]))
      } catch {
        case exp: IllegalArgumentException => JsError(s"The string '${json.as[String]}' is not a valid UUID")
        case exp: Throwable => throw exp
      }
    }
  }

  implicit val writes = new Writes[UUID] {
    def writes(uuid: UUID) = {
      JsString(uuid.toString)
    }
  }
}