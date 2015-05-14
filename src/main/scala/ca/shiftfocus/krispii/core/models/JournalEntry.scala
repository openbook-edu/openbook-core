package ca.shiftfocus.krispii.core.models

import ca.shiftfocus.uuid.UUID
import org.joda.time.DateTime
import play.api.i18n.Messages
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._


case class JournalEntry(
  id: UUID = UUID.random,
  version: Long = 1L,
  userId: UUID,
  projectId: UUID,
  entryType: String,
  item: String,
  message: String = "",
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime
)

object JournalEntry {

  trait Action {
    val entryType: String
    val action: String
  }

  object Action {
    def apply(entryType: String): Action = entryType match {
      case JournalEntryConnect.entryType    => JournalEntryConnect
      case JournalEntryDisconnect.entryType => JournalEntryDisconnect
      case JournalEntryClick.entryType      => JournalEntryClick
      case JournalEntryView.entryType       => JournalEntryView
      case JournalEntryWatch.entryType      => JournalEntryWatch
      case JournalEntryListen.entryType     => JournalEntryListen
      case JournalEntryWrite.entryType      => JournalEntryWrite
      case JournalEntryCreate.entryType     => JournalEntryCreate
      case JournalEntryUpdate.entryType     => JournalEntryUpdate
      case JournalEntryDelete.entryType     => JournalEntryDelete
    }
  }

  object JournalEntryConnect extends Action {
    override val entryType = "connect"
    override val action    = Messages("journalEntry.connect.action")
  }
  object JournalEntryDisconnect extends Action {
    override val entryType = "disconnect"
    override val action    = Messages("journalEntry.Disconnect.action")
  }
  object JournalEntryClick extends Action {
    override val entryType = "click"
    override val action    = Messages("journalEntry.click.action")
  }
  object JournalEntryView extends Action {
    override val entryType = "view"
    override val action    = Messages("journalEntry.view.action")
  }
  object JournalEntryWatch extends Action {
    override val entryType = "watch"
    override val action    = Messages("journalEntry.watch.action")
  }
  object JournalEntryListen extends Action {
    override val entryType = "listen"
    override val action    = Messages("journalEntry.listen.action")
  }
  object JournalEntryWrite extends Action {
    override val entryType = "write"
    override val action    = Messages("journalEntry.write.action")
  }
  object JournalEntryCreate extends Action {
    override val entryType = "create"
    override val action    = Messages("journalEntry.create.action")
  }
  object JournalEntryUpdate extends Action {
    override val entryType = "update"
    override val action    = Messages("journalEntry.update.action")
  }
  object JournalEntryDelete extends Action {
    override val entryType = "delete"
    override val action    = Messages("journalEntry.delete.action")
  }

  implicit val journalEntryReads: Reads[JournalEntry] = (
    (__ \ "id").read[UUID] and
    (__ \ "version").read[Long] and
    (__ \ "userId").read[UUID] and
    (__ \ "projectId").read[UUID] and
    (__ \ "entryType").read[String] and
    (__ \ "item").read[String] and
    (__ \ "message").read[String] and
    (__ \ "createdAt").read[DateTime] and
    (__ \ "updatedAt").read[DateTime]
  )(JournalEntry.apply(_: UUID, _: Long, _: UUID, _: UUID, _: String, _: String, _: String, _: DateTime, _: DateTime))

  implicit val journalEntryWrites: Writes[JournalEntry] = (
    (__ \ "id").write[UUID] and
    (__ \ "version").write[Long] and
    (__ \ "userId").write[UUID] and
    (__ \ "projectId").write[UUID] and
    (__ \ "entryType").write[String] and
    (__ \ "item").write[String] and
    (__ \ "message").write[String] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(JournalEntry.unapply))
}
