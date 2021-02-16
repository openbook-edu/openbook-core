package ca.shiftfocus.krispii.core.services

import java.util.UUID

import ca.shiftfocus.krispii.core.error.{ErrorUnion, RepositoryError}
import ca.shiftfocus.krispii.core.models.Chat
import ca.shiftfocus.krispii.core.models.group.{Exam, Team}
import ca.shiftfocus.krispii.core.models.user.{Scorer, User, UserTrait}
import ca.shiftfocus.krispii.core.models.work.{Score, Test}
import ca.shiftfocus.krispii.core.repositories._
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
    val lastSeenRepository: LastSeenRepository,
    val teamRepository: TeamRepository,
    val testRepository: TestRepository,
    val scoreRepository: ScoreRepository,
    val scorerRepository: ScorerRepository,
    val userRepository: UserRepository
) extends OmsService {

  implicit def conn: Connection = db.pool

  /* Bread-and-butter functions that simply pass on requests to the respective repositories
     because it is easier for the API to just include one omsService */
  override def findExam(examId: UUID): Future[\/[ErrorUnion#Fail, Exam]] =
    examRepository.find(examId)

  override def listTeams(exam: Exam): Future[\/[ErrorUnion#Fail, IndexedSeq[Team]]] =
    teamRepository.list(exam)

  override def findTeam(teamId: UUID): Future[\/[ErrorUnion#Fail, Team]] =
    teamRepository.find(teamId)

  override def insertTeam(team: Team): Future[\/[ErrorUnion#Fail, Team]] =
    teamRepository.insert(team)

  override def updateTeam(team: Team): Future[\/[ErrorUnion#Fail, Team]] =
    teamRepository.update(team)

  override def deleteTeam(team: Team): Future[\/[ErrorUnion#Fail, Team]] =
    teamRepository.delete(team)

  override def findTest(testId: UUID): Future[\/[ErrorUnion#Fail, Test]] =
    testRepository.find(testId)

  override def updateTest(test: Test): Future[\/[ErrorUnion#Fail, Test]] =
    testRepository.update(test)

  override def findScore(scoreId: UUID): Future[\/[ErrorUnion#Fail, Score]] =
    scoreRepository.find(scoreId)

  override def listScorers(teamId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Scorer]]] = for {
    team <- lift(teamRepository.find(teamId))
  } yield team.scorers

  override def listScorers(team: Team): Future[\/[ErrorUnion#Fail, IndexedSeq[Scorer]]] =
    scorerRepository.list(team)

  /**
   * Supply list that contains the team's owner (at head position) and scorers, or an error
   * @param teamId: UUID of the team
   * @return
   */
  override def listMembers(teamId: UUID): Future[ErrorUnion#Fail \/ IndexedSeq[UserTrait]] =
    for {
      team <- lift(teamRepository.find(teamId))
      owner <- lift(userRepository.find(team.ownerId))
      members = owner +: team.scorers
    } yield members

  /**
   * All scorer functions need to request updated team here, since scoreRepository cannot reference teamRepository (circularity!)
   * @param team: the Team
   * @param user: a User to be added to the team
   * @param leader: will this user be the team leader?
   * @return
   */
  override def addScorer(team: Team, user: User, leader: Boolean): Future[RepositoryError.Fail \/ Team] =
    for {
      _ <- scorerRepository.addScorer(team, user, leader)
      updatedTeam <- teamRepository.find(team.id)
    } yield updatedTeam

  override def updateScorer(team: Team, scorer: Scorer, leader: Option[Boolean],
    archived: Option[Boolean], deleted: Option[Boolean]): Future[RepositoryError.Fail \/ Team] =
    for {
      _ <- scorerRepository.updateScorer(team, scorer, leader, archived, deleted)
      updatedTeam <- teamRepository.find(team.id)
    } yield updatedTeam

  override def removeScorer(team: Team, scorerId: UUID): Future[RepositoryError.Fail \/ Team] =
    for {
      _ <- scorerRepository.removeScorer(team, scorerId)
      updatedTeam <- teamRepository.find(team.id)
    } yield updatedTeam

  /*override def addScorers(team: Team, scorerList: IndexedSeq[User],
    leaderList: IndexedSeq[Boolean] = IndexedSeq(false)): Future[\/[RepositoryError.Fail, Team]] =
    for {
      _ <- scorerRepository.addScorers(team, scorerList, leaderList)
      updatedTeam <- teamRepository.find(team.id)
    } yield updatedTeam

  override def removeScorers(team: Team, scorerIdList: IndexedSeq[UUID]): Future[\/[RepositoryError.Fail, Team]] =
    for {
      _ <- scorerRepository.removeScorers(team, scorerIdList)
      updatedTeam <- teamRepository.find(team.id)
    } yield updatedTeam */

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
   * @param idList: list of team IDs
   * @return
   */
  def randomId(idList: IndexedSeq[UUID]): UUID =
    idList(Random.nextInt(idList.length))

  /**
   * Randomly pick one of the integer values nearest to a fraction
   * @param numer: Int, number to be divided
   * @param denom: Int, how many groups to divide the numerator into
   * @return Int, the rounded fraction
   */
  def randomRound(numer: Int, denom: Int): Int = {
    val sure = numer / denom
    val frac = numer.toFloat / denom - sure
    sure + (if (Random.nextFloat >= frac) 1 else 0)
  }

  def mapToFractions(numer: Int, denom: Int): Seq[Int] = {
    val sure = numer / denom
    val frac = numer.toFloat / denom - sure
    val ones = (frac * numer).round
    val before = Seq.fill(ones)(1) ++ Seq.fill(numer - ones)(0)
    val after = Random.shuffle(before)
    (Seq.fill(numer)(sure),after).zipped.map(_ + _)
  }

  /**
   * Randomize either all tests in an exam, or only those exams without a team, to the existing teams.
   * @param exam The exam within which to randomize
   * @param all If false, only assign team IDs to those tests in the exam that don't have a team ID yet
   * @return the vector of updated tests, or an error
   */
  override def randomizeTests(exam: Exam, all: Boolean = false): Future[ErrorUnion#Fail \/ IndexedSeq[Test]] =
    for {
      allTests <- lift(testRepository.list(exam, fetchScores = false))
      n = allTests.length
      toRandomize = allTests filter (all || _.teamId.isEmpty)
      _ = Logger.debug(s"Tests to be randomized in exam ${exam.name}: ${toRandomize.map(_.name).mkString(", ")}")
      teams <- lift(teamRepository.list(exam))
      teamIds = teams map (_.id)
      _ = Logger.debug(s"Teams in exam ${exam.name}: ${teamIds.mkString(", ")}")
      t = teamIds.length
      real  = teamIds zip allTests.filter(_.teamId.isDefined).groupBy(_.teamId).mapValues(_.size)
      _ = Logger.debug(s"Real distribution: ${real}")
      ideal = teamIds zip mapToFractions(n,t)
      _ = Logger.debug(s"Desired distribution: ${ideal}")
      /*missing = (real,ideal).zipped.map(_.2 - _.2)
      _ = Logger.debug(s"To add to each teamId: ${missing}") */
      // TODO: at each position in the missing, choose at random one test
      randomizedTests <- lift(serializedT(toRandomize)(test =>
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
   * @param team: the Team
   * @return
   */
  override def listChats(team: Team): Future[\/[ErrorUnion#Fail, IndexedSeq[Chat]]] =
    chatRepository.list(team)

  /**
   * List all chats for a team (usually one).
   *
   * @param teamId: unique ID of the team
   * @return
   */
  override def listChats(teamId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Chat]]] = for {
    team <- lift(findTeam(teamId))
    chats <- lift(chatRepository.list(team))
  } yield chats

  /**
   * List all chat logs for a team indicating for each log if a reader has already seen it
   *
   * @param team: the Team for which to look up the logs
   * @param reader the User who requested the chat logs
   * @return an ordered sequence of Chat entries, or an error
   */
  override def listChats(team: Team, reader: User, peek: Boolean): Future[\/[ErrorUnion#Fail, IndexedSeq[Chat]]] = for {
    chats <- lift(chatRepository.list(team))
    lastSeen <- lift(lastSeenRepository.find(reader.id, team.id, entityType = "team", peek: Boolean))
    chatsWithSeen = chats.map(chat => chat.copy(seen = chat.createdAt.isBefore(lastSeen)))
  } yield chatsWithSeen

  override def listChats(teamId: UUID, reader: User, peek: Boolean): Future[\/[ErrorUnion#Fail, IndexedSeq[Chat]]] = for {
    team <- lift(findTeam(teamId))
    chatsWithSeen <- lift(listChats(team, reader, peek: Boolean))
  } yield chatsWithSeen

  override def listChats(team: Team, readerId: UUID, peek: Boolean): Future[\/[ErrorUnion#Fail, IndexedSeq[Chat]]] = for {
    reader <- lift(userRepository.find(readerId))
    chatsWithSeen <- lift(listChats(team, reader, peek: Boolean))
  } yield chatsWithSeen

  override def listChats(teamId: UUID, readerId: UUID, peek: Boolean): Future[\/[ErrorUnion#Fail, IndexedSeq[Chat]]] = for {
    team <- lift(findTeam(teamId))
    reader <- lift(userRepository.find(readerId))
    chatsWithSeen <- lift(listChats(team, reader, peek: Boolean))
  } yield chatsWithSeen

}
