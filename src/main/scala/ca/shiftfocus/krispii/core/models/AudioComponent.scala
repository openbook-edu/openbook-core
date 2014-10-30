package ca.shiftfocus.krispii.core.models

import com.github.mauricio.async.db.RowData
import ca.shiftfocus.krispii.core.lib.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class AudioComponent(
  id: UUID = UUID.random,
  version: Long = 0,
  title: String,
  questions: String,
  thingsToThinkAbout: String,
  soundcloudId: String,
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
) extends Component

object AudioComponent {

  def apply(row: RowData): AudioComponent = {
    AudioComponent(
      UUID(row("id").asInstanceOf[Array[Byte]]),
      row("version").asInstanceOf[Long],
      row("title").asInstanceOf[String],
      row("questions").asInstanceOf[String],
      row("things_to_think_about").asInstanceOf[String],
      row("soundcloud_id").asInstanceOf[String],
      Some(row("created_at").asInstanceOf[DateTime]),
      Some(row("updated_at").asInstanceOf[DateTime])
    )
  }

  // implicit val audioComponentReads: Reads[AudioComponent] = (
  //   (__ \ "id").read[UUID] and
  //   (__ \ "version").read[Long] and
  //   (__ \ "projectId").read[UUID] and
  //   (__ \ "name").read[String] and
  //   (__ \ "description").read[String] and
  //   (__ \ "position").read[Int] and
  //   (__ \ "createdAt").readNullable[DateTime] and
  //   (__ \ "updatedAt").readNullable[DateTime]
  // )(AudioComponent.apply _)

  implicit val audioComponentWrites: Writes[AudioComponent] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "title").write[String] and
    (__ \ "questions").write[String] and
    (__ \ "thingsToThinkAbout").write[String] and
    (__ \ "soundcloudId").write[String] and
    (__ \ "createdAt").writeNullable[DateTime] and
    (__ \ "updatedAt").writeNullable[DateTime]
  )(unlift(AudioComponent.unapply))

}


case class AudioComponentPost(
  title: String,
  questions: Option[String],
  thingsToThinkAbout: Option[String],
  soundcloudId: String
)
object AudioComponentPost {
  implicit val projectPostReads = (
    (__ \ "title").read[String] and
    (__ \ "questions").readNullable[String] and
    (__ \ "thingsToThinkAbout").readNullable[String] and
    (__ \ "soundcloudId").read[String]
  )(AudioComponentPost.apply _)
}


case class AudioComponentPut(
  version: Long,
  title: String,
  questions: Option[String],
  thingsToThinkAbout: Option[String],
  soundcloudId: String
)
object AudioComponentPut {
  implicit val projectPutReads = (
    (__ \ "version").read[Long] and
    (__ \ "title").read[String] and
    (__ \ "questions").readNullable[String] and
    (__ \ "thingsToThinkAbout").readNullable[String] and
    (__ \ "soundcloudId").read[String]
  )(AudioComponentPut.apply _)
}
