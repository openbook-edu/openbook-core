package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.Link
import com.github.mauricio.async.db.Connection

import scala.concurrent.Future
import scalaz.\/

trait LimitRepository extends Repository {

  def getCourseLimit(teacherId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]]
  def getStudentLimit(courseId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]]

  def setCourseLimit(teacherId: UUID, limit: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]]
  def setStudentLimit(courseId: UUID, limit: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]]
}
