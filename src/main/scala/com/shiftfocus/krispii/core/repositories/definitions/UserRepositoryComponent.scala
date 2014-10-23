package com.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import com.shiftfocus.krispii.core.lib._
import com.shiftfocus.krispii.core.models._
import scala.concurrent.Future

/**
 * In the "cake pattern", repositories take the role of Table Data Gateways. A
 * repository essentially *is* a TDG, but it's implemented as a class held in
 * a repository component trait.
 *
 * This allows a Service to mix in several repositories and have access to
 * their methods, without hardcoding the Service to a specific implementation.
 */
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
    /**
     * List methods return an indexed sequence of users.
     */

    /**
     * List all users.
     *
     * @return an [[IndexedSeq]] of [[UserInfo]]
     */
    def list: Future[IndexedSeq[User]]

    /**
     * List users with filter for roles and sections.
     *
     * @param rolesFilter an optional list of roles to filter by
     * @param sectionsFilter an optional list of sections to filter by
     * @return an [[IndexedSeq]] of [[UserInfo]]
     */
    def list(userIds: IndexedSeq[UUID]): Future[IndexedSeq[User]]

    /**
     * Authenticates a given identifier/password combination.
     *
     * @param email
     * @param password
     * @return Some(user) if valid, otherwise None.
     */
    def list(section: Section): Future[IndexedSeq[User]]
    def listForSections(sections: IndexedSeq[Section]): Future[IndexedSeq[User]]
    def listForRoles(roles: IndexedSeq[String]): Future[IndexedSeq[User]]
    def listForRolesAndSections(roles: IndexedSeq[String], sections: IndexedSeq[String]): Future[IndexedSeq[User]]

    /**
     * Find methods return a single user.
     */
    def findByEmail(email: String): Future[Option[User]]
    def find(userId: UUID): Future[Option[User]]
    def find(identifier: String): Future[Option[User]]

    /**
     * The C_UD from CRUD.
     */
    def insert(user: User)(implicit conn: Connection): Future[User]
    def update(user: User)(implicit conn: Connection): Future[User]
    def delete(user: User)(implicit conn: Connection): Future[Boolean]
  }
}
