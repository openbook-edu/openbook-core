package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.Link
import com.github.mauricio.async.db.Connection

import scala.concurrent.Future
import scalaz.\/

trait LimitRepository extends Repository {
  def getCourseLimit(userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]]
}
