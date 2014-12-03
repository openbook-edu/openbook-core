package ca.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import org.joda.time.DateTime
import scala.concurrent.Future

trait JournalRepositoryComponent {
  val journalRepository: JournalRepository

  trait JournalRepository {
    def list(implicit conn: Connection): Future[IndexedSeq[JournalEntry]]
    def list(startDateOption: Option[DateTime], endDateOption: Option[DateTime])(implicit conn: Connection): Future[IndexedSeq[JournalEntry]]
    def list(user: User)(implicit conn: Connection): Future[IndexedSeq[JournalEntry]]
    def list(user: User, startDateOption: Option[DateTime], endDateOption: Option[DateTime])(implicit conn: Connection): Future[IndexedSeq[JournalEntry]]
    def find(id: UUID)(implicit conn: Connection): Future[Option[JournalEntry]]
    def insert(journalEntry: JournalEntry)(implicit conn: Connection): Future[JournalEntry]
    def update(journalEntry: JournalEntry)(implicit conn: Connection): Future[JournalEntry]
   // def delete(task: Task)(implicit conn: Connection): Future[Boolean]
  }
}
