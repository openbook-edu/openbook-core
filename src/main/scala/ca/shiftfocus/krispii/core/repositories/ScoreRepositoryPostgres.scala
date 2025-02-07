package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.group.{Exam, Team}
import ca.shiftfocus.krispii.core.models.user.User
import ca.shiftfocus.krispii.core.models.work.{Score, Test}
import com.github.mauricio.async.db.{Connection, RowData}
import org.joda.time.DateTime
import scalaz.{-\/, \/, \/-}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ScoreRepositoryPostgres(
    val cacheRepository: CacheRepository,
    val scorerRepository: ScorerRepository
) extends ScoreRepository with PostgresRepository[Score] {

  override val entityName: String = "score"

  override def constructor(row: RowData): Score =
    Score(
      row("id").asInstanceOf[UUID],
      row("test_id").asInstanceOf[UUID],
      row("scorer_id").asInstanceOf[UUID],
      row("version").asInstanceOf[Long],
      row("orig_comments").asInstanceOf[String],
      row("comments").asInstanceOf[String],
      row("orig_grade").asInstanceOf[String],
      row("grade").asInstanceOf[String],
      row("is_visible").asInstanceOf[Int],
      Option(row("exam_file").asInstanceOf[UUID]),
      Option(row("rubric_file").asInstanceOf[UUID]),
      row("archived").asInstanceOf[Boolean],
      row("deleted").asInstanceOf[Boolean],
      row("created_at").asInstanceOf[DateTime],
      row("updated_at").asInstanceOf[DateTime]
    )

  private val Table = "scores"
  // names and number of fields in SQL
  private val Fields = "id, test_id, scorer_id, version, orig_comments, comments, orig_grade, grade, is_visible, " +
    "exam_file, rubric_file, archived, deleted, created_at, updated_at"
  private val FieldsWithTable = Fields.split(", ").map({ field => s"${Table}." + field }).mkString(", ")
  private val OrderBy = s"${Table}.created_at ASC"

  // User CRUD operations
  private val SelectAll =
    s"""
       |SELECT $Fields
       |FROM $Table
     """.stripMargin

  private val ListByExamScorer =
    s"""
       |SELECT $FieldsWithTable
       |FROM $Table, tests
       |WHERE tests.exam_id = ?
       | AND $Table.test_id = tests.id
       | AND $Table.scorer_id = ?
       |ORDER BY $OrderBy
     """.stripMargin

  private val ListByTeamScorer =
    s"""
       |SELECT $FieldsWithTable
       |FROM $Table, tests
       |WHERE tests.team_id = ?
       | AND $Table.test_id = tests.id
       | AND $Table.scorer_id = ?
       |ORDER BY $OrderBy
     """.stripMargin

  private val ListByTest =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE test_id = ?
       |ORDER BY $OrderBy
     """.stripMargin

  private val SelectOne =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE id = ?
    """.stripMargin

  private val SelectByTestScorer =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE test_id = ?
       | AND scorer_id = ?
       |ORDER BY $OrderBy
     """.stripMargin

  private val Insert =
    s"""
       |INSERT INTO $Table ($Fields)
       |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
       |RETURNING $Fields
    """.stripMargin

  val Update =
    s"""
       |UPDATE $Table
       |SET test_id = ?, scorer_id = ?, version = ?, orig_comments = ?, comments= ?, orig_grade = ?, grade = ?,
       |  is_visible = ?, exam_file = ?, rubric_file = ?, archived = ?, deleted = ?, updated_at = ?
       |WHERE id = ?
       |  AND version = ?
       |RETURNING $Fields
    """.stripMargin

  private val Delete =
    s"""
       |DELETE
       |FROM $Table
       |WHERE id = ?
       | AND version = ?
       |RETURNING $Fields
  """.stripMargin

  /**
   * Find all scores.
   *
   * @return a vector of the returned Scores
   */
  override def list(implicit conn: Connection): Future[RepositoryError.Fail \/ IndexedSeq[Score]] =
    queryList(SelectAll)

  /**
   * Find all scores given by a scorer in a given exam.
   *
   * @param exam The exam that the tests were assigned to
   * @param scorer The user who scored the tests
   * @return a vector of the returned tests
   */
  override def list(exam: Exam, scorer: User)(implicit conn: Connection): Future[RepositoryError.Fail \/ IndexedSeq[Score]] = {
    // no way to remove this list once cached, so don't cache!
    /* val key = cacheScorerKey(exam.id, scorer.id)
    cacheRepository.cacheSeqScore.getCached(key).flatMap {
      case \/-(scoreList) => Future successful \/-(scoreList)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          scoreList <- lift(queryList(ListByExamScorer, Array[Any](exam.id, scorer.id)))
          _ <- lift(cacheRepository.cacheSeqScore.putCache(key)(scoreList, ttl))
        } yield scoreList

      case -\/(error) => Future successful -\/(error)
    } */
    queryList(ListByExamScorer, Array[Any](exam.id, scorer.id))
  }

  /**
   * Find all scores given by a scorer in a given team.
   *
   * @param team The team that the tests were assigned to
   * @param scorer The user who scored the tests
   * @return a vector of the returned tests
   */
  override def list(team: Team, scorer: User)(implicit conn: Connection): Future[RepositoryError.Fail \/ IndexedSeq[Score]] = {
    // no way to remove this list once cached, so don't cache!
    /* val key = cacheScorerKey(team.id, scorer.id)
    cacheRepository.cacheSeqScore.getCached(key).flatMap {
      case \/-(scoreList) => Future successful \/-(scoreList)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          scoreList <- lift(queryList(ListByTeamScorer, Array[Any](team.id, scorer.id)))
          _ <- lift(cacheRepository.cacheSeqScore.putCache(key)(scoreList, ttl))
        } yield scoreList

      case -\/(error) => Future successful -\/(error)
    } */
    queryList(ListByTeamScorer, Array[Any](team.id, scorer.id))
  }

  /**
   * Find all scores given on a certain test.
   *
   * @param test The test to which the scores were added
   * @return a vector of the returned Scores
   */
  override def list(test: Test)(implicit conn: Connection): Future[RepositoryError.Fail \/ IndexedSeq[Score]] = {
    val key = cacheScoresKey(test.id)
    cacheRepository.cacheSeqScore.getCached(key).flatMap {
      case \/-(scoreList) => Future successful \/-(scoreList)
      case -\/(_: RepositoryError.NoResults) =>
        for {
          scoreList <- lift(queryList(ListByTest, Seq[Any](test.id)))
          _ <- lift(cacheRepository.cacheSeqScore.putCache(key)(scoreList, ttl))
        } yield scoreList
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Find all scores given on a certain test by members of a team.
   *
   * @param test The test to which the scores were added
   * @param team: Team, if present, return only scores given by scorers on this team
   * @return a vector of the returned Scores
   */
  override def list(test: Test, team: Team)(implicit conn: Connection): Future[RepositoryError.Fail \/ IndexedSeq[Score]] =
    /* At any given point in time, a test is assigned only to one team, so scores given by previous teams are immutable.
     Therefore, it seems faster and easier to get a (possibly cached) list of all scores and then filter. */
    for {
      allScores <- lift(list(test))
      scorers <- lift(scorerRepository.list(team))
      scorerIds = scorers.map(_.id)
      scores = allScores.filter(t => scorerIds.contains(t.scorerId))
    } yield scores

  /**
   * Find a single entry by ID.
   *
   * @param id the 128-bit UUID, as a byte array, to search for.
   * @return the score or an error
   */
  override def find(id: UUID)(implicit conn: Connection): Future[RepositoryError.Fail \/ Score] = {
    val key = cacheScoreKey(id)
    cacheRepository.cacheScore.getCached(key).flatMap {
      case \/-(score) => Future successful \/-(score)
      case -\/(_: RepositoryError.NoResults) =>
        for {
          score <- lift(queryOne(SelectOne, Array[Any](id)))
          _ <- lift(cacheRepository.cacheScore.putCache(cacheScoreKey(score.id))(score, ttl))
        } yield score
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Find a single score by the unique combination of test and scorer
   *
   * @param testId The test which this scorer scored
   * @param scorerId The user who scored this test
   * @return the score or an error
   */
  override def find(testId: UUID, scorerId: UUID)(implicit conn: Connection): Future[RepositoryError.Fail \/ Score] = {
    val key = cacheScorerKey(testId, scorerId)
    cacheRepository.cacheUUID.getCached(key).flatMap {
      case \/-(scoreId) => find(scoreId)
      case -\/(_: RepositoryError.NoResults) =>
        for {
          score <- lift(queryOne(SelectByTestScorer, Array[Any](testId, scorerId)))
          _ <- lift(cacheRepository.cacheUUID.putCache(key)(score.id, ttl))
          _ <- lift(cacheRepository.cacheScore.putCache(cacheScoreKey(score.id))(score, ttl))
        } yield score
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Save a Score row.
   *
   * @return id of the new Score.
   */
  override def insert(score: Score)(implicit conn: Connection): Future[RepositoryError.Fail \/ Score] = {
    // TODO: check if scorer is in list of scorers for this team! Or do this in services?
    val params = Seq[Any](score.id, score.testId, score.scorerId, 1, score.origComments, score.comments,
      score.origGrade, score.grade, score.isVisible, score.examFile, score.rubricFile,
      score.archived, score.deleted, new DateTime, new DateTime)

    for {
      inserted <- lift(queryOne(Insert, params))
      _ <- lift(cacheRepository.cacheSeqScore.removeCached(cacheScoresKey(inserted.testId)))
      /*test <- lift(testRepository.find(inserted.testId))
      _ <- lift(cacheRepository.cacheSeqScore.removeCached(cacheScorerKey(test.examId, inserted.scorerId)))
      _ <- lift(test.teamId match {
        case Some(teamId) */
    } yield inserted
  }

  /**
   * Update a Score.
   *
   * @return id of the changed Score.
   */
  override def update(score: Score)(implicit conn: Connection): Future[RepositoryError.Fail \/ Score] = {
    val params = Seq[Any](score.testId, score.scorerId, score.version + 1, score.origComments, score.comments,
      score.origGrade, score.grade, score.isVisible, score.examFile, score.rubricFile,
      score.archived, score.deleted, new DateTime, score.id, score.version)

    for {
      updated <- lift(queryOne(Update, params))
      _ <- lift(cacheRepository.cacheUUID.removeCached(cacheScorerKey(updated.testId, updated.scorerId)))
      _ <- lift(cacheRepository.cacheScore.removeCached(cacheScoreKey(score.id)))
      _ <- lift(cacheRepository.cacheSeqScore.removeCached(cacheScoresKey(score.testId)))
      /* test <- lift(testRepository.find(updated.testId))
      _ <- lift(cacheRepository.cacheSeqScore.removeCached(cacheScorerKey(test.examId, updated.scorerId)))
      _ <- lift(test.teamId match {
        case Some(teamId) => cacheRepository.cacheSeqScore.removeCached(cacheScorerKey(teamId, updated.scorerId))
      }) */
    } yield updated
  }

  /**
   * Delete a single score
   *
   * @param score to be deleted
   * @return the deleted score (if the deletion was successful) or an error
   */
  override def delete(score: Score)(implicit conn: Connection): Future[RepositoryError.Fail \/ Score] =
    (for {
      deleted <- lift(queryOne(Delete, Array(score.id, score.version)))
      _ <- lift(cacheRepository.cacheUUID.removeCached(cacheScorerKey(deleted.testId, deleted.scorerId)))
      _ <- lift(cacheRepository.cacheScore.removeCached(cacheScoreKey(score.id)))
      _ <- lift(cacheRepository.cacheSeqScore.removeCached(cacheScoresKey(score.testId)))
      /* test <- lift(testRepository.find(deleted.testId))
      _ <- lift(cacheRepository.cacheSeqScore.removeCached(cacheScorerKey(test.examId, deleted.scorerId)))
      _ <- lift(test.teamId match {
        case Some(teamId) => cacheRepository.cacheSeqScore.removeCached(cacheScorerKey(teamId, deleted.scorerId))
      }) */
    } yield deleted).run

  /*
   * Delete all scores from a test
   *
   * @param test that contains the scores to be deleted
   * @return a vector of the deleted scores (if the deletion was successful) or an error
   *
  // TODO: really useful or not? if so, transfer to omsService
  override def delete(test: Test)(implicit conn: Connection): Future[RepositoryError.Fail \/ IndexedSeq[Score]] =
    for {
      scoreList <- lift(cacheRepository.cacheSeqScore.getCached(cacheScoresKey(test.id)).flatMap {
        case \/-(scoreList) => Future successful \/-(scoreList)
        case -\/(noResults: RepositoryError.NoResults) =>
          lift(queryList(ListByTest, Seq[Any](test.id)))
        case -\/(error) => Future successful -\/(error)
      })
      _ <- lift(serializedT(scoreList)(score => delete(score)))
    } yield scoreList */
}
