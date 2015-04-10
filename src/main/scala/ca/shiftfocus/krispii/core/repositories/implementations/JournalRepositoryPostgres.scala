package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import org.joda.time.format.DateTimeFormat
import play.api.i18n.Messages
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.mauricio.async.db.{Connection, RowData}
import org.joda.time.DateTime

import scala.concurrent.Future
import scalaz._


class JournalRepositoryPostgres (val userRepository: UserRepository,
                                 val projectRepository: ProjectRepository)
  extends JournalRepository with PostgresRepository[JournalEntry] {

  override def constructor(row: RowData): JournalEntry = {
    JournalEntry(
      id        = UUID(row("id").asInstanceOf[Array[Byte]]),
      version   = row("version").asInstanceOf[Long],
      userId    = UUID(row("user_id").asInstanceOf[Array[Byte]]),
      projectId = UUID(row("project_id").asInstanceOf[Array[Byte]]),
      entryType = row("entry_type").asInstanceOf[String],
      item      = row("item").asInstanceOf[String],
      createdAt = row("created_at").asInstanceOf[DateTime],
      updatedAt = row("updated_at").asInstanceOf[DateTime]
    )
  }

  // -- Common query components --------------------------------------------------------------------------------------

  val Table           = "journal"
  val Fields          = "id, version, user_id, project_id, entry_type, item, created_at, updated_at"
  val FieldsWithTable = Fields.split(", ").map({ field => s"${Table}." + field}).mkString(", ")
  val QMarks          = "?, ?, ?, ?, ?, ?, ?, ?"

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
        |WHERE created_at >= ?
  """.stripMargin

  val SelectByEndDate =
    s"""
        |SELECT $Fields
        |FROM $Table
        |WHERE created_at <= ?
  """.stripMargin

  val SelectByPeriod =
    s"""
        |SELECT $Fields
        |FROM $Table
        |WHERE created_at
        |BETWEEN ? and ?
  """.stripMargin

  val SelectById =
    s"""
        |SELECT $Fields
        |FROM $Table
        |WHERE id = ?
  """.stripMargin

  def Insert(suffix: String) =
    s"""
      |INSERT INTO ${Table}_${suffix} ($Fields)
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

  // -- Methods ------------------------------------------------------------------------------------------------------

  /**
   * List Journal Entries by type.
   *
   * @param entryType
   * @param conn
   * @return
   */
  override def list(entryType: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[JournalEntry]]] = {
    queryListJournal(SelectByType, Seq[Any](entryType))
  }

  /**
   * List Journal Entries by user ID.
   *
   * @param userId
   * @param conn
   * @return
   */
  override def list(userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[JournalEntry]]] = {
    queryListJournal(SelectByUser, Seq[Any](userId.bytes))
  }

  /**
   * List Journal Entries by date.
   *
   * @param startDate
   * @param endDate
   * @param conn
   * @return
   */
  override def list(startDate: Option[DateTime] = None, endDate: Option[DateTime] = None)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[JournalEntry]]] = {
    (startDate, endDate) match {
      case (Some(startDate), None) => queryListJournal(SelectByStartDate, Seq[Any](startDate))
      case (None, Some(endDate))   => queryListJournal(SelectByEndDate, Seq[Any](endDate))
      case (Some(startDate), Some(endDate)) => queryListJournal(SelectByPeriod, Seq[Any](startDate, endDate))
      case _ => Future.successful(\/-(IndexedSeq.empty[JournalEntry]))
    }
  }

  /**
   * Find Journal Entry by ID.
   *
   * @param id
   * @param conn
   * @return
   */
  override def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JournalEntry]] = {
    queryOneJournal(SelectById, Seq[Any](id.bytes))
  }

  /**
   * Insert new Journal Entry.
   *
   * @param journalEntry
   * @param conn
   * @return
   */
  override def insert(journalEntry: JournalEntry)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JournalEntry]] = {
    val createdDate  = journalEntry.createdAt
    val formatSuffix = DateTimeFormat.forPattern("YYYYMM")
    val suffix       = formatSuffix.print(createdDate)

    val params = Seq[Any](
      journalEntry.id.bytes, 1, journalEntry.userId.bytes,
      journalEntry.projectId.bytes, journalEntry.entryType, journalEntry.item,
      journalEntry.createdAt, journalEntry.updatedAt
    )

    queryOneJournal(Insert(suffix), params)
  }

  /**
   * Delete Journal Entry by ID.
   *
   * @param journalEntry
   * @param conn
   * @return
   */
  override def delete(journalEntry: JournalEntry)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JournalEntry]] = {
    queryOneJournal(Delete, Seq[Any](journalEntry.id.bytes, journalEntry.version))
  }

  /**
   * Delete all Journal Entries by type.
   *
   * @param entryType
   * @param conn
   * @return
   */
  override def delete(entryType: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[JournalEntry]]] = {
    queryListJournal(DeleteByType, Seq[Any](entryType))
  }

  /**
   * Delete all Journal Entries for the specific user.
   *
   * @param user
   * @param conn
   * @return
   */
  override def delete(user: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[JournalEntry]]] = {
    queryListJournal(DeleteByUser, Seq[Any](user.id.bytes))
  }

  // --- Common methods -----------------------------------------------------------------------------------------------

  // TODO - make translation of date format
  private def buildEntryWithMessage(journalEntry: JournalEntry)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JournalEntry]] = {
    (for {
      user       <- lift(userRepository.find(journalEntry.userId))
      project    <- lift(projectRepository.find(journalEntry.projectId))
      action     = JournalEntry.Action(journalEntry.entryType).action
      formatTime = DateTimeFormat.forPattern("kk:mm:ss")
      formatDate = DateTimeFormat.forPattern("E M d, YYYY")
      message    = Messages("journalEntry.message", user.givenname, user.surname, action, journalEntry.item, formatTime.print(journalEntry.createdAt), formatDate.print(journalEntry.createdAt))
      result     = journalEntry.copy(message = message)
    } yield result).run
  }

  private def queryListJournal(queryText: String, parameters: Seq[Any] = Seq.empty[Any])(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[JournalEntry]]] = {
    (for {
      entryList <- lift(queryList(queryText, parameters))
      result    <- lift(serializedT(entryList)(buildEntryWithMessage(_)))
    } yield result).run
  }

  private def queryOneJournal(queryText: String, parameters: Seq[Any] = Seq.empty[Any])(implicit conn: Connection): Future[\/[RepositoryError.Fail, JournalEntry]] = {
    (for {
      entry  <- lift(queryOne(queryText, parameters))
      result <- lift(buildEntryWithMessage(entry))
    } yield result).run
  }
}
