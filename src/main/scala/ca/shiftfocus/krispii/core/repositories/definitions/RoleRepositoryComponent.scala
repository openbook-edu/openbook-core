package ca.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future

trait RoleRepositoryComponent {
  val roleRepository: RoleRepository

  trait RoleRepository {
    /**
     * The usual CRUD functions for the roles table.
     */
    def list: Future[IndexedSeq[Role]]
    def list(user: User): Future[IndexedSeq[Role]]
    def list(users: IndexedSeq[User]): Future[Map[UUID, IndexedSeq[Role]]]
    def find(id: UUID): Future[Option[Role]]
    def find(name: String): Future[Option[Role]]
    def insert(role: Role)(implicit conn: Connection): Future[Role]
    def update(role: Role)(implicit conn: Connection): Future[Role]
    def delete(role: Role)(implicit conn: Connection): Future[Boolean]

    /**
     * Role <-> User relationship methods.
     *
     * These methods manipulate the table joining users and roles in a many-to-many
     * relationship.
     */
    def addUsers(role: Role, userList: IndexedSeq[User])(implicit conn: Connection): Future[Boolean]
    def removeUsers(role: Role, userList: IndexedSeq[User])(implicit conn: Connection): Future[Boolean]
    def addToUser(user: User, role: Role)(implicit conn: Connection): Future[Boolean]
    def addToUser(user: User, name: String)(implicit conn: Connection): Future[Boolean]
    def removeFromUser(user: User, role: Role)(implicit conn: Connection): Future[Boolean]
    def removeFromUser(user: User, name: String)(implicit conn: Connection): Future[Boolean]
    def removeFromAllUsers(role: Role)(implicit conn: Connection): Future[Boolean]
    def removeFromAllUsers(name: String)(implicit conn: Connection): Future[Boolean]
  }
}
