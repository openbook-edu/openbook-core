package ca.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future
import ca.shiftfocus.krispii.core.error.RepositoryError
import scalaz.{EitherT, \/}


trait UserRepository extends Repository {
  def list(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[User]]]
  def list(userIds: IndexedSeq[UUID])(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[User]]]
  def list(role: Role)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[User]]]
  def list(course: Course)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[User]]]

  def findByEmail(email: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, User]]
  def find(userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, User]]
  def find(identifier: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, User]]

  def insert(user: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, User]]
  def update(user: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, User]]
  def delete(user: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, User]]
}
