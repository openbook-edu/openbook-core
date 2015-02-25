package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.fail._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services.datasource._
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import play.api.i18n.Messages
import scala.concurrent.Future
import scalaz.{-\/, \/-, \/, EitherT}
import webcrank.password._

/**
 * This component provides the default implementation of AuthService.
 *
 * This implementation can easily be replaced by another. The choice of which to
 * use is made in the controller via dependency injection.
 */
trait AuthServiceImplComponent extends AuthServiceComponent {
  // The self-type annotation defines the prerequisites that this implementation
  // requires, such as a repository and a database.
  self: UserRepositoryComponent with
        RoleRepositoryComponent with
        SessionRepositoryComponent with
        SchoolServiceComponent with
        DB =>

  /**
   * Instantiates the authService value with the class implementation.
   */
  override val authService: AuthService = new AuthServiceConcrete

  /**
   * The actual implementation of the AuthService.
   *
   * These methods should accept an implicit Connection object such that they
   * can be chained within a database transaction. They will generally call
   * methods from the userRepository included via the self-type.
   */
  private class AuthServiceConcrete extends AuthService {

    /**
     * List all users.
     *
     * @return an [[IndexedSeq]] of [[UserInfo]]
     */
    override def list: Future[\/[Fail, IndexedSeq[UserInfo]]] = {
      (for {
        users <- lift(userRepository.list)
        result <- liftSeq { users.map { user =>
          val fRoles = roleRepository.list(user)
          val fCourses = schoolService.listCoursesByUser(user.id)
          (for {
            roles <- lift(fRoles)
            courses <- lift(fCourses)
          } yield UserInfo(user, roles, courses)).run
        }}
      } yield result).run
    }

    /**
     * List users with filter for roles and courses.
     *
     * @param rolesFilter an optional list of roles to filter by
     * @param coursesFilter an optional list of courses to filter by
     * @return an [[IndexedSeq]] of [[UserInfo]]
     */
    override def list(rolesFilter: Option[IndexedSeq[String]],
                      coursesFilter: Option[IndexedSeq[UUID]]): Future[\/[Fail, IndexedSeq[UserInfo]]] = {
      (for {
        users <- lift(list)
        result = users.filter { userInfo =>
          rolesFilter.map { roles => userInfo.roles.map(_.name).intersect(roles).nonEmpty }.getOrElse(true) &&
            coursesFilter.map { courses => userInfo.courses.intersect(courses).nonEmpty }.getOrElse(true)
        }
      } yield result).run
    }

    /**
     * Authenticates a given identifier/password combination.
     *
     * @param identifier
     * @param password
     * @return Some(user) if valid, otherwise None.
     */
    override def authenticate(identifier: String, password: String): Future[\/[Fail, User]] = {
      transactional { implicit conn =>
        (for {
          user <- lift(userRepository.find(identifier.trim))
          userHash = user.passwordHash.getOrElse("")
          authUser <- lift(Future.successful {
            if (Passwords.scrypt().verify(password.trim(), userHash)) {
              \/-(user)
            }
            else {
              -\/(AuthFail("The password was invalid."))
            }
          })
        } yield authUser).run
      }
    }

    /**
     * List the active sessions for one user.
     *
     * @param userId
     * @return
     */
    override def listSessions(userId: UUID): Future[\/[Fail, IndexedSeq[Session]]] = {
      sessionRepository.list(userId)
    }

    /**
     * Find one session by its ID.
     *
     * @param sessionId
     * @return
     */
    override def findSession(sessionId: UUID): Future[\/[Fail, Session]] = {
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
    override def createSession(userId: UUID, ipAddress: String, userAgent: String): Future[\/[Fail, Session]] = {
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
    override def updateSession(sessionId: UUID, ipAddress: String, userAgent: String): Future[\/[Fail, Session]] = {
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
    override def deleteSession(sessionId: UUID): Future[\/[Fail, Session]] = {
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
    override def find(id: UUID): Future[\/[Fail, UserInfo]] = {
      (for {
        user <- lift(userRepository.find(id))
        fRoles = roleRepository.list(user)
        fCourses = schoolService.listCoursesByUser(user.id)
        roles <- lift(fRoles)
        courses <- lift(fCourses)
      } yield UserInfo(user, roles, courses)).run
    }

    /**
     * Find a user by their unique identifier.
     *
     * @param identifier  The unique e-mail or username identifying this user.
     * @return a future disjunction containing the user and their information, or a failure
     */
    override def find(identifier: String): Future[\/[Fail, UserInfo]] = {
      (for {
        user <- lift(userRepository.find(identifier))
        fRoles = roleRepository.list(user)
        fCourses = schoolService.listCoursesByUser(user.id)
        roles <- lift(fRoles)
        courses <- lift(fCourses)
      } yield UserInfo(user, roles, courses)).run
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
    override def create(username: String, email: String, password: String, givenname: String, surname: String, id: UUID = UUID.random): Future[\/[Fail, UserInfo]] = {
      transactional { implicit conn =>
        // Before we do anything, we need to verify that the username and email are
        // unique. Throw a temper tantrum if they aren't.
        val fValidEmail = validateEmail(email)
        val fValidUsername = validateUsername(username)
        val webcrank = Passwords.scrypt()

        (for {
          validEmail <- lift(fValidEmail)
          validUsername <- lift(fValidUsername)
          validPassword <- lift(isValidPassword(password))
          passwordHash = Some(webcrank.crypt(password))
          newUser <- lift {
            val newUser = User(
              id = id,
              username = username,
              email = email,
              passwordHash = passwordHash,
              givenname = givenname,
              surname = surname
            )
            userRepository.insert(newUser)
          }
        }
        yield UserInfo(newUser, IndexedSeq(), IndexedSeq())).run
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
    override def updateIdentifier(id: UUID, version: Long, email: Option[String] = None, username: Option[String] = None): Future[\/[Fail, User]] = {
      transactional { implicit conn =>
        val updated = for {
          existingUser <- lift(userRepository.find(id))
          _ <- predicate (existingUser.version == version) (LockFail(Messages("services.AuthService.userUpdate.lockFail", version, existingUser.version)))
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
    override def updateInfo(id: UUID, version: Long, givenname: Option[String] = None, surname: Option[String] = None): Future[\/[Fail, User]] = {
      transactional { implicit conn =>
        val updated = for {
          existingUser <- lift(userRepository.find(id))
          userToUpdate = existingUser.copy(
            version = version,
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
    override def updatePassword(id: UUID, version: Long, password: String): Future[\/[Fail, User]] = {
      transactional { implicit conn =>
        val wc = Passwords.scrypt()
        val updated = for {
          existingUser <- lift(userRepository.find(id))
          u_password <- lift(isValidPassword(password))
          u_hash = wc.crypt(u_password)
          userToUpdate = existingUser.copy(
            version = version,
            passwordHash = Some(u_hash)
          )
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
    override def delete(id: UUID, version: Long): Future[\/[Fail, User]] = {
      transactional { implicit connection =>
        (for {
          user <- lift(userRepository.find(id))
          toDelete = user.copy(version = version)
          deleted <- lift(userRepository.delete(toDelete))
        } yield deleted).run
      }
    }

    /**
     * List all roles.
     *
     * @return an array of Roles
     */
    override def listRoles: Future[\/[Fail, IndexedSeq[Role]]] = {
      roleRepository.list
    }

    /**
     * List all roles for one user.
     *
     * @param userId  The user whose roles should be listed.
     * @return an array of this user's Roles
     */
    override def listRoles(userId: UUID): Future[\/[Fail, IndexedSeq[Role]]] = {
      val result = for {
        user <- lift[User](userRepository.find(userId))
        roles <- lift[IndexedSeq[Role]](roleRepository.list(user))
      } yield roles

      result.run
    }

    /**
     * Find a specific role by its unique id.
     *
     * @param id  the UUID of the Role to find
     * @return an optional Role
     */
    override def findRole(id: UUID): Future[\/[Fail, Role]] = {
      roleRepository.find(id)
    }

    /**
     * Find a specific role by name
     *
     * @param name  the name of the Role to find
     * @return an optional Role
     */
    override def findRole(name: String): Future[\/[Fail, Role]] = {
      roleRepository.find(name)
    }

    /**
     * Create a new role.
     *
     * @param name  the name of the Role to create
     * @return the newly created Role
     */
    override def createRole(name: String, id: UUID = UUID.random): Future[\/[Fail, Role]] = {
      val newRole = Role(name = name, id = id)
      roleRepository.insert(newRole)(db.pool)
    }

    /**
     * Update a Role
     *
     * @param id  the unique id of the Role
     * @param version  the version of the Role for optimistic offline lock
     * @param name  the new name to assign this Role
     * @return the newly updated Role
     */
    override def updateRole(id: UUID, version: Long, name: String): Future[\/[Fail, Role]] = {
      transactional { implicit conn =>
        val result = for {
          existingRole <- lift(roleRepository.find(id))
          updatedRole <- lift(roleRepository.update(existingRole.copy(
            version = version,
            name = name
          )))
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
    override def deleteRole(id: UUID, version: Long): Future[\/[Fail, Role]] = {
      transactional { implicit conn =>
        (for {
          role <- lift(roleRepository.find(id).map {
            case \/-(role) =>
              if (role.version != version)
                -\/(BadInput("Incorrect version, role out of date."))
              else
                \/-(role)
            case -\/(error) => -\/(error)
          })
          wasRemovedFromUsers <- lift(roleRepository.removeFromAllUsers(role))
          wasDeleted <- lift(roleRepository.delete(role))
        }
        yield role).run
      }
    }

    /**
     * Add a role to a user.
     *
     * @param userId  the unique id of the user
     * @param roleName  the name of the role
     * @return a boolean indicator if the role was added
     */
    override def addRole(userId: UUID, roleName: String): Future[\/[Fail, UserInfo]] = {
      transactional { implicit conn =>
        val fUser = userRepository.find(userId)
        val fRole = roleRepository.find(roleName)
        (for {
          user <- lift(fUser)
          role <- lift(fRole)
          roleAdded <- lift(roleRepository.addToUser(user, role))
          userInfo <- lift(find(userId))
        }
        yield userInfo).run
      }
    }


    /**
     * Remove a role from a user.
     *
     * @param userId  the unique id of the user
     * @param roleName  the name of the role
     * @return a boolean indicator if the role was removed
     */
    override def removeRole(userId: UUID, roleName: String): Future[\/[Fail, UserInfo]] = {
      transactional { implicit conn =>
        val fUser = userRepository.find(userId)
        val fRole = roleRepository.find(roleName)
        (for {
          user <- lift(fUser)
          role <- lift(fRole)
          roleRemoved <- lift(roleRepository.removeFromUser(user, role))
          userInfo <- lift(find(userId))
        }
        yield userInfo).run
      }
    }

    /**
     * Add a role to a given list of users.
     *
     * @param roleId the [[UUID]] of the [[Role]] to be added
     * @param userIds an [[IndexedSeq]] of [[UUID]] listing the users to gain the role
     * @return a boolean indicator if the role was added
     */
    override def addUsers(roleId: UUID, userIds: IndexedSeq[UUID]): Future[\/[Fail, Role]] = {
      transactional { implicit conn =>
        val fRole = roleRepository.find(roleId)
        val fUsers = userRepository.list(userIds)

        (for {
          role <- lift(fRole)
          userList <- lift(fUsers)
          addedUsers <- lift(roleRepository.addUsers(role, userList))
        }
        yield addedUsers).run
      }
    }

    /**
     * Remove a role from a given list of users.
     *
     * @param roleId the [[UUID]] of the [[Role]] to be removed
     * @param userIds an [[IndexedSeq]] of [[UUID]] listing the users to lose the role
     * @return a boolean indicator if the role was removed
     */
    override def removeUsers(roleId: UUID, userIds: IndexedSeq[UUID]): Future[\/[Fail, Role]] = {
      transactional { implicit conn =>
        val fRole = roleRepository.find(roleId)
        val fUsers = userRepository.list(userIds)

        (for {
          role <- lift(fRole)
          userList <- lift(fUsers)
          addedUsers <- lift(roleRepository.removeUsers(role, userList))
        }
        yield addedUsers).run
      }
    }

    // ---------- private utility methods ----------

    /**
     * Validate e-mail address.
     *
     * @param email
     * @return
     */
    private def isValidEmail(email: String): Future[\/[Fail, String]] = Future.successful {
      if ("""(\w+)@([\w\.]+)""".r.unapplySeq(email).isDefined) \/-(email)
      else -\/(BadInput(s"$email is not a valid e-mail format."))
    }

    /**
     * Validate username.
     *
     * @param username
     * @return
     */
    private def isValidUsername(username: String): Future[\/[Fail, String]] = Future.successful {
      if (username.length >= 3) \/-(username)
      else -\/(BadInput(s"$username is not a valid format."))
    }

    /**
     * Validate password.
     *
     * @param password
     * @return
     */
    private def isValidPassword(password: String): Future[\/[Fail, String]] = Future.successful {
      if (password.length >= 8) \/-(password)
      else -\/(BadInput(s"The password provided must be at least 8 characters."))
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
    private def validateEmail(email: String, existingId: Option[UUID] = None): Future[\/[Fail, String]] = {
      val existing = for {
        validEmail <- lift(isValidEmail(email))
        existingUser <- lift(userRepository.find(validEmail))
      } yield existingUser

      existing.run.map {
        case \/-(user) =>
          if (existingId.isEmpty || (existingId.get != user.id)) -\/(EntityUniqueFieldError(s"The e-mail address $email is already in use."))
          else \/-(email)
        case -\/(error: NoResults) => \/-(email)
        case -\/(otherErrors: Fail) => -\/(otherErrors)
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
    private def validateUsername(username: String, existingId: Option[UUID] = None): Future[\/[Fail, String]] = {
      val existing = for {
        validUsername <- lift(isValidUsername(username))
        existingUser <- lift(userRepository.find(validUsername))
      } yield existingUser

      existing.run.map {
        case \/-(user) =>
          if (existingId.isEmpty || (existingId.get != user.id)) -\/(EntityUniqueFieldError(s"The username $username is already in use."))
          else \/-(username)
        case -\/(error: NoResults) => \/-(username)
        case -\/(otherErrors: Fail) => -\/(otherErrors)
      }
    }
  }
}
