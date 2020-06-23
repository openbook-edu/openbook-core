package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.course.Exam
import ca.shiftfocus.krispii.core.models.{Team, User}
import ca.shiftfocus.krispii.core.models.work.{Score, Test}
import com.github.mauricio.async.db.{Connection, RowData}
import org.joda.time.DateTime
import scalaz.{-\/, \/, \/-}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ScoreRepositoryPostgres(
    val userRepository: UserRepository,
    val testRepository: TestRepository,
    val cacheRepository: CacheRepository
) extends ScoreRepository with PostgresRepository[Score] {
  override val entityName: String = "score"

  override def constructor(row: RowData): Score =
    Score(
      row("id").asInstanceOf[UUID],
      row("test_id").asInstanceOf[UUID],
      row("scorer_id").asInstanceOf[UUID],
      row("version").asInstanceOf[Long],
      row("grade").asInstanceOf[String],
      row("is_visible").asInstanceOf[Boolean],
      Option(row("exam_file").asInstanceOf[UUID]) match {
        case Some(exam_file) => Some(exam_file)
        case _ => None
      },
      Option(row("rubric_file").asInstanceOf[UUID]) match {
        case Some(rubric_file) => Some(rubric_file)
        case _ => None
      },
      row("orig_comments").asInstanceOf[String],
      row("add_comments").asInstanceOf[String],
      row("created_at").asInstanceOf[DateTime],
      row("updated_at").asInstanceOf[DateTime]
    )

  val Table = "scores"
  // names and number of fields in SQL
  val Fields = "id, test_id, scorer_id, version, grade, is_visible, exam_file, rubric_file," +
    "orig_comments, add_comments, created_at, updated_at"
  val FieldsWithTable = Fields.split(", ").map({ field => s"${Table}." + field }).mkString(", ")
  val OrderBy = s"${Table}.created_at ASC"

  // User CRUD operations
  val SelectAll =
    s"""
       |SELECT $Fields
       |FROM $Table
     """.stripMargin

  val ListByExamScorer =
    s"""
       |SELECT $FieldsWithTable
       |FROM $Table, tests
       |WHERE tests.exam_id = ?
       | AND $Table.test_id = tests.id
       | AND $Table.scorer_id = ?
       |ORDER BY $OrderBy
     """.stripMargin

  val ListByTeamScorer =
    s"""
       |SELECT $FieldsWithTable
       |FROM $Table, tests
       |WHERE tests.team_id = ?
       | AND $Table.test_id = tests.id
       | AND $Table.scorer_id = ?
       |ORDER BY $OrderBy
     """.stripMargin

  val ListByTest =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE test_id = ?
       |ORDER BY $OrderBy
     """.stripMargin

  val SelectOne =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE id = ?
    """.stripMargin

  val SelectByTestScorer =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE test_id = ?
       | AND scorer_id = ?
       |ORDER BY $OrderBy
     """.stripMargin

  val Insert =
    s"""
       |INSERT INTO $Table ($Fields)
       |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
       |RETURNING $Fields
    """.stripMargin

  val Update =
    s"""
       |UPDATE $Table
       |SET test_id = ?, scorer_id = ?, version = ?, grade = ?, is_visible = ?, exam_file = ?, rubric_file = ?, 
       | orig_comments = ?, add_comments= ?, updated_at = ?
       |WHERE id = ?
       |  AND version = ?
       |RETURNING $Fields
    """.stripMargin

  val Delete =
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
  override def list(exam: Exam, scorer: User)(implicit conn: Connection): Future[RepositoryError.Fail \/ IndexedSeq[Score]] =
    // TODO: cache ExamScorerKey ?
    queryList(ListByExamScorer, Array[Any](exam.id, scorer.id))

  /**
   * Find all scores given by a scorer in a given team.
   *
   * @param team The team that the tests were assigned to
   * @param scorer The user who scored the tests
   * @return a vector of the returned tests
   */
  override def list(team: Team, scorer: User)(implicit conn: Connection): Future[RepositoryError.Fail \/ IndexedSeq[Score]] =
    // TODO: cache TeamScorerKey ?
    queryList(ListByTeamScorer, Array[Any](team.id, scorer.id))

  /**
   * Find all scores given on a certain test.
   *
   * @param test The test to which the scores were added
   * @return a vector of the returned Scores
   */
  override def list(test: Test)(implicit conn: Connection): Future[RepositoryError.Fail \/ IndexedSeq[Score]] = {
    cacheRepository.cacheSeqScore.getCached(cacheScoresKey(test.id)).flatMap {
      case \/-(teamList) => Future successful \/-(teamList)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          scoreList <- lift(queryList(ListByTest, Seq[Any](test.id)))
          _ <- lift(cacheRepository.cacheSeqScore.putCache(cacheScoresKey(test.id))(scoreList, ttl))

        } yield scoreList

      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Find a single entry by ID.
   *
   * @param id the 128-bit UUID, as a byte array, to search for.
   * @return the score or an error
   */
  override def find(id: UUID)(implicit conn: Connection): Future[RepositoryError.Fail \/ Score] =
    cacheRepository.cacheScore.getCached(cacheScoreKey(id)).flatMap {
      case \/-(score) => Future successful \/-(score)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          score <- lift(queryOne(SelectOne, Array[Any](id)))
          _ <- lift(cacheRepository.cacheScore.putCache(cacheScoreKey(score.id))(score, ttl))
        } yield score
      case -\/(error) => Future successful -\/(error)
    }

  /**
   * Find a single score by the unique combination of test and scorer
   *
   * @param test The test which this scorer scored
   * @param scorer The user who scored this test
   * @return the score or an error
   */
  override def find(test: Test, scorer: User)(implicit conn: Connection): Future[RepositoryError.Fail \/ Score] =
    // TODO: cacheTestScorerKey?
    queryOne(SelectByTestScorer, Array[Any](test.id, scorer.id))

  /**
   * Save a Score row.
   *
   * @return id of the new Score.
   */
  override def insert(score: Score)(implicit conn: Connection): Future[RepositoryError.Fail \/ Score] = {
    // TODO: check if scorer is in list of scorers for this team! Or do this in services?
    val params = Seq[Any](score.id, score.testId, score.scorerId, 1, score.grade, score.isVisible,
      score.examFile, score.rubricFile, score.origComments, score.addComments, new DateTime, new DateTime)

    queryOne(Insert, params)
  }

  /**
   * Update a Score.
   *
   * @return id of the changed Score.
   */
  override def update(score: Score)(implicit conn: Connection): Future[RepositoryError.Fail \/ Score] = {
    val params = Seq[Any](score.testId, score.scorerId, score.version + 1, score.grade, score.isVisible,
      score.examFile, score.rubricFile, score.origComments, score.addComments, new DateTime,
      score.id, score.version)

    for {
      updated <- lift(queryOne(Update, params))
      _ <- lift(cacheRepository.cacheScore.removeCached(cacheScoreKey(score.id)))
      _ <- lift(cacheRepository.cacheSeqScore.removeCached(cacheScoresKey(score.testId)))
      // TODO: remove ExamScorerKey, TeamScorerKey and TestScorerKey ?
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
      deletedScore <- lift(queryOne(Delete, Array(score.id, score.version)))
      _ <- lift(cacheRepository.cacheScore.removeCached(cacheScoreKey(score.id)))
      // _ <- lift(cacheRepository.cacheUUID.removeCached(cacheTestScorerKey(s"${score.testId},${score.scorerId}"))))
      _ <- lift(cacheRepository.cacheSeqScore.removeCached(cacheScoresKey(score.testId)))
    } yield deletedScore).run

  /**
   * Delete all tests from an exam
   *
   * @param test that contains the scores to be deleted
   * @return a vector of the deleted scores (if the deletion was successful) or an error
   */
  // TODO: really useful or not? 
  override def delete(test: Test)(implicit conn: Connection): Future[RepositoryError.Fail \/ IndexedSeq[Score]] =
    for {
      scoreList <- lift(cacheRepository.cacheSeqScore.getCached(cacheScoresKey(test.id)).flatMap {
        case \/-(scoreList) => Future successful \/-(scoreList)
        case -\/(noResults: RepositoryError.NoResults) =>
          lift(queryList(ListByTest, Seq[Any](test.id)))
        case -\/(error) => Future successful -\/(error)
      })
      _ <- lift(serializedT(scoreList)(score => delete(score)))
    } yield scoreList
}
