import ca.shiftfocus.krispii.core.models.JournalEntry
import ca.shiftfocus.krispii.core.models.work.Work
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.Connection
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import org.scalatest._
import Matchers._
import scala.collection.immutable.TreeMap
import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import scalaz._

class WorkRepositorySpec
  extends TestEnvironment {
  val documentRepository = stub[DocumentRepository]
  val workRepository = new WorkRepositoryPostgres(documentRepository)

  // TODO - check with multiple revisions
  "WorkRepository.list" should {
    inSequence {
      "list the latest revision of work for each task in a project" in {
        val testUser = TestValues.testUserC
        val testProject = TestValues.testProjectA

        val testWorkList = TreeMap[Int, Work](
          0 -> TestValues.testLongAnswerWorkA
        )

        val result = workRepository.list(testUser, testProject)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks

        works.size should be(testWorkList.size)
      }
      "list latest revisions of a specific work for a user" in {
        val testUser = TestValues.testUserC
        val testTask = TestValues.testLongAnswerTaskA

        val testWorkList = TreeMap[Int, Work](
          0 -> TestValues.testLongAnswerWorkA
        )

        val result = workRepository.list(testUser, testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks

        works.size should be(testWorkList.size)
      }
      "list latest revisions of a specific work for all users" in {
        val testTask = TestValues.testLongAnswerTaskA

        val testWorkList = TreeMap[Int, Work](
          0 -> TestValues.testLongAnswerWorkA
        )

        val result = workRepository.list(testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks

        works.size should be(testWorkList.size)
      }
    }
  }

  // TODO - do with LongAnswerWork and ShortAnswerWork
  "WorkRepository.find" should {
    inSequence {
      "find the latest revision of a single work" in {
        val testWork     = TestValues.testLongAnswerWorkA
        val testDocument = TestValues.testDocumentA

        (documentRepository.find(_: UUID)(_: Connection)) when(testDocument.id, *) returns(Future.successful(\/-(testDocument)))

        val result = workRepository.find(testWork.id)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work) = eitherWork

        work.id should be(testWork.id)
      }
      "find the latest revision of a single work for a user" in {
        val testUser     = TestValues.testUserC
        val testTask     = TestValues.testLongAnswerTaskA
        val testWork     = TestValues.testLongAnswerWorkA
        val testDocument = TestValues.testDocumentA

        (documentRepository.find(_: UUID)(_: Connection)) when(testDocument.id, *) returns(Future.successful(\/-(testDocument)))

        val result = workRepository.find(testUser, testTask)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work) = eitherWork

        work.id should be(testWork.id)
      }
      "find a specific revision for a single work for a user" in {
        val testUser     = TestValues.testUserC
        val testTask     = TestValues.testLongAnswerTaskA
        val testWork     = TestValues.testLongAnswerWorkA
        val testDocument = TestValues.testDocumentA

        (documentRepository.find(_: UUID)(_: Connection)) when(testDocument.id, *) returns(Future.successful(\/-(testDocument)))

        val result = workRepository.find(testUser, testTask, testWork.version)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work) = eitherWork

        work.id should be(testWork.id)
      }
    }
  }
}
