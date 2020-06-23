package ca.shiftfocus.krispii.core.services

import java.util.UUID

import ca.shiftfocus.krispii.core.error.ErrorUnion
import ca.shiftfocus.krispii.core.models.work.Test
import ca.shiftfocus.krispii.core.repositories.{ExamRepository, TeamRepository, TestRepository}

import scala.concurrent.Future
import scalaz.\/

trait ExamService extends Service[ErrorUnion#Fail] {
  val examRepository: ExamRepository
  val teamRepository: TeamRepository
  val testRepository: TestRepository

  // don't implement the bread-and-butter functions here, can always use examRepository.list etc.!
  // duplicate (examId:name) will cause a RepositoryError.UniqueKeyConflict which should be handled by API

  def moveTests(testIds: IndexedSeq[UUID], newTeamId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Test]]]

  def randomizeTests(examId: UUID, onlyNewTests: Boolean = false): Future[\/[ErrorUnion#Fail, IndexedSeq[Test]]]

  // exporting CSVs seems more appropriate for krispii-api
  // def exportTests(examId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Test]]] // Unit?

}
