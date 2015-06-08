import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.document._
import ca.shiftfocus.krispii.core.repositories._
import java.util.UUID
import com.github.mauricio.async.db.Connection
import org.scalatest._
import Matchers._

import scala.collection.immutable.TreeMap
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }
import scalaz.\/-

class TaskScratchpadRepositorySpec
    extends TestEnvironment {
  val documentRepository = stub[DocumentRepository]
  val taskScratchpadRepository = new TaskScratchpadRepositoryPostgres(documentRepository)

  // TODO - test all test cases
  "TaskScratchpadRepository.list" should {
    inSequence {
      "list a user's latest revisions for each task in a project" in {
        val testUser = TestValues.testUserC
        val testProject = TestValues.testProjectA

        val testTaskScratchpadList = TreeMap[Int, TaskScratchpad](
          0 -> TestValues.testTaskScratchpadA,
          1 -> TestValues.testTaskScratchpadE
        )

        val testDocumentList = TreeMap[Int, Document](
          0 -> TestValues.testDocumentK,
          1 -> TestValues.testDocumentM
        )

        testDocumentList.foreach {
          case (key, document: Document) => {
            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (document.id, *, *) returns (Future.successful(\/-(document)))
          }
        }

        val result = taskScratchpadRepository.list(testUser, testProject)
        val eitherTaskScratchpads = Await.result(result, Duration.Inf)
        val \/-(taskScratchpads) = eitherTaskScratchpads

        taskScratchpads.size should be(testTaskScratchpadList.size)

        testTaskScratchpadList.foreach {
          case (key, taskScratchpad: TaskScratchpad) => {
            taskScratchpads(key).userId should be(taskScratchpad.userId)
            taskScratchpads(key).taskId should be(taskScratchpad.taskId)
            taskScratchpads(key).version should be(taskScratchpad.version)
            taskScratchpads(key).documentId should be(taskScratchpad.documentId)
            taskScratchpads(key).createdAt.toString should be(taskScratchpad.createdAt.toString)
            taskScratchpads(key).updatedAt.toString should be(taskScratchpad.updatedAt.toString)
          }
        }
      }
      "list a user's latest revisions for all task scratchpads for all projects" in {
        val testUser = TestValues.testUserC

        val testTaskScratchpadList = TreeMap[Int, TaskScratchpad](
          0 -> TestValues.testTaskScratchpadA,
          1 -> TestValues.testTaskScratchpadC,
          2 -> TestValues.testTaskScratchpadE
        )

        val testDocumentList = TreeMap[Int, Document](
          0 -> TestValues.testDocumentK,
          1 -> TestValues.testDocumentM,
          2 -> TestValues.testDocumentN
        )

        testDocumentList.foreach {
          case (key, document: Document) => {
            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (document.id, *, *) returns (Future.successful(\/-(document)))
          }
        }

        val result = taskScratchpadRepository.list(testUser)
        val eitherTaskScratchpads = Await.result(result, Duration.Inf)
        val \/-(taskScratchpads) = eitherTaskScratchpads

        taskScratchpads.size should be(testTaskScratchpadList.size)

        testTaskScratchpadList.foreach {
          case (key, taskScratchpad: TaskScratchpad) => {
            taskScratchpads(key).userId should be(taskScratchpad.userId)
            taskScratchpads(key).taskId should be(taskScratchpad.taskId)
            taskScratchpads(key).version should be(taskScratchpad.version)
            taskScratchpads(key).documentId should be(taskScratchpad.documentId)
            taskScratchpads(key).createdAt.toString should be(taskScratchpad.createdAt.toString)
            taskScratchpads(key).updatedAt.toString should be(taskScratchpad.updatedAt.toString)
          }
        }
      }
      "list all users latest scratchpad revisions to a particular task" in {
        val testTask = TestValues.testMatchingTaskE

        val testTaskScratchpadList = TreeMap[Int, TaskScratchpad](
          0 -> TestValues.testTaskScratchpadC,
          1 -> TestValues.testTaskScratchpadD
        )

        val testDocumentList = TreeMap[Int, Document](
          0 -> TestValues.testDocumentN,
          1 -> TestValues.testDocumentO
        )

        testDocumentList.foreach {
          case (key, document: Document) => {
            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (document.id, *, *) returns (Future.successful(\/-(document)))
          }
        }

        val result = taskScratchpadRepository.list(testTask)
        val eitherTaskScratchpads = Await.result(result, Duration.Inf)
        val \/-(taskScratchpads) = eitherTaskScratchpads

        taskScratchpads.size should be(testTaskScratchpadList.size)

        testTaskScratchpadList.foreach {
          case (key, taskScratchpad: TaskScratchpad) => {
            taskScratchpads(key).userId should be(taskScratchpad.userId)
            taskScratchpads(key).taskId should be(taskScratchpad.taskId)
            taskScratchpads(key).version should be(taskScratchpad.version)
            taskScratchpads(key).documentId should be(taskScratchpad.documentId)
            taskScratchpads(key).createdAt.toString should be(taskScratchpad.createdAt.toString)
            taskScratchpads(key).updatedAt.toString should be(taskScratchpad.updatedAt.toString)
          }
        }
      }
    }
  }

  "TaskScratchpadRepository.find" should {
    inSequence {
      "find the latest revision of a task scratchpad" in {
        val testTask = TestValues.testMatchingTaskE
        val testUser = TestValues.testUserE
        val testTaskScratchpad = TestValues.testTaskScratchpadD
        val testDocument = TestValues.testDocumentO

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (testDocument.id, *, *) returns (Future.successful(\/-(testDocument)))

        val result = taskScratchpadRepository.find(testUser, testTask)
        val eitherTaskScratchpad = Await.result(result, Duration.Inf)
        val \/-(taskScratchpad) = eitherTaskScratchpad

        taskScratchpad.userId should be(testTaskScratchpad.userId)
        taskScratchpad.taskId should be(testTaskScratchpad.taskId)
        taskScratchpad.version should be(testTaskScratchpad.version)
        taskScratchpad.documentId should be(testTaskScratchpad.documentId)
        taskScratchpad.createdAt.toString should be(testTaskScratchpad.createdAt.toString)
        taskScratchpad.updatedAt.toString should be(testTaskScratchpad.updatedAt.toString)
      }
    }
  }

  "TaskScratchpadRepository.insert" should {
    inSequence {
      "insert a new TaskScratchpad" in {
        val testTaskScratchpad = TestValues.testTaskScratchpadF
        val testDocument = TestValues.testDocumentA

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (testDocument.id, *, *) returns (Future.successful(\/-(testDocument)))

        val result = taskScratchpadRepository.insert(testTaskScratchpad)
        val eitherTaskScratchpad = Await.result(result, Duration.Inf)
        val \/-(taskScratchpad) = eitherTaskScratchpad

        taskScratchpad.userId should be(testTaskScratchpad.userId)
        taskScratchpad.taskId should be(testTaskScratchpad.taskId)
        taskScratchpad.version should be(testTaskScratchpad.version)
        taskScratchpad.documentId should be(testTaskScratchpad.documentId)
        taskScratchpad.createdAt.toString should be(testTaskScratchpad.createdAt.toString)
        taskScratchpad.updatedAt.toString should be(testTaskScratchpad.updatedAt.toString)
      }
    }
  }

  "TaskScratchpadRepository.delete" should {
    inSequence {
      "deletes a task scratchpad" in {
        val testTaskScratchpad = TestValues.testTaskScratchpadC
        val testDocument = TestValues.testDocumentN

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (testDocument.id, *, *) returns (Future.successful(\/-(testDocument)))

        val result = taskScratchpadRepository.delete(testTaskScratchpad)
        val eitherTaskScratchpad = Await.result(result, Duration.Inf)
        val \/-(taskScratchpad) = eitherTaskScratchpad

        taskScratchpad.userId should be(testTaskScratchpad.userId)
        taskScratchpad.taskId should be(testTaskScratchpad.taskId)
        taskScratchpad.version should be(testTaskScratchpad.version)
        taskScratchpad.documentId should be(testTaskScratchpad.documentId)
        taskScratchpad.createdAt.toString should be(testTaskScratchpad.createdAt.toString)
        taskScratchpad.updatedAt.toString should be(testTaskScratchpad.updatedAt.toString)
      }
      "deletes all revisions of a task response for a particular task" in {
        val testTask = TestValues.testMatchingTaskE

        val testTaskScratchpadList = TreeMap[Int, TaskScratchpad](
          0 -> TestValues.testTaskScratchpadC,
          1 -> TestValues.testTaskScratchpadD
        )

        val testDocumentList = TreeMap[Int, Document](
          0 -> TestValues.testDocumentN,
          1 -> TestValues.testDocumentO
        )

        testDocumentList.foreach {
          case (key, document: Document) => {
            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (document.id, *, *) returns (Future.successful(\/-(document)))
          }
        }

        val result = taskScratchpadRepository.delete(testTask)
        val eitherTaskScratchpads = Await.result(result, Duration.Inf)
        val \/-(taskScratchpads) = eitherTaskScratchpads

        taskScratchpads.size should be(testTaskScratchpadList.size)

        testTaskScratchpadList.foreach {
          case (key, taskScratchpad: TaskScratchpad) => {
            taskScratchpads(key).userId should be(taskScratchpad.userId)
            taskScratchpads(key).taskId should be(taskScratchpad.taskId)
            taskScratchpads(key).version should be(taskScratchpad.version)
            taskScratchpads(key).documentId should be(taskScratchpad.documentId)
            taskScratchpads(key).createdAt.toString should be(taskScratchpad.createdAt.toString)
            taskScratchpads(key).updatedAt.toString should be(taskScratchpad.updatedAt.toString)
          }
        }
      }
    }
  }
}
