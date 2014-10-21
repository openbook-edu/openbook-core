package com.shiftfocus.krispii.common.repositories

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import com.shiftfocus.krispii.lib._
import com.shiftfocus.krispii.common.models._
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
