package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import com.github.mauricio.async.db.{ Connection, RowData }

import scala.concurrent.Future
import scalaz.\/

class DemoProjectRepositoryPostgres extends DemoProjectRepository with PostgresRepository[DemoProject] {
  override val entityName = "Demo Project"

  def constructor(row: RowData): DemoProject = {
    DemoProject(
      row("land").asInstanceOf[String],
      row("project_id").asInstanceOf[UUID]
    )
  }

  val Table = "demo_projects"
  val Fields = "lang, project_id"
  val QMarks = Fields.split(", ").map({ field => "?" }).mkString(", ")

  val SelectOne =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE lang = ?
       |  AND project_id = ?
       |LIMIT 1
     """.stripMargin

  val SelectAllLang =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE lang = ?
     """.stripMargin

  val Insert = {
    s"""
       |INSERT INTO $Table ($Fields)
       |VALUES ($QMarks)
       |RETURNING $Fields
    """.stripMargin
  }

  val Delete =
    s"""
       |DELETE FROM $Table
       |WHERE lang = ?
       |  AND project_id = ?
       |RETURNING $Fields
     """.stripMargin

  def find(projectId: UUID, lang: String)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, DemoProject]] = {
    queryOne(SelectOne, Seq[Any](lang, projectId))
  }

  def list(lang: String)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[DemoProject]]] = {
    queryList(SelectAllLang, Seq[Any](lang))
  }

  def insert(demoProject: DemoProject)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, DemoProject]] = {
    val params = Seq[Any](
      demoProject.lang, demoProject.projectId
    )

    queryOne(Insert, params)
  }

  def delete(demoProject: DemoProject)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, DemoProject]] = {
    val params = Seq[Any](
      demoProject.lang, demoProject.projectId
    )

    queryOne(Delete, params)
  }
}
