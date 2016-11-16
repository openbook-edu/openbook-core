package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.Gfile
import ca.shiftfocus.krispii.core.models.work.Work
import com.github.mauricio.async.db.{ Connection, RowData }
import org.joda.time.DateTime

import scala.concurrent.Future
import scalaz.\/

class GfileRepositoryPostgres extends GfileRepository with PostgresRepository[Gfile] {
  override val entityName = "Google File"

  override def constructor(row: RowData): Gfile = {
    Gfile(
      id = row("id").asInstanceOf[UUID],
      workId = row("work_id").asInstanceOf[UUID],
      fileId = row("file_id").asInstanceOf[String],
      mimeType = row("mime_type").asInstanceOf[String],
      fileType = row("file_type").asInstanceOf[String],
      fileName = row("file_name").asInstanceOf[String],
      embedUrl = row("embed_url").asInstanceOf[String],
      url = row("url").asInstanceOf[String],
      sharedEmail = Option(row("shared_email").asInstanceOf[String]),
      createdAt = row("created_at").asInstanceOf[DateTime]
    )
  }

  val Table = "gfiles"
  val Fields = "id, work_id, file_id, mime_type, file_type, file_name, embed_url, url, shared_email, created_at"
  val FieldsWithTable = Fields.split(", ").map({ field => s"${Table}." + field }).mkString(", ")
  val QMarks = "?, ?, ?, ?, ?, ?, ?, ?, ?, ?"

  val Select =
    s"""
       |SELECT ${Fields}
       |FROM ${Table}
       |WHERE id = ?
     """.stripMargin

  val SelectByWorkId =
    s"""
       |SELECT ${Fields}
       |FROM ${Table}
       |WHERE work_id = ?
     """.stripMargin

  val Insert =
    s"""
       |INSERT INTO $Table ($Fields)
       |VALUES ($QMarks)
       |RETURNING $Fields
    """.stripMargin

  val Update =
    s"""
       |UPDATE $Table
       |SET shared_email = ?
       |WHERE id = ?
       |RETURNING $Fields
    """.stripMargin

  val Delete =
    s"""
      |DELETE FROM $Table
      |WHERE id = ?
      |RETURNING $Fields
    """.stripMargin

  def get(gFileId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Gfile]] = {
    queryOne(Select, Seq[Any](gFileId))
  }

  def listByWork(work: Work)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Gfile]]] = {
    queryList(SelectByWorkId, Seq[Any](work.id))
  }

  def insert(gfile: Gfile)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Gfile]] = {
    val params = Seq[Any](
      gfile.id, gfile.workId, gfile.fileId, gfile.mimeType, gfile.fileType,
      gfile.fileName, gfile.embedUrl, gfile.url, gfile.sharedEmail, gfile.createdAt
    )

    queryOne(Insert, params)
  }

  def update(gfile: Gfile)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Gfile]] = {
    val params = Seq[Any](gfile.sharedEmail, gfile.id)

    queryOne(Update, params)
  }

  def delete(gfile: Gfile)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Gfile]] = {
    queryOne(Delete, Seq[Any](gfile.id))
  }
}
