package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import play.api.Logger

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.work.Work
import ca.shiftfocus.lib.concurrent.Lifting
import com.github.mauricio.async.db.{Connection, RowData}
import org.joda.time.DateTime
import scalaz.{-\/, \/, \/-}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ScoreRepositoryPostgres(
    val cacheRepository: CacheRepository
) extends ScoreRepository with PostgresRepository[Score] with Lifting[RepositoryError.Fail] {

  override val entityName = "Score"

  override def constructor(row: RowData): Score = {
    Score(
      row("work_id").asInstanceOf[UUID],
      row("scorer_id").asInstanceOf[UUID],
      row("version").asInstanceOf[Long],
      row("created_at").asInstanceOf[DateTime],
      row("updated_at").asInstanceOf[DateTime],
      row("grade").asInstanceOf[String]
    )
  }

  val Fields = "work_id, scorer_id, version, created_at, updated_at, grade"
  val QMarks = "?, ?, ?, ?, ?, ?"
  val Table = "work_scorers"

  // User CRUD operations
  private val SelectByWork = s"""
    |SELECT $Fields
    |FROM $Table
    |WHERE work_id = ?
    |ORDER BY created_at ASC
  """.stripMargin

  private val SelectByScorer = s"""
    |SELECT $Fields
    |FROM $Table
    |WHERE scorer_id = ?
    |ORDER BY created_at ASC
  """.stripMargin

  private val Insert = {
    s"""
      |INSERT INTO $Table ($Fields)
      |VALUES ($QMarks)
      |RETURNING $Fields
    """.stripMargin
  }

  private val Update = {
    s"""
      |UPDATE $Table
      |SET grade = ?, version = ?, updated_at = ?
      |WHERE work_id = ?
      |  AND scorer_id = ?
      |  AND version = ?
      |RETURNING $Fields
    """.stripMargin
  }

  private val Delete =
    s"""
       |DELETE FROM $Table
       |WHERE work_id = ?
       |  AND scorer_id = ?
       |  AND version = ?
       |RETURNING $Fields
     """.stripMargin

  // ---- Users<->Works relationship operations --------------------------------

  private val AddScores = s"""
                   |INSERT INTO $Table (work_id, scorer_id, created_at)
                   |VALUES
  """.stripMargin

  /**
   * Might remove AddScore (simplified case of AddScores)
   */
  private val AddScore = s"""
                    |INSERT INTO $Table (work_id, scorer_id, created_at)
                    |VALUES (?, ?, ?)
  """.stripMargin

  /**
   * If a single scorer with scorer_id is contained in the list given.
   */
  private val RemoveScorers = s"""
                      |DELETE FROM $Table
                      |WHERE work_id = ?
                      | AND ARRAY[scorer_id] <@ ?
  """.stripMargin

  /**
   * Can be removed, its function is fulfilled by RemoveScorers.
   */
  private val RemoveScorer = """
    |DELETE FROM users_scores
    |WHERE work_id = ?
    |  AND scorer_id = ?
  """.stripMargin

  /**
   * If a single work with work_id is contained in the list given.
   * The single scorer_id must come first, so that the rest of the
   * parameters are placed in an array of work_ids.
   */
  private val RemoveWorks = s"""
                         |DELETE FROM $Table
                         |WHERE scorer_id = ?
                         | AND ARRAY[work_id] <@ ?
  """.stripMargin

  private val RemoveFromAllUsers = """
    |DELETE FROM users_scores
    |WHERE score_id = ?
  """.stripMargin

  /**
   * List the scores associated with a work (this should actually pretend the scores are saved within the work!).
   */
  override def list(work: Work)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Score]]] = {
    cacheRepository.cacheSeqScore.getCached(cacheScoresKey(work.id)).flatMap {
      case \/-(scoreList) => Future successful \/-(scoreList)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          scoreList <- lift(queryList(SelectByWork, Array[Any](work.id)))
          _ <- lift(cacheRepository.cacheSeqScore.putCache(cacheScoresKey(work.id))(scoreList, ttl))
        } yield scoreList
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * List the scores associated with a scorer.
   */
  override def list(scorer: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Score]]] = {
    cacheRepository.cacheSeqScore.getCached(cacheScorerKey(scorer.id)).flatMap {
      case \/-(scoreList) => Future successful \/-(scoreList)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          scoreList <- lift(queryList(SelectByScorer, Array[Any](scorer.id)))
          _ <- lift(cacheRepository.cacheSeqScore.putCache(cacheScorerKey(scorer.id))(scoreList, ttl))
        } yield scoreList
      case -\/(error) => Future successful -\/(error)
    }
  }

  def findOrError(maybeScore: Option[Score]): Future[\/[RepositoryError.Fail, Score]] = {
    maybeScore match {
      case Some(score) => Future successful \/-(score)
      case None => Future successful -\/(RepositoryError.NoResults("No score for this scorer in this work"))
    }
  }

  override def find(work: Work, scorer: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Score]] = {
    Logger.debug(s"Looking up score from scorer ${scorer.email} in work ${work.id}")
    cacheRepository.cacheSeqScore.getCached(cacheScorerKey(scorer.id)).flatMap {
      case \/-(scoreList) =>
        Logger.debug(s"Looking for work ${work.id} in the cached values for scorer ${scorer.email}: ${scoreList}")
        findOrError(scoreList.find(score => score.work_id == work.id))
      case -\/(noResults: RepositoryError.NoResults) =>
        Logger.debug(s"No cached value, looking up data base scores from scorer ${scorer.email} in work ${work.id} in the data base")
        queryList(SelectByScorer, Array[Any](scorer.id)).flatMap {
          case \/-(scoreList) =>
            Logger.debug(s"Looking up work ${work.id} in the data base scores for scorer ${scorer.email}: ${scoreList}")
            lift(cacheRepository.cacheSeqScore.putCache(cacheScorerKey(scorer.id))(scoreList, ttl))
            findOrError(scoreList.find(score => score.work_id == work.id))
          case -\/(noResults) =>
            Logger.debug(s"No scores for scorer ${scorer.email} in the data base")
            Future successful -\/(noResults)
        }
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Save a Score row.
   *
   * @return new Score.
   */
  override def insert(scorer: User, work: Work)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Score]] = {
    val params = Seq[Any](work.id, scorer.id, 1, new DateTime, new DateTime, "")

    queryOne(Insert, params)
  }

  /**
   * Update a Score.
   *
   * @return id of the saved/new score.
   */
  override def update(score: Score)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Score]] = {
    val params = Seq[Any](score.grade, score.version + 1, new DateTime, score.work_id, score.scorer_id, score.version)

    for {
      updated <- lift(queryOne(Update, params))
      _ <- lift(cacheRepository.cacheScore.removeCached(cacheScoresKey(score.work_id)))
      _ <- lift(cacheRepository.cacheScore.removeCached(cacheScorerKey(score.scorer_id)))
    } yield updated
  }

  /**
   * Delete a score
   *
   * @param score: Score
   * @return
   */
  override def delete(score: Score)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Score]] = {
    for {
      deleted <- lift(queryOne(Delete, Array(score.work_id, score.scorer_id, score.version)))
      _ <- lift(cacheRepository.cacheScore.removeCached(cacheScoresKey(score.work_id)))
      _ <- lift(cacheRepository.cacheScore.removeCached(cacheScorerKey(score.scorer_id)))
    } yield deleted
  }

  /**
   * Add scorers to a work
   * @param work: Work
   * @param scorerList: IndexedSeq[User]
   * @param conn: Connection
   * @return
   */
  override def addScorers(work: Work, scorerList: IndexedSeq[User])(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]] = {
    val cleanWorkId = work.id.toString filterNot ("-" contains _)
    val query = AddScores + scorerList.map { user =>
      val cleanUserId = user.id.toString filterNot ("-" contains _)
      s"('$cleanWorkId', '$cleanUserId', '${new DateTime()}')"
    }.mkString(",")

    for {
      _ <- lift(queryNumRows(query)(scorerList.length == _).map {
        case \/-(wasSuccessful) => if (wasSuccessful) { \/-(()) } // scalastyle:ignore
        else -\/(RepositoryError.DatabaseError(s"Not all scorers could be added to work ${work.id}.")) // TODO unreachable
        case -\/(error) => -\/(error)
      })
      _ <- liftSeq { scorerList.map { user => cacheRepository.cacheSeqScore.removeCached(cacheScoresKey(user.id)) } }
    } yield ()
  }

  /**
   * Remove scorers from a work
   * @param work: Work
   * @param scorerList: IndexedSeq[User]
   * @param conn: Connection
   * @return
   */
  override def removeScorers(work: Work, scorerList: IndexedSeq[User])(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]] = {
    val cleanWorkId = work.id.toString filterNot ("-" contains _)
    val cleanUserIds = scorerList.map { user =>
      user.id.toString filterNot ("-" contains _)
    }

    for {
      _ <- lift(queryNumRows(RemoveScorers, Array[Any](cleanWorkId, cleanUserIds))(scorerList.length == _).map {
        // number of scorers to remove == number of changed rows
        case \/-(wasSuccessful) =>
          if (wasSuccessful) { \/-(()) } //scalastyle:ignore
          else -\/(RepositoryError.DatabaseError(s"Not all scorers could be removed from work ${work.id}."))
        case -\/(error) => -\/(error)
      }.recover {
        case exception: Throwable => throw exception
      })
      _ <- liftSeq { scorerList.map { user => cacheRepository.cacheSeqScore.removeCached(cacheScorerKey(user.id)) } }
    } yield ()
  }

  /**
   * Associate works to be scored with a scorer.
   *
   * @param workList: IndexedSeq[Work]
   * @param scorer: User
   * @param conn: Connection
   */
  override def addWorks(workList: IndexedSeq[Work], scorer: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]] = {
    val cleanScorerId = scorer.id.toString filterNot ("-" contains _)
    val query = AddScores + workList.map { work =>
      val cleanWorkId = work.id.toString filterNot ("-" contains _)
      s"('$cleanWorkId', '$cleanScorerId', '${new DateTime()}')"
    }.mkString(",")

    for {
      _ <- lift(queryNumRows(query)(workList.length == _).map {
        case \/-(true) => \/-(())
        case \/-(false) => -\/(RepositoryError.DatabaseError("The query succeeded but somehow nothing was modified."))
        case -\/(error) => -\/(error)
      })
      _ <- lift(cacheRepository.cacheSeqScore.removeCached(cacheScorerKey(scorer.id)))
    } yield ()
  }

  /**
   * Remove works to be scored from a potential scorer
   * @param workList: IndexedSeq[Work]
   * @param scorer: User
   * @param conn: Connection
   * @return
   */
  override def removeWorks(workList: IndexedSeq[Work], scorer: User)(implicit conn: Connection): Future[RepositoryError.Fail \/ Unit] = {
    val cleanScorerId = scorer.id.toString filterNot ("-" contains _)
    val cleanWorkIds = workList.map { work =>
      work.id.toString filterNot ("-" contains _)
    }

    for {
      _ <- lift(queryNumRows(RemoveWorks, Array[Any](cleanScorerId, cleanWorkIds))(workList.length == _).map {
        // number of scores to delete == number of rows affected ?
        case \/-(wasSuccessful) =>
          if (wasSuccessful) { \/-(()) } //scalastyle:ignore
          else -\/(RepositoryError.DatabaseError(s"Not all work to be scored could be removed from scorer ${scorer.email}."))
        case -\/(error) => -\/(error)
      }.recover {
        case exception: Throwable => throw exception
      })
      _ <- liftSeq { workList.map { work => cacheRepository.cacheSeqScore.removeCached(cacheScoresKey(work.id)) } }
    } yield ()
  }

  /**
   * Remove works to be scored from all users.
   *
   * @param work: Work
   */
  override def removeFromAllScorers(work: Work)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]] = {
    for {
      scores <- lift(list(work))
      _ <- lift(queryNumRows(RemoveFromAllUsers, Seq[Any](work.id))(_ >= 1).map {
        case \/-(wasSuccessful) =>
          if (wasSuccessful) { \/-(()) } //scalastyle:ignore
          else -\/(RepositoryError.DatabaseError(s"Not all scorers could be removed from work ${work.id}."))
        case -\/(error) => -\/(error)
      })
      _ <- liftSeq { scores.map { score => cacheRepository.cacheSeqScore.removeCached(cacheScorerKey(score.scorer_id)) } }
    } yield ()
  }
}
