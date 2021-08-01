package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.group.Team
import ca.shiftfocus.krispii.core.models.user.{Scorer, User, FutureScorer}
import com.github.mauricio.async.db.Connection
import scalaz.\/

import scala.concurrent.Future

trait ScorerRepository extends Repository {
  def list(team: Team)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Scorer]]]

  def addScorer(team: Team, scorer: User, leader: Boolean = false)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]
  def updateScorer(team: Team, scorer: Scorer, leader: Option[Boolean], archived: Option[Boolean], deleted: Option[Boolean])(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]
  def removeScorer(team: Team, scorerId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]

  def addScorers(team: Team, userList: IndexedSeq[FutureScorer])(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]
  /*def removeScorers(team: Team, scorerIdList: IndexedSeq[UUID])(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]*/
}

