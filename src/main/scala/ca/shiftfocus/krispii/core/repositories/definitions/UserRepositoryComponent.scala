package ca.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future
import ca.shiftfocus.krispii.core.fail.Fail
import scalaz.{EitherT, \/}

trait UserRepositoryComponent extends FutureMonad {
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
    def list: Future[\/[Fail, IndexedSeq[User]]]
    def list(userIds: IndexedSeq[UUID]): Future[\/[Fail, IndexedSeq[User]]]
    def list(course: Course): Future[\/[Fail, IndexedSeq[User]]]
    def listForCourses(course: IndexedSeq[Course]): Future[\/[Fail, IndexedSeq[User]]]
    def listForRoles(roles: IndexedSeq[String]): Future[\/[Fail, IndexedSeq[User]]]
    def listForRolesAndCourses(roles: IndexedSeq[String], classes: IndexedSeq[String]): Future[\/[Fail, IndexedSeq[User]]]

    def findByEmail(email: String): Future[\/[Fail, User]]
    def find(userId: UUID): Future[\/[Fail, User]]
    def find(identifier: String): Future[\/[Fail, User]]

    def insert(user: User)(implicit conn: Connection): Future[\/[Fail, User]]
    def update(user: User)(implicit conn: Connection): Future[\/[Fail, User]]
    def delete(user: User)(implicit conn: Connection): Future[\/[Fail, User]]
  }
}
