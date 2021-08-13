package ca.shiftfocus.krispii.core.services

import java.util.UUID

import ca.shiftfocus.krispii.core.error.{ErrorUnion, RepositoryError, ServiceError}
import ca.shiftfocus.krispii.core.models.{AccountStatus, Chat}
import ca.shiftfocus.krispii.core.models.group.{Exam, Team}
import ca.shiftfocus.krispii.core.models.user.{Scorer, User, UserTrait}
import ca.shiftfocus.krispii.core.models.work.{Score, Test}
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.github.mauricio.async.db.Connection
import play.api.Logger
import scalaz.Scalaz._
import scalaz.{-\/, \/, \/-}

import scala.collection.IndexedSeq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

class OmsServiceDefault(
    val db: DB,
    val accountRepository: AccountRepository,
    val chatRepository: ChatRepository,
    val copiesCountRepository: CopiesCountRepository,
    val examRepository: ExamRepository,
    val lastSeenRepository: LastSeenRepository,
    val teamRepository: TeamRepository,
    val testRepository: TestRepository,
    val scoreRepository: ScoreRepository,
    val scorerRepository: ScorerRepository,
    val userRepository: UserRepository,
    val organizationService: OrganizationService,
    val paymentService: PaymentService,
    val schoolService: SchoolService
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
   * Minimum item number for each team
   * @param nItems: Int - how many items are there to be distributed
   * @param groups: IndexedSeq[UUID] - IDs of all groups that should receive items
   * @return IndexedSeq[Int] number of items to give to each group
   */
  def minItems(nItems: Int, groups: IndexedSeq[UUID]): Map[UUID, Int] = {
    val lower = nItems / groups.length
    groups.map(g => g -> lower).toMap
  }

  /**
   * Return the difference between the real and desired number of items in each group
   * @param real Map[UUID, Int] with the number of items already assigned to each group
   * @param desired Map[UUID, Int] with the number of items that should be in each group
   * @return Map[UUID, Int] with the lacking items for each group
   */
  def topUp(real: Map[UUID, Int], desired: Map[UUID, Int]): Map[UUID, Int] = {
    /* (desired.keySet ++ real.keys).map { i => i -> (desired.get(i).getOrElse(0) - real.get(i).getOrElse(0)) } */
    val minusReal = real.transform((_, n) => -n)
    val redistribute = minusReal |+| desired
    redistribute.transform((_, n) => (if (n >= 0) n else 0))
  }

  /**
   * Choose randomly teams to give the excess items to.
   * @param nItems: Int - how many items are there to be distributed
   * @param groups: IndexedSeq[UUID] - IDs of all groups that should receive items
   * @return IndexedSeq[UUID] list of group IDs that will receive items
   */
  def surplusItems(nItems: Int, groups: IndexedSeq[UUID]): IndexedSeq[UUID] = nItems match {
    case n if n > 0 =>
      val excess = n % groups.length
      Random.shuffle(groups).slice(0, excess)
    case _ => IndexedSeq[UUID]()
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
   * @param ids IndexedSeq[UUID], the IDs of the tests to randomize, or empty to randomize all
   * @return the vector of updated tests, or an error
   */
  override def randomizeTests(
    exam: Exam,
    ids: IndexedSeq[UUID] = IndexedSeq[UUID]()
  ): Future[ErrorUnion#Fail \/ IndexedSeq[Test]] =
    for {
      allTests <- lift(testRepository.list(exam, fetchScores = false))
      toRandomize = ids match {
        case IndexedSeq() => allTests
        case _ => allTests.filter(t => ids.contains(t.id))
      }
      _ = Logger.debug(s"Randomize old assignments ${toRandomize.map(_.name) zip toRandomize.map(_.teamId.getOrElse("none"))}")

      teams <- lift(teamRepository.list(exam))
      teamIds = teams.filterNot(_.archived).map(_.id)
      real = allTests.diff(toRandomize).filter(_.teamId.isDefined).groupBy(_.teamId.get).mapValues(_.size)
      // _ = Logger.debug(s"Immutable distribution: $real")

      minEqual = minItems(allTests.length, teamIds)
      firstStep = topUp(real, minEqual)
      _ = Logger.debug(s"Randomize: would need to add to each team this many tests to equalize: $firstStep")

      excess = surplusItems(toRandomize.length - firstStep.values.sum, teamIds)
      _ = Logger.debug(s"Excess tests will be given to teams $excess")
      orderFill = stratRandom(firstStep) ++ excess
      _ = Logger.debug(s"Randomize: new team IDs ${toRandomize.map(_.name) zip orderFill}")

      randomizedTests <- lift(serializedT(toRandomize zip orderFill)(el =>
        testRepository.update(el._1.copy(teamId = Some(el._2)))))
    } yield randomizedTests

  override def automaticScoring(examId: UUID): Future[ErrorUnion#Fail \/ IndexedSeq[Test]] =
    for {
      /* reject automatic scoring with an error message if tests are not multiple score,
         or if the correct answers are not provided in the rubric */
      exam <- lift(examRepository.find(examId))
      existingTests <- lift(testRepository.list(exam, fetchScores = true))
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
   * @param peek: whether to leave the messages marked as "unseen"
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

  /********************* Counting student copies that have been uploaded **************************/

  override def getUserCopies(userId: UUID): Future[\/[ErrorUnion#Fail, Long]] =
    copiesCountRepository.get(entityType = "user", userId)

  /**
   * If the user has a Stripe plan and the limit has not been reached, increase the copy count.
   * @param userId UUID
   * @param n Int, default 1
   * @return a Future containing the increased copy count, or an error
   */
  override def incUserCopies(userId: UUID, n: Int = 1): Future[\/[ErrorUnion#Fail, Long]] =
    for {
      copies <- lift(getUserCopies(userId))
      account <- lift(accountRepository.getByUserId(userId))
      planId <- lift(account.subscriptions.headOption match {
        case Some(sub) => Future successful \/-(sub.planId)
        case None => Future successful -\/(ServiceError.PaymentRequired("No subscription"))
      })
      limit <- lift(schoolService.getPlanCopiesLimit(planId))
      _ <- predicate(limit >= copies + n)(ServiceError.LimitReached(
        "Reached copies limit"
      )) // this message will be overwritten in API
      newCopies <- lift(copiesCountRepository.inc(entityType = "organization", userId, n))
    } yield newCopies

  /**
   * Should only be run after it has been checked that a deleted test had no scores.
   */
  private def decUserCopies(userId: UUID, n: Int = 1): Future[\/[ErrorUnion#Fail, Long]] =
    copiesCountRepository.dec(entityType = "user", userId, n)

  /**
   * Remove the counter of copies for a user, equivalent to setting it to zero.
   * To be run at renewal of a payment plan.
   * @param userId UUID
   * @return a Future containing either the count before resetting, or an error
   */
  override def deleteUserCopies(userId: UUID): Future[\/[ErrorUnion#Fail, Long]] =
    copiesCountRepository.delete(entityType = "user", userId)

  override def getOrgCopies(orgId: UUID): Future[\/[ErrorUnion#Fail, Long]] =
    copiesCountRepository.get(entityType = "organization", orgId)

  /**
   * Increase the copy count for the organization unless the limit has been reached.
   * @param orgId UUID
   * @param n amount to be increased (default 1)
   * @return a Future containing either the increased count, or an error
   */
  override def incOrgCopies(orgId: UUID, n: Int = 1): Future[\/[ErrorUnion#Fail, Long]] =
    for {
      copies <- lift(getOrgCopies(orgId))
      limit <- lift(schoolService.getOrganizationCopiesLimit(orgId))
      _ <- predicate(limit >= copies + n)(ServiceError.LimitReached(
        "Reached copies limit"
      )) // this message will be overwritten in API
      newCopies <- lift(copiesCountRepository.inc(entityType = "organization", orgId, n))
    } yield newCopies

  /**
   * Should only be run after it has been checked that a deleted test had no scores.
   */
  private def decOrgCopies(orgId: UUID, n: Int = 1): Future[\/[ErrorUnion#Fail, Long]] =
    copiesCountRepository.dec(entityType = "organization", orgId, n)

  /**
   * Remove the counter of copies for an organization, equivalent to setting it to zero.
   * To be run at renewal of an organization payment plan.
   * @param userId UUID
   * @return Future containing either the count before deleting, or an error
   */
  override def deleteOrgCopies(userId: UUID): Future[\/[ErrorUnion#Fail, Long]] =
    copiesCountRepository.delete(entityType = "organization", userId)

  /**
   * Get the unique ID of the first organization a user is member in.
   * TODO: change to ExamCoordinators; listByMember; guarantee that examCoordinators are members in only one organization
   * @param user User
   * @return Future containing either the organization's ID, or an error
   */
  override def getOrgId(user: User): Future[\/[ErrorUnion#Fail, UUID]] =
    organizationService.listByAdmin(user.email).map {
      case \/-(orgList) if orgList.nonEmpty => \/-(orgList.head.id)
      case \/-(_) => -\/(ServiceError.PaymentRequired("User is member of no organization"))
      case -\/(error) => -\/(error)
    }

  /**
   * Get the current number of copies debited to the first organization a user is member in.
   * TODO: change to ExamCoordinators; listByMember; guarantee that examCoordinators are members in only one organization
   * @param user User
   * @return Future containing either the copies already debited to the organization, or an error
   */
  override def getOrgCopies(user: User): Future[\/[ErrorUnion#Fail, Long]] =
    for {
      orgId <- lift(getOrgId(user))
      copies <- lift(getOrgCopies(orgId))
    } yield copies

  override def getCopies(user: User): Future[\/[ErrorUnion#Fail, Long]] =
    for {
      account <- lift(paymentService.getAccount(user.id))
      copies <- lift(account.status match {
        case AccountStatus.group => getOrgCopies(user)
        case AccountStatus.paid => getUserCopies(user.id)
        case AccountStatus.free => Future successful \/-(0.toLong)
        case _ => Future successful -\/(ServiceError.BadInput("User needs to pay to upload student copies"))
      })
    } yield copies

  /**
   * Increment the number of copies of the user's organization.
   * @param user User
   * @return Future containing either the new number of copies debited to the organization, or an error
   */
  override def getOrgAndIncCopy(user: User): Future[\/[ErrorUnion#Fail, Long]] =
    for {
      orgId <- lift(getOrgId(user))
      newCopies <- lift(incOrgCopies(orgId))
    } yield newCopies

  /**
   * Debit the uploading of one student copy either to the user's organization or the user's personal account.
   * @param user User
   * @return Future containing either the new number of the debited copies, or an error
   */
  override def incCopy(user: User): Future[\/[ErrorUnion#Fail, Long]] =
    for {
      account <- lift(paymentService.getAccount(user.id))
      newCopies <- lift(account.status match {
        case AccountStatus.group => getOrgAndIncCopy(user)
        case AccountStatus.paid => incUserCopies(user.id)
        case AccountStatus.free => Future successful \/-(0.toLong)
        case _ => Future successful -\/(ServiceError.BadInput("User needs to pay to upload student copies"))
      })
    } yield newCopies

  /**
   * Decrement the number of copies of the user's organization.
   * This should only be run after it has been checked that the deleted test had no scores.
   * @param user User
   * @return Future containing either the new number of the copies debited to the organization, or an error
   */
  private def getOrgAndDecCopy(user: User): Future[\/[ErrorUnion#Fail, Long]] =
    for {
      orgId <- lift(getOrgId(user))
      newCopies <- lift(decOrgCopies(orgId))
    } yield newCopies

  /**
   * If no scores have yet been created for a test (no scorer has viewed the test copy), then deleting the test will
   * also decrease the number of copies debited to the owner's organization.
   * @param test Test
   * @param user User
   * @return Future containing either the (possibly corrected) number of copies, or an error
   */
  override def maybeGetOrgAndDecCopy(test: Test, user: User): Future[\/[ErrorUnion#Fail, Long]] =
    if (test.scores.isEmpty)
      getOrgAndDecCopy(user)
    else
      getOrgCopies(user)

  /**
   * If no scores have yet been created for a test (no scorer has viewed the test copy), then deleting the test will
   * also decrease the number of copies debited to the owner's account.
   * @param test Test
   * @param user User
   * @return Future containing either the (possibly corrected) number of copies, or an error
   */
  override def maybeUserDecCopy(test: Test, user: User): Future[\/[ErrorUnion#Fail, Long]] =
    if (test.scores.isEmpty)
      getUserCopies(user.id)
    else
      decUserCopies(user.id)

  /**
   * If no scores have yet been created for a test (no scorer has viewed the test copy), then reduce the debit of
   * either the context user's organization or the context user's personal account.
   * @param test Test
   * @param user User
   * @return Future containing either the (possibly corrected) number of debited copies, or an error
   */
  override def maybeDecCopy(test: Test, user: User): Future[\/[ErrorUnion#Fail, Long]] =
    for {
      account <- lift(paymentService.getAccount(user.id))
      newCopies <- lift(account.status match {
        case AccountStatus.group => maybeGetOrgAndDecCopy(test, user)
        case AccountStatus.paid => maybeUserDecCopy(test, user)
        case AccountStatus.free => Future successful \/-(0.toLong)
        case _ => Future successful -\/(ServiceError.BadInput("User needs to pay to upload student copies"))
      })
    } yield newCopies

}
