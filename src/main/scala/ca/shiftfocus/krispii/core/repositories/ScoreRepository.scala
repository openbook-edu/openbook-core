package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.Connection
import ca.shiftfocus.krispii.core.models._

import ca.shiftfocus.krispii.core.models.work.Work

import scala.concurrent.Future
import scalaz.\/

trait ScoreRepository extends Repository {
  val cacheRepository: CacheRepository

  /**
   * Some of the usual CRUD functions for the "work_scorers" table.
   * Scores are not really an independent construct, just a link between users,
   * works and grades, so they don't have their own IDs.
   */
  def list(scorer: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Score]]]
  def list(work: Work)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Score]]]

  def find(work: Work, scorer: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Score]]

  def insert(scorer: User, work: Work)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Score]]
  def update(score: Score)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Score]]
  def delete(score: Score)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Score]]

  /**
   * Work <-> User relationship methods.
   *
   * These methods manipulate the "work_scorers" table joining users and works in a many-to-many
   * relationship. The table "scorers" joining courses (classrooms) and scorers will be manipulated
   * in ScorerRepository/TeamRepository.
   */
  def addScorers(work: Work, scorerList: IndexedSeq[User])(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]
  def removeScorers(work: Work, scorerList: IndexedSeq[User])(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]
  def addWorks(workList: IndexedSeq[Work], scorer: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]
  def removeWorks(workList: IndexedSeq[Work], scorer: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]
  def removeFromAllScorers(work: Work)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]
}
