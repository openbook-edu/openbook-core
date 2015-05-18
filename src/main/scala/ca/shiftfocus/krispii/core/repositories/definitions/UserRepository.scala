package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.Connection

import scala.concurrent.Future
import scalacache.ScalaCache
import scalaz.\/


trait UserRepository extends Repository {
  def list(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[User]]]
  def list(userIds: IndexedSeq[UUID])(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[User]]]
  def list(role: Role)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[User]]]
  def list(course: Course)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[User]]]

  def find(userId: UUID)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, User]]
  def find(identifier: String)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, User]]

  def insert(user: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, User]]
  def update(user: User)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, User]]
  def delete(user: User)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, User]]
}
