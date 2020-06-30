package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.Team
import ca.shiftfocus.krispii.core.models.course.Exam
import ca.shiftfocus.krispii.core.models.work.Test
import com.github.mauricio.async.db.Connection
import scalaz.\/

import scala.concurrent.Future

trait TestRepository extends Repository {
  val userRepository: UserRepository
  val scoreRepository: ScoreRepository

  def list(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Test]]]
  def list(exam: Exam)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Test]]]
  def list(exam: Exam, fetchScores: Boolean)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Test]]]
  def list(team: Team)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Test]]]
  def list(team: Team, fetchScores: Boolean)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Test]]]

  def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Test]]
  def find(name: String, exam: Exam)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Test]]

  def insert(test: Test)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Test]]
  def update(test: Test)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Test]]
  def delete(test: Test)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Test]]
  def delete(exam: Exam)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Test]]]

  // no need for addScorer(s)   : adding a Score will require that the scorer is currently in the team
  // no need for removeScorer(s): when a scorer is removed from the team, he can add no Score anymore
  // no need to add or remove Scores because they refer back to their Test.
}
