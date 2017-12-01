package ca.shiftfocus.krispii.core.repositories

import java.awt.Color
import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import com.github.mauricio.async.db.{ Connection, RowData }
import org.joda.time.DateTime

import scala.concurrent.Future
import scalaz.\/

class DemoCourseRepositoryPostgres extends DemoCourseRepository with PostgresRepository[DemoCourse] {
  override val entityName = "Demo Course"

  def constructor(row: RowData): DemoCourse = {
    DemoCourse(
      row("id").asInstanceOf[UUID],
      row("version").asInstanceOf[Long],
      row("name").asInstanceOf[String],
      row("land").asInstanceOf[String],
      new Color(Option(row("color").asInstanceOf[Int]).getOrElse(new Color(0, 100, 0).getRGB)),
      row("created_at").asInstanceOf[DateTime],
      row("updated_at").asInstanceOf[DateTime]
    )
  }

  val Table = "demo_demoCourses"
  val Fields = "id, version, name, lang, color, created_at, updated_at"
  val QMarks = Fields.split(", ").map({ field => "?" }).mkString(", ")

  val SelectOneId =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE id = ?
     """.stripMargin

  val SelectOneLang =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE lang = ?
       |LIMIT 1
     """.stripMargin

  val Insert = {
    s"""
       |INSERT INTO $Table ($Fields)
       |VALUES ($QMarks)
       |RETURNING $Fields
    """.stripMargin
  }

  val Update =
    s"""
       |UPDATE $Table
       |SET version = ?, name = ?, color = ?, updated_at = ?
       |WHERE id = ?
       |  AND version = ?
       |RETURNING $Fields
     """.stripMargin

  val Delete =
    s"""
       |DELETE FROM $Table
       |WHERE id = ?
       |  AND version = ?
       |RETURNING $Fields
     """.stripMargin

  def find(id: UUID)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, DemoCourse]] = {
    queryOne(SelectOneId, Seq[Any](id))
  }

  def find(lang: String)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, DemoCourse]] = {
    queryOne(SelectOneLang, Seq[Any](lang))
  }

  def insert(demoCourse: DemoCourse)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, DemoCourse]] = {
    val params = Seq[Any](
      demoCourse.id, 1, demoCourse.name, demoCourse.lang, demoCourse.color.getRGB, new DateTime, new DateTime
    )

    queryOne(Insert, params)
  }

  def update(demoCourse: DemoCourse)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, DemoCourse]] = {
    val params = Seq[Any](
      demoCourse.version + 1, demoCourse.name, demoCourse.color.getRGB, new DateTime, demoCourse.id, demoCourse.version
    )

    queryOne(Update, params)
  }

  def delete(demoCourse: DemoCourse)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, DemoCourse]] = {
    val params = Seq[Any](
      demoCourse.id, demoCourse.version
    )

    queryOne(Delete, params)
  }
}
