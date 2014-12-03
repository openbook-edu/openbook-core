package ca.shiftfocus.krispii.core.models

import com.github.mauricio.async.db.RowData
import ca.shiftfocus.uuid.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._


case class VideoComponent(
  id: UUID = UUID.random,
  version: Long = 0,
  title: String,
  questions: String,
  thingsToThinkAbout: String,
  vimeoId: String,
  width: Int,
  height: Int,
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
) extends Component

object VideoComponent {

  def apply(row: RowData): VideoComponent = {
    VideoComponent(
      UUID(row("id").asInstanceOf[Array[Byte]]),
      row("version").asInstanceOf[Long],
      row("title").asInstanceOf[String],
      row("questions").asInstanceOf[String],
      row("things_to_think_about").asInstanceOf[String],
      row("vimeo_id").asInstanceOf[String],
      row("width").asInstanceOf[Int],
      row("height").asInstanceOf[Int],
      Some(row("created_at").asInstanceOf[DateTime]),
      Some(row("updated_at").asInstanceOf[DateTime])
    )
  }

  // implicit val videoComponentReads: Reads[VideoComponent] = (
  //   (__ \ "id").read[UUID] and
  //   (__ \ "version").read[Long] and
  //   (__ \ "projectId").read[UUID] and
  //   (__ \ "name").read[String] and
  //   (__ \ "description").read[String] and
  //   (__ \ "position").read[Int] and
  //   (__ \ "createdAt").readNullable[DateTime] and
  //   (__ \ "updatedAt").readNullable[DateTime]
  // )(VideoComponent.apply _)

  implicit val videoComponentWrites: Writes[VideoComponent] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "title").write[String] and
    (__ \ "questions").write[String] and
    (__ \ "thingsToThinkAbout").write[String] and
    (__ \ "vimeoId").write[String] and
    (__ \ "width").write[Int] and
    (__ \ "height").write[Int] and
    (__ \ "createdAt").writeNullable[DateTime] and
    (__ \ "updatedAt").writeNullable[DateTime]
  )(unlift(VideoComponent.unapply))

}


case class VideoComponentPost(
  title: String,
  questions: Option[String],
  thingsToThinkAbout: Option[String],
  vimeoId: String,
  width: Int,
  height: Int
)
object VideoComponentPost {
  implicit val projectPostReads = (
    (__ \ "title").read[String] and
    (__ \ "questions").readNullable[String] and
    (__ \ "thingsToThinkAbout").readNullable[String] and
    (__ \ "vimeoId").read[String] and
    (__ \ "width").read[Int] and
    (__ \ "height").read[Int]
  )(VideoComponentPost.apply _)
}


case class VideoComponentPut(
  version: Long,
  title: String,
  questions: String,
  thingsToThinkAbout: String,
  vimeoId: String,
  width: Int,
  height: Int
)
object VideoComponentPut {
  implicit val projectPutReads = (
    (__ \ "version").read[Long] and
    (__ \ "title").read[String] and
    (__ \ "questions").read[String] and
    (__ \ "thingsToThinkAbout").read[String] and
    (__ \ "vimeoId").read[String] and
    (__ \ "width").read[Int] and
    (__ \ "height").read[Int]
  )(VideoComponentPut.apply _)
}
