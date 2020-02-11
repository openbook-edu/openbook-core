package ca.shiftfocus.krispii.core.models

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.json.JodaWrites._

case class AudioComponent(
    id: UUID = UUID.randomUUID,
    version: Long = 1L,
    ownerId: UUID,
    title: String,
    questions: String,
    thingsToThinkAbout: String,
    mediaData: MediaData = MediaData(),
    order: Int,
    isPrivate: Boolean = false,
    description: String = "",
    parentId: Option[UUID] = None,
    parentVersion: Option[Long] = None,
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
          this.mediaData == anotherAudioComponent.mediaData &&
          this.order == anotherAudioComponent.order
        this.isPrivate == anotherAudioComponent.isPrivate
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
    (__ \ "isPrivate").write[Boolean] and
    (__ \ "description").write[String] and
    (__ \ "parentId").writeNullable[UUID] and
    (__ \ "parentVersion").writeNullable[Long] and
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
  order: Int,
  isPrivate: Boolean,
  description: String,
  parentId: Option[UUID],
  parentVersion: Option[Long]
)
object AudioComponentPost {
  implicit val projectPostReads = (
    (__ \ "ownerId").read[UUID] and
    (__ \ "title").read[String] and
    (__ \ "questions").readNullable[String] and
    (__ \ "thingsToThinkAbout").readNullable[String] and
    (__ \ "audioData").read[MediaData] and
    (__ \ "order").read[Int] and
    (__ \ "isPrivate").read[Boolean] and
    (__ \ "description").read[String] and
    (__ \ "parentId").readNullable[UUID] and
    (__ \ "parentVersion").readNullable[Long]
  )(AudioComponentPost.apply _)
}

case class AudioComponentPut(
  version: Long,
  title: String,
  questions: Option[String],
  thingsToThinkAbout: Option[String],
  audioData: MediaData,
  order: Int,
  isPrivate: Boolean,
  description: String,
  parentId: Option[UUID],
  parentVersion: Option[Long]
)
object AudioComponentPut {
  implicit val projectPutReads = (
    (__ \ "version").read[Long] and
    (__ \ "title").read[String] and
    (__ \ "questions").readNullable[String] and
    (__ \ "thingsToThinkAbout").readNullable[String] and
    (__ \ "audioData").read[MediaData] and
    (__ \ "order").read[Int] and
    (__ \ "isPrivate").read[Boolean] and
    (__ \ "description").read[String] and
    (__ \ "parentId").readNullable[UUID] and
    (__ \ "parentVersion").readNullable[Long]
  )(AudioComponentPut.apply _)
}
