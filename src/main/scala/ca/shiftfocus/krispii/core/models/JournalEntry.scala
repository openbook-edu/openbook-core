package ca.shiftfocus.krispii.core.models

import com.github.mauricio.async.db.RowData
import ca.shiftfocus.uuid.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._


case class JournalEntry(
  id: UUID = UUID.random,
  version: Long = 1L,
  userId: UUID,
  projectId: UUID,
  entryType: String,
  message: String,
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
)

object JournalEntry {

  val View = "view"
  val Input  = "input"

//  def apply(row: RowData): JournalEntry = {
//    JournalEntry(
//      UUID(row("id").asInstanceOf[Array[Byte]]),
//      row("version").asInstanceOf[Long],
//      UUID(row("user_id").asInstanceOf[Array[Byte]]),
//      UUID(row("project_id").asInstanceOf[Array[Byte]]),
//      row("entry_type").asInstanceOf[String],
//      row("message").asInstanceOf[String],
//      Some(row("created_at").asInstanceOf[DateTime]),
//      Some(row("updated_at").asInstanceOf[DateTime])
//    )
//  }

  implicit val journalEntryReads: Reads[JournalEntry] = (
    (__ \ "id").read[UUID] and
    (__ \ "version").read[Long] and
    (__ \ "userId").read[UUID] and
    (__ \ "projectId").read[UUID] and
    (__ \ "entryType").read[String] and
    (__ \ "message").read[String] and
    (__ \ "createdAt").readNullable[DateTime] and
    (__ \ "updatedAt").readNullable[DateTime]
  )(JournalEntry.apply(_: UUID, _: Long, _: UUID, _: UUID, _: String, _: String, _: Option[DateTime], _: Option[DateTime]))

  implicit val journalEntryWrites: Writes[JournalEntry] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "userId").write[UUID] and
    (__ \ "projectId").write[UUID] and
    (__ \ "entryType").write[String] and
    (__ \ "message").write[String] and
    (__ \ "createdAt").writeNullable[DateTime] and
    (__ \ "updatedAt").writeNullable[DateTime]
  )(unlift(JournalEntry.unapply))

}

//case class JournalEntryPut(
//  version: Long,
//  message: String
//)
//object JournalEntryPut {
//  implicit val journalEntryPutReads = (
//    (__ \ "version").read[Long] and
//    (__ \ "message").read[String]
//  )(JournalEntryPut.apply _)
//}
