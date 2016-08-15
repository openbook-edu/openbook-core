package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.UserPreference
import com.github.mauricio.async.db.Connection

import scala.concurrent.Future
import scalaz.\/

trait UserPreferenceRepository extends Repository {
  def list(userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[UserPreference]]]
  def set(userPreference: UserPreference)(implicit conn: Connection): Future[\/[RepositoryError.Fail, UserPreference]]
}
