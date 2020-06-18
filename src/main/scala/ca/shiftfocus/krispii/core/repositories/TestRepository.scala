package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.Team
import ca.shiftfocus.krispii.core.models.work.Test
import com.github.mauricio.async.db.Connection
import scalaz.\/

import scala.concurrent.Future

trait TestRepository extends Repository {
  def list(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Test]]]
  def list(team: Team)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Test]]]
}
