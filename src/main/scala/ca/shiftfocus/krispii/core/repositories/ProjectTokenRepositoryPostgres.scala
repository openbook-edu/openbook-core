package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.ProjectToken
import com.github.mauricio.async.db.{ Connection, RowData }
import org.joda.time.DateTime

import scala.concurrent.Future
import scalaz.\/

class ProjectTokenRepositoryPostgres extends ProjectTokenRepository with PostgresRepository[ProjectToken] {

  override val entityName = "Project Token"

  override def constructor(row: RowData): ProjectToken = {
    ProjectToken(
      projectId = row("project_id").asInstanceOf[UUID],
      email = row("email").asInstanceOf[String],
      token = row("token").asInstanceOf[String],
      createdAt = row("created_at").asInstanceOf[DateTime]
    )
  }

  val Table = "project_tokens"
  val Fields = "project_id, email, token, created_at"
  val QMarks = "?, ?, ?, ?"

  val Select =
    s"""
     |SELECT $Fields
     |FROM $Table
     |WHERE token = ?
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
     |WHERE token = ?
     |RETURNING $Fields
     """.stripMargin

  def get(token: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, ProjectToken]] = {
    queryOne(Select, Seq[Any](token))
  }

  def insert(projectToken: ProjectToken)(implicit conn: Connection): Future[\/[RepositoryError.Fail, ProjectToken]] = {
    val params = Seq[Any](
      projectToken.projectId, projectToken.email, projectToken.token, projectToken.createdAt
    )

    queryOne(Insert, params)
  }

  def delete(projectToken: ProjectToken)(implicit conn: Connection): Future[\/[RepositoryError.Fail, ProjectToken]] = {
    queryOne(Delete, Seq[Any](projectToken.token))
  }
}
