package ca.shiftfocus.krispii.core.models.tasks

import java.util.UUID

import ca.shiftfocus.krispii.core.models.MediaData
import com.github.mauricio.async.db.RowData
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._

case class CommonTaskSettings(
  title: String = "",
  help: String = "",
  description: String = "",
  instructions: String = "",
  notesAllowed: Boolean = true,
  notesTitle: Option[String] = None,
  responseTitle: Option[String] = None,
  hideResponse: Boolean = false,
  allowGfile: Boolean = true,
  mediaData: Option[MediaData] = None,
  parentId: Option[UUID] = None
)

object CommonTaskSettings {

  /**
   * Overloaded constructor to create a TaskSettings object from
   * a database result row.
   *
   * @param row a RowData object returned from the db.
   * @return a [[CommonTaskSettings]] object
   */
  def apply(row: RowData): CommonTaskSettings = {
    CommonTaskSettings(
      title = row("name").asInstanceOf[String],
      help = row("help_text").asInstanceOf[String],
      description = row("description").asInstanceOf[String],
      instructions = row("instructions").asInstanceOf[String],
      notesAllowed = row("notes_allowed").asInstanceOf[Boolean],
      notesTitle = Option(row("notes_title").asInstanceOf[String]) match {
      case Some(notesTitle) => Some(notesTitle)
      case _ => None
    },
      responseTitle = Option(row("response_title").asInstanceOf[String]) match {
      case Some(responseTitle) => Some(responseTitle)
      case _ => None
    },
      hideResponse = row("hide_response").asInstanceOf[Boolean],
      allowGfile = row("allow_gfile").asInstanceOf[Boolean],
      mediaData = Option(row("media_data")).map(_.asInstanceOf[String]) match {
      case Some(mediaData) => Some(Json.parse(mediaData).as[MediaData])
      case _ => None
    },
      parentId = Option(row("parent_id")).map(_.asInstanceOf[UUID])
    )
  }

  implicit val tsReads: Reads[CommonTaskSettings] = (
    (__ \ "title").read[String] and
    (__ \ "help").read[String] and
    (__ \ "description").read[String] and
    (__ \ "instructions").read[String] and
    (__ \ "notesAllowed").read[Boolean] and
    (__ \ "notesTitle").readNullable[String] and
    (__ \ "responseTitle").readNullable[String] and
    (__ \ "hideResponse").read[Boolean] and
    (__ \ "allowGfile").read[Boolean] and
    (__ \ "mediaData").readNullable[MediaData] and
    (__ \ "parentId").readNullable[UUID]
  )(CommonTaskSettings.apply(_: String, _: String, _: String, _: String, _: Boolean, _: Option[String], _: Option[String], _: Boolean, _: Boolean, _: Option[MediaData], _: Option[UUID]))

  implicit val tsWrites: Writes[CommonTaskSettings] = (
    (__ \ "name").write[String] and
    (__ \ "help").write[String] and
    (__ \ "description").write[String] and
    (__ \ "instructions").write[String] and
    (__ \ "notesAllowed").write[Boolean] and
    (__ \ "notesTitle").writeNullable[String] and
    (__ \ "responseTitle").writeNullable[String] and
    (__ \ "hideResponse").write[Boolean] and
    (__ \ "allowGfile").write[Boolean] and
    (__ \ "mediaData").writeNullable[MediaData] and
    (__ \ "parentId").writeNullable[UUID]
  )(unlift(CommonTaskSettings.unapply))
}
