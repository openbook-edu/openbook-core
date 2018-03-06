package ca.shiftfocus.krispii.core.models

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json.Writes._
import play.api.libs.json._
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._

case class ImageComponent(
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
      case anotherImageComponent: ImageComponent => {
        this.id == anotherImageComponent.id &&
          this.version == anotherImageComponent.version &&
          this.ownerId == anotherImageComponent.ownerId &&
          this.title == anotherImageComponent.title &&
          this.questions == anotherImageComponent.questions &&
          this.thingsToThinkAbout == anotherImageComponent.thingsToThinkAbout &&
          this.mediaData == anotherImageComponent.mediaData &&
          this.order == anotherImageComponent.order
        this.isPrivate == anotherImageComponent.isPrivate
      }
      case _ => false
    }
  }
}

object ImageComponent {

  val SoundCloud = "sound_cloud"
  val S3 = "s3"

  implicit val imageComponentWrites: Writes[ImageComponent] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "ownerId").write[UUID] and
    (__ \ "title").write[String] and
    (__ \ "questions").write[String] and
    (__ \ "thingsToThinkAbout").write[String] and
    (__ \ "imageData").write[MediaData] and
    (__ \ "order").write[Int] and
    (__ \ "isPrivate").write[Boolean] and
    (__ \ "description").write[String] and
    (__ \ "parentId").writeNullable[UUID] and
    (__ \ "parentVersion").writeNullable[Long] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(ImageComponent.unapply))

}

case class ImageComponentPost(
  ownerId: UUID,
  title: String,
  questions: Option[String],
  thingsToThinkAbout: Option[String],
  imageData: MediaData,
  order: Int,
  isPrivate: Boolean,
  description: String,
  parentId: Option[UUID],
  parentVersion: Option[Long]
)

object ImageComponentPost {
  implicit val projectPostReads = (
    (__ \ "ownerId").read[UUID] and
    (__ \ "title").read[String] and
    (__ \ "questions").readNullable[String] and
    (__ \ "thingsToThinkAbout").readNullable[String] and
    (__ \ "imageData").read[MediaData] and
    (__ \ "order").read[Int] and
    (__ \ "isPrivate").read[Boolean] and
    (__ \ "description").read[String] and
    (__ \ "parentId").readNullable[UUID] and
    (__ \ "parentVersion").readNullable[Long]
  )(ImageComponentPost.apply _)
}

case class ImageComponentPut(
  version: Long,
  title: String,
  questions: Option[String],
  thingsToThinkAbout: Option[String],
  imageData: MediaData,
  order: Int,
  isPrivate: Boolean,
  description: String,
  parentId: Option[UUID],
  parentVersion: Option[Long]
)

object ImageComponentPut {
  implicit val projectPutReads = (
    (__ \ "version").read[Long] and
    (__ \ "title").read[String] and
    (__ \ "questions").readNullable[String] and
    (__ \ "thingsToThinkAbout").readNullable[String] and
    (__ \ "imageData").read[MediaData] and
    (__ \ "order").read[Int] and
    (__ \ "isPrivate").read[Boolean] and
    (__ \ "description").read[String] and
    (__ \ "parentId").readNullable[UUID] and
    (__ \ "parentVersion").readNullable[Long]
  )(ImageComponentPut.apply _)
}
