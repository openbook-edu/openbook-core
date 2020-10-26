package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.User
import ca.shiftfocus.krispii.core.models.group.{Exam, Team}
import com.github.mauricio.async.db.Connection
import scalaz.\/

import scala.concurrent.Future

trait TeamRepository extends Repository {
  val userRepository: UserRepository
  val testRepository: TestRepository

  def list(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Team]]]
  def list(exam: Exam)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Team]]]
  def list(exam: Exam, fetchTests: Boolean)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Team]]]
  def list(user: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Team]]]
  def list(user: User, isScorer: Boolean, fetchTests: Boolean)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Team]]]

  def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Team]]
  def find(id: UUID, fetchTests: Boolean)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Team]]

  def insert(team: Team)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Team]]
  def update(team: Team)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Team]]
  def delete(team: Team)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Team]]

  def addScorer(team: Team, scorer: User, leader: Boolean = false)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]
  def updateScorer(team: Team, scorer: User, leader: Option[Boolean], archived: Option[Boolean], deleted: Option[Boolean])(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]
  def removeScorer(team: Team, scorer: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]

  def addScorers(team: Team, scorerList: IndexedSeq[User], leaderList: IndexedSeq[Boolean] = IndexedSeq(false))(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]
  def removeScorers(team: Team, scorerList: IndexedSeq[User])(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]

  // no need for addTest because tests refer back to Team with teamId

}
