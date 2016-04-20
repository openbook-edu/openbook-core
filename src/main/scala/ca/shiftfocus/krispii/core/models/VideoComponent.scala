package ca.shiftfocus.krispii.core.models

import com.github.mauricio.async.db.RowData
import java.util.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._

case class VideoComponent(
  id: UUID = UUID.randomUUID,
  version: Long = 1L,
  ownerId: UUID,
  title: String,
  questions: String,
  thingsToThinkAbout: String,
  videoData: MediaData = MediaData(),
  width: Int,
  height: Int,
  order: Int,
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime
) extends Component

object VideoComponent {

  val Youtube = "youtube"
  val Vimeo = "vimeo"
  val S3 = "s3"

  implicit val videoComponentWrites: Writes[VideoComponent] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "ownerId").write[UUID] and
    (__ \ "title").write[String] and
    (__ \ "questions").write[String] and
    (__ \ "thingsToThinkAbout").write[String] and
    (__ \ "videoData").write[MediaData] and
    (__ \ "width").write[Int] and
    (__ \ "height").write[Int] and
    (__ \ "order").write[Int] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(VideoComponent.unapply))

}

case class VideoComponentPost(
  ownerId: UUID,
  title: String,
  questions: Option[String],
  thingsToThinkAbout: Option[String],
  videoData: MediaData,
  width: Int,
  height: Int,
  order: Int
)
object VideoComponentPost {
  implicit val projectPostReads = (
    (__ \ "ownerId").read[UUID] and
    (__ \ "title").read[String] and
    (__ \ "questions").readNullable[String] and
    (__ \ "thingsToThinkAbout").readNullable[String] and
    (__ \ "videoData").read[MediaData] and
    (__ \ "width").read[Int] and
    (__ \ "height").read[Int] and
    (__ \ "order").read[Int]
  )(VideoComponentPost.apply _)
}

case class VideoComponentPut(
  version: Long,
  title: String,
  questions: String,
  thingsToThinkAbout: String,
  videoData: MediaData,
  width: Int,
  height: Int,
  order: Int
)
object VideoComponentPut {
  implicit val projectPutReads = (
    (__ \ "version").read[Long] and
    (__ \ "title").read[String] and
    (__ \ "questions").read[String] and
    (__ \ "thingsToThinkAbout").read[String] and
    (__ \ "videoData").read[MediaData] and
    (__ \ "width").read[Int] and
    (__ \ "height").read[Int] and
    (__ \ "order").read[Int]
  )(VideoComponentPut.apply _)
}
