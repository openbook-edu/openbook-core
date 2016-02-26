package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.{ Link }
import com.github.mauricio.async.db.{ RowData, Connection }
import org.joda.time.DateTime
import play.api.Logger

import scala.concurrent.Future
import scalaz.\/

/**
 * Handles student registration links
 */
class LinkRepositoryPostgres extends LinkRepository with PostgresRepository[Link] {
  override val entityName = "Link"

  override def constructor(row: RowData): Link = {
    Link(
      link = row("link").asInstanceOf[String],
      courseId = row("course_id").asInstanceOf[UUID],
      createdAt = row("created_at").asInstanceOf[DateTime]
    )
  }

  val Table = "links"
  val Fields = "link, course_id, created_at"
  val QMarks = "?, ?, ?"

  val Insert = s"""
                  |INSERT INTO $Table ($Fields)
                  |VALUES ($QMarks)
                  |RETURNING $Fields
              """.stripMargin

  val Delete = s"""
                   |DELETE FROM $Table
                   |WHERE link = ?
                   |RETURNING $Fields
                """.stripMargin

  val DeleteByCourse = s"""
                          |DELETE FROM $Table
                          |WHERE course_id = ?
                          |RETURNING $Fields
                       """.stripMargin

  val Find = s"""
                  |SELECT $Fields
                  |FROM $Table
                  |WHERE link = ?
                  |LIMIT 1
              """.stripMargin

  val FindByCourse = s"""
                        |SELECT $Fields
                        |FROM $Table
                        |WHERE course_id = ?
                        |LIMIT 1
              """.stripMargin

  override def create(link: Link)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Link]] = {
    queryOne(Insert, Seq[Any](link.link, link.courseId, new DateTime()))
  }

  override def delete(link: Link)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Link]] = {
    queryOne(Delete, Seq[Any](link.link))
  }

  override def find(link: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Link]] = {
    queryOne(Find, Seq[Any](link))
  }

  def deleteByCourse(courseId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Link]] = {
    queryOne(DeleteByCourse, Seq[Any](courseId))
  }

  override def findByCourse(courseId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Link]] = {
    queryOne(FindByCourse, Seq[Any](courseId))
  }

}