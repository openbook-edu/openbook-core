package ca.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future
import error._
import scalaz.{EitherT, \/}

trait UserRepositoryComponent {
  val userRepository: UserRepository

  /**
   * Defines the API that a UserRepository must provide.
   *   NB: 'list' methods should return IndexedSeq[User]
   *       'find' methods should return Option[User]
   *
   * By accepting an implicit Connection, these methods can be chained together
   * inside a transactional block.
   */
  trait UserRepository {
    def list: Future[\/[RepositoryError, IndexedSeq[User]]]
    def list(userIds: IndexedSeq[UUID]): Future[\/[RepositoryError, IndexedSeq[User]]]
    def list(course: Course): Future[\/[RepositoryError, IndexedSeq[User]]]
    def listForCourses(course: IndexedSeq[Course]): Future[\/[RepositoryError, IndexedSeq[User]]]
    def listForRoles(roles: IndexedSeq[String]): Future[\/[RepositoryError, IndexedSeq[User]]]
    def listForRolesAndCourses(roles: IndexedSeq[String], classes: IndexedSeq[String]): Future[\/[RepositoryError, IndexedSeq[User]]]

    def findByEmail(email: String): Future[\/[RepositoryError, User]]
    def find(userId: UUID): Future[\/[RepositoryError, User]]
    def find(identifier: String): Future[\/[RepositoryError, User]]

    def insert(user: User)(implicit conn: Connection): Future[\/[RepositoryError, User]]
    def update(user: User)(implicit conn: Connection): Future[\/[RepositoryError, User]]
    def delete(user: User)(implicit conn: Connection): Future[\/[RepositoryError, User]]

    protected def lift = EitherT.eitherT[Future, RepositoryError, User] _
    protected def liftList = EitherT.eitherT[Future, RepositoryError, IndexedSeq[User]] _
  }
}
