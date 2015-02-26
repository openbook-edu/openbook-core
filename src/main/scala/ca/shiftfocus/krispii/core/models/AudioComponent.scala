package ca.shiftfocus.krispii.core.models

import com.github.mauricio.async.db.RowData
import ca.shiftfocus.uuid.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class AudioComponent(
  id: UUID = UUID.random,
  version: Long = 0,
  ownerId: UUID,
  title: String,
  questions: String,
  thingsToThinkAbout: String,
  soundcloudId: String,
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime
) extends Component

object AudioComponent {

  implicit val audioComponentWrites: Writes[AudioComponent] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "ownerId").write[UUID] and
    (__ \ "title").write[String] and
    (__ \ "questions").write[String] and
    (__ \ "thingsToThinkAbout").write[String] and
    (__ \ "soundcloudId").write[String] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(AudioComponent.unapply))

}


case class AudioComponentPost(
  ownerId: UUID,
  title: String,
  questions: Option[String],
  thingsToThinkAbout: Option[String],
  soundcloudId: String
)
object AudioComponentPost {
  implicit val projectPostReads = (
    (__ \ "ownerId").read[UUID] and
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
