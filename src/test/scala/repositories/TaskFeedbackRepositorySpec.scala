import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.document.Document
import ca.shiftfocus.krispii.core.repositories._
import java.util.UUID
import com.github.mauricio.async.db.Connection

import org.scalatest._
import Matchers._
import scala.collection.immutable.TreeMap
import scala.concurrent.{ Future, Await }
import scala.concurrent.duration.Duration
import scalaz.{ -\/, \/- }

class TaskFeedbackRepositorySpec
    extends TestEnvironment {
  val documentRepository = stub[DocumentRepository]
  val taskFeedbackRepository = new TaskFeedbackRepositoryPostgres(documentRepository)

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
            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (document.id, *, *) returns (Future.successful(\/-(document)))
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
      "return empty Vector() if student doesn't exist" in {
        val testUser = TestValues.testUserD
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
            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (document.id, *, *) returns (Future.successful(\/-(document)))
          }
        }

        val result = taskFeedbackRepository.list(testUser, testProject)
        Await.result(result, Duration.Inf) should be(\/-(Vector()))
      }
      "return empty Vector() if project doesn't exist" in {
        val testUser = TestValues.testUserC
        val testProject = TestValues.testProjectD

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
            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (document.id, *, *) returns (Future.successful(\/-(document)))
          }
        }

        val result = taskFeedbackRepository.list(testUser, testProject)
        Await.result(result, Duration.Inf) should be(\/-(Vector()))
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
            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (document.id, *, *) returns (Future.successful(\/-(document)))
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
      "return empty Vector() if task doesn't exist" in {
        val testTask = TestValues.testMatchingTaskJ

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
            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (document.id, *, *) returns (Future.successful(\/-(document)))
          }
        }

        val result = taskFeedbackRepository.list(testTask)
        Await.result(result, Duration.Inf) should be(\/-(Vector()))
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

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (testDocument.id, *, *) returns (Future.successful(\/-(testDocument)))

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
      "return RepositoryError.NoResults if user doesn't exist" in {
        val testTask = TestValues.testMatchingTaskE
        val testUser = TestValues.testUserD
        val testTaskFeedback = TestValues.testTaskFeedbackD
        val testDocument = TestValues.testDocumentI

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (testDocument.id, *, *) returns (Future.successful(\/-(testDocument)))

        val result = taskFeedbackRepository.find(testUser, testTask)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type TaskFeedback")))
      }
      "return RepositoryError.NoResults if task doesn't exist" in {
        val testTask = TestValues.testMatchingTaskJ
        val testUser = TestValues.testUserE
        val testTaskFeedback = TestValues.testTaskFeedbackD
        val testDocument = TestValues.testDocumentI

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (testDocument.id, *, *) returns (Future.successful(\/-(testDocument)))

        val result = taskFeedbackRepository.find(testUser, testTask)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type TaskFeedback")))
      }
    }
  }

  "TaskRepositoryFeedback.insert" should {
    inSequence {
      "create a new feedback for a task" in {
        val testTaskFeedback = TestValues.testTaskFeedbackF
        val testDocument = TestValues.testDocumentA

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (testDocument.id, *, *) returns (Future.successful(\/-(testDocument)))

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
      "return RepositoryError.PrimaryKeyConflict if task feedback already exists for the user in this task" in {
        val testTaskFeedback = TestValues.testTaskFeedbackA
        val testDocument = TestValues.testDocumentF

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (testDocument.id, *, *) returns (Future.successful(\/-(testDocument)))

        val result = taskFeedbackRepository.insert(testTaskFeedback)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
      }
    }
  }

  "TaskRepositoryFeedback.delete" should {
    inSequence {
      "delete a feedback" in {
        val testTaskFeedback = TestValues.testTaskFeedbackC
        val testDocument = TestValues.testDocumentH

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (testDocument.id, *, *) returns (Future.successful(\/-(testDocument)))

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
      "rerurn RepositoryError.NoResults if TaskFeedback doesn't exist" in {
        val testTaskFeedback = TestValues.testTaskFeedbackF
        val testDocument = TestValues.testDocumentH

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (testDocument.id, *, *) returns (Future.successful(\/-(testDocument)))

        val result = taskFeedbackRepository.delete(testTaskFeedback)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type TaskFeedback")))
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
            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (document.id, *, *) returns (Future.successful(\/-(document)))
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
      "return empty Vector() if taks doesn't exist" in {
        val testTask = TestValues.testMatchingTaskJ

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
            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (document.id, *, *) returns (Future.successful(\/-(document)))
          }
        }

        val result = taskFeedbackRepository.delete(testTask)
        Await.result(result, Duration.Inf) should be(\/-(Vector()))
      }
    }
  }
}
