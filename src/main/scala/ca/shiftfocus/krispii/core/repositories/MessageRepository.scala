package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.krispii.core.models.Message
import com.github.mauricio.async.db.Connection
import org.joda.time.DateTime

import scala.concurrent.Future
import scalaz.\/

trait MessageRepository extends Repository {
  def find(id: UUID)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Message]]
  def list(conversationId: UUID, limit: Int = 0, offset: Int = 0)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[Message]]]
  def list(conversationId: UUID, afterDate: DateTime)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[Message]]]
  def insert(course: Message)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Message]]
  def delete(course: Message)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Message]]
}
