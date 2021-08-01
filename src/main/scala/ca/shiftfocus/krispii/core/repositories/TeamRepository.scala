package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.group.{Exam, Team}
import ca.shiftfocus.krispii.core.models.user.User
import com.github.mauricio.async.db.Connection
import scalaz.\/

import scala.concurrent.Future

trait TeamRepository extends Repository {

  def list(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Team]]]
  def list(exam: Exam)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Team]]]
  def list(user: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Team]]]
  def list(user: User, isScorer: Boolean)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Team]]]

  def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Team]]

  def insert(team: Team)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Team]]
  def update(team: Team)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Team]]
  def delete(team: Team)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Team]]

  // no need for addTest because tests refer back to Team with teamId

}
