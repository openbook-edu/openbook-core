import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.document.Document
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.Connection

import org.scalatest._
import Matchers._
import scala.collection.immutable.TreeMap
import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import scalaz.\/-

class TaskFeedbackRepositorySpec
  extends TestEnvironment
{
  val documentRepository = stub[DocumentRepository]
  val taskFeedbackRepository = new TaskFeedbackRepositoryPostgres(documentRepository)

  // TODO - test all test cases
  "TaskFeedbackRepository.list" should {
    inSequence {
      "list all feedbacks in a project for one student" in {
        val testUser = TestValues.testUserC
        val testProject = TestValues.testProjectA

        val testTaskFeedbackList = TreeMap[Int, TaskFeedback](
          0 -> TestValues.testTaskFeedbackA,
          1 -> TestValues.testTaskFeedbackE
        )

        val testDocumentList = TreeMap[Int, Document](
          0 -> TestValues.testDocumentF,
          1 -> TestValues.testDocumentJ
        )

        testDocumentList.foreach {
          case (key, document: Document) => {
            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(document.id, *, *) returns(Future.successful(\/-(document)))
          }
        }

        val result = taskFeedbackRepository.list(testUser, testProject)
        val eitherTaskFeedbacks = Await.result(result, Duration.Inf)
        val \/-(taskFeedbacks) = eitherTaskFeedbacks

        taskFeedbacks.size should be(testTaskFeedbackList.size)

        testTaskFeedbackList.foreach {
          case (key, taskFeedback: TaskFeedback) => {
            taskFeedbacks(key).studentId should be(taskFeedback.studentId)
            taskFeedbacks(key).taskId should be(taskFeedback.taskId)
            taskFeedbacks(key).version should be(taskFeedback.version)
            taskFeedbacks(key).documentId should be(taskFeedback.documentId)
            taskFeedbacks(key).createdAt.toString should be(taskFeedback.createdAt.toString)
            taskFeedbacks(key).updatedAt.toString should be(taskFeedback.updatedAt.toString)
          }
        }
      }
      "List all feedbacks for a given task" in {
        val testTask = TestValues.testMatchingTaskE

        val testTaskFeedbackList = TreeMap[Int, TaskFeedback](
          0 -> TestValues.testTaskFeedbackC,
          1 -> TestValues.testTaskFeedbackD
        )

        val testDocumentList = TreeMap[Int, Document](
          0 -> TestValues.testDocumentH,
          1 -> TestValues.testDocumentI
        )

        testDocumentList.foreach {
          case (key, document: Document) => {
            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(document.id, *, *) returns(Future.successful(\/-(document)))
          }
        }

        val result = taskFeedbackRepository.list(testTask)
        val eitherTaskFeedbacks = Await.result(result, Duration.Inf)
        val \/-(taskFeedbacks) = eitherTaskFeedbacks

        taskFeedbacks.size should be(testTaskFeedbackList.size)

        testTaskFeedbackList.foreach {
          case (key, taskFeedback: TaskFeedback) => {
            taskFeedbacks(key).studentId should be(taskFeedback.studentId)
            taskFeedbacks(key).taskId should be(taskFeedback.taskId)
            taskFeedbacks(key).version should be(taskFeedback.version)
            taskFeedbacks(key).documentId should be(taskFeedback.documentId)
            taskFeedbacks(key).createdAt.toString should be(taskFeedback.createdAt.toString)
            taskFeedbacks(key).updatedAt.toString should be(taskFeedback.updatedAt.toString)
          }
        }
      }
    }
  }

  "TaskRepositoryFeedback.find" should {
    inSequence {
      "find a single feedback for one task and student" in {
        val testTask = TestValues.testMatchingTaskE
        val testUser = TestValues.testUserE
        val testTaskFeedback = TestValues.testTaskFeedbackD
        val testDocument = TestValues.testDocumentI

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(testDocument.id, *, *) returns(Future.successful(\/-(testDocument)))

        val result = taskFeedbackRepository.find(testUser, testTask)
        val eitherTaskFeedback = Await.result(result, Duration.Inf)
        val \/-(taskFeedback) = eitherTaskFeedback

        taskFeedback.studentId should be(testTaskFeedback.studentId)
        taskFeedback.taskId should be(testTaskFeedback.taskId)
        taskFeedback.version should be(testTaskFeedback.version)
        taskFeedback.documentId should be(testTaskFeedback.documentId)
        taskFeedback.createdAt.toString should be(testTaskFeedback.createdAt.toString)
        taskFeedback.updatedAt.toString should be(testTaskFeedback.updatedAt.toString)
      }
    }
  }

  "TaskRepositoryFeedback.insert" should {
    inSequence {
      "create a new feedback for a task" in {
        val testTaskFeedback = TestValues.testTaskFeedbackF
        val testDocument = TestValues.testDocumentA

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(testDocument.id, *, *) returns(Future.successful(\/-(testDocument)))

        val result = taskFeedbackRepository.insert(testTaskFeedback)
        val eitherTaskFeedback = Await.result(result, Duration.Inf)
        val \/-(taskFeedback) = eitherTaskFeedback

        taskFeedback.studentId should be(testTaskFeedback.studentId)
        taskFeedback.taskId should be(testTaskFeedback.taskId)
        taskFeedback.version should be(testTaskFeedback.version)
        taskFeedback.documentId should be(testTaskFeedback.documentId)
        taskFeedback.createdAt.toString should be(testTaskFeedback.createdAt.toString)
        taskFeedback.updatedAt.toString should be(testTaskFeedback.updatedAt.toString)
      }
      "return error if task feedback already exists for the user in this task" in {}
    }
  }

  "TaskRepositoryFeedback.delete" should {
    inSequence {
      "delete a feedback" in {
        val testTaskFeedback = TestValues.testTaskFeedbackC
        val testDocument = TestValues.testDocumentH

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(testDocument.id, *, *) returns(Future.successful(\/-(testDocument)))

        val result = taskFeedbackRepository.delete(testTaskFeedback)
        val eitherTaskFeedback = Await.result(result, Duration.Inf)
        val \/-(taskFeedback) = eitherTaskFeedback

        taskFeedback.studentId should be(testTaskFeedback.studentId)
        taskFeedback.taskId should be(testTaskFeedback.taskId)
        taskFeedback.version should be(testTaskFeedback.version)
        taskFeedback.documentId should be(testTaskFeedback.documentId)
        taskFeedback.createdAt.toString should be(testTaskFeedback.createdAt.toString)
        taskFeedback.updatedAt.toString should be(testTaskFeedback.updatedAt.toString)
      }
      "delete all feedbacks for a task" in {
        val testTask = TestValues.testMatchingTaskE

        val testTaskFeedbackList = TreeMap[Int, TaskFeedback](
          0 -> TestValues.testTaskFeedbackC,
          1 -> TestValues.testTaskFeedbackD
        )

        val testDocumentList = TreeMap[Int, Document](
          0 -> TestValues.testDocumentH,
          1 -> TestValues.testDocumentI
        )

        testDocumentList.foreach {
          case (key, document: Document) => {
            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(document.id, *, *) returns(Future.successful(\/-(document)))
          }
        }

        val result = taskFeedbackRepository.delete(testTask)
        val eitherTaskFeedbacks = Await.result(result, Duration.Inf)
        val \/-(taskFeedbacks) = eitherTaskFeedbacks

        taskFeedbacks.size should be(testTaskFeedbackList.size)

        testTaskFeedbackList.foreach {
          case (key, taskFeedback: TaskFeedback) => {
            taskFeedbacks(key).studentId should be(taskFeedback.studentId)
            taskFeedbacks(key).taskId should be(taskFeedback.taskId)
            taskFeedbacks(key).version should be(taskFeedback.version)
            taskFeedbacks(key).documentId should be(taskFeedback.documentId)
            taskFeedbacks(key).createdAt.toString should be(taskFeedback.createdAt.toString)
            taskFeedbacks(key).updatedAt.toString should be(taskFeedback.updatedAt.toString)
          }
        }
      }
    }
  }
}
