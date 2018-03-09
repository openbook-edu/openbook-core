package ca.shiftfocus.krispii.core.services

import java.util.UUID

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models._
import org.joda.time.DateTime
import scala.concurrent.Future
import scalaz.\/

trait ConversationService extends Service[ErrorUnion#Fail] {

  // ####### CONVERSATIONS #############################################################################################

  def listByEntityId(entityId: UUID, limit: Int = 0, offset: Int = 0): Future[\/[ErrorUnion#Fail, IndexedSeq[Conversation]]]
  def listByEntityId(entityId: UUID, afterDate: DateTime): Future[\/[ErrorUnion#Fail, IndexedSeq[Conversation]]]
  def listByEntityAndUserId(entityId: UUID, userId: UUID, limit: Int = 0, offset: Int = 0): Future[\/[ErrorUnion#Fail, IndexedSeq[Conversation]]]
  def listByEntityAndUserId(entityId: UUID, userId: UUID, afterDate: DateTime): Future[\/[ErrorUnion#Fail, IndexedSeq[Conversation]]]
  def findConversation(conversationId: UUID): Future[\/[ErrorUnion#Fail, Conversation]]
  def addMember(conversationId: UUID, userId: UUID): Future[\/[ErrorUnion#Fail, Conversation]]
  def createConversation(ownerId: UUID, title: String, entityId: Option[UUID], entityType: Option[String]): Future[\/[ErrorUnion#Fail, Conversation]]
  def deleteConversation(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, Conversation]]

  // ####### MESSAGES ##################################################################################################

  def listByConversationId(conversationId: UUID, limit: Int = 0, offset: Int = 0): Future[\/[ErrorUnion#Fail, IndexedSeq[Message]]]
  def listByConversationId(conversationId: UUID, afterDate: DateTime): Future[\/[ErrorUnion#Fail, IndexedSeq[Message]]]
  def findMessage(messageId: UUID): Future[\/[ErrorUnion#Fail, Message]]
  def hasNewMessage(entityId: UUID, userId: UUID): Future[\/[ErrorUnion#Fail, Boolean]]
  def setLastRead(conversationId: UUID, userId: UUID, messageId: UUID, reatAt: DateTime): Future[\/[ErrorUnion#Fail, Unit]]
  def getLastRead(conversationId: UUID, userId: UUID): Future[\/[ErrorUnion#Fail, Message]]
  def createMessage(conversationId: UUID, userId: UUID, content: String, revisionId: Option[String], revisionType: Option[String], revisionVersion: Option[String]): Future[\/[ErrorUnion#Fail, Message]]
  def deleteMessage(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, Message]]
}
