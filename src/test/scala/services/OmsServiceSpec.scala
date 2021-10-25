import org.scalatest._
import Matchers._
import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.group.{Exam, Team}
import ca.shiftfocus.krispii.core.models.user.{Scorer, User}
import ca.shiftfocus.krispii.core.models.work.{Score, Test}
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services.{OmsServiceDefault, OrganizationService, PaymentService, SchoolService}
import ca.shiftfocus.krispii.core.services.datasource._
import com.github.mauricio.async.db.Connection
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import scala.concurrent.{Await, Future}
import scalaz._

import java.util.UUID
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits._

@RunWith(classOf[JUnitRunner])
class OmsServiceSpec
    extends TestEnvironment(writeToDb = false) {
  val db = stub[DB]
  val mockConnection = stub[Connection]

  val accountRepository = stub[AccountRepository]
  val chatRepository = stub[ChatRepository]
  val copiesCountRepository = stub[CopiesCountRepository]
  val examRepository = stub[ExamRepository]
  val lastSeenRepository = stub[LastSeenRepository]
  val limitRepository = stub[LimitRepository]
  val roleRepository = stub[RoleRepository]
  val teamRepository = stub[TeamRepository]
  val testRepository = stub[TestRepository]
  val scoreRepository = stub[ScoreRepository]
  val scorerRepository = stub[ScorerRepository]
  val userRepository = stub[UserRepository]
  val organizationService = stub[OrganizationService]
  val paymentService = stub[PaymentService]
  val schoolService = stub[SchoolService]

  val omsService = new OmsServiceDefault(db, accountRepository, chatRepository, copiesCountRepository, examRepository,
    lastSeenRepository, limitRepository, roleRepository, teamRepository, testRepository, scoreRepository, scorerRepository,
    userRepository, organizationService, paymentService, schoolService) {
    override implicit def conn: Connection = mockConnection
    override def transactional[A](f: Connection => Future[A]): Future[A] = {
      f(mockConnection)
    }
  }

  "OmsService.findExam" should {
    val testExam = TestValues.testExamA

    inSequence {
      "return exam if the identifier exists" in {
        (examRepository.find(_: UUID)(_: Connection)) when (testExam.id, *) returns (Future.successful(\/-(testExam)))

        val fExam = omsService.findExam(testExam.id)

        Await.result(fExam, Duration.Inf) should be (\/-(testExam))
      }

      "return RepositoryError.NoResults if exam doesn't exist" in {
        (examRepository.find(_: UUID)(_: Connection)) when (testExam.id, *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))

        val fExam = omsService.findExam(testExam.id)
        Await.result(fExam, Duration.Inf) should be (-\/(RepositoryError.NoResults("")))
      }
    }
  }

  "OmsService.findTeam" should {
    val testTeam = TestValues.testTeamA
    inSequence {
      "return team if identifier exists" in {
        (teamRepository.find(_: UUID)(_: Connection)) when (testTeam.id, *) returns (Future.successful(\/-(testTeam)))

        val fTeam = omsService.findTeam(testTeam.id)
        Await.result(fTeam, Duration.Inf) should be (\/-(testTeam))
      }

      "return RepositoryError.NoResults if team doesn't exist" in {
        (teamRepository.find(_: UUID)(_: Connection)) when (testTeam.id, *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))

        val fExam = omsService.findTeam(testTeam.id)
        Await.result(fExam, Duration.Inf) should be (-\/(RepositoryError.NoResults("")))
      }
    }
  }

  "OmsService.listTeam" should {
    val testExam = TestValues.testExamA
    inSequence {
      "return all teams assigned if exam exists" in {
        val listTeams = IndexedSeq(TestValues.testTeamA)
        (teamRepository.list(_: Exam)(_: Connection)) when (testExam, *) returns (Future.successful(\/-(listTeams)))

        val fTeamList = omsService.listTeams(testExam)
        Await.result(fTeamList, Duration.Inf) should be (\/-(listTeams))
      }

      "return RepositoryError.NoResults if exam doesn't exist" in {
        (teamRepository.list(_: Exam)(_: Connection)) when (testExam, *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))

        val fTeamList = omsService.listTeams(testExam)
        Await.result(fTeamList, Duration.Inf) should be (-\/(RepositoryError.NoResults("")))
      }
    }
  }

  "OmsService.updateTeam" should {
    val testTeam = TestValues.testTeamA
    inSequence {
      "update team with ALL values" in {
        val updatedTeam = testTeam.copy(
          version = testTeam.version + 1,
          examId = TestValues.testExamB.id,
          ownerId = TestValues.testUserA.id,
          name = "beauport",
          slug = "slug beauport"
        )

        (teamRepository.update(_: Team)(_: Connection)) when(testTeam, *) returns (Future.successful(\/-(updatedTeam)))

        val result = omsService.updateTeam(testTeam)
        val team = Await.result(result, Duration.Inf)
        val \/-(newTeam) = team

        newTeam.id should be(updatedTeam.id)
        newTeam.version should be(updatedTeam.version)
        newTeam.examId should be(updatedTeam.examId)
        newTeam.ownerId should be(updatedTeam.ownerId)
        newTeam.name should be(updatedTeam.name)
        newTeam.slug should be(updatedTeam.slug)
      }

      "update team with unique values (name, examId)" in {
        val updatedTeam = testTeam.copy(
          examId = TestValues.testExamB.id,
          name = "beauport"
        )
        (teamRepository.update(_: Team)(_: Connection)) when(testTeam, *) returns (Future.successful(\/-(updatedTeam)))

        val result = omsService.updateTeam(testTeam)
        val team = Await.result(result, Duration.Inf)
        val \/-(newTeam) = team

        newTeam.id should be(updatedTeam.id)
        newTeam.version should be(updatedTeam.version)
        newTeam.examId should be(updatedTeam.examId)
        newTeam.ownerId should be(updatedTeam.ownerId)
        newTeam.name should be(updatedTeam.name)
        newTeam.slug should be(updatedTeam.slug)
      }

      "return RepositoryError.NoResults if team doesn't exist" in {
        (teamRepository.update(_: Team)(_: Connection)) when(testTeam, *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))

        val fTeam = omsService.updateTeam(testTeam)

        Await.result(fTeam, Duration.Inf) should be (-\/(RepositoryError.NoResults("")))
      }
    }
  }

  "omsService.deleteTeam" should {
    inSequence {
      "return deleted team if exists" in {
        val teamToDelete = TestValues.testTeamA
        (teamRepository.delete(_: Team)(_: Connection)) when(teamToDelete, *) returns (Future.successful(\/-(teamToDelete)))

        val result = omsService.deleteTeam(teamToDelete)

        Await.result(result, Duration.Inf) should be(\/-(teamToDelete))
      }
    }

    "return RepositoryError.NoResult if team doesn't exist" in {
      val teamToDelete = TestValues.testTeamB
      (teamRepository.delete(_: Team)(_: Connection)) when(teamToDelete, *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))

      val result = omsService.deleteTeam(teamToDelete)

      Await.result(result, Duration.Inf) should be (-\/(RepositoryError.NoResults("")))
    }
  }

  "omsService.findTest" should {
    inSequence {
      "return Test if exists" in {
        val testTest = TestValues.testTestA

        (testRepository.find(_: UUID)(_: Connection)) when (testTest.id, *) returns (Future.successful(\/-(testTest)))

        val fTest = omsService.findTest(testTest.id)

        Await.result(fTest, Duration.Inf) should be (\/-(testTest))
      }

      "return RepositoryError.NoResult if test doesn't exist" in {
        val testTest = TestValues.testTestA
        (testRepository.find(_: UUID)(_: Connection)) when (testTest.id, *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))

        val fTest = omsService.findTest(testTest.id)

        Await.result(fTest, Duration.Inf) should be (-\/(RepositoryError.NoResults("")))
      }
    }
  }

  "omsService.updateTest" should {
    inSequence {
      "return updated test if test exists" in {
        val testTest = TestValues.testTestA
        val updatedTest = testTest.copy(
          name = "oncology",
          grade = "6th of medicine",
        )
        (testRepository.update(_: Test)(_: Connection)) when (testTest, *) returns (Future.successful(\/-(updatedTest)))

        val result = omsService.updateTest(testTest)
        val test = Await.result(result, Duration.Inf)
        val \/-(fTest) = test

        fTest.id should be (updatedTest.id)
        fTest.examId should be (updatedTest.examId)
        fTest.version should be (updatedTest.version)
        fTest.name should be (updatedTest.name)
        fTest.grade should be (updatedTest.grade)
      }

      "return RepositoryError.NoResults if test doesn't exist" in {
        val testTest = TestValues.testTestA
        (testRepository.update(_: Test)(_: Connection)) when (testTest, *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))

        val result = omsService.updateTest(testTest)

        Await.result(result, Duration.Inf) should be (-\/(RepositoryError.NoResults("")))
      }
    }
  }

  "omsService.moveTests" should {
    val testTestA = TestValues.testTestA.copy(teamId = Some(TestValues.testTeamA.id))
    val testTestB = TestValues.testTestB.copy(teamId = Some(TestValues.testTeamA.id))
    val testIds = IndexedSeq(testTestA.id, testTestB.id)
    inSequence {
      "assign several tests to a specific team" in {
        val testToMove = IndexedSeq(testTestA, testTestB)
        (testRepository.find(_: UUID)(_: Connection)) when (testTestA.id, *) returns (Future.successful(\/-(testTestA)))
        (testRepository.find(_: UUID)(_: Connection)) when (testTestB.id, *) returns (Future.successful(\/-(testTestB)))

        val fTests = omsService.moveTests(testIds, TestValues.testTeamA.id)

        Await.result(fTests, Duration.Inf) should be (\/-(testToMove))
      }

      "return RepositoryError.NoResults if one test doesn't exist" in {
        (testRepository.find(_: UUID)(_: Connection)) when (testTestA.id, *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (testRepository.find(_: UUID)(_: Connection)) when (testTestB.id, *) returns (Future.successful(\/-(testTestB)))

        val fTests = omsService.moveTests(testIds, TestValues.testTeamA.id)

        Await.result(fTests, Duration.Inf) should be (-\/(RepositoryError.NoResults("")))
      }
    }
  }

  "omsService.randomizeTests" should {
    val testTestA = TestValues.testTestA
    val testTestB = TestValues.testTestB
    val testList = IndexedSeq(testTestA.copy(teamId = Some(TestValues.testTeamB.id)),
      testTestB.copy(teamId = Some(TestValues.testTeamA.id)))
    val testIds = IndexedSeq(testTestA.id, testTestB.id)
    val teamList = IndexedSeq(TestValues.testTeamB, TestValues.testTeamA)
    val exam = TestValues.testExamA
    inSequence {
      "randomize all tests to the existing teams" in {
        (testRepository.list(_: Exam, _: Boolean)(_: Connection)) when (exam, *, *) returns (Future.successful(\/-(testList)))
        (teamRepository.list(_: Exam)(_: Connection)) when (exam, *) returns (Future.successful(\/-(teamList)))
        (testRepository.update(_: Test)(_: Connection)) when (testTestA, *) returns (Future.successful(\/-(testTestA.copy(teamId = Some(TestValues.testTeamB.id)))))
        (testRepository.update(_: Test)(_: Connection)) when (testTestB, *) returns (Future.successful(\/-(testTestB.copy(teamId = Some(TestValues.testTeamA.id)))))

        val fRandomizedTests = omsService.randomizeTests(exam, testIds)

        Await.result(fRandomizedTests, Duration.Inf) should be (\/-(testList))
      }
    }
  }

  "omsService.findScore" should {
    val score = TestValues.testScoreA
    inSequence {
      "return score if exists" in {
        (scoreRepository.find(_: UUID)(_: Connection)) when (score.id, *) returns (Future.successful(\/-(score)))

        val fScore = omsService.findScore(score.id)

        Await.result(fScore, Duration.Inf) should be (\/-(score))
      }

      "return RepositoryError.Noresults if score doesn't exist" in {
        (scoreRepository.find(_: UUID)(_: Connection)) when (score.id, *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))

        val fScore = omsService.findScore(score.id)

        Await.result(fScore, Duration.Inf) should be (-\/(RepositoryError.NoResults("")))
      }
    }
  }

  "omsService.listScorers" should {
    val team = TestValues.testTeamA
    inSequence {
      "return Scorers list if team exists" in {
        val scorers = IndexedSeq(TestValues.testScorerA, TestValues.testScorerB)
        (scorerRepository.list(_: Team)(_: Connection)) when(team, *) returns (Future.successful(\/-(scorers)))

        val fScorers = omsService.listScorers(team)

        Await.result(fScorers, Duration.Inf) should be(\/-(scorers))
      }

      "return RepositoryError.NoResults if team doesn't exist" in {
        (scorerRepository.list(_: Team)(_: Connection)) when(team, *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))

        val fNoScorers = omsService.listScorers(team)

        Await.result(fNoScorers, Duration.Inf) should be(-\/(RepositoryError.NoResults("")))
      }
    }
  }

  "omsService.addScorer" should {
    val team = TestValues.testTeamA
    val scorers = IndexedSeq(TestValues.testScorerA)
    val updatedTeam = team.copy(scorers = scorers)
    val scorerToAdd = TestValues.testUserA
    inSequence {
      "return scorer list with the new scorer" in {
        (scorerRepository.addScorer(_: Team, _: User, _: Boolean)(_: Connection)) when (team, scorerToAdd, *, *) returns (Future.successful(\/-(updatedTeam.scorers)))
        (teamRepository.find(_: UUID)(_: Connection)) when (team.id, *) returns (Future.successful(\/-(updatedTeam)))

        val fTeam = omsService.addScorer(team, scorerToAdd)
        val result = Await.result(fTeam, Duration.Inf)
        val \/-(newTeam) = result

        newTeam.scorers should be (updatedTeam.scorers)
        newTeam.id should be (updatedTeam.id)
        newTeam.name should be (updatedTeam.name)
      }

      "return RepositoryError if team doesn't exist" in {
        (scorerRepository.addScorer(_: Team, _: User, _: Boolean)(_: Connection)) when (team, scorerToAdd, *, *) returns (Future.successful(\/-(updatedTeam.scorers)))
        (teamRepository.find(_: UUID)(_: Connection)) when (team.id, *) returns (Future.successful(-\/(RepositoryError.NoResults(s"Scorer ${scorerToAdd.email} could not be added to team ${team.id} : ${team} doesn't exist"))))

        val result = omsService.addScorer(team, scorerToAdd)

        Await.result(result, Duration.Inf) should be (-\/(RepositoryError.NoResults(s"Scorer ${scorerToAdd.email} could not be added to team ${team.id} : ${team} doesn't exist")))

      }
    }
  }

  "omsService.updatedScorer" should {
    val scorer = TestValues.testScorerA
    val updatedScorer = scorer.copy(
      isDeleted = true,
      isArchived = true
    )
    inSequence {
      "return updated team with new values of scorer" in {
        val scorerTeam = TestValues.testTeamA.copy(scorers = IndexedSeq(scorer))

        (scorerRepository.updateScorer(_: Team, _: Scorer, _: Option[Boolean], _: Option[Boolean], _: Option[Boolean])(_: Connection)) when(scorerTeam, scorer, Option(false), Option(true), Option(true), *) returns (Future.successful(\/-(updatedScorer)))
        (teamRepository.find(_: UUID)(_: Connection)) when (scorerTeam.id, *) returns (Future.successful(\/-(scorerTeam)))

        val fTeam = omsService.updateScorer(scorerTeam, scorer, Option(false), Option(true), Option(true))
        val result = Await.result(fTeam, Duration.Inf)
        val \/-(updatedTeam) = result

        updatedTeam.scorers should be (IndexedSeq(updatedScorer))
        updatedTeam.id should be (scorerTeam.id)
        updatedTeam.name should be (scorerTeam.name)
      }

      "return RepositoryError.BadParam if scorer doesn't exist" in {
        val scorerTeam = TestValues.testTeamA

        (scorerRepository.updateScorer(_: Team, _: Scorer, _: Option[Boolean], _: Option[Boolean], _: Option[Boolean])(_: Connection)) when(scorerTeam, scorer, Option(false), Option(true), Option(true), *) returns (Future.successful(-\/(RepositoryError.BadParam(s"Nothing to update for scorer ${scorer.email} in team ${scorerTeam.id}"))))
        (teamRepository.find(_: UUID)(_: Connection)) when (scorerTeam.id, *) returns (Future.successful(-\/(RepositoryError.BadParam(s"Nothing to update for scorer ${scorer.email} in team ${scorerTeam.id}"))))

        val result = omsService.updateScorer(scorerTeam, scorer, Option(false), Option(true), Option(true))

        Await.result(result, Duration.Inf) should be (-\/(RepositoryError.BadParam(s"Nothing to update for scorer ${scorer.email} in team ${scorerTeam.id}")))
      }

      "return RepositoryError if team doesn't exist" in {
        val scorerTeam = TestValues.testTeamA
        (scorerRepository.updateScorer(_: Team, _: Scorer, _: Option[Boolean], _: Option[Boolean], _: Option[Boolean])(_: Connection)) when(scorerTeam, scorer, Option(false), Option(true), Option(true), *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (teamRepository.find(_: UUID)(_: Connection)) when (scorerTeam.id, *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))

        val result = omsService.updateScorer(scorerTeam, scorer, Option(false), Option(true), Option(true))

        Await.result(result, Duration.Inf) should be (-\/(RepositoryError.NoResults("")))
      }
    }
  }

  "omsService.removeScorer" should {
    inSequence {
      "return team without the deleted scorer if scorer exists" in {
        val scorersList = IndexedSeq(TestValues.testScorerA, TestValues.testScorerB)
        val team = TestValues.testTeamA.copy(scorers = scorersList)
        val updatedTeam = TestValues.testTeamA.copy(scorers = IndexedSeq(scorersList(0)))
        val scorerToDelete = scorersList(1)
        (scorerRepository.removeScorer(_: Team, _: UUID)(_: Connection)) when (team, scorerToDelete.id, *) returns (Future.successful(\/-(scorerToDelete)))
        (teamRepository.find(_: UUID)(_: Connection)) when (team.id, *) returns (Future.successful(\/-(updatedTeam)))

        val result = omsService.removeScorer(team, scorerToDelete.id)

        Await.result(result, Duration.Inf) should be (\/-(updatedTeam))
      }
    }

    "return same team if scorer doesn't exist" in {
      val team = TestValues.testTeamA
      val scorerToDelete = TestValues.testScorerA
      (scorerRepository.removeScorer(_: Team, _: UUID)(_: Connection)) when (team, scorerToDelete.id, *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
      (teamRepository.find(_: UUID)(_: Connection)) when (team.id, *) returns (Future.successful(\/-(team)))

      val result = omsService.removeScorer(team, scorerToDelete.id)

      Await.result(result, Duration.Inf) should be (\/-(team))
    }

    "return RepositoryError if team doesn't exist" in {
      val team = TestValues.testTeamA
      val scorerToDelete = TestValues.testScorerA
      (scorerRepository.removeScorer(_: Team, _: UUID)(_: Connection)) when (team, scorerToDelete.id, *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
      (teamRepository.find(_: UUID)(_: Connection)) when (team.id, *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))

      val result = omsService.removeScorer(team, scorerToDelete.id)

      Await.result(result, Duration.Inf) should be (-\/(RepositoryError.NoResults("")))
    }
  }
}