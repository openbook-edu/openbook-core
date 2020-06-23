package ca.shiftfocus.krispii.core.services

import java.util.UUID

import ca.shiftfocus.krispii.core.error.ErrorUnion
import ca.shiftfocus.krispii.core.models.work.Test
import ca.shiftfocus.krispii.core.repositories.{ExamRepository, TeamRepository, TestRepository}
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.github.mauricio.async.db.Connection
import scalaz.\/

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

class ExamServiceDefault(
    val db: DB,
    val examRepository: ExamRepository,
    val teamRepository: TeamRepository,
    val testRepository: TestRepository
) extends ExamService {

  implicit def conn: Connection = db.pool

  /**
   * Move a vector of Tests, identified by their IDs, to a certain team.
   * @param testIds IndexedSeq of Test IDs
   * @param newTeamId ID of the team to assign the Tests to
   * @return the vector of updated tests, or an error
   */
  override def moveTests(testIds: IndexedSeq[UUID], newTeamId: UUID): Future[ErrorUnion#Fail \/ IndexedSeq[Test]] =
    for {
      existingTests <- lift(serializedT(testIds)(testId => testRepository.find(testId)))
    } yield existingTests.map(_.copy(teamId = Some(newTeamId)))

  /**
   * Take a list of teamIDs and return some random value from it.
   * TODO: take a Map of teamIds with their frequencies and favor the teams with lower frequencies
   * @param idList
   * @return
   */
  def randomId(idList: IndexedSeq[UUID]): UUID =
    idList(Random.nextInt(idList.length))

  /**
   * Randomize either all tests in an exam, or only those exams without a team, to the existing teams.
   * @param examId The ID of the exam within which to randomize
   * @param onlyNewTests If true, only assign team IDs to those tests in the exam that don't have a team ID yet
   * @return the vector of updated tests, or an error
   */
  override def randomizeTests(examId: UUID, onlyNewTests: Boolean = false): Future[ErrorUnion#Fail \/ IndexedSeq[Test]] =
    for {
      exam <- lift(examRepository.find(examId))
      allTests <- lift(testRepository.list(exam, fetchScores = false))
      existingTests = if (onlyNewTests) allTests filter (_.teamId.isEmpty) else allTests
      teams <- lift(teamRepository.list(exam))
      teamIds = teams map (_.id)
      randomizedTests <- lift(serializedT(existingTests)(test =>
        testRepository.update(test.copy(teamId = Some(randomId(teamIds))))))
    } yield randomizedTests

}
