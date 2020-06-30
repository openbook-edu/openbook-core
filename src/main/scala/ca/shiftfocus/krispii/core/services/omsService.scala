package ca.shiftfocus.krispii.core.services

import java.util.UUID

import ca.shiftfocus.krispii.core.error.ErrorUnion
import ca.shiftfocus.krispii.core.models.work.Test
import ca.shiftfocus.krispii.core.repositories.{ExamRepository, TeamRepository, TestRepository, UserRepository}
import scalaz.\/

import scala.concurrent.Future

trait OmsService extends Service[ErrorUnion#Fail] {
  val examRepository: ExamRepository
  val teamRepository: TeamRepository
  val testRepository: TestRepository
  val userRepository: UserRepository

  // don't implement the bread-and-butter functions here, can always use examRepository.list etc.!
  // duplicate (examId:name) will cause a RepositoryError.UniqueKeyConflict which should be handled by API

  def moveTests(testIds: IndexedSeq[UUID], newTeamId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Test]]]

  def randomizeTests(examId: UUID, onlyNewTests: Boolean = false): Future[\/[ErrorUnion#Fail, IndexedSeq[Test]]]

  def automaticScoring(examId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Test]]]

  // exporting CSVs seems more appropriate for krispii-api
  // def exportTests(examId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Test]]] // Unit?

  // scoreRepository cannot reference testRepository, so need to implement the following test here or in API:
  // a score can only be created (inserted) for (team, scorer) if the scorer is in the team of the test

}
