package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services.datasource._
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.Connection
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.i18n.Messages
import scala.concurrent.Future
import scalacache.ScalaCache
import scalaz.{-\/, \/-, \/, EitherT}
import webcrank.password._

class AuthServiceDefault(val db: DB,
                         val scalaCache: ScalaCache,
                         val userRepository: UserRepository,
                         val roleRepository: RoleRepository,
                         val sessionRepository: SessionRepository)
  extends AuthService {

  implicit def conn: Connection = db.pool
  implicit def cache: ScalaCache = scalaCache
  
  /**
   * List all users.
   *
   * @return an IndexedSeq of user
   */
  override def list: Future[\/[ErrorUnion#Fail, IndexedSeq[User]]] = {
    for {
      users <- lift(userRepository.list(db.pool))
      result <- liftSeq { users.map { user =>
        val fRoles = roleRepository.list(user)(db.pool, cache)
        (for {
          roles <- lift(fRoles)
        } yield user.copy(roles = roles)).run
      }}
    } yield result
  }

  /**
   * List users with filter for roles and courses.
   *
   * @param roles an optional list of roles to filter by
   * @return an IndexedSeq of user
   */
  override def list(roles: IndexedSeq[String]): Future[\/[ErrorUnion#Fail, IndexedSeq[User]]] = {
    for {
      users <- lift(list)
      result = users.filter { user =>
        user.roles.map(_.name).intersect(roles).nonEmpty
      }
    } yield result
  }

  /**
   * Authenticates a given identifier/password combination.
   *
   * @param identifier
   * @param password
   * @return Some(user) if valid, otherwise None.
   */
  override def authenticate(identifier: String, password: String): Future[\/[ErrorUnion#Fail, User]] = {
    transactional { implicit conn =>
      for {
        user <- lift(userRepository.find(identifier.trim))
        roles <- lift(roleRepository.list(user))
        userHash = user.hash.getOrElse("")
        authUser <- lift(Future.successful {
          if (Passwords.scrypt().verify(password.trim(), userHash)) {
            \/-(user.copy(roles = roles))
          }
          else {
            -\/(ServiceError.BadPermissions("The password was invalid."))
          }
        })
      } yield authUser
    }
  }

  /**
   * List the active sessions for one user.
   *
   * @param userId
   * @return
   */
  override def listSessions(userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Session]]] = {
    sessionRepository.list(userId)
  }

  /**
   * Find one session by its ID.
   *
   * @param sessionId
   * @return
   */
  override def findSession(sessionId: UUID): Future[\/[ErrorUnion#Fail, Session]] = {
    sessionRepository.find(sessionId)
  }

  /**
   * Create a new session.
   *
   * @param userId
   * @param ipAddress
   * @param userAgent
   * @return
   */
  override def createSession(userId: UUID, ipAddress: String, userAgent: String): Future[\/[ErrorUnion#Fail, Session]] = {
    sessionRepository.create(Session(
      userId = userId,
      ipAddress = ipAddress,
      userAgent = userAgent
    ))
  }

  /**
   * Update an existing session.
   *
   * @param sessionId
   * @param ipAddress
   * @param userAgent
   * @return
   */
  override def updateSession(sessionId: UUID, ipAddress: String, userAgent: String): Future[\/[ErrorUnion#Fail, Session]] = {
    val fUpdated = for {
      session <- lift(sessionRepository.find(sessionId))
      updated <- lift(sessionRepository.update(session.copy(
        ipAddress = ipAddress,
        userAgent = userAgent
      )))
    } yield updated

    fUpdated.run
  }

  /**
   * Delete a session.
   *
   * @param sessionId
   * @return
   */
  override def deleteSession(sessionId: UUID): Future[\/[ErrorUnion#Fail, Session]] = {
    val fDeleted = for {
      session <- lift(sessionRepository.find(sessionId))
      deleted <- lift(sessionRepository.delete(session))
    } yield deleted

    fDeleted.run
  }

  /**
   * Find a user by their UUID.
   *
   * @param id the unique id for the user
   * @return a future disjunction containing the user and their information, or a failure
   */
  override def find(id: UUID): Future[\/[ErrorUnion#Fail, User]] = {
    for {
      user <- lift(userRepository.find(id))
      fRoles = roleRepository.list(user)
      roles <- lift(fRoles)
    } yield user.copy(roles = roles)
  }

  /**
   * Find a user by their unique identifier.
   *
   * @param identifier  The unique e-mail or username identifying this user.
   * @return a future disjunction containing the user and their information, or a failure
   */
  override def find(identifier: String): Future[\/[ErrorUnion#Fail, User]] = {
    for {
      user <- lift(userRepository.find(identifier))
      fRoles = roleRepository.list(user)
      roles <- lift(fRoles)
    } yield user.copy(roles = roles)
  }

  /**
   * Create a new user. Throws exceptions if the e-mail and username aren't unique.
   *
   * @param username  A unique identifier for this user.
   * @param email  The user's unique e-mail address.
   * @param password  The user's password.
   * @param givenname  The user's first name.
   * @param surname  The user's family name.
   * @param id The ID to allocate for this user, if left out, it will be random.
   * @return the created user
   */
  override def create(username: String, email: String, password: String, givenname: String, surname: String, id: UUID = UUID.random): Future[\/[ErrorUnion#Fail, User]] = {
    transactional { implicit conn =>
      val webcrank = Passwords.scrypt()

      for {
        validEmail <- lift(validateEmail(email))
        validUsername <- lift(validateUsername(username))
        validPassword <- lift(isValidPassword(password))
        passwordHash = Some(webcrank.crypt(password))
        newUser <- lift {
          val newUser = User(
            id = id,
            username = username,
            email = email,
            hash = passwordHash,
            givenname = givenname,
            surname = surname
          )
          userRepository.insert(newUser)
        }
      }
      yield newUser
    }
  }

  /**
   * Update a user's identifiers.
   *
   * @param id the unique id of the user
   * @param version the latest version of the user for O.O.L.
   * @param email optionally update the e-mail
   * @param username optionally update the username
   * @return a future disjunction containing the updated user, or a failure
   */
  override def update(id: UUID, version: Long, email: Option[String], username: Option[String], givenname: Option[String], surname: Option[String]): Future[\/[ErrorUnion#Fail, User]] = {
    transactional { implicit conn =>
      val updated = for {
        existingUser <- lift(userRepository.find(id))
        _ <- predicate (existingUser.version == version) (RepositoryError.OfflineLockFail)
        u_email <- lift(email.map { someEmail => validateEmail(someEmail, Some(id))}.getOrElse(Future.successful(\/-(existingUser.email))))
        u_username <- lift(username.map { someUsername => validateUsername(someUsername, Some(id))}.getOrElse(Future.successful(\/-(existingUser.username))))
        userToUpdate = existingUser.copy(
          email = u_email,
          username = u_username,
          givenname = givenname.getOrElse(existingUser.givenname),
          surname = surname.getOrElse(existingUser.surname)
        )
        updatedUser <- lift(userRepository.update(userToUpdate))
      } yield existingUser
      updated.run
    }
  }


  /**
   * Update a user's identifiers.
   *
   * @param id the unique id of the user
   * @param version the latest version of the user for O.O.L.
   * @param email optionally update the e-mail
   * @param username optionally update the username
   * @return a future disjunction containing the updated user, or a failure
   */
  override def updateIdentifier(id: UUID, version: Long, email: Option[String] = None, username: Option[String] = None): Future[\/[ErrorUnion#Fail, User]] = {
    transactional { implicit conn =>
      val updated = for {
        existingUser <- lift(userRepository.find(id))
        _ <- predicate (existingUser.version == version) (RepositoryError.OfflineLockFail)
        u_email <- lift(email.map { someEmail => validateEmail(someEmail, Some(id))}.getOrElse(Future.successful(\/-(existingUser.email))))
        u_username <- lift(username.map { someUsername => validateUsername(someUsername, Some(id))}.getOrElse(Future.successful(\/-(existingUser.username))))
        userToUpdate = existingUser.copy(
          email = u_email,
          username = u_username
        )
        updatedUser <- lift(userRepository.update(userToUpdate))
      } yield existingUser
      updated.run
    }
  }

  /**
   * Update a user's "non-identifying" information.
   *
   * @param id the unique id of the user to be updated
   * @param version the latest version of the user for O.O.L.
   * @param givenname the user's updated given name
   * @param surname the user's updated family name
   * @return a future disjunction containing the updated user, or a failure
   */
  override def updateInfo(id: UUID, version: Long, givenname: Option[String] = None, surname: Option[String] = None): Future[\/[ErrorUnion#Fail, User]] = {
    transactional { implicit conn =>
      val updated = for {
        existingUser <- lift(userRepository.find(id))
        _ <- predicate (existingUser.version == version) (RepositoryError.OfflineLockFail)
        userToUpdate = existingUser.copy(
          givenname = givenname.getOrElse(existingUser.givenname),
          surname = surname.getOrElse(existingUser.surname)
        )
        updatedUser <- lift(userRepository.update(userToUpdate))
      } yield existingUser
      updated.run
    }
  }

  /**
   * Update the user's password.
   *
   * @param id the unique id of the user to be updated
   * @param version the latest version of the user for O.O.L.
   * @param password the new password
   * @return a future disjunction containing the updated user, or a failure
   */
  override def updatePassword(id: UUID, version: Long, password: String): Future[\/[ErrorUnion#Fail, User]] = {
    transactional { implicit conn =>
      val wc = Passwords.scrypt()
      val updated = for {
        existingUser <- lift(userRepository.find(id))
        _ <- predicate (existingUser.version == version) (RepositoryError.OfflineLockFail)
        u_password <- lift(isValidPassword(password))
        u_hash = wc.crypt(u_password)
        userToUpdate = existingUser.copy(hash = Some(u_hash))
        updatedUser <- lift(userRepository.update(userToUpdate))
      } yield existingUser
      updated.run
    }
  }

  /**
   * Deletes a user.
   *
   * TODO: delete the user's work
   *
   * @param id the unique id of the user to be updated
   * @param version the latest version of the user for O.O.L.
   * @return a future disjunction containing the deleted user, or a failure
   */
  override def delete(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, User]] = {
    transactional { implicit conn =>
      for {
        existingUser <- lift(userRepository.find(id))
        _ <- predicate (existingUser.version == version) (RepositoryError.OfflineLockFail)
        toDelete = existingUser.copy(version = version)
        deleted <- lift(userRepository.delete(toDelete))
      } yield deleted
    }
  }

  /**
   * List all roles.
   *
   * @return an array of Roles
   */
  override def listRoles: Future[\/[ErrorUnion#Fail, IndexedSeq[Role]]] = {
    roleRepository.list
  }

  /**
   * List all roles for one user.
   *
   * @param userId  The user whose roles should be listed.
   * @return an array of this user's Roles
   */
  override def listRoles(userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Role]]] = {
    val result = for {
      user <- lift(userRepository.find(userId))
      roles <- lift(roleRepository.list(user))
    } yield roles

    result.run
  }

  /**
   * Find a specific role by its unique id.
   *
   * @param id  the UUID of the Role to find
   * @return an optional Role
   */
  override def findRole(id: UUID): Future[\/[ErrorUnion#Fail, Role]] = {
    roleRepository.find(id)
  }

  /**
   * Find a specific role by name
   *
   * @param name  the name of the Role to find
   * @return an optional Role
   */
  override def findRole(name: String): Future[\/[ErrorUnion#Fail, Role]] = {
    roleRepository.find(name)
  }

  /**
   * Create a new role.
   *
   * @param name  the name of the Role to create
   * @return the newly created Role
   */
  override def createRole(name: String, id: UUID = UUID.random): Future[\/[ErrorUnion#Fail, Role]] = {
    val newRole = Role(name = name, id = id)
    roleRepository.insert(newRole)
  }

  /**
   * Update a Role
   *
   * @param id  the unique id of the Role
   * @param version  the version of the Role for optimistic offline lock
   * @param name  the new name to assign this Role
   * @return the newly updated Role
   */
  override def updateRole(id: UUID, version: Long, name: String): Future[\/[ErrorUnion#Fail, Role]] = {
    transactional { implicit conn =>
      val result = for {
        existingRole <- lift(roleRepository.find(id))
        _ <- predicate (existingRole.version == version) (RepositoryError.OfflineLockFail)
        updatedRole <- lift(roleRepository.update(existingRole.copy(name = name)))
      }
      yield updatedRole
      result.run
    }
  }

  /**
   *  Delete a role.
   *
   *  @param id  the unique id of the role
   *  @param version  the version of the role for optimistic offline lock
   *  @return the deleted role
   */
  override def deleteRole(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, Role]] = {
    transactional { implicit conn =>
      for {
        role <- lift(roleRepository.find(id).map {
          case \/-(role) =>
            if (role.version != version)
              -\/(ServiceError.BadInput("Incorrect version, role out of date."))
            else
              \/-(role)
          case -\/(error) => -\/(error)
        })
        _ <- predicate (role.version == version) (RepositoryError.OfflineLockFail)
        wasRemovedFromUsers <- lift(roleRepository.removeFromAllUsers(role))
        wasDeleted <- lift(roleRepository.delete(role))
      }
      yield role
    }
  }

  /**
   * Add a role to a user.
   *
   * @param userId  the unique id of the user
   * @param roleName  the name of the role
   * @return a boolean indicator if the role was added
   */
  override def addRole(userId: UUID, roleName: String): Future[\/[ErrorUnion#Fail, User]] = {
    transactional { implicit conn =>
      val fUser = userRepository.find(userId)(db.pool, cache)
      val fRole = roleRepository.find(roleName)(db.pool, cache)

      for {
        user <- lift(fUser)
        role <- lift(fRole)
        roleAdded <- lift(roleRepository.addToUser(user, role))
        userInfo <- lift(find(userId))
      }
      yield userInfo
    }
  }


  /**
   * Add a role to a user.
   *
   * @param userId  the unique id of the user
   * @param roleNames  the name of the role
   * @return a boolean indicator if the role was added
   */
  override def addRoles(userId: UUID, roleNames: IndexedSeq[String]): Future[\/[ErrorUnion#Fail, User]] = {
    transactional { implicit conn =>
      for {
        user <- lift(userRepository.find(userId))
        rolesAdded <- lift(serializedT(roleNames)(roleRepository.addToUser(user, _)))
      } yield user
    }
  }

  /**
   * Remove a role from a user.
   *
   * @param userId  the unique id of the user
   * @param roleName  the name of the role
   * @return a boolean indicator if the role was removed
   */
  override def removeRole(userId: UUID, roleName: String): Future[\/[ErrorUnion#Fail, User]] = {
    transactional { implicit conn =>
      val fUser = userRepository.find(userId)(db.pool, cache)
      val fRole = roleRepository.find(roleName)(db.pool, cache)
      for {
        user <- lift(fUser)
        role <- lift(fRole)
        roleRemoved <- lift(roleRepository.removeFromUser(user, role))
        userInfo <- lift(find(userId))
      }
      yield userInfo
    }
  }

  /**
   * Add a role to a given list of users.
   *
   * @param roleId the UUID of the Role to be added
   * @param userIds an IndexedSeq of UUID listing the users to gain the role
   * @return a boolean indicator if the role was added
   */
  override def addUsers(roleId: UUID, userIds: IndexedSeq[UUID]): Future[\/[ErrorUnion#Fail, Unit]] = {
    transactional { implicit conn =>
      val fRole = roleRepository.find(roleId)(db.pool, cache)
      val fUsers = userRepository.list(userIds)(db.pool, cache)

      for {
        role <- lift(fRole)
        userList <- lift(fUsers)
        addedUsers <- lift(roleRepository.addUsers(role, userList))
      }
      yield addedUsers
    }
  }

  /**
   * Remove a role from a given list of users.
   *
   * @param roleId the UUID of the Role to be removed
   * @param userIds an IndexedSeq of UUID listing the users to lose the role
   * @return a boolean indicator if the role was removed
   */
  override def removeUsers(roleId: UUID, userIds: IndexedSeq[UUID]): Future[\/[ErrorUnion#Fail, Unit]] = {
    transactional { implicit conn =>
      val fRole = roleRepository.find(roleId)(db.pool, cache)
      val fUsers = userRepository.list(userIds)(db.pool, cache)

      for {
        role <- lift(fRole)
        userList <- lift(fUsers)
        removedUsers <- lift(roleRepository.removeUsers(role, userList))
      }
      yield removedUsers
    }
  }

  // ---------- private utility methods ----------

  /**
   * Validate e-mail address.
   *
   * @param email
   * @return
   */
  private def isValidEmail(email: String): Future[\/[ErrorUnion#Fail, String]] = Future.successful {
    val parts = email.split("@")
    if (parts.length != 2 ||
        !parts(0).charAt(0).isLetter ||
        !parts(1).charAt(parts(1).length-1).isLetter ||
        parts(1).indexOf("..") != -1 ||
        !"""([\w\._-]+)@([\w\._-]+)""".r.unapplySeq(email.trim).isDefined
    ) {
      \/.left(ServiceError.BadInput(s"'$email' is not a valid format"))
    } else {
      \/.right(email.trim)
    }
  }

  /**
   * Validate username.
   *
   * @param username
   * @return
   */
  private def isValidUsername(username: String): Future[\/[ErrorUnion#Fail, String]] = Future.successful {
    if (username.length >= 3) \/-(username)
    else -\/(ServiceError.BadInput(s"Your username must be at least 3 characters."))
  }

  /**
   * Validate password.
   *
   * @param password
   * @return
   */
  private def isValidPassword(password: String): Future[\/[ErrorUnion#Fail, String]] = Future.successful {
    if (password.length >= 3) \/-(password)
    else -\/(ServiceError.BadInput(s"The password provided must be at least 8 characters."))
  }

  /**
   * Validate whether a given identifier can be used. Checks its format, and then checks
   * whether it is in use by another user. For updates, an existingId can be passed in so that
   * a false positive isn't received for updating an existing user.
   *
   * @param email
   * @param existingId
   * @return
   */
  private def validateEmail(email: String, existingId: Option[UUID] = None)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, String]] = {
    val existing = for {
      validEmail <- lift(isValidEmail(email))
      existingUser <- lift(userRepository.find(validEmail))
    } yield existingUser

    existing.run.map {
      case \/-(user) =>
        if (existingId.isEmpty || (existingId.get != user.id)) -\/(RepositoryError.UniqueKeyConflict("email", s"The e-mail address $email is already in use."))
        else \/-(email)
      case -\/(RepositoryError.NoResults) => \/-(email)
      case -\/(otherErrors: ErrorUnion#Fail) => -\/(otherErrors)
    }
  }

  /**
   * Validate whether a given identifier can be used. Checks its format, and then checks
   * whether it is in use by another user. For updates, an existingId can be passed in so that
   * a false positive isn't received for updating an existing user.
   *
   * @param username
   * @param existingId
   * @return
   */
  private def validateUsername(username: String, existingId: Option[UUID] = None)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, String]] = {
    val existing = for {
      validUsername <- lift(isValidUsername(username))
      existingUser <- lift(userRepository.find(validUsername))
    } yield existingUser

    existing.run.map {
      case \/-(user) =>
        if (existingId.isEmpty || (existingId.get != user.id)) -\/(RepositoryError.UniqueKeyConflict("username", s"The username $username is already in use."))
        else \/-(username)
      case -\/(RepositoryError.NoResults) => \/-(username)
      case -\/(otherErrors: ErrorUnion#Fail) => -\/(otherErrors)
    }
  }
}
