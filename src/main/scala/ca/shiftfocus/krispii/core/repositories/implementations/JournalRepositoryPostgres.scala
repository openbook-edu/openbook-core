package ca.shiftfocus.krispii.core.repositories.implementations

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.{User, JournalEntry}
import ca.shiftfocus.krispii.core.repositories.{JournalRepository, PostgresRepository}
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.{Connection, RowData}
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime

import scala.concurrent.Future
import scalaz.\/


class JournalRepositoryPostgres
  extends JournalRepository with PostgresRepository[JournalEntry] {

  private val config     = ConfigFactory.load()
  private val useJournal = config.getBoolean("journal.logging.use")

  override def constructor(row: RowData): JournalEntry = {
    JournalEntry(
      UUID(row("id").asInstanceOf[Array[Byte]]),
      row("version").asInstanceOf[Long],
      UUID(row("user_id").asInstanceOf[Array[Byte]]),
      UUID(row("project_id").asInstanceOf[Array[Byte]]),
      row("entry_type").asInstanceOf[String],
      row("message").asInstanceOf[String],
      Some(row("created_at").asInstanceOf[DateTime]),
      Some(row("updated_at").asInstanceOf[DateTime])
    )
  }

  // -- Common query components --------------------------------------------------------------------------------------

  val Table           = "journals"
  val Fields          = "id, version, user_id, project_id, entry_type, message, created_at, updated_at"
  val FieldsWithTable = Fields.split(", ").map({ field => s"${Table}." + field}).mkString(", ")

  val QMarks  = "?, ?, ?, ?, ?, ?, ?, ?"
//  val GroupByUser = s"${Table}.user_id"

  // -- CRUD Operations -----------------------------------------------------------------------------------------------

  val SelectByType =
    s"""
        |SELECT $Fields
        |FROM $Table
        |WHERE entry_type = ?
  """.stripMargin

  val SelectByUser =
    s"""
        |SELECT $Fields
        |FROM $Table
        |WHERE user_id = ?
  """.stripMargin

  val SelectByStartDate =
    s"""
        |SELECT $Fields
        |FROM $Table
        |WHERE user_id = ?
  """.stripMargin

  val SelectByEndDate =
    s"""
        |SELECT $Fields
        |FROM $Table
        |WHERE user_id = ?
  """.stripMargin

  val SelectByPeriod =
    s"""
        |SELECT $Fields
        |FROM $Table
        |WHERE user_id = ?
  """.stripMargin

  val SelectById =
    s"""
        |SELECT $Fields
        |FROM $Table
        |WHERE id = ?
  """.stripMargin

  val Insert =
    s"""
      |INSERT INTO $Table ($Fields)
      |VALUES ($QMarks)
      |RETURNING $Fields
    """.stripMargin

  val Delete =
    s"""
      |DELETE FROM $Table
      |WHERE id = ?
      |  AND version = ?
      |RETURNING $Fields
     """.stripMargin

  val DeleteByType =
    s"""
      |DELETE FROM $Table
      |WHERE entry_type = ?
      |RETURNING $Fields
     """.stripMargin

  val DeleteByUser =
    s"""
      |DELETE FROM $Table
      |WHERE user_id = ?
      |RETURNING $Fields
     """.stripMargin

  override def list(entryType: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[JournalEntry]]] = {
    queryList(SelectByType, Seq[Any](entryType))
  }

  override def list(userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[JournalEntry]]] = {
    queryList(SelectByUser, Seq[Any](userId.bytes))
  }

  override def list(startDate: Option[DateTime], endDate: Option[DateTime])(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[JournalEntry]]] = {
    (startDate, endDate) match {
      case (startDate: Option[DateTime], None) => queryList(SelectByStartDate, Seq[Any](startDate))
      case (None, endDate: Option[DateTime]) => queryList(SelectByEndDate, Seq[Any](endDate))
      case (startDate: Option[DateTime], endDate: Option[DateTime]) => queryList(SelectByPeriod, Seq[Any](startDate, endDate))
    }
  }

  override def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JournalEntry]] = {
    queryOne(SelectById, Seq[Any](id.bytes))
  }

  override def insert(journalEntry: JournalEntry)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JournalEntry]] = {
    val params = Seq[Any](
      journalEntry.id.bytes, 1, journalEntry.userId.bytes,
      journalEntry.projectId.bytes, journalEntry.entryType, journalEntry.message,
      new DateTime, new DateTime
    )

    queryOne(Insert, params)
  }

  override def delete(journalEntry: JournalEntry)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JournalEntry]] = {
    queryOne(Delete, Seq[Any](journalEntry.id.bytes, journalEntry.version))
  }

  override def delete(entryType: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JournalEntry]] = {
    queryOne(DeleteByType, Seq[Any](entryType))
  }

  override def delete(user: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JournalEntry]] = {
    queryOne(DeleteByUser, Seq[Any](user.id.bytes))
  }
}
