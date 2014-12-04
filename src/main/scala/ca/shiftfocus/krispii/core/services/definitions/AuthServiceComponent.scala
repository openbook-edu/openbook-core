package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.uuid.UUID
import ca.shiftfocus.krispii.core.models._
import scala.concurrent.Future

/**
 * In the "cake pattern", Services replaces the companion object in the domain models.
 * Provides domain-specific methods that the controllers need to call to accomplish
 * certain tasks.
 *
 * It consists of the AuthServiceDef trait, which defines the interface that
 * a AuthService must have. This trait can then be extended by one or many concrete
 * implementation traits. It is the responsibility of the Controller to inject
 * the appropriate implementation trait.
 */
trait AuthServiceComponent {

  /**
   * Defines the value that the AuthService instance should be stored in (and
   * accessed by). Controllers that implement the AuthServiceDef will access
   * its methods by calling this value.
   */
  val authService: AuthService

  /**
   * AuthService trait. Defines the interface that the authentication service must
   * provide. Generally, it provides the domain-level interface for managing
   * users and roles.
   */
  trait AuthService {

    /**
     * Authenticates a given identifier/password combination.
     *
     * @param identifier a [[String]] representing the user's e-mail or username
     * @param password a the user's password
     * @return the optionally authenticated user info
     */
    def authenticate(identifier: String, password: String): Future[Option[User]]

    def listSessions(userId: UUID): Future[IndexedSeq[Session]]

    def findSession(sessionId: UUID): Future[Option[Session]]

    def createSession(userId: UUID, ipAddress: String, userAgent: String): Future[Session]

    def updateSession(sessionId: UUID, ipAddress: String, userAgent: String): Future[Session]

    def deleteSession(sessionId: UUID): Future[Session]

    /**
     * List all users.
     *
     * @return a list of users with their roles and sections
     */
    def list: Future[IndexedSeq[UserInfo]]

    /**
     * List users with filter for roles and sections.
     *
     * @param rolesFilter an optional list of roles to filter by
     * @param sectionsFilter an optional list of sections to filter by
     * @return a list of users with their roles and sections
     */
    def list(rolesFilter: Option[IndexedSeq[String]], sectionsFilter: Option[IndexedSeq[UUID]]): Future[IndexedSeq[UserInfo]]

    /**
     * Find a user by their UUID.
     *
     * @param id  The user's universally unique identifier.
     * @return the optionally authenticated user info
     */
    def find(id: UUID): Future[Option[UserInfo]]

    /**
     * Find a user by their unique identifier.
     *
     * @param identifier  The unique e-mail or username identifying this user.
     * @return the optionally authenticated user info
     */
    def find(identifier: String): Future[Option[UserInfo]]

    /**
     * Create a new user. Throws exceptions if the e-mail and username aren't unique.
     *
     * @param username  A unique identifier for this user.
     * @param email  The user's unique e-mail address.
     * @param password  The user's password.
     * @param givenname  The user's first name.
     * @param surname  The user's family name.
     * @return the created user
     */
    def create(username: String, email: String, password: String, givenname: String, surname: String): Future[User]

    def create(username: String, email: String, password: String, givenname: String, surname: String, id: UUID): Future[User]

    /**
     * Update an existing user. Throws exceptions if the e-mail and username aren't unique.
     *
     * @param id  The unique ID of the user to be updated
     * @param version  The current version of the user
     * @param values  A hashmap of the values to be updated
     * @return the updated user
     */
    def update(id: UUID, version: Long, values: Map[String, String]): Future[User]

    /**
     * Deletes a user. This is a VERY DESTRUCTIVE operation.
     */
    def delete(id: UUID, version: Long): Future[Boolean]

    /**
     * List all roles.
     *
     * @return an array of Roles
     */
    def listRoles: Future[IndexedSeq[Role]]

    /**
     * List all roles for one user.
     *
     * @param user  The user whose roles should be listed.
     * @return an array of this user's Roles
     */
    def listRoles(userId: UUID): Future[IndexedSeq[Role]]

    /**
     * Find a specific role by its unique id.
     *
     * @param id  the UUID of the Role to find
     * @return an optional Role
     */
    def findRole(id: UUID): Future[Option[Role]]

    /**
     * Find a specific role by name
     *
     * @param id  the name of the Role to find
     * @return an optional Role
     */
    def findRole(name: String): Future[Option[Role]]

    /**
     * Create a new role.
     *
     * @param name  the name of the Role to create
     * @return the newly created Role
     */
    def createRole(name: String): Future[Role]

    /**
     * Update a Role
     *
     * @param id  the unique id of the Role
     * @param version  the version of the Role for optimistic offline lock
     * @param name  the new name to assign this Role
     * @return the newly updated Role
     */
    def updateRole(id: UUID, version: Long, name: String): Future[Role]

    /**
     *  Delete a role.
     *
     *  @param id  the unique id of the role
     *  @param version  the version of the role for optimistic offline lock
     *  @return the deleted role
     */
    def deleteRole(id: UUID, version: Long): Future[Role]

    /**
     * Add a role to a user.
     *
     * @param userId  the unique id of the user
     * @param roleName  the name of the role
     * @return a boolean indicator if the role was added
     */
    def addRole(userId: UUID, roleName: String)

    /**
     * Remove a role from a user.
     *
     * @param userId  the unique id of the user
     * @param roleName  the name of the role
     * @return a boolean indicator if the role was removed
     */
    def removeRole(userId: UUID, roleName: String)

    /**
     * Add a role to a given list of users.
     *
     * @param roleId the [[UUID]] of the [[Role]] to be added
     * @param userIds an [[IndexedSeq]] of [[UUID]] listing the users to gain the role
     * @return a boolean indicator if the role was added
     */
    def addUsers(roleId: UUID, userIds: IndexedSeq[UUID]): Future[Boolean]

    /**
     * Remove a role from a given list of users.
     *
     * @param roleId the [[UUID]] of the [[Role]] to be removed
     * @param userIds an [[IndexedSeq]] of [[UUID]] listing the users to lose the role
     * @return a boolean indicator if the role was removed
     */
    def removeUsers(roleId: UUID, userIds: IndexedSeq[UUID]): Future[Boolean]
  }
}
