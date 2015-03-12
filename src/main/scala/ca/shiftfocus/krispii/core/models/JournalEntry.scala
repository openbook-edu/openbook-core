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
  remoteAddress: String = "",
  requestUri: String = "",
  userAgent: String = "",
  userId: UUID,
  message: String,
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
)

object JournalEntry {

  def apply(row: RowData): JournalEntry = {
    JournalEntry(
      UUID(row("id").asInstanceOf[Array[Byte]]),
      row("version").asInstanceOf[Long],
      row("remoteAddress").asInstanceOf[String],
      row("requestUri").asInstanceOf[String],
      row("userAgent").asInstanceOf[String],
      UUID(row("userId").asInstanceOf[Array[Byte]]),
      row("message").asInstanceOf[String],
      Some(row("created_at").asInstanceOf[DateTime]),
      Some(row("updated_at").asInstanceOf[DateTime])
    )
  }

  implicit val journalEntryReads: Reads[JournalEntry] = (
    (__ \ "id").read[UUID] and
    (__ \ "version").read[Long] and
    (__ \ "remoteAddress").read[String] and
    (__ \ "requestUri").read[String] and
    (__ \ "userAgent").read[String] and
    (__ \ "userId").read[UUID] and
    (__ \ "message").read[String] and
    (__ \ "createdAt").readNullable[DateTime] and
    (__ \ "updatedAt").readNullable[DateTime]
  )(JournalEntry.apply(_: UUID, _: Long, _: String, _: String, _: String, _: UUID, _: String, _: Option[DateTime], _: Option[DateTime]))

  implicit val journalEntryWrites: Writes[JournalEntry] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "remoteAddress").write[String] and
    (__ \ "requestUri").write[String] and
    (__ \ "userAgent").write[String] and
    (__ \ "userId").write[UUID] and
    (__ \ "message").write[String] and
    (__ \ "createdAt").writeNullable[DateTime] and
    (__ \ "updatedAt").writeNullable[DateTime]
  )(unlift(JournalEntry.unapply))

}

case class JournalEntryPut(
  version: Long,
  message: String
)
object JournalEntryPut {
  implicit val journalEntryPutReads = (
    (__ \ "version").read[Long] and
    (__ \ "message").read[String]
  )(JournalEntryPut.apply _)
}
