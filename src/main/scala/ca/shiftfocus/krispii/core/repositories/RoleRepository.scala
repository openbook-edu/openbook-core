package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.Connection
import ca.shiftfocus.krispii.core.models._
import java.util.UUID
import scala.concurrent.Future
import scalaz.{\/}

trait RoleRepository extends Repository {
  val userRepository: UserRepository

  /**
   * The usual CRUD functions for the roles table.
   */
  def list(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Role]]]
  def list(user: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Role]]]

  def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Role]]
  def find(name: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Role]]

  def insert(role: Role)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Role]]
  def update(role: Role)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Role]]
  def delete(role: Role)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Role]]

  /**
   * Role <-> User relationship methods.
   *
   * These methods manipulate the table joining users and roles in a many-to-many
   * relationship.
   */
  def addUsers(role: Role, userList: IndexedSeq[User])(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]
  def removeUsers(role: Role, userList: IndexedSeq[User])(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]
  def addToUser(user: User, role: Role)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]
  def addToUser(user: User, name: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]
  def removeFromUser(user: User, role: Role)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]
  def removeFromUser(user: User, name: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]
  def removeFromAllUsers(role: Role)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]
  def removeFromAllUsers(name: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]
}
