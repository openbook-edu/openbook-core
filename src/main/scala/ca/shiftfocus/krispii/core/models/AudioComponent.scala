package ca.shiftfocus.krispii.core.models

import com.github.mauricio.async.db.RowData
import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.Writes._
import sun.audio.AudioData

case class AudioComponent(
    id: UUID = UUID.randomUUID,
    version: Long = 1L,
    ownerId: UUID,
    title: String,
    questions: String,
    thingsToThinkAbout: String,
    data: MediaData = MediaData(),
    order: Int,
    createdAt: DateTime = new DateTime,
    updatedAt: DateTime = new DateTime
) extends Component with DataCarrier {
  override def equals(anotherObject: Any): Boolean = {
    anotherObject match {
      case anotherAudioComponent: AudioComponent => {
        this.id == anotherAudioComponent.id &&
          this.version == anotherAudioComponent.version &&
          this.ownerId == anotherAudioComponent.ownerId &&
          this.title == anotherAudioComponent.title &&
          this.questions == anotherAudioComponent.questions &&
          this.thingsToThinkAbout == anotherAudioComponent.thingsToThinkAbout &&
          this.data == anotherAudioComponent.data &&
          this.order == anotherAudioComponent.order
      }
      case _ => false
    }
  }
}

object AudioComponent {

  val SoundCloud = "sound_cloud"
  val S3 = "s3"

  implicit val audioComponentWrites: Writes[AudioComponent] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "ownerId").write[UUID] and
    (__ \ "title").write[String] and
    (__ \ "questions").write[String] and
    (__ \ "thingsToThinkAbout").write[String] and
    (__ \ "audioData").write[MediaData] and
    (__ \ "order").write[Int] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(AudioComponent.unapply))

}

case class AudioComponentPost(
  ownerId: UUID,
  title: String,
  questions: Option[String],
  thingsToThinkAbout: Option[String],
  audioData: MediaData,
  order: Int
)
object AudioComponentPost {
  implicit val projectPostReads = (
    (__ \ "ownerId").read[UUID] and
    (__ \ "title").read[String] and
    (__ \ "questions").readNullable[String] and
    (__ \ "thingsToThinkAbout").readNullable[String] and
    (__ \ "audioData").read[MediaData] and
    (__ \ "order").read[Int]
  )(AudioComponentPost.apply _)
}

case class AudioComponentPut(
  version: Long,
  title: String,
  questions: Option[String],
  thingsToThinkAbout: Option[String],
  audioData: MediaData,
  order: Int
)
object AudioComponentPut {
  implicit val projectPutReads = (
    (__ \ "version").read[Long] and
    (__ \ "title").read[String] and
    (__ \ "questions").readNullable[String] and
    (__ \ "thingsToThinkAbout").readNullable[String] and
    (__ \ "audioData").read[MediaData] and
    (__ \ "order").read[Int]
  )(AudioComponentPut.apply _)
}
