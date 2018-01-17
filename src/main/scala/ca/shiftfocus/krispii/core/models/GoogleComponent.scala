package ca.shiftfocus.krispii.core.models

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json.Writes._
import play.api.libs.json._

case class GoogleComponent(
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
      case anotherGoogleComponent: GoogleComponent => {
        this.id == anotherGoogleComponent.id &&
          this.version == anotherGoogleComponent.version &&
          this.ownerId == anotherGoogleComponent.ownerId &&
          this.title == anotherGoogleComponent.title &&
          this.questions == anotherGoogleComponent.questions &&
          this.thingsToThinkAbout == anotherGoogleComponent.thingsToThinkAbout &&
          this.mediaData == anotherGoogleComponent.mediaData &&
          this.order == anotherGoogleComponent.order
        this.isPrivate == anotherGoogleComponent.isPrivate
      }
      case _ => false
    }
  }
}

object GoogleComponent {

  val SoundCloud = "sound_cloud"
  val S3 = "s3"

  implicit val googleComponentWrites: Writes[GoogleComponent] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "ownerId").write[UUID] and
    (__ \ "title").write[String] and
    (__ \ "questions").write[String] and
    (__ \ "thingsToThinkAbout").write[String] and
    (__ \ "googleData").write[MediaData] and
    (__ \ "order").write[Int] and
    (__ \ "isPrivate").write[Boolean] and
    (__ \ "description").write[String] and
    (__ \ "parentId").writeNullable[UUID] and
    (__ \ "parentVersion").writeNullable[Long] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(GoogleComponent.unapply))

}

case class GoogleComponentPost(
  ownerId: UUID,
  title: String,
  questions: Option[String],
  thingsToThinkAbout: Option[String],
  googleData: MediaData,
  order: Int,
  isPrivate: Boolean,
  description: String,
  parentId: Option[UUID],
  parentVersion: Option[Long]
)

object GoogleComponentPost {
  implicit val projectPostReads = (
    (__ \ "ownerId").read[UUID] and
    (__ \ "title").read[String] and
    (__ \ "questions").readNullable[String] and
    (__ \ "thingsToThinkAbout").readNullable[String] and
    (__ \ "googleData").read[MediaData] and
    (__ \ "order").read[Int] and
    (__ \ "isPrivate").read[Boolean] and
    (__ \ "description").read[String] and
    (__ \ "parentId").readNullable[UUID] and
    (__ \ "parentVersion").readNullable[Long]
  )(GoogleComponentPost.apply _)
}

case class GoogleComponentPut(
  version: Long,
  title: String,
  questions: Option[String],
  thingsToThinkAbout: Option[String],
  googleData: MediaData,
  order: Int,
  isPrivate: Boolean,
  description: String,
  parentId: Option[UUID],
  parentVersion: Option[Long]
)

object GoogleComponentPut {
  implicit val projectPutReads = (
    (__ \ "version").read[Long] and
    (__ \ "title").read[String] and
    (__ \ "questions").readNullable[String] and
    (__ \ "thingsToThinkAbout").readNullable[String] and
    (__ \ "googleData").read[MediaData] and
    (__ \ "order").read[Int] and
    (__ \ "isPrivate").read[Boolean] and
    (__ \ "description").read[String] and
    (__ \ "parentId").readNullable[UUID] and
    (__ \ "parentVersion").readNullable[Long]
  )(GoogleComponentPut.apply _)
}
