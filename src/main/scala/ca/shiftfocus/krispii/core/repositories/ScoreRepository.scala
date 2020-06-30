package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.course.Exam
import ca.shiftfocus.krispii.core.models.work.{Score, Test}
import ca.shiftfocus.krispii.core.models.{Team, User}
import com.github.mauricio.async.db.Connection
import scalaz.\/

import scala.concurrent.Future

trait ScoreRepository extends Repository {
  def list(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Score]]]
  // def list(exam: Exam)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Score]]]
  def list(exam: Exam, scorer: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Score]]]
  def list(team: Team, scorer: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Score]]]
  def list(test: Test)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Score]]]

  def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Score]]
  def find(testId: UUID, scorerId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Score]]

  def insert(score: Score)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Score]]
  def update(score: Score)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Score]]
  def delete(score: Score)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Score]]
  def delete(test: Test)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Score]]]
}
