package ca.shiftfocus.krispii.core.models.tasks

import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.RowData
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._

case class CommonTaskSettings(
  dependencyId: Option[UUID] = None,
  title: String = "",
  description: String = "",
  notesAllowed: Boolean = true
)

object CommonTaskSettings {

  /**
   * Overloaded constructor to create a TaskSettings object from
   * a database result row.
   *
   * @param row a [[RowData]] object returned from the db.
   * @return a [[CommonTaskSettings]] object
   */
  def apply(row: RowData): CommonTaskSettings = {
    CommonTaskSettings(
      dependencyId = Option(row("dependency_id").asInstanceOf[Array[Byte]]) match {
        case Some(bytes) => Some(UUID(bytes))
        case _ => None
      },
      title = row("name").asInstanceOf[String],
      description = row("description").asInstanceOf[String],
      notesAllowed = row("notes_allowed").asInstanceOf[Boolean]
    )
  }

  implicit val tsReads: Reads[CommonTaskSettings] = (
    (__ \ "dependencyId").readNullable[UUID] and
      (__ \ "title").read[String] and
      (__ \ "description").read[String] and
      (__ \ "notesAllowed").read[Boolean]
  )(CommonTaskSettings.apply(_: Option[UUID], _: String, _: String, _: Boolean))

  implicit val tsWrites: Writes[CommonTaskSettings] = (
    (__ \ "dependencyId").writeNullable[UUID] and
      (__ \ "name").write[String] and
      (__ \ "description").write[String] and
      (__ \ "notesAllowed").write[Boolean]
    )(unlift(CommonTaskSettings.unapply))

}