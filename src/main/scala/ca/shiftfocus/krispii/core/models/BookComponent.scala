package ca.shiftfocus.krispii.core.models

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Created by ryanez on 16/05/16.
 */
case class BookComponent(
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
  order: Int
)

object BookComponentPost {
  implicit val projectPostReads = (
    (__ \ "ownerId").read[UUID] and
    (__ \ "title").read[String] and
    (__ \ "questions").readNullable[String] and
    (__ \ "thingsToThinkAbout").readNullable[String] and
    (__ \ "fileData").read[MediaData] and
    (__ \ "order").read[Int]
  )(BookComponentPost.apply _)
}

case class BookComponentPut(
  version: Long,
  title: String,
  questions: String,
  thingsToThinkAbout: String,
  fileData: MediaData,
  order: Int
)

object BookComponentPut {
  implicit val projectPutReads = (
    (__ \ "version").read[Long] and
    (__ \ "title").read[String] and
    (__ \ "questions").read[String] and
    (__ \ "thingsToThinkAbout").read[String] and
    (__ \ "fileData").read[MediaData] and
    (__ \ "order").read[Int]
  )(BookComponentPut.apply _)
}
