package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.Team
import ca.shiftfocus.krispii.core.models.course.Exam
import ca.shiftfocus.krispii.core.models.work.Test
import com.github.mauricio.async.db.{Connection, RowData}
import org.joda.time.DateTime
import scalaz.{-\/, \/, \/-}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TestRepositoryPostgres(
  val userRepository: UserRepository,
  val scoreRepository: ScoreRepository,
  val cacheRepository: CacheRepository
)
    extends TestRepository with PostgresRepository[Test] {

  override val entityName: String = "test"

  override def constructor(row: RowData): Test =
    Test(
      row("id").asInstanceOf[UUID],
      row("exam_id").asInstanceOf[UUID],
      Option(row("team_id").asInstanceOf[UUID]) /*match {
        case Some(teamId) => Some(teamId)
        case _ => None
      } */ ,
      row("name").asInstanceOf[String],
      row("version").asInstanceOf[Long],
      row("grade").asInstanceOf[String],
      row("orig_response").asInstanceOf[UUID],
      None, // scorers
      None, // scores
      row("created_at").asInstanceOf[DateTime],
      row("updated_at").asInstanceOf[DateTime]
    )

  val Table = "tests"
  val Fields = "id, exam_id, team_id, name, version, grade, orig_response, created_at, updated_at"
  val FieldsWithTable =
    Fields.split(", ").map({ field => s"${Table}." + field }).mkString(", ")
  val OrderBy = s"${Table}.created_at ASC"

  // User CRUD operations
  val SelectAll =
    s"""
       |SELECT $Fields
       |FROM $Table
     """.stripMargin

  val ListByExam =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE exam_id = ?
       |ORDER BY $OrderBy
     """.stripMargin

  val ListByTeam =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE team_id = ?
       |ORDER BY $OrderBy
     """.stripMargin

  val SelectOne =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE id = ?
    """.stripMargin

  val SelectByNameExam =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE name = ?
       | AND exam_id = ?
    """.stripMargin

  val Insert =
    s"""
       |INSERT INTO $Table ($Fields)
       |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
       |RETURNING $Fields
    """.stripMargin

  val Update =
    s"""
       |UPDATE $Table
       |SET exam_id = ?, team_id = ?, name = ?, version = ?, grade = ?, orig_response = ?,  updated_at = ?
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
   * Find all tests.
   *
   * @return a vector of the returned tests (without their associated scores)
   */
  override def list(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Test]]] =
    queryList(SelectAll)

  /**
   * Find all tests assigned to a given exam.
   *
   * @param exam The exam that the tests were assigned to
   * @return a vector of the returned tests with their scores or an error
   */
  override def list(exam: Exam)(implicit conn: Connection): Future[RepositoryError.Fail \/ IndexedSeq[Test]] =
    list(exam, fetchScores = true)

  /**
   * Find all tests assigned to a given exam.
   *
   * @param exam The exam that the tests were assigned to
   * @param fetchScores whether to include the scores associated with each test
   * @return a vector of the returned tests or an error
   */
  override def list(exam: Exam, fetchScores: Boolean)(implicit conn: Connection): Future[RepositoryError.Fail \/ IndexedSeq[Test]] = {
    val key = cacheTestsKey(exam.id)
    cacheRepository.cacheSeqTest.getCached(key).flatMap {
      case \/-(testList) => Future successful \/-(testList)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          testList <- lift(queryList(ListByExam, Seq[Any](exam.id)))
          _ <- lift(cacheRepository.cacheSeqTest.putCache(key)(testList, ttl))
          finalTestList <- if (fetchScores)
            liftSeq(testList.map { test =>
              (for {
                scoreList <- lift(scoreRepository.list(test))
                result = test.copy(scores = Some(scoreList))
              } yield result).run
            })
          else
            lift(Future successful \/-(testList))
        } yield finalTestList

      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Find all tests assigned to a given team.
   *
   * @param team The exam that the tests were assigned to
   * @return a vector of the returned tests with their scores or an error
   */
  override def list(team: Team)(implicit conn: Connection): Future[RepositoryError.Fail \/ IndexedSeq[Test]] =
    list(team, fetchScores = true)

  /**
   * Find all tests assigned to a given team.
   *
   * @param team The team that the tests were assigned to
   * @param fetchScores whether to include the scores associated with each test
   * @return a vector of the returned tests or an error
   */
  override def list(team: Team, fetchScores: Boolean)(implicit conn: Connection): Future[RepositoryError.Fail \/ IndexedSeq[Test]] = {
    val key = cacheTestsKey(team.id)
    cacheRepository.cacheSeqTest.getCached(key).flatMap {
      case \/-(testList) => Future successful \/-(testList)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          testList <- lift(queryList(ListByTeam, Seq[Any](team.id)))
          _ <- lift(cacheRepository.cacheSeqTest.putCache(key)(testList, ttl))
          finalTestList <- if (fetchScores)
            liftSeq(testList.map { test =>
              (for {
                scoreList <- lift(scoreRepository.list(test))
                result = test.copy(scores = Some(scoreList))
              } yield result).run
            })
          else
            lift(Future successful \/-(testList))
        } yield finalTestList

      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Find a single entry by ID.
   *
   * @param id the 128-bit UUID, as a byte array, to search for.
   * @return the test or an error
   */
  override def find(id: UUID)(implicit conn: Connection): Future[RepositoryError.Fail \/ Test] = {
    val key = cacheTestKey(id)
    cacheRepository.cacheTest.getCached(key).flatMap {
      case \/-(test) => Future successful \/-(test)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          test <- lift(queryOne(SelectOne, Array[Any](id)))
          _ <- lift(cacheRepository.cacheTest.putCache(key)(test, ttl))
        } yield test
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Find a single test by the unique combination of exam and test name
   *
   * @param name the name of the test (student name or external ID, unique to the exam)
   * @param exam the Exam to which the test was assigned
   * @return the test or an error
   */
  override def find(name: String, exam: Exam)(implicit conn: Connection): Future[RepositoryError.Fail \/ Test] = {
    val key = cacheTestNameKey(name, exam.id)
    cacheRepository.cacheUUID.getCached(key).flatMap {
      case \/-(testId) => find(testId)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          test <- lift(queryOne(SelectByNameExam, Array[Any](name, exam.id)))
          _ <- lift(cacheRepository.cacheUUID.putCache(key)(test.id, ttl))
          _ <- lift(cacheRepository.cacheTest.putCache(cacheTestKey(test.id))(test, ttl))
        } yield test
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Save a Test to the database.
   *
   * @return the new test or an error
   */
  override def insert(test: Test)(implicit conn: Connection): Future[RepositoryError.Fail \/ Test] = {
    // TODO: check if scorer is in list of scorers for this team! Or do this in omsService?
    val params = Seq[Any](test.id, test.examId, test.teamId, test.name, 1, test.grade,
      test.origResponse, new DateTime, new DateTime)

    for {
      inserted <- lift(queryOne(Insert, params))
      _ <- lift(cacheRepository.cacheSeqTest.removeCached(cacheTestsKey(test.examId)))
      _ <- lift(inserted.teamId match {
        case Some(teamId) => cacheRepository.cacheSeqTest.removeCached(cacheTestsKey(teamId))
        case None => Future successful \/-(())
      })
    } yield inserted
  }

  /**
   * Update a Test.
   *
   * @return the changed Test or an error
   */
  override def update(test: Test)(implicit conn: Connection): Future[RepositoryError.Fail \/ Test] = {
    val params = Seq[Any](test.examId, test.teamId, test.name, test.version + 1, test.grade,
      test.origResponse, new DateTime, test.id, test.version)

    for {
      updated <- lift(queryOne(Update, params))
      _ <- lift(cacheRepository.cacheUUID.removeCached(cacheTestNameKey(updated.name, updated.examId)))
      _ <- lift(cacheRepository.cacheTest.removeCached(cacheTestKey(updated.id)))
      _ <- lift(cacheRepository.cacheSeqTest.removeCached(cacheTestsKey(updated.examId)))
      _ <- lift(updated.teamId match {
        case Some(teamId) => lift(cacheRepository.cacheSeqTest.removeCached(cacheTestsKey(teamId)))
        case None => Future successful \/-(())
      })
    } yield updated
  }

  /**
   * Delete a single test
   *
   * @param test to be deleted
   * @return the deleted test including associated scores (if the deletion was successful) or an error
   */
  override def delete(test: Test)(implicit conn: Connection): Future[RepositoryError.Fail \/ Test] =
    (for {
      deleted <- lift(queryOne(Delete, Array(test.id, test.version)))
      oldScores = test.scores
      _ <- lift(cacheRepository.cacheUUID.removeCached(cacheTestNameKey(deleted.name, deleted.examId)))
      _ <- lift(cacheRepository.cacheTest.removeCached(cacheTestKey(deleted.id)))
      _ <- lift(cacheRepository.cacheSeqTest.removeCached(cacheTestsKey(deleted.examId)))
      _ <- lift(deleted.teamId match {
        case Some(teamId) => cacheRepository.cacheSeqTest.removeCached(cacheTestsKey(teamId))
        case None => Future successful \/-(())
      })
    } yield deleted.copy(scores = oldScores)).run

  /*
   * Delete all tests from an exam
   *
   * @param exam that contains the tests
   * @return a vector of the deleted tests including associated scores (if the deletion was successful) or an error

  // TODO: transfer to omsService so we don't need to implement teamRepository here
  override def delete(exam: Exam)(implicit conn: Connection): Future[RepositoryError.Fail \/ IndexedSeq[Test]] =
    for {
      testList <- lift(cacheRepository.cacheSeqTest.getCached(cacheTestsKey(exam.id)).flatMap {
        case \/-(testList) => Future successful \/-(testList)
        case -\/(noResults: RepositoryError.NoResults) =>
          lift(queryList(ListByExam, Seq[Any](exam.id)))
        case -\/(error) => Future successful -\/(error)
      })
      _ <- lift(serializedT(testList)(test => delete(test)))
      _ <- lift(cacheRepository.cacheSeqTest.removeCached(cacheTestsKey(exam.id)))
      teamList <- lift(teamRepository.list(exam.id))
      _ <- lift(serializedT(teamList)(team => delete(team)))
    } yield testList */

}
