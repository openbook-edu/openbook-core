package ca.shiftfocus.krispii.core.lib

import org.joda.time.{LocalDate, LocalTime}
import org.joda.time.format.DateTimeFormat
import play.api.libs.json._

trait LocalDateTimeJson {

  implicit val localTimeWrites = new Writes[LocalTime] {
    def writes(localTime: LocalTime): JsValue = {
      JsString(localTime.getMillisOfDay().toString())
    }
  }
  implicit val localTimeReads = new Reads[LocalTime] {
    def reads(json: JsValue) = {
      JsSuccess(LocalTime.fromMillisOfDay(json.as[Long]))
    }
  }

  implicit val localDateWrites = new Writes[LocalDate] {
    def writes(localDate: LocalDate): JsValue = {
      JsString(localDate.toString())
    }
  }
  implicit val localDateReads = new Reads[LocalDate] {
    def reads(json: JsValue) = {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val localdate = formatter.parseLocalDate(json.as[String])
      JsSuccess(localdate)
    }
  }

}
