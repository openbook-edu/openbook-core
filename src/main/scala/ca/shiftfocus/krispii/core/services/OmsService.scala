package ca.shiftfocus.krispii.core.services

import java.util.UUID

import ca.shiftfocus.krispii.core.error.ErrorUnion
import ca.shiftfocus.krispii.core.models.Chat
import ca.shiftfocus.krispii.core.models.group.{Exam, Team}
import ca.shiftfocus.krispii.core.models.user.{Scorer, UserTrait}
import ca.shiftfocus.krispii.core.models.work.Test
import ca.shiftfocus.krispii.core.repositories.{ExamRepository, TeamRepository, TestRepository, UserRepository}
import scalaz.\/

import scala.collection.IndexedSeq
import scala.concurrent.Future

trait OmsService extends Service[ErrorUnion#Fail] {
  val examRepository: ExamRepository
  val teamRepository: TeamRepository
  val testRepository: TestRepository
  val userRepository: UserRepository

  /* Don't repackage all the bread-and-butter functions here, can always use examRepository.list etc.!
     However, sometimes hard to avoid */
  def findExam(examid: UUID): Future[\/[ErrorUnion#Fail, Exam]]

  def findTeam(teamid: UUID): Future[\/[ErrorUnion#Fail, Team]]

  def findTest(testId: UUID): Future[\/[ErrorUnion#Fail, Test]]

  def updateTest(test: Test): Future[\/[ErrorUnion#Fail, Test]]

  // duplicate (examId, name) will cause a RepositoryError.UniqueKeyConflict which should be handled by API

  def listScorers(teamId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Scorer]]]

  def listMembers(teamId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[UserTrait]]]

  def moveTests(testIds: IndexedSeq[UUID], newTeamId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Test]]]

  def randomizeTests(exam: Exam, all: Boolean = false): Future[\/[ErrorUnion#Fail, IndexedSeq[Test]]]

  def automaticScoring(examId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Test]]]

  def listChats(teamId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Chat]]]

  // exporting CSVs seems more appropriate to handle in krispii-api
  // def exportTests(examId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Test]]] // Unit?

  // scoreRepository cannot reference testRepository, so need to implement the following test here or in API:
  // a score can only be created (inserted) for (team, scorer) if the scorer is in the team of the test

}
