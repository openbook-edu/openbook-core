package ca.shiftfocus.krispii.core.services

import java.util.UUID

import ca.shiftfocus.krispii.core.error.{ErrorUnion, ServiceError}
import ca.shiftfocus.krispii.core.models.{Chat, User}
import ca.shiftfocus.krispii.core.models.group.{Exam, Team}
import ca.shiftfocus.krispii.core.models.work.{Score, Test}
import ca.shiftfocus.krispii.core.repositories.{ChatRepository, ExamRepository, ScoreRepository, TeamRepository, TestRepository, UserRepository}
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.github.mauricio.async.db.Connection
import play.api.Logger
import scalaz.\/

import scala.collection.IndexedSeq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

class OmsServiceDefault(
    val db: DB,
    val chatRepository: ChatRepository,
    val examRepository: ExamRepository,
    val teamRepository: TeamRepository,
    val testRepository: TestRepository,
    val scoreRepository: ScoreRepository,
    val userRepository: UserRepository
) extends OmsService {

  implicit def conn: Connection = db.pool

  /* Bread-and-butter functions that simply pass on requests to the respective repositories
     because it is easier for the API to just include one omsService */
  def findExam(examId: UUID): Future[\/[ErrorUnion#Fail, Exam]] =
    examRepository.find(examId)
  def findTeam(teamId: UUID): Future[\/[ErrorUnion#Fail, Team]] =
    teamRepository.find(teamId)
  def findTest(testId: UUID): Future[\/[ErrorUnion#Fail, Test]] =
    testRepository.find(testId)
  def updateTest(test: Test): Future[\/[ErrorUnion#Fail, Test]] =
    testRepository.update(test)
  def findScore(scoreId: UUID): Future[\/[ErrorUnion#Fail, Score]] =
    scoreRepository.find(scoreId)
  def listScorers(teamId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[User]]] = for {
    team <- lift(teamRepository.find(teamId))
  } yield team.scorers

  /**
   * Supply list that contains the exam's owner and the team's scorers, or an error
   * @param examId: UUID of the exam
   * @param teamId: UUID of the team
   * @return
   */
  def listMembers(examId: UUID, teamId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[User]]] =
    for {
      team <- lift(teamRepository.find(teamId))
      _ <- predicate(examId.equals(team.examId))(ServiceError.BadInput("The team needs to be part of the exam"))
      exam <- lift(examRepository.find(examId))
      owner <- lift(userRepository.find(exam.ownerId))
      members = owner +: team.scorers
    } yield members

  /**
   * Move a vector of Tests, identified by their IDs, to a certain team.
   * @param testIds IndexedSeq of Test IDs
   * @param newTeamId ID of the team to assign the Tests to
   * @return the vector of updated tests, or an error
   * TODO: check that the old team and new team are in the same exam? Or that the exam administrator has rights in both?
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
   * @param exam The exam within which to randomize
   * @param all If false, only assign team IDs to those tests in the exam that don't have a team ID yet
   * @return the vector of updated tests, or an error
   */
  override def randomizeTests(exam: Exam, all: Boolean = false): Future[ErrorUnion#Fail \/ IndexedSeq[Test]] =
    for {
      allTests <- lift(testRepository.list(exam, fetchScores = false))
      existingTests = allTests filter (all || _.teamId.isEmpty)
      _ = Logger.debug(s"Tests to be randomized in exam ${exam.name}: " +
        existingTests.map(_.name).mkString(", "))
      teams <- lift(teamRepository.list(exam))
      teamIds = teams map (_.id)
      _ = Logger.debug(s"Teams in exam ${exam.name}: " + teamIds.mkString(", "))
      randomizedTests <- lift(serializedT(existingTests)(test =>
        testRepository.update(test.copy(teamId = Some(randomId(teamIds))))))
    } yield randomizedTests

  override def automaticScoring(examId: UUID): Future[ErrorUnion#Fail \/ IndexedSeq[Test]] =
    for {
      /* reject automatic scoring with an error message if tests are not multiple score,
         or if the correct answers are not provided in the rubric */
      exam <- lift(examRepository.find(examId))
      existingTests <- lift(testRepository.list(exam))
      // apply automatic scoring to tests
    } yield existingTests

  /**
   * List all chats for a team (usually one).
   *
   * @param teamId
   * @return
   */
  override def listChats(teamId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Chat]]] = {
    for {
      team <- lift(findTeam(teamId))
      chats <- lift(chatRepository.list(team))
    } yield chats
  }
}
