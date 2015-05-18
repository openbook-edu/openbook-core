package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.krispii.core.models.{User, JournalEntry, Part}
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.Connection
import org.joda.time.DateTime

import scala.concurrent.Future
import scalacache.ScalaCache
import scalaz._

trait JournalRepository extends Repository {
  val userRepository: UserRepository
  val projectRepository: ProjectRepository

  def list(entryType: String)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[JournalEntry]]]
  def list(user: User)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[JournalEntry]]]
  def list(startDate: Option[DateTime], endDate: Option[DateTime])(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[JournalEntry]]]

  def find(id: UUID)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, JournalEntry]]

  def insert(journalEntry: JournalEntry)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, JournalEntry]]
  def delete(journalEntry: JournalEntry)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, JournalEntry]]
  def delete(entryType: String)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[JournalEntry]]]
  def delete(user: User)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[JournalEntry]]]
}
