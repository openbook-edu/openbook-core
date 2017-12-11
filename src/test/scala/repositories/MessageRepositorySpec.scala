import java.util.UUID

import ca.shiftfocus.krispii.core.repositories._
import org.joda.time.DateTime
import org.scalatest.Matchers._

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scalaz._

class MessageRepositorySpec
    extends TestEnvironment {
  val messageRepository = new MessageRepositoryPostgres()

  "MessageRepository.listNew" should {
    inSequence {
      "list new messages within all conversations for entity" in {
        val result = messageRepository.listNew(TestValues.testLongAnswerWorkA.id, TestValues.testUserA.id)
        val eitherMessages = Await.result(result, Duration.Inf)
        val \/-(messages) = eitherMessages

        messages.foreach { message =>
          println(Console.GREEN + "---------------" + Console.RESET)
          println(Console.GREEN + message.id + Console.RESET)
          println(Console.GREEN + message.conversationId + Console.RESET)
          println(Console.GREEN + message.userId + Console.RESET)
          println(Console.GREEN + message.content + Console.RESET)
          println(Console.GREEN + Console.RESET)
        }

        1 should be(1)
      }
    }
  }

  "MessageRepository.getLastRead" should {
    inSequence {
      "list new messages within all conversations for entity" in {
        // testConversationC
        val result = messageRepository.getLastRead(UUID.fromString("c9cbdcb5-9ecb-484b-bcac-bfa641d574c6"), TestValues.testUserA.id)
        val eitherMessage = Await.result(result, Duration.Inf)
        val \/-(message) = eitherMessage

        println(Console.GREEN + "Last read message = " + message)

        1 should be(1)
      }
    }
  }

  "MessageRepository.setLastRead" should {
    inSequence {
      "update last read" in {
        // testConversationC
        val result = messageRepository.setLastRead(UUID.fromString("c9cbdcb5-9ecb-484b-bcac-bfa641d574c6"), TestValues.testUserA.id, UUID.fromString("e2d34ec2-7a40-4985-aa0f-05d379d3fe70"), new DateTime)
        val eitherMessage = Await.result(result, Duration.Inf)
        val \/-(message) = eitherMessage

        println(Console.GREEN + "Last read message = " + message)

        1 should be(1)
      }
      "insert last read" in {
        // testConversationB
        val result = messageRepository.setLastRead(UUID.fromString("1ba184c5-b315-47d6-b1e7-feee619eea97"), TestValues.testUserA.id, UUID.fromString("57e5f223-c107-45d2-b383-5973d6357a3a"), new DateTime)
        val eitherMessage = Await.result(result, Duration.Inf)
        val \/-(message) = eitherMessage

        println(Console.GREEN + "Last read message = " + message)

        1 should be(1)
      }
    }
  }

  "MessageRepository.hasNew" should {
    inSequence {
      "list new messages within all conversations for entity" in {
        val result = messageRepository.hasNew(TestValues.testLongAnswerWorkA.id, TestValues.testUserA.id)
        val eitherMessages = Await.result(result, Duration.Inf)
        val \/-(messages) = eitherMessages

        println(Console.GREEN + "Has new = " + messages)

        1 should be(1)
      }
    }
  }
}