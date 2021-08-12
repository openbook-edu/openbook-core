package ca.shiftfocus.krispii.core.services

import java.util.UUID

import ca.shiftfocus.krispii.core.error.{ErrorUnion, RepositoryError}
import ca.shiftfocus.krispii.core.models.Chat
import ca.shiftfocus.krispii.core.models.group.{Exam, Team}
import ca.shiftfocus.krispii.core.models.user.{Scorer, User, UserTrait}
import ca.shiftfocus.krispii.core.models.work.{Score, Test}
import ca.shiftfocus.krispii.core.repositories._
import scalaz.\/

import scala.collection.IndexedSeq
import scala.concurrent.Future

trait OmsService extends Service[ErrorUnion#Fail] {
  val chatRepository: ChatRepository
  val examRepository: ExamRepository
  val lastSeenRepository: LastSeenRepository
  val teamRepository: TeamRepository
  val testRepository: TestRepository
  val scoreRepository: ScoreRepository
  val scorerRepository: ScorerRepository
  val userRepository: UserRepository

  /* Better to repackage even the bread-and-butter functions here */

  def listMembers(teamId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[UserTrait]]]

  /* EXAM functions */

  def findExam(examId: UUID): Future[\/[ErrorUnion#Fail, Exam]]

  /* TEAM functions */

  def listTeams(exam: Exam): Future[\/[ErrorUnion#Fail, IndexedSeq[Team]]]
  def findTeam(teamId: UUID): Future[\/[ErrorUnion#Fail, Team]]
  def insertTeam(team: Team): Future[\/[ErrorUnion#Fail, Team]]
  def updateTeam(team: Team): Future[\/[ErrorUnion#Fail, Team]]
  def deleteTeam(team: Team): Future[\/[ErrorUnion#Fail, Team]]

  /* TEST functions */

  def findTest(testId: UUID): Future[\/[ErrorUnion#Fail, Test]]
  def updateTest(test: Test): Future[\/[ErrorUnion#Fail, Test]]

  def moveTests(testIds: IndexedSeq[UUID], newTeamId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Test]]]

  def randomizeTests(exam: Exam, ids: IndexedSeq[UUID] = IndexedSeq[UUID]()): Future[ErrorUnion#Fail \/ IndexedSeq[Test]]

  def automaticScoring(examId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Test]]]

  // duplicate (examId, name) will cause a RepositoryError.UniqueKeyConflict which should be handled by API

  /* SCORE functions */
  def findScore(scoreId: UUID): Future[\/[ErrorUnion#Fail, Score]]

  /* SCORER functions */

  def listScorers(teamId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Scorer]]]
  def listScorers(team: Team): Future[\/[ErrorUnion#Fail, IndexedSeq[Scorer]]]

  def addScorer(team: Team, user: User, leader: Boolean = false): Future[\/[RepositoryError.Fail, Team]]

  def updateScorer(team: Team, scorer: Scorer, leader: Option[Boolean], archived: Option[Boolean],
    deleted: Option[Boolean]): Future[\/[RepositoryError.Fail, Team]]

  def removeScorer(team: Team, scorerId: UUID): Future[\/[RepositoryError.Fail, Team]]

  /*def addScorers(team: Team, scorerList: IndexedSeq[User], leaderList: IndexedSeq[Boolean] = IndexedSeq(false)): Future[\/[RepositoryError.Fail, Team]]

  def removeScorers(team: Team, scorerIdList: IndexedSeq[UUID]): Future[\/[RepositoryError.Fail, Team]]*/

  /* CHAT functions */

  def listChats(team: Team): Future[\/[ErrorUnion#Fail, IndexedSeq[Chat]]]
  def listChats(teamId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Chat]]]
  def listChats(team: Team, reader: User, peek: Boolean): Future[\/[ErrorUnion#Fail, IndexedSeq[Chat]]]
  def listChats(teamId: UUID, reader: User, peek: Boolean): Future[\/[ErrorUnion#Fail, IndexedSeq[Chat]]]
  def listChats(team: Team, readerId: UUID, peek: Boolean): Future[\/[ErrorUnion#Fail, IndexedSeq[Chat]]]
  def listChats(teamId: UUID, readerId: UUID, peek: Boolean): Future[\/[ErrorUnion#Fail, IndexedSeq[Chat]]]

  /* Copies functions */

  def getUserCopies(userId: UUID): Future[\/[ErrorUnion#Fail, BigInt]]
  def incUserCopies(userId: UUID, n: Int = 1): Future[\/[ErrorUnion#Fail, BigInt]]
  def decUserCopies(userId: UUID, n: Int = 1): Future[\/[ErrorUnion#Fail, BigInt]]
  def deleteUserCopies(userId: UUID): Future[\/[ErrorUnion#Fail, BigInt]]

  def getOrgCopies(userId: UUID): Future[\/[ErrorUnion#Fail, BigInt]]
  def incOrgCopies(userId: UUID, n: Int = 1): Future[\/[ErrorUnion#Fail, BigInt]]
  def decOrgCopies(userId: UUID, n: Int = 1): Future[\/[ErrorUnion#Fail, BigInt]]
  def deleteOrgCopies(userId: UUID): Future[\/[ErrorUnion#Fail, BigInt]]
}
