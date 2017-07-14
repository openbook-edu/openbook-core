package ca.shiftfocus.krispii.core.services

import java.util.UUID

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.krispii.core.models.{ Conversation, Message }
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.github.mauricio.async.db.Connection

import scala.concurrent.Future
import scalaz.\/

class ConversationServiceDefault(
    val db: DB,
    val scalaCache: ScalaCachePool,
    val conversationRepository: ConversationRepository,
    val messageRepository: MessageRepository,
    val userRepository: UserRepository
) extends ConversationService {

  implicit def conn: Connection = db.pool
  implicit def cache: ScalaCachePool = scalaCache

  /**
   * List conversations by entityId
   *
   * @param entityId
   * @param limit
   * @param offset
   * @return
   */
  def listByEntityId(entityId: UUID, limit: Int = 0, offset: Int = 0): Future[\/[ErrorUnion#Fail, IndexedSeq[Conversation]]] = {
    conversationRepository.list(entityId, limit, offset)
  }

  def findConversation(conversationId: UUID): Future[\/[ErrorUnion#Fail, Conversation]] = {
    conversationRepository.find(conversationId)
  }

  def addMember(conversationId: UUID, userId: UUID): Future[\/[ErrorUnion#Fail, Conversation]] = {
    for {
      conversation <- lift(conversationRepository.find(conversationId))
      newMember <- lift(userRepository.find(userId))
      result <- lift(conversationRepository.addMember(conversation, newMember))
    } yield result
  }

  def createConversation(ownerId: UUID, title: String, entityId: Option[UUID], entityType: Option[String]): Future[\/[ErrorUnion#Fail, Conversation]] = {
    conversationRepository.insert(
      Conversation(
        ownerId = ownerId,
        title = title,
        entityId = entityId,
        entityType = entityType
      )
    )
  }

  def deleteConversation(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, Conversation]] = {
    for {
      conversation <- lift(conversationRepository.find(id))
      _ <- predicate(conversation.version == version)(ServiceError.OfflineLockFail)
      deleted <- lift(conversationRepository.delete(conversation))
    } yield deleted
  }

  // ####### Messages ##################################################################################################

  /**
   * List messages related to a specific conversation
   *
   * @param conversationId
   * @param limit
   * @param offset
   * @return
   */
  def listByConversationId(conversationId: UUID, limit: Int = 0, offset: Int = 0): Future[\/[ErrorUnion#Fail, IndexedSeq[Message]]] = {
    messageRepository.list(conversationId, limit, offset)
  }

  def findMessage(messageId: UUID): Future[\/[ErrorUnion#Fail, Message]] = {
    messageRepository.find(messageId)
  }

  def createMessage(conversationId: UUID, userId: UUID, content: String, revisionId: Option[String], revisionType: Option[String], revisionVersion: Option[String]): Future[\/[ErrorUnion#Fail, Message]] = {
    messageRepository.insert(Message(
      conversationId = conversationId,
      userId = userId,
      content = content,
      revisionId = revisionId,
      revisionType = revisionType,
      revisionVersion = revisionVersion
    ))
  }

  def deleteMessage(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, Message]] = {
    for {
      message <- lift(messageRepository.find(id))
      deleted <- lift(messageRepository.delete(message))
    } yield deleted
  }
}
