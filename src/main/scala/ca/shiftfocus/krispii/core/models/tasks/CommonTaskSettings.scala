package ca.shiftfocus.krispii.core.models.tasks

import java.util.UUID
import com.github.mauricio.async.db.RowData
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._

case class CommonTaskSettings(
  dependencyId: Option[UUID] = None,
  title: String = "",
  description: String = "",
  notesAllowed: Boolean = true,
  notesTitle: Option[String] = None,
  responseTitle: Option[String] = None
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
      dependencyId = Option(row("dependency_id").asInstanceOf[UUID]),
      title = row("name").asInstanceOf[String],
      description = row("description").asInstanceOf[String],
      notesAllowed = row("notes_allowed").asInstanceOf[Boolean],
      notesTitle = Option(row("notes_title").asInstanceOf[String]) match {
        case Some(notesTitle) => Some(notesTitle)
        case _ => None
      },
      responseTitle = Option(row("response_title").asInstanceOf[String]) match {
        case Some(responseTitle) => Some(responseTitle)
        case _ => None
      }
    )
  }

  implicit val tsReads: Reads[CommonTaskSettings] = (
    (__ \ "dependencyId").readNullable[UUID] and
    (__ \ "title").read[String] and
    (__ \ "description").read[String] and
    (__ \ "notesAllowed").read[Boolean] and
    (__ \ "notesTitle").readNullable[String] and
    (__ \ "responseTitle").readNullable[String]
  )(CommonTaskSettings.apply(_: Option[UUID], _: String, _: String, _: Boolean, _: Option[String], _: Option[String]))

  implicit val tsWrites: Writes[CommonTaskSettings] = (
    (__ \ "dependencyId").writeNullable[UUID] and
    (__ \ "name").write[String] and
    (__ \ "description").write[String] and
    (__ \ "notesAllowed").write[Boolean] and
    (__ \ "notesTitle").writeNullable[String] and
    (__ \ "responseTitle").writeNullable[String]
  )(unlift(CommonTaskSettings.unapply))
}