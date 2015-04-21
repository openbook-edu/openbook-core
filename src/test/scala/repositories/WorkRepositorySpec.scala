import ca.shiftfocus.krispii.core.models.JournalEntry
import ca.shiftfocus.krispii.core.models.work.Work
import ca.shiftfocus.krispii.core.models.document._
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
  val revisionRepository = stub[RevisionRepository]
  val workRepository = new WorkRepositoryPostgres(documentRepository, revisionRepository)

  "WorkRepository.list" should {
    inSequence {
      "list the latest revision of work for each task in a project for a user (MultipleChoiceWork, LongAnswerWork, ShortAnswerWork, OrderingWork)" in {
        val testUser = TestValues.testUserC
        val testProject = TestValues.testProjectA

        val testWorkList = TreeMap[Int, Work](
          0 -> TestValues.testLongAnswerWorkA,
          1 -> TestValues.testShortAnswerWorkG,
          2 -> TestValues.testMultipleChoiceWorkC,
          3 -> TestValues.testOrderingWorkD
        )

        val testDocumentList = TreeMap[Int, Document](
          0 -> TestValues.testDocumentA,
          1 -> TestValues.testDocumentD
        )

        testDocumentList.foreach {
          case (key, document: Document) => {
            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(document.id, *, *) returns(Future.successful(\/-(document)))
          }
        }

        val result = workRepository.list(testUser, testProject)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks

        works.size should be(testWorkList.size)

        testWorkList.foreach {
          case (key, work: Work) => {
            works(key).id should be(work.id)
            works(key).studentId should be(work.studentId)
            works(key).taskId should be(work.taskId)
            works(key).version should be(work.version)
            works(key).response should be(work.response)
            works(key).isComplete should be(work.isComplete)
            works(key).createdAt.toString should be(work.createdAt.toString)
            works(key).updatedAt.toString should be(work.updatedAt.toString)
          }
        }
      }
      "list the latest revision of work for each task in a project for a user (MatchingWork)" in {
        val testUser = TestValues.testUserC
        val testProject = TestValues.testProjectB

        val testWorkList = TreeMap[Int, Work](
          0 -> TestValues.testMatchingWorkE
        )

        val result = workRepository.list(testUser, testProject)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks
        works.size should be(testWorkList.size)

        testWorkList.foreach {
          case (key, work: Work) => {
            works(key).id should be(work.id)
            works(key).studentId should be(work.studentId)
            works(key).taskId should be(work.taskId)
            works(key).version should be(work.version)
            works(key).response should be(work.response)
            works(key).isComplete should be(work.isComplete)
            works(key).createdAt.toString should be(work.createdAt.toString)
            works(key).updatedAt.toString should be(work.updatedAt.toString)
          }
        }
      }
//      "list the latest revisions of a specific work for a user (LongAnswerWork)" in {
//        val testUser = TestValues.testUserE
//        val testTask = TestValues.testLongAnswerTaskA
//
//        val testWorkList = TreeMap[Int, Work](
//          0 -> TestValues.testLongAnswerWorkF
//        )
//
//        val testDocumentList = TreeMap[Int, Document](
//          0 -> TestValues.testDocumentA
//        )
//
//        testDocumentList.foreach {
//          case (key, document: Document) => {
//            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(document.id, *, *) returns(Future.successful(\/-(document)))
//          }
//        }
//
//        val result = workRepository.list(testUser, testTask)
//        val eitherWorks = Await.result(result, Duration.Inf)
//        val \/-(works) = eitherWorks
//
//        works.size should be(testWorkList.size)
//
//        testWorkList.foreach {
//          case (key, work: Work) => {
//            works(key).id should be(work.id)
//            works(key).studentId should be(work.studentId)
//            works(key).taskId should be(work.taskId)
//            works(key).version should be(work.version)
//            works(key).response should be(work.response)
//            works(key).isComplete should be(work.isComplete)
//            works(key).createdAt.toString should be(work.createdAt.toString)
//            works(key).updatedAt.toString should be(work.updatedAt.toString)
//          }
//        }
//      }
//      "list the latest revisions of a specific work for a user (ShortAnswerWork)" in {
//        val testUser = TestValues.testUserE
//        val testTask = TestValues.testShortAnswerTaskB
//
//        val testWorkList = TreeMap[Int, Work](
//          0 -> TestValues.testShortAnswerWorkB
//        )
//
//        val testDocumentList = TreeMap[Int, Document](
//          0 -> TestValues.testDocumentB
//        )
//
//        testDocumentList.foreach {
//          case (key, document: Document) => {
//            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(document.id, *, *) returns(Future.successful(\/-(document)))
//          }
//        }
//
//        val result = workRepository.list(testUser, testTask)
//        val eitherWorks = Await.result(result, Duration.Inf)
//        val \/-(works) = eitherWorks
//
//        works.size should be(testWorkList.size)
//
//        testWorkList.foreach {
//          case (key, work: Work) => {
//            works(key).id should be(work.id)
//            works(key).studentId should be(work.studentId)
//            works(key).taskId should be(work.taskId)
//            works(key).version should be(work.version)
//            works(key).response should be(work.response)
//            works(key).isComplete should be(work.isComplete)
//            works(key).createdAt.toString should be(work.createdAt.toString)
//            works(key).updatedAt.toString should be(work.updatedAt.toString)
//          }
//        }
//      }
//      "list the latest revisions of a specific work for a user (MultipleChoiceWork)" in {
//        val testUser = TestValues.testUserE
//        val testTask = TestValues.testMultipleChoiceTaskC
//
//        val testWorkList = TreeMap[Int, Work](
//          0 -> TestValues.testMultipleChoiceWorkH
//        )
//
//        val result = workRepository.list(testUser, testTask)
//        val eitherWorks = Await.result(result, Duration.Inf)
//        val \/-(works) = eitherWorks
//
//        works.size should be(testWorkList.size)
//
//        testWorkList.foreach {
//          case (key, work: Work) => {
//            works(key).id should be(work.id)
//            works(key).studentId should be(work.studentId)
//            works(key).taskId should be(work.taskId)
//            works(key).version should be(work.version)
//            works(key).response should be(work.response)
//            works(key).isComplete should be(work.isComplete)
//            works(key).createdAt.toString should be(work.createdAt.toString)
//            works(key).updatedAt.toString should be(work.updatedAt.toString)
//          }
//        }
//      }
//      "list the latest revisions of a specific work for a user (OrderingWork)" in {
//        val testUser = TestValues.testUserE
//        val testTask = TestValues.testOrderingTaskN
//
//        val testWorkList = TreeMap[Int, Work](
//          0 -> TestValues.testOrderingWorkI
//        )
//
//        val result = workRepository.list(testUser, testTask)
//        val eitherWorks = Await.result(result, Duration.Inf)
//        val \/-(works) = eitherWorks
//
//        works.size should be(testWorkList.size)
//
//        testWorkList.foreach {
//          case (key, work: Work) => {
//            works(key).id should be(work.id)
//            works(key).studentId should be(work.studentId)
//            works(key).taskId should be(work.taskId)
//            works(key).version should be(work.version)
//            works(key).response should be(work.response)
//            works(key).isComplete should be(work.isComplete)
//            works(key).createdAt.toString should be(work.createdAt.toString)
//            works(key).updatedAt.toString should be(work.updatedAt.toString)
//          }
//        }
//      }
//      "list the latest revisions of a specific work for a user (MatchingWork)" in {
//        val testUser = TestValues.testUserE
//        val testTask = TestValues.testMatchingTaskE
//
//        val testWorkList = TreeMap[Int, Work](
//          0 -> TestValues.testMatchingWorkJ
//        )
//
//        val result = workRepository.list(testUser, testTask)
//        val eitherWorks = Await.result(result, Duration.Inf)
//        val \/-(works) = eitherWorks
//
//        works.size should be(testWorkList.size)
//
//        testWorkList.foreach {
//          case (key, work: Work) => {
//            works(key).id should be(work.id)
//            works(key).studentId should be(work.studentId)
//            works(key).taskId should be(work.taskId)
//            works(key).version should be(work.version)
//            works(key).response should be(work.response)
//            works(key).isComplete should be(work.isComplete)
//            works(key).createdAt.toString should be(work.createdAt.toString)
//            works(key).updatedAt.toString should be(work.updatedAt.toString)
//          }
//        }
//      }
      "list latest revisions of a specific work for all users (LongAnswerWork)" in {
        val testTask = TestValues.testLongAnswerTaskA

        val testWorkList = TreeMap[Int, Work](
          0 -> TestValues.testLongAnswerWorkA,
          1 -> TestValues.testLongAnswerWorkF
        )

        val testDocumentList = TreeMap[Int, Document](
          0 -> TestValues.testDocumentA,
          1 -> TestValues.testDocumentB
        )

        testDocumentList.foreach {
          case (key, document: Document) => {
            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(document.id, *, *) returns(Future.successful(\/-(document)))
          }
        }

        val result = workRepository.list(testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks

        works.size should be(testWorkList.size)

        testWorkList.foreach {
          case (key, work: Work) => {
            works(key).id should be(work.id)
            works(key).studentId should be(work.studentId)
            works(key).taskId should be(work.taskId)
            works(key).version should be(work.version)
            works(key).response should be(work.response)
            works(key).isComplete should be(work.isComplete)
            works(key).createdAt.toString should be(work.createdAt.toString)
            works(key).updatedAt.toString should be(work.updatedAt.toString)
          }
        }
      }
      "list latest revisions of a specific work for all users (ShortAnswerWork)" in {
        val testTask = TestValues.testShortAnswerTaskB

        val testWorkList = TreeMap[Int, Work](
          0 -> TestValues.testShortAnswerWorkB,
          1 -> TestValues.testShortAnswerWorkG
        )

        val testDocumentList = TreeMap[Int, Document](
          0 -> TestValues.testDocumentC,
          1 -> TestValues.testDocumentD
        )

        testDocumentList.foreach {
          case (key, document: Document) => {
            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(document.id, *, *) returns(Future.successful(\/-(document)))
          }
        }

        val result = workRepository.list(testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks

        works.size should be(testWorkList.size)

        testWorkList.foreach {
          case (key, work: Work) => {
            works(key).id should be(work.id)
            works(key).studentId should be(work.studentId)
            works(key).taskId should be(work.taskId)
            works(key).version should be(work.version)
            works(key).response should be(work.response)
            works(key).isComplete should be(work.isComplete)
            works(key).createdAt.toString should be(work.createdAt.toString)
            works(key).updatedAt.toString should be(work.updatedAt.toString)
          }
        }
      }
      "list latest revisions of a specific work for all users (MultipleChoiceWork)" in {
        val testTask = TestValues.testMultipleChoiceTaskC

        val testWorkList = TreeMap[Int, Work](
          0 -> TestValues.testMultipleChoiceWorkC,
          1 -> TestValues.testMultipleChoiceWorkH
        )

        val result = workRepository.list(testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks

        works.size should be(testWorkList.size)

        testWorkList.foreach {
          case (key, work: Work) => {
            works(key).id should be(work.id)
            works(key).studentId should be(work.studentId)
            works(key).taskId should be(work.taskId)
            works(key).version should be(work.version)
            works(key).response should be(work.response)
            works(key).isComplete should be(work.isComplete)
            works(key).createdAt.toString should be(work.createdAt.toString)
            works(key).updatedAt.toString should be(work.updatedAt.toString)
          }
        }
      }
      "list latest revisions of a specific work for all users (OrderingWork)" in {
        val testTask = TestValues.testOrderingTaskN

        val testWorkList = TreeMap[Int, Work](
          0 -> TestValues.testOrderingWorkI,
          1 -> TestValues.testOrderingWorkD
        )

        val result = workRepository.list(testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks

        works.size should be(testWorkList.size)

        testWorkList.foreach {
          case (key, work: Work) => {
            works(key).id should be(work.id)
            works(key).studentId should be(work.studentId)
            works(key).taskId should be(work.taskId)
            works(key).version should be(work.version)
            works(key).response should be(work.response)
            works(key).isComplete should be(work.isComplete)
            works(key).createdAt.toString should be(work.createdAt.toString)
            works(key).updatedAt.toString should be(work.updatedAt.toString)
          }
        }
      }
      "list latest revisions of a specific work for all users (MatchingWork)" in {
        val testTask = TestValues.testMatchingTaskE

        val testWorkList = TreeMap[Int, Work](
          0 -> TestValues.testMatchingWorkJ,
          1 -> TestValues.testMatchingWorkE
        )

        val result = workRepository.list(testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks

        works.size should be(testWorkList.size)

        testWorkList.foreach {
          case (key, work: Work) => {
            works(key).id should be(work.id)
            works(key).studentId should be(work.studentId)
            works(key).taskId should be(work.taskId)
            works(key).version should be(work.version)
            works(key).response should be(work.response)
            works(key).isComplete should be(work.isComplete)
            works(key).createdAt.toString should be(work.createdAt.toString)
            works(key).updatedAt.toString should be(work.updatedAt.toString)
          }
        }
      }
    }
  }

  // TODO - do with LongAnswerWork and ShortAnswerWork
  "WorkRepository.find" should {
    inSequence {
      "find the latest revision of a single work (LongAnswerWork)" in {
        val testWork     = TestValues.testLongAnswerWorkA
        val testDocument = TestValues.testDocumentA

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(testDocument.id, *, *) returns(Future.successful(\/-(testDocument)))

        val result = workRepository.find(testWork.id)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find the latest revision of a single work (ShortAnswerWork)" in {
        val testWork     = TestValues.testShortAnswerWorkB
        val testDocument = TestValues.testDocumentC

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(testDocument.id, *, *) returns(Future.successful(\/-(testDocument)))

        val result = workRepository.find(testWork.id)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find the latest revision of a single work (MultipleChoiceWork)" in {
        val testWork     = TestValues.testMultipleChoiceWorkC

        val result = workRepository.find(testWork.id)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find the latest revision of a single work (OrderingWork)" in {
        val testWork     = TestValues.testOrderingWorkD

        val result = workRepository.find(testWork.id)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find the latest revision of a single work (MatchingWork)" in {
        val testWork     = TestValues.testMatchingWorkJ

        val result = workRepository.find(testWork.id)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find the latest revision of a single work for a user (LongAnswerWork)" in {
        val testUser     = TestValues.testUserE
        val testTask     = TestValues.testLongAnswerTaskA
        val testWork     = TestValues.testLongAnswerWorkF
        val testDocument = TestValues.testDocumentB

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(testDocument.id, *, *) returns(Future.successful(\/-(testDocument)))

        val result = workRepository.find(testUser, testTask)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find the latest revision of a single work for a user (ShortAnswerWork)" in {
        val testUser     = TestValues.testUserE
        val testTask     = TestValues.testShortAnswerTaskB
        val testWork     = TestValues.testShortAnswerWorkB
        val testDocument = TestValues.testDocumentC

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(testDocument.id, *, *) returns(Future.successful(\/-(testDocument)))

        val result = workRepository.find(testUser, testTask)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find the latest revision of a single work for a user (MultipleChoice)" in {
        val testUser     = TestValues.testUserE
        val testTask     = TestValues.testMultipleChoiceTaskC
        val testWork     = TestValues.testMultipleChoiceWorkH

        val result = workRepository.find(testUser, testTask)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find the latest revision of a single work for a user (OrderingWork)" in {
        val testUser     = TestValues.testUserE
        val testTask     = TestValues.testOrderingTaskN
        val testWork     = TestValues.testOrderingWorkI

        val result = workRepository.find(testUser, testTask)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find the latest revision of a single work for a user (MatchingWork)" in {
        val testUser     = TestValues.testUserE
        val testTask     = TestValues.testMatchingTaskE
        val testWork     = TestValues.testMatchingWorkJ

        val result = workRepository.find(testUser, testTask)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find a specific revision for a single work for a user" in {
        val testUser     = TestValues.testUserC
        val testTask     = TestValues.testLongAnswerTaskA
        val testWork     = TestValues.testLongAnswerWorkA
        val testDocument = TestValues.testDocumentA

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(testDocument.id, *, *) returns(Future.successful(\/-(testDocument)))

        val result = workRepository.find(testUser, testTask, testWork.version)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
    }
  }
}
