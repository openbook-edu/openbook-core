package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.Connection
import scalaz.\/

import scala.concurrent.Future

trait CopiesCountRepository extends Repository {
  def get(entityType: String, entityId: String)(implicit conn: Connection): Future[RepositoryError.Fail \/ BigInt]
  def inc(entityType: String, entityId: String, n: Int = 1)(implicit conn: Connection): Future[RepositoryError.Fail \/ BigInt]
  def dec(entityType: String, entityId: String, n: Int = 1)(implicit conn: Connection): Future[RepositoryError.Fail \/ BigInt]
  def delete(entityType: String, entityId: String)(implicit conn: Connection): Future[RepositoryError.Fail \/ Unit]
}
