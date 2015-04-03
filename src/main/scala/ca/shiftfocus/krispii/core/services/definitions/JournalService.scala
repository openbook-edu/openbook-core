package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error.{ErrorUnion, RepositoryError}
import ca.shiftfocus.krispii.core.models.{User, JournalEntry}
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.Connection
import org.joda.time.DateTime

import scala.concurrent.Future
import scalaz.\/

trait JournalService extends Service[ErrorUnion#Fail] {
  val config: Boolean
  val journalRepository: JournalRepository
  val userRepository: UserRepository
  val projectRepository: ProjectRepository

  def list(entryType: String)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, IndexedSeq[JournalEntry]]]
  def list(userId: UUID)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, IndexedSeq[JournalEntry]]]
  def list(startDate: Option[DateTime], endDate: Option[DateTime])(implicit conn: Connection): Future[\/[ErrorUnion#Fail, IndexedSeq[JournalEntry]]]

  def find(id: UUID)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, JournalEntry]]

  def logView(userId: UUID, projectId: UUID, location: String)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, Unit]]
  def logClick(userId: UUID, projectId: UUID, location: String)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, Unit]]
  def logWatch(userId: UUID, projectId: UUID, location: String)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, Unit]]
  def logListen(userId: UUID, projectId: UUID, location: String)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, Unit]]
  def logInput(userId: UUID, projectId: UUID, location: String)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, Unit]]
  def logCreate(userId: UUID, projectId: UUID, location: String)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, Unit]]
  def logUpdate(userId: UUID, projectId: UUID, location: String)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, Unit]]
  def logDelete(userId: UUID, projectId: UUID, location: String)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, Unit]]

  def delete(journalEntry: JournalEntry)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, JournalEntry]]
  def delete(entryType: String)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, IndexedSeq[JournalEntry]]]
  def delete(user: User)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, IndexedSeq[JournalEntry]]]
}
