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
import scalaz.Scalaz._

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
   * @param idList: list of team IDs
   * @return
   */
  def randomId(idList: IndexedSeq[UUID]): UUID =
    idList(Random.nextInt(idList.length))

  /**
   * Distribute the available items randomly as fair as possible
   * @param nItems: Int - how many items are there to be distributed
   * @param nGroups: Int - how many groups to distribute
   * @return IndexedSeq[Int] number of items to give to each group
   */
  def apprFractions(nItems: Int, nGroups: Int): IndexedSeq[Int] = {
    val lower = nItems / nGroups
    val excess = nItems % nGroups
    val before = IndexedSeq.fill(excess)(lower + 1) ++ IndexedSeq.fill(nGroups - excess)(lower)
    Random.shuffle(before)
  }

  /**
   * Take a Map (teamId: how many tests to add) and returns a shuffled list of team IDs to assign to each test.
   * Negative or zero values mean that team won't get any tests.
   * The list may be longer than there are tests available, in which case the last team IDs will be ignored.
   * @param toFill: Map[UUID, Int] teamId -> how many tests to add
   * @return IndexedSeq[UUID] shuffled list of team IDs
   */
  def stratRandom(toFill: Map[UUID, Int]): IndexedSeq[UUID] = {
    val r = toFill.values.max
    val allIds = (r to 1 by -1).map(i => toFill.filter(el => el._2 >= i).keys.toList)
    allIds.flatMap(el => Random.shuffle(el))
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
      toRandomize = allTests filter (all || _.teamId.isEmpty)
      // _ = Logger.debug(s"Tests to be randomized in exam ${exam.name}: ${toRandomize.map(_.name)}")

      teams <- lift(teamRepository.list(exam))
      teamIds = teams.filterNot(_.archived).map(_.id)
      _ = Logger.debug(s"Teams in exam ${exam.name}: $teamIds")

      // real = allTests.diff(toRandomize).filter(_.teamId.isDefined).groupBy(_.teamId.get).mapValues(_.size)
      // _ = Logger.debug(s"Constant distribution (not to be changed): $real")

      desired = (teamIds zip apprFractions(allTests.length, teamIds.length)).toMap
      // _ = Logger.debug(s"Desired distribution for all tests: $desired")

      /* toFill = (desired.keySet ++ real.keys).map { i => i -> (desired.get(i).getOrElse(0) - real.get(i).getOrElse(0)) }
      _ = Logger.debug(s"Will add to each team: $toFill") */
      minusReal = allTests.diff(toRandomize).filter(_.teamId.isDefined).groupBy(_.teamId.get).mapValues(-_.size)
      toFill = minusReal |+| desired
      _ = Logger.debug(s"Randomize: will add to each team this many tests: $toFill")

      _ = Logger.debug(s"Randomize: old team IDs ${toRandomize.map(_.name) zip toRandomize.map(_.teamId.getOrElse("none"))}")
      orderFill = stratRandom(toFill)
      _ = Logger.debug(s"Randomize: wew team IDs ${toRandomize.map(_.name) zip orderFill}")

      randomizedTests <- lift(serializedT(toRandomize zip orderFill)(el =>
        testRepository.update(el._1.copy(teamId = Some(el._2)))))
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
