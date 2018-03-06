package ca.shiftfocus.krispii.core.models

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._

case class BookComponent(
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
) extends Component with DataCarrier

object BookComponent {

  implicit val bookComponentWrites: Writes[BookComponent] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "ownerId").write[UUID] and
    (__ \ "title").write[String] and
    (__ \ "questions").write[String] and
    (__ \ "thingsToThinkAbout").write[String] and
    (__ \ "fileData").write[MediaData] and
    (__ \ "order").write[Int] and
    (__ \ "isPrivate").write[Boolean] and
    (__ \ "description").write[String] and
    (__ \ "parentId").writeNullable[UUID] and
    (__ \ "parentVersion").writeNullable[Long] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(BookComponent.unapply))

}

case class BookComponentPost(
  ownerId: UUID,
  title: String,
  questions: Option[String],
  thingsToThinkAbout: Option[String],
  fileData: MediaData,
  order: Int,
  isPrivate: Boolean,
  description: String,
  parentId: Option[UUID],
  parentVersion: Option[Long]
)

object BookComponentPost {
  implicit val projectPostReads = (
    (__ \ "ownerId").read[UUID] and
    (__ \ "title").read[String] and
    (__ \ "questions").readNullable[String] and
    (__ \ "thingsToThinkAbout").readNullable[String] and
    (__ \ "fileData").read[MediaData] and
    (__ \ "order").read[Int] and
    (__ \ "isPrivate").read[Boolean] and
    (__ \ "description").read[String] and
    (__ \ "parentId").readNullable[UUID] and
    (__ \ "parentVersion").readNullable[Long]
  )(BookComponentPost.apply _)
}

case class BookComponentPut(
  version: Long,
  title: String,
  questions: String,
  thingsToThinkAbout: String,
  fileData: MediaData,
  order: Int,
  isPrivate: Boolean,
  description: String,
  parentId: Option[UUID],
  parentVersion: Option[Long]
)

object BookComponentPut {
  implicit val projectPutReads = (
    (__ \ "version").read[Long] and
    (__ \ "title").read[String] and
    (__ \ "questions").read[String] and
    (__ \ "thingsToThinkAbout").read[String] and
    (__ \ "fileData").read[MediaData] and
    (__ \ "order").read[Int] and
    (__ \ "isPrivate").read[Boolean] and
    (__ \ "description").read[String] and
    (__ \ "parentId").readNullable[UUID] and
    (__ \ "parentVersion").readNullable[Long]
  )(BookComponentPut.apply _)
}
