package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.JournalEntry
import java.util.UUID

import ca.shiftfocus.krispii.core.models.user.User
import com.github.mauricio.async.db.Connection
import org.joda.time.DateTime

import scala.concurrent.Future
import scalaz._

trait JournalRepository extends Repository {
  val userRepository: UserRepository
  val projectRepository: ProjectRepository

  def list(entryType: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[JournalEntry]]]
  def list(user: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[JournalEntry]]]
  def list(startDate: Option[DateTime], endDate: Option[DateTime]) // format: OFF
          (implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[JournalEntry]]] // format: ON

  def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JournalEntry]]

  def insert(journalEntry: JournalEntry)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JournalEntry]]
  def delete(journalEntry: JournalEntry)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JournalEntry]]
  def delete(entryType: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[JournalEntry]]]
  def delete(user: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[JournalEntry]]]
}
