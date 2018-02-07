package ca.shiftfocus.krispii.core.models

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json.Writes._
import play.api.libs.json._

case class MicrosoftComponent(
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
      case anotherMicrosoftComponent: MicrosoftComponent => {
        this.id == anotherMicrosoftComponent.id &&
          this.version == anotherMicrosoftComponent.version &&
          this.ownerId == anotherMicrosoftComponent.ownerId &&
          this.title == anotherMicrosoftComponent.title &&
          this.questions == anotherMicrosoftComponent.questions &&
          this.thingsToThinkAbout == anotherMicrosoftComponent.thingsToThinkAbout &&
          this.mediaData == anotherMicrosoftComponent.mediaData &&
          this.order == anotherMicrosoftComponent.order
        this.isPrivate == anotherMicrosoftComponent.isPrivate
      }
      case _ => false
    }
  }
}

object MicrosoftComponent {
  implicit val microsoftComponentWrites: Writes[MicrosoftComponent] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "ownerId").write[UUID] and
    (__ \ "title").write[String] and
    (__ \ "questions").write[String] and
    (__ \ "thingsToThinkAbout").write[String] and
    (__ \ "microsoftData").write[MediaData] and
    (__ \ "order").write[Int] and
    (__ \ "isPrivate").write[Boolean] and
    (__ \ "description").write[String] and
    (__ \ "parentId").writeNullable[UUID] and
    (__ \ "parentVersion").writeNullable[Long] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(MicrosoftComponent.unapply))

}

case class MicrosoftComponentPost(
  ownerId: UUID,
  title: String,
  questions: Option[String],
  thingsToThinkAbout: Option[String],
  microsoftData: MediaData,
  order: Int,
  isPrivate: Boolean,
  description: String,
  parentId: Option[UUID],
  parentVersion: Option[Long]
)

object MicrosoftComponentPost {
  implicit val projectPostReads = (
    (__ \ "ownerId").read[UUID] and
    (__ \ "title").read[String] and
    (__ \ "questions").readNullable[String] and
    (__ \ "thingsToThinkAbout").readNullable[String] and
    (__ \ "microsoftData").read[MediaData] and
    (__ \ "order").read[Int] and
    (__ \ "isPrivate").read[Boolean] and
    (__ \ "description").read[String] and
    (__ \ "parentId").readNullable[UUID] and
    (__ \ "parentVersion").readNullable[Long]
  )(MicrosoftComponentPost.apply _)
}

case class MicrosoftComponentPut(
  version: Long,
  title: String,
  questions: Option[String],
  thingsToThinkAbout: Option[String],
  microsoftData: MediaData,
  order: Int,
  isPrivate: Boolean,
  description: String,
  parentId: Option[UUID],
  parentVersion: Option[Long]
)

object MicrosoftComponentPut {
  implicit val projectPutReads = (
    (__ \ "version").read[Long] and
    (__ \ "title").read[String] and
    (__ \ "questions").readNullable[String] and
    (__ \ "thingsToThinkAbout").readNullable[String] and
    (__ \ "microsoftData").read[MediaData] and
    (__ \ "order").read[Int] and
    (__ \ "isPrivate").read[Boolean] and
    (__ \ "description").read[String] and
    (__ \ "parentId").readNullable[UUID] and
    (__ \ "parentVersion").readNullable[Long]
  )(MicrosoftComponentPut.apply _)
}

