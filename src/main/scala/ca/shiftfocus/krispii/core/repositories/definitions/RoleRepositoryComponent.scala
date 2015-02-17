package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.models.tasks.Task
import ca.shiftfocus.krispii.core.repositories.error.RepositoryError
import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future
import scalaz.{\/, EitherT}

trait RoleRepositoryComponent {
  self: UserRepositoryComponent =>

  val roleRepository: RoleRepository

  trait RoleRepository {
    /**
     * The usual CRUD functions for the roles table.
     */
    def list: Future[\/[RepositoryError, IndexedSeq[Role]]]
    def list(user: User): Future[\/[RepositoryError, IndexedSeq[Role]]]
    def list(users: IndexedSeq[User]): Future[\/[RepositoryError, Map[UUID, IndexedSeq[Role]]]]

    def find(id: UUID): Future[\/[RepositoryError, Role]]
    def find(name: String): Future[\/[RepositoryError, Role]]

    def insert(role: Role)(implicit conn: Connection): Future[\/[RepositoryError, Role]]
    def update(role: Role)(implicit conn: Connection): Future[\/[RepositoryError, Role]]
    def delete(role: Role)(implicit conn: Connection): Future[\/[RepositoryError, Role]]

    /**
     * Role <-> User relationship methods.
     *
     * These methods manipulate the table joining users and roles in a many-to-many
     * relationship.
     */
    def addUsers(role: Role, userList: IndexedSeq[User])(implicit conn: Connection): Future[\/[RepositoryError, Role]]
    def removeUsers(role: Role, userList: IndexedSeq[User])(implicit conn: Connection): Future[\/[RepositoryError, Role]]
    def addToUser(user: User, role: Role)(implicit conn: Connection): Future[\/[RepositoryError, Role]]
    def addToUser(user: User, name: String)(implicit conn: Connection): Future[\/[RepositoryError, Role]]
    def removeFromUser(user: User, role: Role)(implicit conn: Connection): Future[\/[RepositoryError, Role]]
    def removeFromUser(user: User, name: String)(implicit conn: Connection): Future[\/[RepositoryError, Role]]
    def removeFromAllUsers(role: Role)(implicit conn: Connection): Future[\/[RepositoryError, Role]]
    def removeFromAllUsers(name: String)(implicit conn: Connection): Future[\/[RepositoryError, Role]]

    protected def lift = EitherT.eitherT[Future, RepositoryError, Role] _
    protected def liftList = EitherT.eitherT[Future, RepositoryError, IndexedSeq[Role]] _
    protected def liftMap = EitherT.eitherT[Future, RepositoryError, Map[UUID, IndexedSeq[Role]]] _
  }
}
