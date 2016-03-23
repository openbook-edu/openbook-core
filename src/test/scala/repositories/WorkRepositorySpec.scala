import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.document._
import ca.shiftfocus.krispii.core.models.work._
import ca.shiftfocus.krispii.core.repositories._
import com.github.mauricio.async.db.Connection
import org.joda.time.{ DateTimeZone, DateTime }
import org.scalatest.Matchers._
import org.scalatest._

import scala.collection.immutable.TreeMap
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }
import scala.util.Right
import scalaz._

class WorkRepositorySpec
    extends TestEnvironment {
  val documentRepository = stub[DocumentRepository]
  val revisionRepository = stub[RevisionRepository]
  val workRepository = new WorkRepositoryPostgres(documentRepository, revisionRepository)

  "WorkRepository.list" should {
    inSequence {
      /* --- list(testUser, testProject) --- */
      "list the latest revision of work for each task in a project for a user (MultipleChoiceWork, LongAnswerWork, ShortAnswerWork, OrderingWork, BlanksWork, MediaWork)" in {
        val testUser = TestValues.testUserC
        val testProject = TestValues.testProjectA

        val testWorkList = IndexedSeq[Work](
          TestValues.testShortAnswerWorkG,
          TestValues.testMultipleChoiceWorkC,
          TestValues.testLongAnswerWorkA,
          TestValues.testOrderingWorkD,
          TestValues.testBlanksWorkK,
          TestValues.testMediaWorkA
        )

        val testDocumentList = IndexedSeq[Document](
          TestValues.testDocumentA
        )

        testDocumentList.foreach { document =>
          (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (document.id, *, *) returns (Future.successful(\/-(document)))
        }

        val result = workRepository.list(testUser, testProject)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks

        works.size should be(testWorkList.size)

        works.foreach { work =>
          var testWork = if (testWorkList.exists(_.id == work.id)) testWorkList.filter(_.id == work.id).head
          else fail("There is no task with such ID in testTaskList: " + work.id)

          testWork.id should be(work.id)
          testWork.studentId should be(work.studentId)
          testWork.taskId should be(work.taskId)
          testWork.version should be(work.version)
          testWork.isComplete should be(work.isComplete)
          testWork.createdAt.toString should be(work.createdAt.toString)
          testWork.updatedAt.toString should be(work.updatedAt.toString)

          //Specific
          testWork match {
            case documentWork: DocumentWork => {
              documentWork.response should be(work.asInstanceOf[DocumentWork].response)
            }
            case questionWork: QuestionWork => {
              questionWork.response should be(work.asInstanceOf[QuestionWork].response)
            }
            case mediaWork: MediaWork => {
              mediaWork.fileData should be(work.asInstanceOf[MediaWork].fileData)
            }
          }
        }
      }
      "list the latest revision of work for each task in a project for a user (MatchingWork)" in {
        val testUser = TestValues.testUserC
        val testProject = TestValues.testProjectB

        val testWorkList = IndexedSeq[Work](
          TestValues.testMatchingWorkE
        )

        val result = workRepository.list(testUser, testProject)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks

        works.size should be(testWorkList.size)

        works.foreach { work =>
          var testWork = if (testWorkList.exists(_.id == work.id)) testWorkList.filter(_.id == work.id).head
          else fail("There is no task with such ID in testTaskList: " + work.id)

          testWork.id should be(work.id)
          testWork.studentId should be(work.studentId)
          testWork.taskId should be(work.taskId)
          testWork.version should be(work.version)
          testWork.isComplete should be(work.isComplete)
          testWork.createdAt.toString should be(work.createdAt.toString)
          testWork.updatedAt.toString should be(work.updatedAt.toString)

          //Specific
          testWork match {
            case documentWork: DocumentWork => {
              documentWork.response should be(work.asInstanceOf[DocumentWork].response)
            }
            case questionWork: QuestionWork => {
              questionWork.response should be(work.asInstanceOf[QuestionWork].response)
            }
          }
        }
      }
      "return empty Vector() if user doesn't exist" in {
        val testUser = TestValues.testUserD
        val testProject = TestValues.testProjectB

        val result = workRepository.list(testUser, testProject)
        Await.result(result, Duration.Inf) should be(\/-(Vector()))
      }
      "return empty Vector() if project doesn't exist" in {
        val testUser = TestValues.testUserC
        val testProject = TestValues.testProjectD

        val result = workRepository.list(testUser, testProject)
        Await.result(result, Duration.Inf) should be(\/-(Vector()))
      }
      "return empty Vector() if user doesn't have this project" in {
        val testUser = TestValues.testUserH
        val testProject = TestValues.testProjectB

        val result = workRepository.list(testUser, testProject)
        Await.result(result, Duration.Inf) should be(\/-(Vector()))
      }

      /* --- list(testUser, testTask) --- */
      "list all revisions of a specific work for a user (LongAnswerWork)" in {
        val testUser = TestValues.testUserC
        val testTask = TestValues.testLongAnswerTaskA
        val testWork = TestValues.testLongAnswerWorkA

        val testDocumentList = TreeMap[Int, Document](
          0 -> TestValues.testDocumentA
        )

        val testRevisionList = Map[UUID, IndexedSeq[Revision]](
          testDocumentList(0).id -> IndexedSeq(
            TestValues.testCurrentRevisionA,
            TestValues.testPreviousRevisionA
          )
        )

        val testWorkResult = testWork.copy(
          response = Some(testWork.response.get.copy(
            revisions = testRevisionList(testDocumentList(0).id)
          ))
        )

        testDocumentList.foreach {
          case (key, document: Document) => {
            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (document.id, *, *) returns (Future.successful(\/-(document)))
            (revisionRepository.list(_: Document, _: Long, _: Long)(_: Connection)) when (document, *, document.version, *) returns (Future.successful(\/-(testRevisionList(testDocumentList(key).id))))
          }
        }

        val result = workRepository.list(testUser, testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks
        val Left(documentWork: DocumentWork) = works

        documentWork.id should be(testWorkResult.id)
        documentWork.studentId should be(testWorkResult.studentId)
        documentWork.taskId should be(testWorkResult.taskId)
        documentWork.version should be(testWorkResult.version)
        documentWork.response should be(testWorkResult.response)
        documentWork.isComplete should be(testWorkResult.isComplete)
        documentWork.createdAt.toString should be(testWorkResult.createdAt.toString)
        documentWork.updatedAt.toString should be(testWorkResult.updatedAt.toString)
      }
      "list all revisions of a specific work for a user (ShortAnswerWork)" in {
        val testUser = TestValues.testUserE
        val testTask = TestValues.testShortAnswerTaskB

        val testWorkList = IndexedSeq[Work](
          TestValues.testShortAnswerWorkB,
          TestValues.testShortAnswerWorkRevisionB
        )

        val result = workRepository.list(testUser, testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks
        val Right(questionWork) = works

        questionWork.size should be(testWorkList.size)

        questionWork.foreach { work =>
          var testWork = if (testWorkList.exists({ item => item.id == work.id && item.version == work.version })) testWorkList.filter({ item => item.id == work.id && item.version == work.version }).head
          else fail("There is no task with such ID and version in testTaskList: " + work.id)

          testWork.id should be(work.id)
          testWork.studentId should be(work.studentId)
          testWork.taskId should be(work.taskId)
          testWork.version should be(work.version)
          testWork.isComplete should be(work.isComplete)
          testWork.createdAt.toString should be(work.createdAt.toString)
          testWork.updatedAt.toString should be(work.updatedAt.toString)

          //Specific
          testWork match {
            case documentWork: DocumentWork => {
              documentWork.response should be(work.asInstanceOf[DocumentWork].response)
            }
            case questionWork: QuestionWork => {
              questionWork.response should be(work.asInstanceOf[QuestionWork].response)
            }
          }
        }
      }
      "list all revisions of a specific work for a user (MultipleChoiceWork)" in {
        val testUser = TestValues.testUserE
        val testTask = TestValues.testMultipleChoiceTaskC

        val testWorkList = IndexedSeq[Work](
          TestValues.testMultipleChoiceWorkH,
          TestValues.testMultipleChoiceWorkRevisionH
        )

        val result = workRepository.list(testUser, testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks
        val Right(questionWork) = works

        questionWork.size should be(testWorkList.size)

        questionWork.foreach { work =>
          var testWork = if (testWorkList.exists({ item => item.id == work.id && item.version == work.version })) testWorkList.filter({ item => item.id == work.id && item.version == work.version }).head
          else fail("There is no task with such ID and version in testTaskList: " + work.id)

          testWork.id should be(work.id)
          testWork.studentId should be(work.studentId)
          testWork.taskId should be(work.taskId)
          testWork.version should be(work.version)
          testWork.isComplete should be(work.isComplete)
          testWork.createdAt.toString should be(work.createdAt.toString)
          testWork.updatedAt.toString should be(work.updatedAt.toString)

          //Specific
          testWork match {
            case documentWork: DocumentWork => {
              documentWork.response should be(work.asInstanceOf[DocumentWork].response)
            }
            case questionWork: QuestionWork => {
              questionWork.response should be(work.asInstanceOf[QuestionWork].response)
            }
          }
        }
      }
      "list all revisions of a specific work for a user (OrderingWork)" in {
        val testUser = TestValues.testUserE
        val testTask = TestValues.testOrderingTaskN

        val testWorkList = IndexedSeq[Work](
          TestValues.testOrderingWorkI,
          TestValues.testOrderingWorkRevisionI
        )

        val result = workRepository.list(testUser, testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks
        val Right(questionWork) = works

        questionWork.size should be(testWorkList.size)

        questionWork.foreach { work =>
          var testWork = if (testWorkList.exists({ item => item.id == work.id && item.version == work.version })) testWorkList.filter({ item => item.id == work.id && item.version == work.version }).head
          else fail("There is no task with such ID and version in testTaskList: " + work.id)

          testWork.id should be(work.id)
          testWork.studentId should be(work.studentId)
          testWork.taskId should be(work.taskId)
          testWork.version should be(work.version)
          testWork.isComplete should be(work.isComplete)
          testWork.createdAt.toString should be(work.createdAt.toString)
          testWork.updatedAt.toString should be(work.updatedAt.toString)

          //Specific
          testWork match {
            case documentWork: DocumentWork => {
              documentWork.response should be(work.asInstanceOf[DocumentWork].response)
            }
            case questionWork: QuestionWork => {
              questionWork.response should be(work.asInstanceOf[QuestionWork].response)
            }
          }
        }
      }
      "list all revisions of a specific work for a user (MatchingWork)" in {
        val testUser = TestValues.testUserE
        val testTask = TestValues.testMatchingTaskE

        val testWorkList = IndexedSeq[Work](
          TestValues.testMatchingWorkJ,
          TestValues.testMatchingWorkRevisionJ
        )

        val result = workRepository.list(testUser, testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks
        val Right(questionWork) = works

        questionWork.size should be(testWorkList.size)

        questionWork.foreach { work =>
          var testWork = if (testWorkList.exists({ item => item.id == work.id && item.version == work.version })) testWorkList.filter({ item => item.id == work.id && item.version == work.version }).head
          else fail("There is no task with such ID and version in testTaskList: " + work.id)

          testWork.id should be(work.id)
          testWork.studentId should be(work.studentId)
          testWork.taskId should be(work.taskId)
          testWork.version should be(work.version)
          testWork.isComplete should be(work.isComplete)
          testWork.createdAt.toString should be(work.createdAt.toString)
          testWork.updatedAt.toString should be(work.updatedAt.toString)

          //Specific
          testWork match {
            case documentWork: DocumentWork => {
              documentWork.response should be(work.asInstanceOf[DocumentWork].response)
            }
            case questionWork: QuestionWork => {
              questionWork.response should be(work.asInstanceOf[QuestionWork].response)
            }
          }
        }
      }
      "list all revisions of a specific work for a user (BlanksWork)" in {
        val testUser = TestValues.testUserE
        val testTask = TestValues.testBlanksTaskP

        val testWorkList = IndexedSeq[Work](
          TestValues.testBlanksWorkL,
          TestValues.testBlanksWorkRevisionL
        )

        val result = workRepository.list(testUser, testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks
        val Right(questionWork) = works

        questionWork.size should be(testWorkList.size)

        questionWork.foreach { work =>
          var testWork = if (testWorkList.exists({ item => item.id == work.id && item.version == work.version })) testWorkList.filter({ item => item.id == work.id && item.version == work.version }).head
          else fail("There is no task with such ID and version in testTaskList: " + work.id)

          testWork.id should be(work.id)
          testWork.studentId should be(work.studentId)
          testWork.taskId should be(work.taskId)
          testWork.version should be(work.version)
          testWork.isComplete should be(work.isComplete)
          testWork.createdAt.toString should be(work.createdAt.toString)
          testWork.updatedAt.toString should be(work.updatedAt.toString)

          //Specific
          testWork match {
            case documentWork: DocumentWork => {
              documentWork.response should be(work.asInstanceOf[DocumentWork].response)
            }
            case questionWork: QuestionWork => {
              questionWork.response should be(work.asInstanceOf[QuestionWork].response)
            }
          }
        }
      }
      "return RepositoryError.NoResults for task if user doesn't exist (document work)" in {
        val testUser = TestValues.testUserD
        val testTask = TestValues.testLongAnswerTaskA

        val result = workRepository.list(testUser, testTask)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults(s"ResultSet returned no rows. Could not build entity of type Work")))
      }
      "return RepositoryError.NoResults for task if user doesn't exist (question work)" in {
        val testUser = TestValues.testUserD
        val testTask = TestValues.testMatchingTaskE

        val result = workRepository.list(testUser, testTask)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults(s"Could not find question work for user ${testUser.id} for task ${testTask.id}")))
      }
      "return RepositoryError.NoResults if the task doesn't exist for a user (document work)" in {
        val testUser = TestValues.testUserE
        val testTask = TestValues.testLongAnswerTaskF

        val result = workRepository.list(testUser, testTask)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults(s"ResultSet returned no rows. Could not build entity of type Work")))
      }
      "return RepositoryError.NoResults if the task doesn't exist for a user (question work)" in {
        val testUser = TestValues.testUserE
        val testTask = TestValues.testMatchingTaskJ

        val result = workRepository.list(testUser, testTask)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults(s"Could not find question work for user ${testUser.id} for task ${testTask.id}")))
      }
      "return RepositoryError.NoResults if the user is not connected with this task (document work)" in {
        val testUser = TestValues.testUserG
        val testTask = TestValues.testLongAnswerTaskA

        val result = workRepository.list(testUser, testTask)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults(s"ResultSet returned no rows. Could not build entity of type Work")))
      }
      "return RepositoryError.NoResults if the user is not connected with this task (question work)" in {
        val testUser = TestValues.testUserG
        val testTask = TestValues.testMatchingTaskE

        val result = workRepository.list(testUser, testTask)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults(s"Could not find question work for user ${testUser.id} for task ${testTask.id}")))
      }
      "return RepositoryError.NoResults if the user doesn't have any work within this task (question work)" in {
        val testUser = TestValues.testUserC
        val testTask = TestValues.testOrderingTaskD

        val result = workRepository.list(testUser, testTask)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults(s"Could not find question work for user ${testUser.id} for task ${testTask.id}")))
      }

      /* --- list(testTask) --- */
      "list latest revisions of a specific work for all users (LongAnswerWork)" in {
        val testTask = TestValues.testLongAnswerTaskA

        val testWorkList = IndexedSeq[Work](
          TestValues.testLongAnswerWorkA,
          TestValues.testLongAnswerWorkF
        )

        val testDocumentList = TreeMap[Int, Document](
          0 -> TestValues.testDocumentA,
          1 -> TestValues.testDocumentB
        )

        testDocumentList.foreach {
          case (key, document: Document) => {
            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (document.id, *, *) returns (Future.successful(\/-(document)))
          }
        }

        val result = workRepository.list(testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks

        works.size should be(testWorkList.size)

        works.foreach { work =>
          var testWork = if (testWorkList.exists({ item => item.id == work.id && item.version == work.version })) testWorkList.filter({ item => item.id == work.id && item.version == work.version }).head
          else fail("There is no task with such ID and version in testTaskList: " + work.id)

          testWork.id should be(work.id)
          testWork.studentId should be(work.studentId)
          testWork.taskId should be(work.taskId)
          testWork.version should be(work.version)
          testWork.isComplete should be(work.isComplete)
          testWork.createdAt.toString should be(work.createdAt.toString)
          testWork.updatedAt.toString should be(work.updatedAt.toString)

          //Specific
          testWork match {
            case documentWork: DocumentWork => {
              documentWork.response should be(work.asInstanceOf[DocumentWork].response)
            }
            case questionWork: QuestionWork => {
              questionWork.response should be(work.asInstanceOf[QuestionWork].response)
            }
          }
        }
      }
      "list latest revisions of a specific work for all users (ShortAnswerWork)" in {
        val testTask = TestValues.testShortAnswerTaskB

        val testWorkList = IndexedSeq[Work](
          TestValues.testShortAnswerWorkB,
          TestValues.testShortAnswerWorkG
        )

        val result = workRepository.list(testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks

        works.size should be(testWorkList.size)

        works.foreach { work =>
          var testWork = if (testWorkList.exists({ item => item.id == work.id && item.version == work.version })) testWorkList.filter({ item => item.id == work.id && item.version == work.version }).head
          else fail("There is no task with such ID and version in testTaskList: " + work.id)

          testWork.id should be(work.id)
          testWork.studentId should be(work.studentId)
          testWork.taskId should be(work.taskId)
          testWork.version should be(work.version)
          testWork.isComplete should be(work.isComplete)
          testWork.createdAt.toString should be(work.createdAt.toString)
          testWork.updatedAt.toString should be(work.updatedAt.toString)

          //Specific
          testWork match {
            case documentWork: DocumentWork => {
              documentWork.response should be(work.asInstanceOf[DocumentWork].response)
            }
            case questionWork: QuestionWork => {
              questionWork.response should be(work.asInstanceOf[QuestionWork].response)
            }
          }
        }
      }
      "list latest revisions of a specific work for all users (MultipleChoiceWork)" in {
        val testTask = TestValues.testMultipleChoiceTaskC

        val testWorkList = IndexedSeq[Work](
          TestValues.testMultipleChoiceWorkC,
          TestValues.testMultipleChoiceWorkH
        )

        val result = workRepository.list(testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks

        works.size should be(testWorkList.size)

        works.foreach { work =>
          var testWork = if (testWorkList.exists({ item => item.id == work.id && item.version == work.version })) testWorkList.filter({ item => item.id == work.id && item.version == work.version }).head
          else fail("There is no task with such ID and version in testTaskList: " + work.id)

          testWork.id should be(work.id)
          testWork.studentId should be(work.studentId)
          testWork.taskId should be(work.taskId)
          testWork.version should be(work.version)
          testWork.isComplete should be(work.isComplete)
          testWork.createdAt.toString should be(work.createdAt.toString)
          testWork.updatedAt.toString should be(work.updatedAt.toString)

          //Specific
          testWork match {
            case documentWork: DocumentWork => {
              documentWork.response should be(work.asInstanceOf[DocumentWork].response)
            }
            case questionWork: QuestionWork => {
              questionWork.response should be(work.asInstanceOf[QuestionWork].response)
            }
          }
        }
      }
      "list latest revisions of a specific work for all users (OrderingWork)" in {
        val testTask = TestValues.testOrderingTaskN

        val testWorkList = IndexedSeq[Work](
          TestValues.testOrderingWorkI,
          TestValues.testOrderingWorkD
        )

        val result = workRepository.list(testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks

        works.size should be(testWorkList.size)

        works.foreach { work =>
          var testWork = if (testWorkList.exists({ item => item.id == work.id && item.version == work.version })) testWorkList.filter({ item => item.id == work.id && item.version == work.version }).head
          else fail("There is no task with such ID and version in testTaskList: " + work.id)

          testWork.id should be(work.id)
          testWork.studentId should be(work.studentId)
          testWork.taskId should be(work.taskId)
          testWork.version should be(work.version)
          testWork.isComplete should be(work.isComplete)
          testWork.createdAt.toString should be(work.createdAt.toString)
          testWork.updatedAt.toString should be(work.updatedAt.toString)

          //Specific
          testWork match {
            case documentWork: DocumentWork => {
              documentWork.response should be(work.asInstanceOf[DocumentWork].response)
            }
            case questionWork: QuestionWork => {
              questionWork.response should be(work.asInstanceOf[QuestionWork].response)
            }
          }
        }
      }
      "list latest revisions of a specific work for all users (MatchingWork)" in {
        val testTask = TestValues.testMatchingTaskE

        val testWorkList = IndexedSeq[Work](
          TestValues.testMatchingWorkJ,
          TestValues.testMatchingWorkE
        )

        val result = workRepository.list(testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks

        works.size should be(testWorkList.size)

        works.foreach { work =>
          var testWork = if (testWorkList.exists({ item => item.id == work.id && item.version == work.version })) testWorkList.filter({ item => item.id == work.id && item.version == work.version }).head
          else fail("There is no task with such ID and version in testTaskList: " + work.id)

          testWork.id should be(work.id)
          testWork.studentId should be(work.studentId)
          testWork.taskId should be(work.taskId)
          testWork.version should be(work.version)
          testWork.isComplete should be(work.isComplete)
          testWork.createdAt.toString should be(work.createdAt.toString)
          testWork.updatedAt.toString should be(work.updatedAt.toString)

          //Specific
          testWork match {
            case documentWork: DocumentWork => {
              documentWork.response should be(work.asInstanceOf[DocumentWork].response)
            }
            case questionWork: QuestionWork => {
              questionWork.response should be(work.asInstanceOf[QuestionWork].response)
            }
          }
        }
      }
      "list latest revisions of a specific work for all users (BlanksWork)" in {
        val testTask = TestValues.testBlanksTaskP

        val testWorkList = IndexedSeq[Work](
          TestValues.testBlanksWorkK,
          TestValues.testBlanksWorkL
        )

        val result = workRepository.list(testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks

        works.size should be(testWorkList.size)

        works.foreach { work =>
          var testWork = if (testWorkList.exists({ item => item.id == work.id && item.version == work.version })) testWorkList.filter({ item => item.id == work.id && item.version == work.version }).head
          else fail("There is no task with such ID and version in testTaskList: " + work.id)

          testWork.id should be(work.id)
          testWork.studentId should be(work.studentId)
          testWork.taskId should be(work.taskId)
          testWork.version should be(work.version)
          testWork.isComplete should be(work.isComplete)
          testWork.createdAt.toString should be(work.createdAt.toString)
          testWork.updatedAt.toString should be(work.updatedAt.toString)

          //Specific
          testWork match {
            case documentWork: DocumentWork => {
              documentWork.response should be(work.asInstanceOf[DocumentWork].response)
            }
            case questionWork: QuestionWork => {
              questionWork.response should be(work.asInstanceOf[QuestionWork].response)
            }
          }
        }
      }
      "return empty Vector() if the task doesn't exist" in {
        val testTask = TestValues.testMatchingTaskJ

        val result = workRepository.list(testTask)
        Await.result(result, Duration.Inf) should be(\/-(Vector()))
      }
      "return empty Vector() if the task doesn't have any work" in {
        val testTask = TestValues.testMatchingTaskM

        val result = workRepository.list(testTask)
        Await.result(result, Duration.Inf) should be(\/-(Vector()))
      }
    }
  }

  "WorkRepository.find" should {
    inSequence {
      /* --- find(workId) --- */
      "find the latest revision of a single work (LongAnswerWork)" in {
        val testWork = TestValues.testLongAnswerWorkA
        val testDocument = TestValues.testDocumentA

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (testDocument.id, *, *) returns (Future.successful(\/-(testDocument)))

        val result = workRepository.find(testWork.id)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: DocumentWork) = eitherWork

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
        val testWork = TestValues.testShortAnswerWorkB

        val result = workRepository.find(testWork.id)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

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
        val testWork = TestValues.testMultipleChoiceWorkC

        val result = workRepository.find(testWork.id)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

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
        val testWork = TestValues.testOrderingWorkD

        val result = workRepository.find(testWork.id)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

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
        val testWork = TestValues.testMatchingWorkJ

        val result = workRepository.find(testWork.id)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find the latest revision of a single work (BlanksWork)" in {
        val testWork = TestValues.testBlanksWorkK

        val result = workRepository.find(testWork.id)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find the latest revision of a single work (MediaWork)" in {
        val testWork = TestValues.testMediaWorkA

        val result = workRepository.find(testWork.id)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: MediaWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.fileData should be(testWork.fileData)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "return RepositoryError.NoResults if work doesn't exist" in {
        val testWork = TestValues.testMatchingWorkO

        val result = workRepository.find(testWork.id)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Work")))
      }

      /* --- find(workId, version) --- */
      "find a specific revision of a single work (LongAnswerWork)" in {
        val testWork = TestValues.testLongAnswerWorkRevisionA
        val testDocument = TestValues.testDocumentRevisionA

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (testDocument.id, testDocument.version, *) returns (Future.successful(\/-(testDocument)))

        val result = workRepository.find(testWork.id, testDocument.version)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: DocumentWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find a specific revision of a single work (ShortAnswerWork)" in {
        val testWork = TestValues.testShortAnswerWorkRevisionB

        val result = workRepository.find(testWork.id, testWork.version)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find a specifict revision of a single work (MultipleChoiceWork)" in {
        val testWork = TestValues.testMultipleChoiceWorkRevisionC

        val result = workRepository.find(testWork.id, testWork.version)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find a specific revision of a single work (OrderingWork)" in {
        val testWork = TestValues.testOrderingWorkRevisionD

        val result = workRepository.find(testWork.id, testWork.version)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find a specific revision of a single work (MatchingWork)" in {
        val testWork = TestValues.testMatchingWorkRevisionJ

        val result = workRepository.find(testWork.id, testWork.version)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find a specific revision of a single work (BlanksWork)" in {
        val testWork = TestValues.testBlanksWorkRevisionL

        val result = workRepository.find(testWork.id, testWork.version)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "return RepositoryError.NoResults if work doesn't exist with version" in {
        val testWork = TestValues.testMatchingWorkO

        val result = workRepository.find(testWork.id, testWork.version)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Work")))
      }

      /* --- find(user, task) --- */
      "find the latest revision of a single work for a user within a Task (LongAnswerWork)" in {
        val testUser = TestValues.testUserE
        val testTask = TestValues.testLongAnswerTaskA
        val testWork = TestValues.testLongAnswerWorkF
        val testDocument = TestValues.testDocumentB

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (testDocument.id, *, *) returns (Future.successful(\/-(testDocument)))

        val result = workRepository.find(testUser, testTask)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: DocumentWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find the latest revision of a single work for a user within a Task (ShortAnswerWork)" in {
        val testUser = TestValues.testUserE
        val testTask = TestValues.testShortAnswerTaskB
        val testWork = TestValues.testShortAnswerWorkB

        val result = workRepository.find(testUser, testTask)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find the latest revision of a single work for a user within a Task (MultipleChoice)" in {
        val testUser = TestValues.testUserE
        val testTask = TestValues.testMultipleChoiceTaskC
        val testWork = TestValues.testMultipleChoiceWorkH

        val result = workRepository.find(testUser, testTask)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find the latest revision of a single work for a user within a Task (OrderingWork)" in {
        val testUser = TestValues.testUserE
        val testTask = TestValues.testOrderingTaskN
        val testWork = TestValues.testOrderingWorkI

        val result = workRepository.find(testUser, testTask)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find the latest revision of a single work for a user within a Task (MatchingWork)" in {
        val testUser = TestValues.testUserE
        val testTask = TestValues.testMatchingTaskE
        val testWork = TestValues.testMatchingWorkJ

        val result = workRepository.find(testUser, testTask)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find the latest revision of a single work for a user within a Task (BlanksWork)" in {
        val testUser = TestValues.testUserE
        val testTask = TestValues.testBlanksTaskP
        val testWork = TestValues.testBlanksWorkL

        val result = workRepository.find(testUser, testTask)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find the latest revision of a single work for a user within a Task (MediaWork)" in {
        val testUser = TestValues.testUserC
        val testTask = TestValues.testMediaTaskA
        val testWork = TestValues.testMediaWorkA

        val result = workRepository.find(testUser, testTask)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: MediaWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.fileData should be(testWork.fileData)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "return RepositoryError.NoResults if user doesn't exist" in {
        val testUser = TestValues.testUserD
        val testTask = TestValues.testMatchingTaskE

        val result = workRepository.find(testUser, testTask)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Work")))
      }
      "return RepositoryError.NoResults if task doesn't exist" in {
        val testUser = TestValues.testUserE
        val testTask = TestValues.testMatchingTaskJ

        val result = workRepository.find(testUser, testTask)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Work")))
      }
      "return RepositoryError.NoResults if user is not connected with a task" in {
        val testUser = TestValues.testUserG
        val testTask = TestValues.testMatchingTaskE

        val result = workRepository.find(testUser, testTask)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Work")))
      }
      "return RepositoryError.NoResults if user doesn't have work within a task" in {
        val testUser = TestValues.testUserE
        val testTask = TestValues.testMatchingTaskM

        val result = workRepository.find(testUser, testTask)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Work")))
      }

      /* --- find(user, task, version) --- */
      "find a specific revision for a single work for a user within a Task (LongAnswerWork)" in {
        val testUser = TestValues.testUserC
        val testTask = TestValues.testLongAnswerTaskA
        val testWork = TestValues.testLongAnswerWorkRevisionA
        val testDocument = TestValues.testDocumentRevisionA

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (testDocument.id, *, *) returns (Future.successful(\/-(testDocument)))

        val result = workRepository.find(testUser, testTask, testWork.version)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: DocumentWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find a specific revision for a single work for a user within a Task (ShortAnswerWork)" in {
        val testUser = TestValues.testUserC
        val testTask = TestValues.testShortAnswerTaskB
        val testWork = TestValues.testShortAnswerWorkRevisionG

        val result = workRepository.find(testUser, testTask, testWork.version)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find a specific revision for a single work for a user within a Task (MultipleChoiceWork)" in {
        val testUser = TestValues.testUserC
        val testTask = TestValues.testMultipleChoiceTaskC
        val testWork = TestValues.testMultipleChoiceWorkRevisionC

        val result = workRepository.find(testUser, testTask, testWork.version)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find a specific revision for a single work for a user within a Task (OrderingWork)" in {
        val testUser = TestValues.testUserC
        val testTask = TestValues.testOrderingTaskN
        val testWork = TestValues.testOrderingWorkRevisionD

        val result = workRepository.find(testUser, testTask, testWork.version)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find a specific revision for a single work for a user within a Task (MatchingWork)" in {
        val testUser = TestValues.testUserC
        val testTask = TestValues.testMatchingTaskE
        val testWork = TestValues.testMatchingWorkRevisionE

        val result = workRepository.find(testUser, testTask, testWork.version)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find a specific revision for a single work for a user within a Task (BlanksWork)" in {
        val testUser = TestValues.testUserC
        val testTask = TestValues.testBlanksTaskP
        val testWork = TestValues.testBlanksWorkRevisionK

        val result = workRepository.find(testUser, testTask, testWork.version)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "return RepositoryError.NoResults if user doesn't exist (with version)" in {
        val testUser = TestValues.testUserD
        val testTask = TestValues.testMatchingTaskE
        val testWork = TestValues.testMatchingWorkRevisionE

        val result = workRepository.find(testUser, testTask, testWork.version)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Work")))
      }
      "return RepositoryError.NoResults if task doesn't exist (with version)" in {
        val testUser = TestValues.testUserC
        val testTask = TestValues.testMatchingTaskJ
        val testWork = TestValues.testMatchingWorkRevisionE

        val result = workRepository.find(testUser, testTask, testWork.version)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Work")))
      }
      "return RepositoryError.NoResults if user is not connected with a task (with version)" in {
        val testUser = TestValues.testUserG
        val testTask = TestValues.testMatchingTaskE
        val testWork = TestValues.testMatchingWorkRevisionE

        val result = workRepository.find(testUser, testTask, testWork.version)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Work")))
      }
      "return RepositoryError.NoResults if user doesn't have work within a task (with version)" in {
        val testUser = TestValues.testUserE
        val testTask = TestValues.testMatchingTaskM
        val testWork = TestValues.testMatchingWorkRevisionE

        val result = workRepository.find(testUser, testTask, testWork.version)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Work")))
      }
      "return RepositoryError.NoResults if version is wrong" in {
        val testUser = TestValues.testUserC
        val testTask = TestValues.testMatchingTaskE
        val testWork = TestValues.testMatchingWorkRevisionE

        val result = workRepository.find(testUser, testTask, testWork.version + 99)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Work")))
      }
    }
  }

  "WorkRepository.insert" should {
    inSequence {
      "insert new LongAnswerWork" in {
        val testWork = TestValues.testLongAnswerWorkK

        val result = workRepository.insert(testWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: DocumentWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(1L)
        work.response should be(None)
        work.isComplete should be(testWork.isComplete)
      }
      "insert new ShortAnswerWork" in {
        val testWork = TestValues.testShortAnswerWorkL

        val result = workRepository.insert(testWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(1L)
        work.response should be(Answers(Map()))
        work.isComplete should be(testWork.isComplete)
      }
      "insert new MultipleChoiceWork" in {
        val testWork = TestValues.testMultipleChoiceWorkM

        val result = workRepository.insert(testWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(1L)
        work.response should be(Answers(Map()))
        work.isComplete should be(testWork.isComplete)
      }
      "insert new OrderingWork" in {
        val testWork = TestValues.testOrderingWorkN

        val result = workRepository.insert(testWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(1L)
        work.response should be(Answers(Map()))
        work.isComplete should be(testWork.isComplete)
      }
      "insert new MatchingWork" in {
        val testWork = TestValues.testMatchingWorkO

        val result = workRepository.insert(testWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(1L)
        work.response should be(Answers(Map()))
        work.isComplete should be(testWork.isComplete)
      }
      "insert new BlanksWork" in {
        val testWork = TestValues.testBlanksWorkM
        val result = workRepository.insert(testWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(1L)
        work.response should be(Answers(Map()))
        work.isComplete should be(testWork.isComplete)
      }
      "insert new MediaWork" in {
        val testWork = TestValues.testMediaWorkB
        val result = workRepository.insert(testWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: MediaWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(1L)
        work.fileData should be(MediaAnswer())
        work.isComplete should be(testWork.isComplete)
      }
      "return RepositoryError.PrimaryKeyConflict if this work allready exists (DocumentWork)" in {
        val testWork = TestValues.testLongAnswerWorkA.copy(
          studentId = TestValues.testUserG.id
        )

        val result = workRepository.insert(testWork)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
      }
      "return RepositoryError.PrimaryKeyConflict if this work allready exists (QuestionWork)" in {
        val testWork = TestValues.testShortAnswerWorkB.copy(
          studentId = TestValues.testUserG.id
        )

        val result = workRepository.insert(testWork)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
      }
    }
  }

  "WorkRepository.update" should {
    inSequence {
      /* Only thing we should be able to update is isComplete */
      "update LongAnswerWork" in {
        val testWork = TestValues.testLongAnswerWorkF
        // Work for new values
        val oppositeWork = TestValues.testLongAnswerWorkA
        val testDocument = testWork.response.get
        val updatedWork = testWork.copy(
          // Shouldn't be updated
          studentId = oppositeWork.studentId,
          taskId = oppositeWork.taskId,
          documentId = oppositeWork.documentId,
          response = oppositeWork.response,
          createdAt = oppositeWork.createdAt,

          // Should be updated
          isComplete = !testWork.isComplete
        )

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (testDocument.id, *, *) returns (Future.successful(\/-(testDocument)))

        val result = workRepository.update(updatedWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: DocumentWork) = eitherWork

        // This values should remain unchanged
        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.response should be(testWork.response)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.version should be(testWork.version)

        // This should be updated
        work.isComplete should be(updatedWork.isComplete)
        work.updatedAt.toString should not be (testWork.updatedAt.toString)
      }
      "update ShortAnswerWork" in {
        val testWork = TestValues.testShortAnswerWorkB
        // Work for new values
        val oppositeWork = TestValues.testShortAnswerWorkG
        val updatedWork = testWork.copy(
          // Shouldn't be updated
          studentId = oppositeWork.studentId,
          taskId = oppositeWork.taskId,
          createdAt = oppositeWork.createdAt,

          // Should be updated
          response = Answers(Map[UUID, Answer](
            TestValues.testShortQuestionA.id -> TestValues.testShortAnswerA,
            TestValues.testMultipleChoiceQuestionB.id -> TestValues.testMultipleChoiceAnswerB
          )),
          isComplete = !testWork.isComplete
        )

        val result = workRepository.update(updatedWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

        // This values should remain unchanged
        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.createdAt.toString should be(testWork.createdAt.toString)

        // This should be updated
        work.response should be(updatedWork.response)
        work.isComplete should be(updatedWork.isComplete)
        work.version should be(updatedWork.version + 1)
        work.updatedAt.toString should not be (testWork.updatedAt.toString)
      }
      "update MultipleChoiceWork" in {
        val testWork = TestValues.testMultipleChoiceWorkC
        // Work for new values
        val oppositeWork = TestValues.testMultipleChoiceWorkH
        val updatedWork = testWork.copy(
          // Shouldn't be updated
          studentId = oppositeWork.studentId,
          taskId = oppositeWork.taskId,
          createdAt = oppositeWork.createdAt,

          // Should be updated
          response = Answers(Map[UUID, Answer](
            TestValues.testShortQuestionA.id -> TestValues.testShortAnswerA,
            TestValues.testMultipleChoiceQuestionB.id -> TestValues.testMultipleChoiceAnswerB
          )),
          isComplete = !testWork.isComplete
        )

        val result = workRepository.update(updatedWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

        // This values should remain unchanged
        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.createdAt.toString should be(testWork.createdAt.toString)

        // This should be updated
        work.response should be(updatedWork.response)
        work.isComplete should be(updatedWork.isComplete)
        work.version should be(updatedWork.version + 1)
        work.updatedAt.toString should not be (testWork.updatedAt.toString)
      }
      "update OrderingWork" in {
        val testWork = TestValues.testOrderingWorkI
        // Work for new values
        val oppositeWork = TestValues.testOrderingWorkD
        val updatedWork = testWork.copy(
          // Shouldn't be updated
          studentId = oppositeWork.studentId,
          taskId = oppositeWork.taskId,
          createdAt = oppositeWork.createdAt,

          // Should be updated
          response = Answers(Map[UUID, Answer](
            TestValues.testShortQuestionA.id -> TestValues.testShortAnswerA,
            TestValues.testMultipleChoiceQuestionB.id -> TestValues.testMultipleChoiceAnswerB
          )),
          isComplete = !testWork.isComplete
        )

        val result = workRepository.update(updatedWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

        // This values should remain unchanged
        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.createdAt.toString should be(testWork.createdAt.toString)

        // This should be updated
        work.response should be(updatedWork.response)
        work.isComplete should be(updatedWork.isComplete)
        work.version should be(updatedWork.version + 1)
        work.updatedAt.toString should not be (testWork.updatedAt.toString)
      }
      "update MatchingWork" in {
        val testWork = TestValues.testMatchingWorkJ
        // Work for new values
        val oppositeWork = TestValues.testMatchingWorkE
        val updatedWork = testWork.copy(
          // Shouldn't be updated
          studentId = oppositeWork.studentId,
          taskId = oppositeWork.taskId,
          createdAt = oppositeWork.createdAt,

          // Should be updated
          response = Answers(Map[UUID, Answer](
            TestValues.testShortQuestionA.id -> TestValues.testShortAnswerA,
            TestValues.testMultipleChoiceQuestionB.id -> TestValues.testMultipleChoiceAnswerB
          )),
          isComplete = !testWork.isComplete
        )

        val result = workRepository.update(updatedWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

        // This values should remain unchanged
        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.createdAt.toString should be(testWork.createdAt.toString)

        // This should be updated
        work.response should be(updatedWork.response)
        work.isComplete should be(updatedWork.isComplete)
        work.version should be(updatedWork.version + 1)
        work.updatedAt.toString should not be (testWork.updatedAt.toString)
      }
      "update BlanksWork" in {
        val testWork = TestValues.testBlanksWorkK
        // Work for new values
        val oppositeWork = TestValues.testBlanksWorkL
        val updatedWork = testWork.copy(
          // Shouldn't be updated
          studentId = oppositeWork.studentId,
          taskId = oppositeWork.taskId,
          createdAt = oppositeWork.createdAt,

          // Should be updated
          response = Answers(Map[UUID, Answer](
            TestValues.testShortQuestionA.id -> TestValues.testShortAnswerA,
            TestValues.testMultipleChoiceQuestionB.id -> TestValues.testMultipleChoiceAnswerB
          )),
          isComplete = !testWork.isComplete
        )

        val result = workRepository.update(updatedWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

        // This values should remain unchanged
        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.createdAt.toString should be(testWork.createdAt.toString)

        // This should be updated
        work.response should be(updatedWork.response)
        work.isComplete should be(updatedWork.isComplete)
        work.version should be(updatedWork.version + 1)
        work.updatedAt.toString should not be (testWork.updatedAt.toString)
      }
      "update MediaWork" in {
        val testWork = TestValues.testMediaWorkA
        // Work for new values
        val updatedWork = testWork.copy(
          // Shouldn't be updated
          studentId = UUID.randomUUID(),
          taskId = UUID.randomUUID(),
          createdAt = new DateTime(2014, 8, 18, 14, 1, 19, 545, DateTimeZone.forID("-04")),

          // Should be updated
          fileData = MediaAnswer(Some("2"), Some("video.mp4")),
          isComplete = !testWork.isComplete
        )

        val result = workRepository.update(updatedWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: MediaWork) = eitherWork

        // This values should remain unchanged
        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.createdAt.toString should be(testWork.createdAt.toString)

        // This should be updated
        work.fileData should be(updatedWork.fileData)
        work.isComplete should be(updatedWork.isComplete)
        work.version should be(updatedWork.version + 1)
        work.updatedAt.toString should not be (testWork.updatedAt.toString)
      }
      "update LongAnswerWork if version is wrong" in {
        val testWork = TestValues.testLongAnswerWorkF
        // Work for new values
        val oppositeWork = TestValues.testLongAnswerWorkA
        val testDocument = testWork.response.get
        val updatedWork = testWork.copy(
          version = 99L,

          // Shouldn't be updated
          studentId = oppositeWork.studentId,
          taskId = oppositeWork.taskId,
          documentId = oppositeWork.documentId,
          response = oppositeWork.response,
          createdAt = oppositeWork.createdAt,

          // Should be updated
          isComplete = !testWork.isComplete
        )

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (testDocument.id, *, *) returns (Future.successful(\/-(testDocument)))

        val result = workRepository.update(updatedWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: DocumentWork) = eitherWork

        // This values should remain unchanged
        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.response should be(testWork.response)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.version should be(testWork.version)

        // This should be updated
        work.isComplete should be(updatedWork.isComplete)
        work.updatedAt.toString should not be (testWork.updatedAt.toString)
      }
      "return RepositoryError.NoResults if version is wrong" in {
        val testWork = TestValues.testMatchingWorkJ
        // Work for new values
        val oppositeWork = TestValues.testMatchingWorkE
        val updatedWork = testWork.copy(
          version = 99L,

          // Shouldn't be updated
          studentId = oppositeWork.studentId,
          taskId = oppositeWork.taskId,
          response = oppositeWork.response,
          createdAt = oppositeWork.createdAt,

          // Should be updated
          isComplete = !testWork.isComplete
        )

        val result = workRepository.update(updatedWork)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Work")))
      }
      "return RepositoryError.NoResults if work doesn't exist" in {
        val testWork = TestValues.testMatchingWorkO
        // Work for new values
        val oppositeWork = TestValues.testMatchingWorkE
        val updatedWork = testWork.copy(
          // Shouldn't be updated
          studentId = oppositeWork.studentId,
          taskId = oppositeWork.taskId,
          response = oppositeWork.response,
          createdAt = oppositeWork.createdAt,

          // Should be updated
          isComplete = !testWork.isComplete
        )

        val result = workRepository.update(updatedWork)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Work")))
      }
      "return RepositoryError.NoResults if try to update work previous revision" in {
        val testWork = TestValues.testMatchingWorkRevisionJ
        // Work for new values
        val oppositeWork = TestValues.testMatchingWorkE
        val updatedWork = testWork.copy(
          // Shouldn't be updated
          studentId = oppositeWork.studentId,
          taskId = oppositeWork.taskId,
          response = oppositeWork.response,
          createdAt = oppositeWork.createdAt,

          // Should be updated
          isComplete = !testWork.isComplete
        )

        val result = workRepository.update(updatedWork)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Work")))
      }
    }
  }

  "WorkRepository.delete" should {
    inSequence {
      /* --- delete(Work) --- */
      "delete all revisions of a work (LongAnswerWork)" in {
        val testWork = TestValues.testLongAnswerWorkF
        val testDocument = testWork.response.get

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (testDocument.id, *, *) returns (Future.successful(\/-(testDocument)))

        val result = workRepository.delete(testWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: DocumentWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.documentId should be(testWork.documentId)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "delete all revisions of a work (ShortAnswerWork)" in {
        val testWork = TestValues.testShortAnswerWorkG

        val result = workRepository.delete(testWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "delete all revisions of a work (MultipleChoiceWork)" in {
        val testWork = TestValues.testMultipleChoiceWorkC

        val result = workRepository.delete(testWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "delete all revisions of a work (OrderingWork)" in {
        val testWork = TestValues.testOrderingWorkD

        val result = workRepository.delete(testWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "delete all revisions of a work (MatchingWork)" in {
        val testWork = TestValues.testMatchingWorkJ

        val result = workRepository.delete(testWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "delete all revisions of a work (BlanksWork)" in {
        val testWork = TestValues.testBlanksWorkK

        val result = workRepository.delete(testWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: QuestionWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "delete all revisions of a work (MediaWork)" in {
        val testWork = TestValues.testMediaWorkA

        val result = workRepository.delete(testWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: MediaWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.fileData should be(testWork.fileData)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "return RepositoryError.NoResults if work doesn't exist" in {
        val testWork = TestValues.testMatchingWorkO

        val result = workRepository.delete(testWork)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Work")))
      }

      /* --- delete(Task) --- */
      "delete all work for a given task (LongAnswerWork)" in {
        val testTask = TestValues.testLongAnswerTaskA

        val testWorkList = IndexedSeq[Work](
          TestValues.testLongAnswerWorkA,
          TestValues.testLongAnswerWorkF
        )

        val testDocumentList = TreeMap[Int, Document](
          0 -> TestValues.testDocumentA,
          1 -> TestValues.testDocumentB
        )

        testDocumentList.foreach {
          case (key, document: Document) => {
            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (document.id, *, *) returns (Future.successful(\/-(document)))
          }
        }

        val result = workRepository.delete(testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works: IndexedSeq[DocumentWork]) = eitherWorks

        works.size should be(testWorkList.size)

        works.foreach { work =>
          var testWork = if (testWorkList.exists({ item => item.id == work.id && item.version == work.version })) testWorkList.filter({ item => item.id == work.id && item.version == work.version }).head
          else fail("There is no task with such ID and version in testTaskList: " + work.id)

          testWork.id should be(work.id)
          testWork.studentId should be(work.studentId)
          testWork.taskId should be(work.taskId)
          testWork.version should be(work.version)
          testWork.isComplete should be(work.isComplete)
          testWork.createdAt.toString should be(work.createdAt.toString)
          testWork.updatedAt.toString should be(work.updatedAt.toString)

          //Specific
          testWork match {
            case documentWork: DocumentWork => {
              documentWork.response should be(work.asInstanceOf[DocumentWork].response)
            }
            case questionWork: QuestionWork => {
              questionWork.response should be(work.asInstanceOf[QuestionWork].response)
            }
          }
        }
      }
      "delete all work for a given task (ShortAnswerWork)" in {
        val testTask = TestValues.testShortAnswerTaskB

        val testWorkList = IndexedSeq[Work](
          TestValues.testShortAnswerWorkB,
          TestValues.testShortAnswerWorkG
        )

        val result = workRepository.delete(testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works: IndexedSeq[QuestionWork]) = eitherWorks

        works.size should be(testWorkList.size)

        works.foreach { work =>
          var testWork = if (testWorkList.exists({ item => item.id == work.id && item.version == work.version })) testWorkList.filter({ item => item.id == work.id && item.version == work.version }).head
          else fail("There is no task with such ID and version in testTaskList: " + work.id)

          testWork.id should be(work.id)
          testWork.studentId should be(work.studentId)
          testWork.taskId should be(work.taskId)
          testWork.version should be(work.version)
          testWork.isComplete should be(work.isComplete)
          testWork.createdAt.toString should be(work.createdAt.toString)
          testWork.updatedAt.toString should be(work.updatedAt.toString)

          //Specific
          testWork match {
            case documentWork: DocumentWork => {
              documentWork.response should be(work.asInstanceOf[DocumentWork].response)
            }
            case questionWork: QuestionWork => {
              questionWork.response should be(work.asInstanceOf[QuestionWork].response)
            }
          }
        }
      }
      "delete all work for a given task (MultipleChoiceWork)" in {
        val testTask = TestValues.testMultipleChoiceTaskC

        val testWorkList = IndexedSeq[Work](
          TestValues.testMultipleChoiceWorkC,
          TestValues.testMultipleChoiceWorkH
        )

        val result = workRepository.delete(testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works: IndexedSeq[QuestionWork]) = eitherWorks

        works.size should be(testWorkList.size)

        works.foreach { work =>
          var testWork = if (testWorkList.exists({ item => item.id == work.id && item.version == work.version })) testWorkList.filter({ item => item.id == work.id && item.version == work.version }).head
          else fail("There is no task with such ID and version in testTaskList: " + work.id)

          testWork.id should be(work.id)
          testWork.studentId should be(work.studentId)
          testWork.taskId should be(work.taskId)
          testWork.version should be(work.version)
          testWork.isComplete should be(work.isComplete)
          testWork.createdAt.toString should be(work.createdAt.toString)
          testWork.updatedAt.toString should be(work.updatedAt.toString)

          //Specific
          testWork match {
            case documentWork: DocumentWork => {
              documentWork.response should be(work.asInstanceOf[DocumentWork].response)
            }
            case questionWork: QuestionWork => {
              questionWork.response should be(work.asInstanceOf[QuestionWork].response)
            }
          }
        }
      }
      "delete all work for a given task (OrderingWork)" in {
        val testTask = TestValues.testOrderingTaskN

        val testWorkList = IndexedSeq[Work](
          TestValues.testOrderingWorkI,
          TestValues.testOrderingWorkD
        )

        val result = workRepository.delete(testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works: IndexedSeq[QuestionWork]) = eitherWorks

        works.size should be(testWorkList.size)

        works.foreach { work =>
          var testWork = if (testWorkList.exists({ item => item.id == work.id && item.version == work.version })) testWorkList.filter({ item => item.id == work.id && item.version == work.version }).head
          else fail("There is no task with such ID and version in testTaskList: " + work.id)

          testWork.id should be(work.id)
          testWork.studentId should be(work.studentId)
          testWork.taskId should be(work.taskId)
          testWork.version should be(work.version)
          testWork.isComplete should be(work.isComplete)
          testWork.createdAt.toString should be(work.createdAt.toString)
          testWork.updatedAt.toString should be(work.updatedAt.toString)

          //Specific
          testWork match {
            case documentWork: DocumentWork => {
              documentWork.response should be(work.asInstanceOf[DocumentWork].response)
            }
            case questionWork: QuestionWork => {
              questionWork.response should be(work.asInstanceOf[QuestionWork].response)
            }
          }
        }
      }
      "delete all work for a given task (MatchingWork)" in {
        val testTask = TestValues.testMatchingTaskE

        val testWorkList = IndexedSeq[Work](
          TestValues.testMatchingWorkJ,
          TestValues.testMatchingWorkE
        )

        val result = workRepository.delete(testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works: IndexedSeq[QuestionWork]) = eitherWorks

        works.size should be(testWorkList.size)

        works.foreach { work =>
          var testWork = if (testWorkList.exists({ item => item.id == work.id && item.version == work.version })) testWorkList.filter({ item => item.id == work.id && item.version == work.version }).head
          else fail("There is no task with such ID and version in testTaskList: " + work.id)

          testWork.id should be(work.id)
          testWork.studentId should be(work.studentId)
          testWork.taskId should be(work.taskId)
          testWork.version should be(work.version)
          testWork.isComplete should be(work.isComplete)
          testWork.createdAt.toString should be(work.createdAt.toString)
          testWork.updatedAt.toString should be(work.updatedAt.toString)

          //Specific
          testWork match {
            case documentWork: DocumentWork => {
              documentWork.response should be(work.asInstanceOf[DocumentWork].response)
            }
            case questionWork: QuestionWork => {
              questionWork.response should be(work.asInstanceOf[QuestionWork].response)
            }
          }
        }
      }
      "delete all work for a given task (BlanksWork)" in {
        val testTask = TestValues.testBlanksTaskP

        val testWorkList = IndexedSeq[Work](
          TestValues.testBlanksWorkK,
          TestValues.testBlanksWorkL
        )

        val result = workRepository.delete(testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works: IndexedSeq[QuestionWork]) = eitherWorks

        works.size should be(testWorkList.size)

        works.foreach { work =>
          var testWork = if (testWorkList.exists({ item => item.id == work.id && item.version == work.version })) testWorkList.filter({ item => item.id == work.id && item.version == work.version }).head
          else fail("There is no task with such ID and version in testTaskList: " + work.id)

          testWork.id should be(work.id)
          testWork.studentId should be(work.studentId)
          testWork.taskId should be(work.taskId)
          testWork.version should be(work.version)
          testWork.isComplete should be(work.isComplete)
          testWork.createdAt.toString should be(work.createdAt.toString)
          testWork.updatedAt.toString should be(work.updatedAt.toString)

          //Specific
          testWork match {
            case documentWork: DocumentWork => {
              documentWork.response should be(work.asInstanceOf[DocumentWork].response)
            }
            case questionWork: QuestionWork => {
              questionWork.response should be(work.asInstanceOf[QuestionWork].response)
            }
          }
        }
      }
      "return empty Vector() if task doesn't exist" in {
        val testTask = TestValues.testMatchingTaskJ

        val result = workRepository.delete(testTask)
        Await.result(result, Duration.Inf) should be(\/-(Vector()))
      }
    }
  }
}
