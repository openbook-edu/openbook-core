import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.Chat
import ca.shiftfocus.krispii.core.repositories.ChatRepositoryPostgres
import org.scalatest.Matchers._
import org.scalatest._

import scala.collection.immutable.TreeMap
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scalaz.{-\/, \/-}

class ChatRepositorySpec
    extends TestEnvironment {
  val chatRepository = new ChatRepositoryPostgres

  "ChatRepository.list" should {
    inSequence {
      "list all chat_logs for a course" in {
        val testCourse = TestValues.testCourseA

        val testChatLogList = TreeMap[Int, Chat](
          0 -> TestValues.testChatA,
          1 -> TestValues.testChatB,
          2 -> TestValues.testChatC,
          3 -> TestValues.testChatD,
          4 -> TestValues.testChatE,
          5 -> TestValues.testChatF
        )

        val result = chatRepository.list(testCourse)
        val eitherChatLogs = Await.result(result, Duration.Inf)
        val \/-(chatLogs) = eitherChatLogs

        chatLogs.size should be(testChatLogList.size)

        testChatLogList.foreach {
          case (key, chatLog: Chat) => {
            chatLogs(key).courseId should be(chatLog.courseId)
            chatLogs(key).messageNum should be(chatLog.messageNum)
            chatLogs(key).userId should be(chatLog.userId)
            chatLogs(key).message should be(chatLog.message)
            chatLogs(key).hidden should be(chatLog.hidden)
            chatLogs(key).createdAt.toString should be(chatLog.createdAt.toString)
          }
        }
      }
      "list only an indicated portion of chat logs for a course" in {
        val testCourse = TestValues.testCourseA
        val num = 2
        val offset = 2

        val testChatLogList = TreeMap[Int, Chat](
          0 -> TestValues.testChatC,
          1 -> TestValues.testChatD
        )

        val result = chatRepository.list(testCourse, num, offset)
        val eitherChatLogs = Await.result(result, Duration.Inf)
        val \/-(chatLogs) = eitherChatLogs

        chatLogs.size should be(testChatLogList.size)

        testChatLogList.foreach {
          case (key, chatLog: Chat) => {
            chatLogs(key).courseId should be(chatLog.courseId)
            chatLogs(key).messageNum should be(chatLog.messageNum)
            chatLogs(key).userId should be(chatLog.userId)
            chatLogs(key).message should be(chatLog.message)
            chatLogs(key).hidden should be(chatLog.hidden)
            chatLogs(key).createdAt.toString should be(chatLog.createdAt.toString)
          }
        }
      }
      "list all chat logs for a course for a user" in {
        val testCourse = TestValues.testCourseA
        val testUser = TestValues.testUserE

        val testChatLogList = TreeMap[Int, Chat](
          0 -> TestValues.testChatA,
          1 -> TestValues.testChatD
        )

        val result = chatRepository.list(testCourse, testUser)
        val eitherChatLogs = Await.result(result, Duration.Inf)
        val \/-(chatLogs) = eitherChatLogs

        chatLogs.size should be(testChatLogList.size)

        testChatLogList.foreach {
          case (key, chatLog: Chat) => {
            chatLogs(key).courseId should be(chatLog.courseId)
            chatLogs(key).messageNum should be(chatLog.messageNum)
            chatLogs(key).userId should be(chatLog.userId)
            chatLogs(key).message should be(chatLog.message)
            chatLogs(key).hidden should be(chatLog.hidden)
            chatLogs(key).createdAt.toString should be(chatLog.createdAt.toString)
          }
        }
      }
      "list only an indicated portion of chat logs for a course for a user" in {
        val testCourse = TestValues.testCourseA
        val testUser = TestValues.testUserC
        val num = 2
        val offset = 1

        val testChatLogList = TreeMap[Int, Chat](
          0 -> TestValues.testChatC,
          1 -> TestValues.testChatE
        )

        val result = chatRepository.list(testCourse, testUser, num, offset)
        val eitherChatLogs = Await.result(result, Duration.Inf)
        val \/-(chatLogs) = eitherChatLogs

        chatLogs.size should be(testChatLogList.size)

        testChatLogList.foreach {
          case (key, chatLog: Chat) => {
            chatLogs(key).courseId should be(chatLog.courseId)
            chatLogs(key).messageNum should be(chatLog.messageNum)
            chatLogs(key).userId should be(chatLog.userId)
            chatLogs(key).message should be(chatLog.message)
            chatLogs(key).hidden should be(chatLog.hidden)
            chatLogs(key).createdAt.toString should be(chatLog.createdAt.toString)
          }
        }
      }
    }
  }

  "ChatRepository.find" should {
    inSequence {
      "find a chat log for a course by number" in {
        val testCourse = TestValues.testCourseA
        val testChatLog = TestValues.testChatC

        val result = chatRepository.find(testCourse, testChatLog.messageNum)
        val eitherChatLog = Await.result(result, Duration.Inf)
        val \/-(chatLog) = eitherChatLog

        chatLog.courseId should be(testChatLog.courseId)
        chatLog.messageNum should be(testChatLog.messageNum)
        chatLog.userId should be(testChatLog.userId)
        chatLog.message should be(testChatLog.message)
        chatLog.hidden should be(testChatLog.hidden)
        chatLog.createdAt.toString should be(testChatLog.createdAt.toString)
      }
    }
  }

  "ChatRepository.insert" should {
    inSequence {
      "create a new chat log" in {
        val testChatLog = TestValues.testChatG

        val result = chatRepository.insert(testChatLog)
        val eitherChatLog = Await.result(result, Duration.Inf)
        val \/-(chatLog) = eitherChatLog

        chatLog.courseId should be(testChatLog.courseId)
        chatLog.messageNum should be(testChatLog.messageNum)
        chatLog.userId should be(testChatLog.userId)
        chatLog.message should be(testChatLog.message)
        chatLog.hidden should be(testChatLog.hidden)
      }
    }
  }

  "ChatRepository.update" should {
    inSequence {
      "update a chat log" in {
        val testChatLog = TestValues.testChatC
        val updatedChatLog = testChatLog.copy(
          userId = TestValues.testUserG.id,
          message = "new" + testChatLog.message,
          hidden = !testChatLog.hidden
        )

        val result = chatRepository.update(updatedChatLog)
        val eitherChatLog = Await.result(result, Duration.Inf)
        val \/-(chatLog) = eitherChatLog

        chatLog.courseId should be(updatedChatLog.courseId)
        chatLog.messageNum should be(updatedChatLog.messageNum)
        // User id is not changed
        chatLog.userId should be(testChatLog.userId)
        chatLog.userId should not be (updatedChatLog.userId)
        // Message is not changed
        chatLog.message should not be (updatedChatLog.message)
        chatLog.message should be(testChatLog.message)
        chatLog.hidden should be(updatedChatLog.hidden)
        chatLog.createdAt.toString should be(updatedChatLog.createdAt.toString)
      }
      "return RepositoryError.NoResults if course_id is wrong" in {
        val testChatLog = TestValues.testChatC
        val updatedChatLog = testChatLog.copy(
          courseId = TestValues.testCourseB.id,
          userId = TestValues.testUserG.id,
          message = "new" + testChatLog.message,
          hidden = !testChatLog.hidden
        )

        val result = chatRepository.update(updatedChatLog)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Chat")))
      }
    }
  }
}
