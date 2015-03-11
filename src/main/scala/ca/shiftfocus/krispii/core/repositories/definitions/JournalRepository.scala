//package ca.shiftfocus.krispii.core.repositories
//
//import ca.shiftfocus.krispii.core.error.RepositoryError
//import com.github.mauricio.async.db.Connection
//import scala.concurrent.ExecutionContext.Implicits.global
//import ca.shiftfocus.krispii.core.lib._
//import ca.shiftfocus.krispii.core.models._
//import ca.shiftfocus.uuid.UUID
//import org.joda.time.DateTime
//import scala.concurrent.Future
//import scalaz.{\/, EitherT}
//
//trait JournalRepositoryComponent extends FutureMonad {
//  self: UserRepositoryComponent =>
//
//  val journalRepository: JournalRepository
//
//  trait JournalRepository {
//    def list(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[JournalEntry]]]
//    def list(startDateOption: Option[DateTime], endDateOption: Option[DateTime])(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[JournalEntry]]]
//    def list(user: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[JournalEntry]]]
//    def list(user: User, startDateOption: Option[DateTime], endDateOption: Option[DateTime])(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[JournalEntry]]]
//
//    def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JournalEntry]]
//
//    def insert(journalEntry: JournalEntry)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JournalEntry]]
//    def update(journalEntry: JournalEntry)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JournalEntry]]
//    def delete(task: JournalEntry)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JournalEntry]]
//  }
//}
