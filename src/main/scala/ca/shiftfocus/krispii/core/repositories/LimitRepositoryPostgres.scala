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

  val TeacherLimit =
    """
      |SELECT *
      |FROM teacher_limit
      |WHERE teacher_id = ?
      | AND type = ?
    """.stripMargin

  val CourseLimit =
    """
      |SELECT *
      |FROM course_limit
      |WHERE course_id = ?
      | AND type = ?
    """.stripMargin

  /**
    * Get number of courses that teacher is allowed to have
    *
    * @param teacherId
    * @return
    */
  def getCourseLimit(teacherId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]] = {
    queryOne(TeacherLimit, Seq[Any](teacherId, Limits.course))
  }

  /**
    * Get number of students that course is allowed to have
    *
    * @param courseId
    * @return
    */
  def getStudentLimit(courseId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]] = {
    queryOne(CourseLimit, Seq[Any](courseId, Limits.student))
  }
}


/**
  * Types of limits
  */
object Limits {
  val course = "course"
  val student = "student"
}
