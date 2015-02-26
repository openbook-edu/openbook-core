package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.models.tasks.Task
import ca.shiftfocus.krispii.core.fail.Fail
import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future
import scalaz.{\/, EitherT}

trait RoleRepositoryComponent extends FutureMonad {
  self: UserRepositoryComponent =>

  val roleRepository: RoleRepository

  trait RoleRepository {
    /**
     * The usual CRUD functions for the roles table.
     */
    def list: Future[\/[Fail, IndexedSeq[Role]]]
    def list(user: User): Future[\/[Fail, IndexedSeq[Role]]]
    def list(users: IndexedSeq[User]): Future[\/[Fail, Map[UUID, IndexedSeq[Role]]]]

    def find(id: UUID): Future[\/[Fail, Role]]
    def find(name: String): Future[\/[Fail, Role]]

    def insert(role: Role)(implicit conn: Connection): Future[\/[Fail, Role]]
    def update(role: Role)(implicit conn: Connection): Future[\/[Fail, Role]]
    def delete(role: Role)(implicit conn: Connection): Future[\/[Fail, Role]]

    /**
     * Role <-> User relationship methods.
     *
     * These methods manipulate the table joining users and roles in a many-to-many
     * relationship.
     */
    def addUsers(role: Role, userList: IndexedSeq[User])(implicit conn: Connection): Future[\/[Fail, Role]]
    def removeUsers(role: Role, userList: IndexedSeq[User])(implicit conn: Connection): Future[\/[Fail, Role]]
    def addToUser(user: User, role: Role)(implicit conn: Connection): Future[\/[Fail, Unit]]
    def addToUser(user: User, name: String)(implicit conn: Connection): Future[\/[Fail, Unit]]
    def removeFromUser(user: User, role: Role)(implicit conn: Connection): Future[\/[Fail, Unit]]
    def removeFromUser(user: User, name: String)(implicit conn: Connection): Future[\/[Fail, Unit]]
    def removeFromAllUsers(role: Role)(implicit conn: Connection): Future[\/[Fail, Unit]]
    def removeFromAllUsers(name: String)(implicit conn: Connection): Future[\/[Fail, Unit]]
  }
}
