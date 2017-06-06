package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories.{ CourseRepository, RoleRepository, SessionRepository, UserRepository }
import java.util.UUID

import play.api.i18n.{ Lang, MessagesApi }

import scala.concurrent.Future
import scalacache.ScalaCache
import scalaz.\/

trait AuthService extends Service[ErrorUnion#Fail] {
  val scalaCache: ScalaCachePool
  val userRepository: UserRepository
  val roleRepository: RoleRepository
  val sessionRepository: SessionRepository

  /**
   * Authenticates a given identifier/password combination.
   *
   * @param identifier a String representing the user's e-mail or username
   * @param password a the user's password
   * @return the optionally authenticated user info
   */
  def authenticate(identifier: String, password: String): Future[\/[ErrorUnion#Fail, User]]
  def authenticateWithoutPassword(identifier: String): Future[\/[ErrorUnion#Fail, User]]

  /*
   * Session definitions
   */
  def listSessions(userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Session]]]
  def findSession(sessionId: UUID): Future[\/[ErrorUnion#Fail, Session]]
  def createSession(userId: UUID, ipAddress: String, userAgent: String): Future[\/[ErrorUnion#Fail, Session]]
  def updateSession(sessionId: UUID, ipAddress: String, userAgent: String): Future[\/[ErrorUnion#Fail, Session]]
  def deleteSession(sessionId: UUID): Future[\/[ErrorUnion#Fail, Session]]

  /**
   * List user by similiarity to a key word
   */
  def listByKey(key: String, includeDeleted: Boolean = false): Future[\/[ErrorUnion#Fail, IndexedSeq[User]]]

  /**
   * List all users.
   *
   * @return a list of users with their roles and courses
   */
  def list: Future[\/[ErrorUnion#Fail, IndexedSeq[User]]]
  def listByRange(limit: Int, offset: Int): Future[\/[ErrorUnion#Fail, IndexedSeq[User]]]

  /**
   * List users with filter for roles and courses.
   *
   * @param rolesFilter an optional list of roles to filter by
   * @return a list of users with their roles and courses
   */
  def list(rolesFilter: IndexedSeq[String]): Future[\/[ErrorUnion#Fail, IndexedSeq[User]]]

  /**
   * Find a user by their UUID.
   *
   * @param id the unique id of the user
   */
  def find(id: UUID, includeDeleted: Boolean = false): Future[\/[ErrorUnion#Fail, User]]

  /**
   * Find a user by their unique identifier.
   *
   * @param identifier  The unique e-mail or username identifying this user.
   * @return the optionally authenticated user info
   */
  def find(identifier: String): Future[\/[ErrorUnion#Fail, User]]

  /**
   * Create a new user.
   *
   * @param username  A unique identifier for this user.
   * @param email  The user's unique e-mail address.
   * @param password  The user's password.
   * @param givenname  The user's first name.
   * @param surname  The user's family name.
   * @return a future disjunction containing the created user, or a failure
   */
  def create(
    username: String,
    email: String,
    password: String,
    givenname: String,
    surname: String,
    id: UUID = UUID.randomUUID
  ): Future[\/[ErrorUnion#Fail, User]]

  def syncWithDeletedUser(newUser: User): Future[\/[ErrorUnion#Fail, Account]]

  /**
   * Creates a new user with the given role.
   * @param username
   * @param email
   * @param password
   * @param givenname
   * @param surname
   * @param role
   * @return
   */
  def createWithRole(
    username: String,
    email: String,
    password: String,
    givenname: String,
    surname: String,
    role: String,
    hostname: Option[String]
  )(messagesApi: MessagesApi, lang: Lang): Future[\/[ErrorUnion#Fail, User]]

  def createGoogleUser(
    email: String,
    givenname: String,
    surname: String
  ): Future[\/[ErrorUnion#Fail, User]]

  def updateToGoogleUser(
    email: String
  ): Future[\/[ErrorUnion#Fail, User]]

  /**
   * Update a user
   *
   * @param id the unique id of the user
   * @param version the latest version of the user for O.O.L.
   * @param email optionally update the e-mail
   * @param username optionally update the username
   * @param givenname the user's updated given name
   * @param surname the user's updated family name
   * @return a future disjunction containing the updated user, or a failure
   */
  def update(
    id: UUID,
    version: Long,
    email: Option[String],
    username: Option[String],
    givenname: Option[String],
    surname: Option[String],
    alias: Option[String],
    password: Option[String],
    isDeleted: Option[Boolean]
  ): Future[\/[ErrorUnion#Fail, User]]

  /**
   * Update a user's identifiers.
   *
   * @param id the unique id of the user
   * @param version the latest version of the user for O.O.L.
   * @param email optionally update the e-mail
   * @param username optionally update the username
   * @return a future disjunction containing the updated user, or a failure
   */
  def updateIdentifier(id: UUID, version: Long, email: Option[String], username: Option[String]): Future[\/[ErrorUnion#Fail, User]]

  /**
   * Update the user's password.
   *
   * @param id the unique id of the user to be updated
   * @param version the latest version of the user for O.O.L.
   * @param password the new password
   * @return a future disjunction containing the updated user, or a failure
   */
  def updatePassword(id: UUID, version: Long, password: String): Future[\/[ErrorUnion#Fail, User]]

  /**
   * Update a user's "non-identifying" information.
   *
   * @param id the unique id of the user to be updated
   * @param version the latest version of the user for O.O.L.
   * @param givenname the user's updated given name
   * @param surname the user's updated family name
   * @return a future disjunction containing the updated user, or a failure
   */
  def updateInfo(id: UUID, version: Long, givenname: Option[String], surname: Option[String], alias: Option[String]): Future[\/[ErrorUnion#Fail, User]]

  /**
   * Deletes a user.
   *
   * TODO: delete the user's work
   *
   * @param id the unique id of the user to be updated
   * @param version the latest version of the user for O.O.L.
   * @return a future disjunction containing the deleted user, or a failure
   */
  def delete(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, User]]

  /**
   * List all roles.
   *
   * @return an array of Roles
   */
  def listRoles: Future[\/[ErrorUnion#Fail, IndexedSeq[Role]]]

  /**
   * List all roles for one user.
   *
   * @param userId  The user whose roles should be listed.
   * @return an array of this user's Roles
   */
  def listRoles(userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Role]]]

  /**
   * Find a specific role by its unique id.
   *
   * @param id  the UUID of the Role to find
   * @return an optional Role
   */
  def findRole(id: UUID): Future[\/[ErrorUnion#Fail, Role]]

  /**
   * Find a specific role by name
   *
   * @param name  the name of the Role to find
   * @return an optional Role
   */
  def findRole(name: String): Future[\/[ErrorUnion#Fail, Role]]

  /**
   * Create a new role.
   *
   * @param name  the name of the Role to create
   * @return the newly created Role
   */
  def createRole(name: String, id: UUID = UUID.randomUUID): Future[\/[ErrorUnion#Fail, Role]]

  /**
   * Update a Role
   *
   * @param id  the unique id of the Role
   * @param version  the version of the Role for optimistic offline lock
   * @param name  the new name to assign this Role
   * @return the newly updated Role
   */
  def updateRole(id: UUID, version: Long, name: String): Future[\/[ErrorUnion#Fail, Role]]

  /**
   *  Delete a role.
   *
   *  @param id  the unique id of the role
   *  @param version  the version of the role for optimistic offline lock
   *  @return the deleted role
   */
  def deleteRole(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, Role]]

  /**
   * Add a role to a user.
   *
   * @param userId  the unique id of the user
   * @param roleName  the name of the role
   * @return a boolean indicator if the role was added
   */
  def addRole(userId: UUID, roleName: String): Future[\/[ErrorUnion#Fail, Role]]

  /**
   * Add several roles to a user.
   *
   * @param userId
   * @param roleNames
   * @return
   */
  def addRoles(userId: UUID, roleNames: IndexedSeq[String]): Future[\/[ErrorUnion#Fail, User]]

  /**
   * Remove a role from a user.
   *
   * @param userId  the unique id of the user
   * @param roleName  the name of the role
   * @return a boolean indicator if the role was removed
   */
  def removeRole(userId: UUID, roleName: String): Future[\/[ErrorUnion#Fail, Role]]

  /**
   * Add a role to a given list of users.
   *
   * @param roleId the UUID of the Role to be added
   * @param userIds an IndexedSeq of UUID listing the users to gain the role
   * @return a boolean indicator if the role was added
   */
  def addUsers(roleId: UUID, userIds: IndexedSeq[UUID]): Future[\/[ErrorUnion#Fail, Unit]]

  /**
   * Remove a role from a given list of users.
   *
   * @param roleId the UUID of the Role to be removed
   * @param userIds an IndexedSeq of UUID listing the users to lose the role
   * @return a boolean indicator if the role was removed
   */
  def removeUsers(roleId: UUID, userIds: IndexedSeq[UUID]): Future[\/[ErrorUnion#Fail, Unit]]

  /**
   * Activates a new user.
   * @param userId the UUID of the user to be activated
   * @param activationCode the activation code to be verified
   * @return
   */
  def activate(userId: UUID, activationCode: String): Future[\/[ErrorUnion#Fail, User]]

  /**
   * create a password reset token for a user
   * @param user
   * @return
   */
  def createPasswordResetToken(user: User, host: String)(messages: MessagesApi, lang: Lang): Future[\/[ErrorUnion#Fail, UserToken]]

  def createActivationToken(user: User, host: String)(messagesApi: MessagesApi, lang: Lang): Future[\/[ErrorUnion#Fail, UserToken]]

  /**
   * finding a user token by nonce
   * @param nonce
   * @param tokenType
   * @return
   */
  def findUserToken(nonce: String, tokenType: String): Future[\/[ErrorUnion#Fail, UserToken]]

  /**
   * find token by user id and type
   */
  def findToken(userId: UUID, tokenType: String): Future[\/[ErrorUnion#Fail, UserToken]]
  /**
   * destroy user token
   * @param token
   * @return
   */
  def deleteToken(token: UserToken): Future[\/[ErrorUnion#Fail, UserToken]]

  /**
   * create password reset link for students
   */
  def studentPasswordReset(user: User, lang: String): Future[\/[ErrorUnion#Fail, UserToken]]

  /**
   * redeem password reset token for students
   */
  def redeemStudentPasswordReset(token: UserToken): Future[\/[ErrorUnion#Fail, User]]

  def reactivate(email: String, hostname: Option[String])(messagesApi: MessagesApi, lang: Lang): Future[\/[ErrorUnion#Fail, UserToken]]

  //##### EMAIL CHANGE #################################################################################################

  def findEmailChange(userId: UUID): Future[\/[ErrorUnion#Fail, EmailChangeRequest]]

  def requestEmailChange(user: User, newEmail: String, host: String)(messagesApi: MessagesApi, lang: Lang): Future[\/[ErrorUnion#Fail, EmailChangeRequest]]

  /**
   * Confirm a user's changed e-mail address.
   *
   * Workflow:
   *   1. Load the change request and validate the token.
   *   2. Load the user to be changed.
   *   3. Save the user's new e-mail address.
   *   4. Send an e-mail to the old address, informing them that the address was changed.
   *   5. Send an e-mail to the new address, informing them that the address was changed.
   *
   * @param email the new e-mail that was requested
   * @param token the secure token that was generated to protect the change request
   * @return the updated user
   */
  def confirmEmailChange(email: String, token: String)(messagesApi: MessagesApi, lang: Lang): Future[\/[ErrorUnion#Fail, User]]

  /**
   * Cancel a user's e-mail change request.
   *
   * Workflow:
   *   1. Delete the e-mail change request.
   *   2. Send an e-mail to the old and new addresses notifying them that the request was cancelled.
   *
   * @param userId the e-mail address that was requested
   * @return the deleted e-mail change request
   */
  def cancelEmailChange(userId: UUID)(messagesApi: MessagesApi, lang: Lang): Future[\/[ErrorUnion#Fail, EmailChangeRequest]]
}
