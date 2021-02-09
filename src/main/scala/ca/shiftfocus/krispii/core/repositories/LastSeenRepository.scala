package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.Connection
import org.joda.time.DateTime
import scalaz.\/
import scala.concurrent.Future

trait LastSeenRepository extends Repository {
  def find(readerId: UUID, entityId: UUID, entityType: String, peek: Boolean)(implicit conn: Connection): Future[RepositoryError.Fail \/ DateTime]
  def put(readerId: UUID, entityId: UUID, entityType: String, seen: DateTime)(implicit conn: Connection): Future[RepositoryError.Fail \/ DateTime]
  def delete(readerId: UUID, entityId: UUID, entityType: String)(implicit conn: Connection): Future[RepositoryError.Fail \/ DateTime]
}
