package ca.shiftfocus.krispii.core.models

import com.github.mauricio.async.db.RowData
import ca.shiftfocus.uuid.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._

case class TextComponent(
  id: UUID = UUID.random,
  version: Long = 0,
  ownerId: UUID,
  title: String,
  questions: String,
  thingsToThinkAbout: String,
  content: String,
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
) extends Component

object TextComponent {

  def apply(row: RowData): TextComponent = {
    TextComponent(
      UUID(row("id").asInstanceOf[Array[Byte]]),
      row("version").asInstanceOf[Long],
      UUID(row("ownerId").asInstanceOf[Array[Byte]]),
      row("title").asInstanceOf[String],
      row("questions").asInstanceOf[String],
      row("things_to_think_about").asInstanceOf[String],
      row("content").asInstanceOf[String],
      Some(row("created_at").asInstanceOf[DateTime]),
      Some(row("updated_at").asInstanceOf[DateTime])
    )
  }

  // implicit val textComponentReads: Reads[TextComponent] = (
  //   (__ \ "id").read[UUID] and
  //   (__ \ "version").read[Long] and
  //   (__ \ "projectId").read[UUID] and
  //   (__ \ "name").read[String] and
  //   (__ \ "description").read[String] and
  //   (__ \ "position").read[Int] and
  //   (__ \ "createdAt").readNullable[DateTime] and
  //   (__ \ "updatedAt").readNullable[DateTime]
  // )(TextComponent.apply _)

  implicit val textComponentWrites: Writes[TextComponent] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
      (__ \ "ownerId").write[UUID] and
    (__ \ "title").write[String] and
    (__ \ "questions").write[String] and
    (__ \ "thingsToThinkAbout").write[String] and
    (__ \ "content").write[String] and
    (__ \ "createdAt").writeNullable[DateTime] and
    (__ \ "updatedAt").writeNullable[DateTime]
  )(unlift(TextComponent.unapply))

}


case class TextComponentPost(
  ownerId: UUID,
  title: String,
  questions: Option[String],
  thingsToThinkAbout: Option[String],
  content: String
)
object TextComponentPost {
  implicit val projectPostReads = (
    (__ \ "ownerId").read[UUID] and
    (__ \ "title").read[String] and
    (__ \ "questions").readNullable[String] and
    (__ \ "thingsToThinkAbout").readNullable[String] and
    (__ \ "content").read[String]
  )(TextComponentPost.apply _)
}


case class TextComponentPut(
  version: Long,
  title: String,
  questions: String,
  thingsToThinkAbout: String,
  content: String
)
object TextComponentPut {
  implicit val projectPutReads = (
    (__ \ "version").read[Long] and
    (__ \ "title").read[String] and
    (__ \ "questions").read[String] and
    (__ \ "thingsToThinkAbout").read[String] and
    (__ \ "content").read[String]
  )(TextComponentPut.apply _)
}
