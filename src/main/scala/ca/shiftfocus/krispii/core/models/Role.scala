package ca.shiftfocus.krispii.core.models

import com.github.mauricio.async.db.RowData
import ca.shiftfocus.uuid.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._


case class Role(
  id: UUID = UUID.random,
  version: Long = 0,
  name: String,
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
)

object Role {

  implicit val roleReads: Reads[Role] = (
    (__ \ "id").read[UUID] and
    (__ \ "version").read[Long] and
    (__ \ "name").read[String] and
    (__ \ "createdAt").readNullable[DateTime] and
    (__ \ "updatedAt").readNullable[DateTime]
  )(Role.apply(_: UUID, _: Long, _: String, _: Option[DateTime], _: Option[DateTime]))

  implicit val roleWrites: Writes[Role] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "name").write[String] and
    (__ \ "createdAt").writeNullable[DateTime] and
    (__ \ "updatedAt").writeNullable[DateTime]
  )(unlift(Role.unapply))

  def apply(row: RowData): Role = rowToModel(row)
  def rowToModel(row: RowData): Role = {
    Role(
      UUID(row("id").asInstanceOf[Array[Byte]]),
      row("version").asInstanceOf[Long],
      row("name").asInstanceOf[String],
      Some(row("created_at").asInstanceOf[DateTime]),
      Some(row("updated_at").asInstanceOf[DateTime])
    )
  }
}
