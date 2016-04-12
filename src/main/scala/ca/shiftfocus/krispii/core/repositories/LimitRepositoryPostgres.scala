package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.{ Connection, RowData }

import scala.concurrent.Future
import scalaz.{ -\/, \/, \/- }

class LimitRepositoryPostgres extends LimitRepository with PostgresRepository[Int] {
  override val entityName = "Limit"
  override def constructor(row: RowData): Int = {
    row("limited").asInstanceOf[Int]
  }

  val GetTeacherLimit =
    """
      |SELECT teacher_id, type, limited
      |FROM teacher_limit
      |WHERE teacher_id = ?
      | AND type = ?
    """.stripMargin

  val GetCourseLimit =
    """
      |SELECT course_id, type, limited
      |FROM course_limit
      |WHERE course_id = ?
      | AND type = ?
    """.stripMargin

  val InsertTeacherLimit =
    """
      |INSERT INTO teacher_limit (teacher_id, type, limited)
      |VALUES (?, ?, ?)
      |RETURNING teacher_id, type, limited
    """.stripMargin

  val InsertCourseLimit =
    """
      |INSERT INTO course_limit (course_id, type, limited)
      |VALUES (?, ?, ?)
      |RETURNING course_id, type, limited
    """.stripMargin

  val UpdateTeacherLimit =
    """
      |UPDATE teacher_limit
      |SET limited = ?
      |WHERE teacher_id = ?
      | AND type = ?
      |RETURNING teacher_id, type, limited
    """.stripMargin

  val UpdateCourseLimit =
    """
      |UPDATE course_limit
      |SET limited = ?
      |WHERE course_id = ?
      | AND type = ?
      |RETURNING course_id, type, limited
    """.stripMargin

  /**
   * Get number of courses that teacher is allowed to have
   *
   * @param teacherId
   * @return
   */
  def getCourseLimit(teacherId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]] = {
    queryOne(GetTeacherLimit, Seq[Any](teacherId, Limits.course))
  }

  /**
   * Get number of students that course is allowed to have
   *
   * @param courseId
   * @return
   */
  def getStudentLimit(courseId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]] = {
    queryOne(GetCourseLimit, Seq[Any](courseId, Limits.student))
  }

  /**
   * Upsert course limit for teachers
   */
  def setCourseLimit(teacherId: UUID, limit: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]] = {
    queryOne(UpdateTeacherLimit, Seq[Any](limit, teacherId, Limits.course)).flatMap {
      case \/-(limit) => Future successful \/-(limit)
      case -\/(error: RepositoryError.NoResults) => {
        for {
          insert <- lift(queryOne(InsertTeacherLimit, Seq[Any](teacherId, Limits.course, limit)))
        } yield insert
      }
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Upsert student limit within course
   */
  def setStudentLimit(courseId: UUID, limit: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]] = {
    queryOne(UpdateCourseLimit, Seq[Any](limit, courseId, Limits.student)).flatMap {
      case \/-(limit) => Future successful \/-(limit)
      case -\/(error: RepositoryError.NoResults) => {
        for {
          insert <- lift(queryOne(InsertCourseLimit, Seq[Any](courseId, Limits.student, limit)))
        } yield insert
      }
      case -\/(error) => Future successful -\/(error)
    }
  }
}

/**
 * Types of limits
 */
object Limits {
  val course: String = "course"
  val student: String = "student"
}
