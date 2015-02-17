package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.repositories.error.RepositoryError
import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import org.joda.time.DateTime
import scala.concurrent.Future
import scalaz.{\/, EitherT}

trait JournalRepositoryComponent {
  self: UserRepositoryComponent =>

  val journalRepository: JournalRepository

  trait JournalRepository {
    def list(implicit conn: Connection): Future[\/[RepositoryError, IndexedSeq[JournalEntry]]]
    def list(startDateOption: Option[DateTime], endDateOption: Option[DateTime])(implicit conn: Connection): Future[\/[RepositoryError, IndexedSeq[JournalEntry]]]
    def list(user: User)(implicit conn: Connection): Future[\/[RepositoryError, IndexedSeq[JournalEntry]]]
    def list(user: User, startDateOption: Option[DateTime], endDateOption: Option[DateTime])(implicit conn: Connection): Future[\/[RepositoryError, IndexedSeq[JournalEntry]]]

    def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError, JournalEntry]]

    def insert(journalEntry: JournalEntry)(implicit conn: Connection): Future[\/[RepositoryError, JournalEntry]]
    def update(journalEntry: JournalEntry)(implicit conn: Connection): Future[\/[RepositoryError, JournalEntry]]
    def delete(task: JournalEntry)(implicit conn: Connection): Future[\/[RepositoryError, JournalEntry]]

    protected def lift = EitherT.eitherT[Future, RepositoryError, JournalEntry] _
    protected def liftList = EitherT.eitherT[Future, RepositoryError, IndexedSeq[JournalEntry]] _
  }
}
