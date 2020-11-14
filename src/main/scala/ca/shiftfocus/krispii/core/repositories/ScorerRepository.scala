package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.group.Team
import ca.shiftfocus.krispii.core.models.user.Scorer
import com.github.mauricio.async.db.Connection
import scalaz.\/

import scala.concurrent.Future

trait ScorerRepository extends Repository {
  def list(team: Team)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Scorer]]]
}

