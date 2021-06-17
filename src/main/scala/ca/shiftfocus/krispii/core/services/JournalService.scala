package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error.ErrorUnion
import ca.shiftfocus.krispii.core.models.JournalEntry
import ca.shiftfocus.krispii.core.repositories._
import java.util.UUID

import ca.shiftfocus.krispii.core.models.user.User
import org.joda.time.DateTime

import scala.concurrent.Future
import scalaz.\/

trait JournalService extends Service[ErrorUnion#Fail] {
  val config: Boolean
  val journalRepository: JournalRepository
  val userRepository: UserRepository
  val projectRepository: ProjectRepository

  def list(entryType: String): Future[\/[ErrorUnion#Fail, IndexedSeq[JournalEntry]]]
  def list(userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[JournalEntry]]]
  def list(startDate: Option[DateTime], endDate: Option[DateTime]): Future[\/[ErrorUnion#Fail, IndexedSeq[JournalEntry]]]

  def find(id: UUID): Future[\/[ErrorUnion#Fail, JournalEntry]]

  def logConnect(userId: UUID, projectId: UUID, location: String): Future[\/[ErrorUnion#Fail, Unit]]
  def logDisconnect(userId: UUID, projectId: UUID, location: String): Future[\/[ErrorUnion#Fail, Unit]]
  def logView(userId: UUID, projectId: UUID, location: String): Future[\/[ErrorUnion#Fail, Unit]]
  def logClick(userId: UUID, projectId: UUID, location: String): Future[\/[ErrorUnion#Fail, Unit]]
  def logWatch(userId: UUID, projectId: UUID, location: String): Future[\/[ErrorUnion#Fail, Unit]]
  def logListen(userId: UUID, projectId: UUID, location: String): Future[\/[ErrorUnion#Fail, Unit]]
  def logInput(userId: UUID, projectId: UUID, location: String): Future[\/[ErrorUnion#Fail, Unit]]
  def logCreate(userId: UUID, projectId: UUID, location: String): Future[\/[ErrorUnion#Fail, Unit]]
  def logUpdate(userId: UUID, projectId: UUID, location: String): Future[\/[ErrorUnion#Fail, Unit]]
  def logDelete(userId: UUID, projectId: UUID, location: String): Future[\/[ErrorUnion#Fail, Unit]]

  def delete(journalEntry: JournalEntry): Future[\/[ErrorUnion#Fail, JournalEntry]]
  def delete(entryType: String): Future[\/[ErrorUnion#Fail, IndexedSeq[JournalEntry]]]
  def delete(user: User): Future[\/[ErrorUnion#Fail, IndexedSeq[JournalEntry]]]
}
