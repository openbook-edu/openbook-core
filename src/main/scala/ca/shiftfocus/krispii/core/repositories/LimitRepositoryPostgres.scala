package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.{ Connection, RowData }

import scala.concurrent.Future
import scalaz.\/

class LimitRepositoryPostgres extends LimitRepository with PostgresRepository[Int] {
  override val entityName = "Link"
  override def constructor(row: RowData): Int = {
    row("limited").asInstanceOf[Int]
  }

  val CourseLimit =
    """
      |SELECT *
      |FROM course_limit
      |WHERE teacher_id = ?
      | AND type = ?
    """.stripMargin

  def getCourseLimit(userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]] = {
    queryOne(CourseLimit, Seq[Any](userId, "course"))
  }
}
