package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models._
import com.github.mauricio.async.db.Connection

import scala.concurrent.Future
import scalaz.\/

trait UserLogRepository extends Repository {
  def insert(userLog: UserLog)(implicit conn: Connection): Future[\/[RepositoryError.Fail, UserLog]]
}
