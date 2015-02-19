package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.fail._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services.datasource._
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import play.api.Logger
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
        CourseRepositoryComponent with
        SessionRepositoryComponent with
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
        intermediate <- Future sequence users.map { user =>
          val fRoles = roleRepository.list(user)
          val fCourses = courseRepository.list(user)
          (for {
            roles <- lift(fRoles)
            courses <- lift(fCourses)
          } yield UserInfo(user, roles, courses)).run
        }
        result <- lift(Future.successful {
          if (intermediate.filter(_.isLeft).nonEmpty) -\/(intermediate.filter(_.isLeft).head.swap.toOption.get)
          else \/-(intermediate.map(_.toOption.get))
        })
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
      // First build a future returning a list of users
      val fUsers = (rolesFilter, coursesFilter) match {
        case (Some(roles), Some(courses)) => userRepository.listForRolesAndCourses(roles, courses.map(_.string))
        case (Some(roles), None) => userRepository.listForRoles(roles)
        case (None, Some(courseIds)) => {
          val users = Future.sequence(courseIds.map { courseId => courseRepository.find(courseId).map(_.get) }).flatMap {
            courses => userRepository.listForCourses(courses)
          }
          users
        }
        case (None, None) => {
          userRepository.list
        }
      }

      // Now fetch their roles and courses, and return the list
      (for {
        users <- lift(fUsers)
        fRoles = roleRepository.list(users)
        fCourses = courseRepository.list(users)

        roles <- lift(fRoles)
        courses <- lift(fCourses)

        userInfoList = users.map { user =>
          UserInfo(
            user,
            roles.getOrElse(user.id, IndexedSeq()),
            courses.getOrElse(user.id, IndexedSeq())
          )
        }
      } yield userInfoList).run
    }

    /**
     * Authenticates a given identifier/password combination.
     *
     * @param email
     * @param password
     * @return Some(user) if valid, otherwise None.
     */
    override def authenticate(identifier: String, password: String): Future[\/[Fail, User]] = {
      transactional { implicit conn =>
        (for {
          user <- lift(userRepository.find(identifier.trim))
          userHash = user.passwordHash.getOrElse("")
          authUser <- lift(Future.successful {
            if (Passwords.scrypt().verify(password.trim(), hash)) {
              \/-(user)
            }
            else {
              -\/(AuthFail("The password was invalid.")
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
        session <- lift(sessionRepository.find(sessionId).map(_.get))
        deleted <- lift(sessionRepository.delete(session))
      } yield deleted

      fDeleted.run
    }

    /**
     * Find a user by their UUID.
     *
     * @param id  The user's universally unique identifier.
     * @return if found, returns some UserInfo including their roles and courses.
     */
    override def find(id: UUID): Future[\/[Fail, UserInfo]] = {
      (for {
        user <- lift(userRepository.find(id))
        fRoles = roleRepository.list(user)
        fCourses = courseRepository.list(user)
        roles <- lift(fRoles)
        courses <- lift(fCourses)
      } yield UserInfo(user, roles, courses)).run
    }

    /**
     * Find a user by their unique identifier.
     *
     * @param identifier  The unique e-mail or username identifying this user.
     * @return if found, returns some UserInfo including their roles and courses.
     */
    override def find(identifier: String): Future[\/[Fail, UserInfo]] = {
      (for {
        user <- lift(userRepository.find(identifier))
        fRoles = roleRepository.list(user)
        fCourses = courseRepository.list(user)
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
    override def create(username: String, email: String, password: String, givenname: String, surname: String, id: UUID = UUID.random): Future[User] = {
      transactional { implicit conn =>
        // Before we do anything, we need to verify that the username and email are
        // unique. Throw a temper tantrum if they aren't.
        val fExistingEmail = userRepository.find(email)
        val fExistingUsername = userRepository.find(username)
        for {
          existingEmailOption <- fExistingEmail
          existingUsernameOption <- fExistingUsername
          newUser <- { (existingEmailOption, existingUsernameOption) match {
            case (Some(user), None) => {
              throw new EmailAlreadyExistsException(s"The e-mail ${user.email} has already been registered.")
            }
            case (None, Some(user)) => {
              throw new UsernameAlreadyExistsException(s"The username ${user.username} has already been registered.")
            }
            case (Some(userEmail), Some(userUsername)) => {
              throw new EmailAndUsernameAlreadyExistException(s"Both the username ${userEmail.username} and e-mail ${userUsername.email} have already been registered.")
            }
            case (None, None) => {
              val webcrank = Passwords.scrypt()
              val passwordHash = Some(webcrank.crypt(password))
              val newUser = User(
                id = id,
                username = username,
                email = email,
                passwordHash = passwordHash,
                givenname = givenname,
                surname = surname
              )
              val insert = userRepository.insert(newUser)
              println(insert.isCompleted)
              insert
            }
          }
        }}
        yield newUser
      }
    }

    /**
     * Update an existing user. Throws exceptions if the e-mail and username aren't unique.
     *
     * @param id  The unique ID of the user to be updated
     * @param version  The current version of the user
     * @param values  A hashmap of the values to be updated
     * @return the updated user
     */
    override def update(id: UUID, version: Long, values: Map[String, String]): Future[User] =
      transactional { implicit conn =>
        val webcrank = Passwords.scrypt()
        val updated = for {
          existingUserInfoOption: Option[UserInfo] <- find(id)
          existingUser: User <- Future.successful(existingUserInfoOption.get.user)
          conflictingEmail <- { values.get("email") match {
            case Some(email) => userRepository.find(email).map {
              case Some(conflictingUser) =>
                if (conflictingUser.id == existingUser.id) None
                else Some(conflictingUser)
              case None => None
            }
            case None => Future.successful(None)
          }}
          conflictingUsername <- { values.get("username") match {
            case Some(username) => userRepository.find(username).map {
              case Some(conflictingUser) =>
                if (conflictingUser.id == existingUser.id) None
                else Some(conflictingUser)
              case None => None
            }
            case None => Future.successful(None)
          }}
          userToUpdate: User <- Future.successful {
            (conflictingEmail, conflictingUsername) match {
              case (Some(user), None) => {
                throw new EmailAlreadyExistsException(s"The e-mail ${user.email} has already been registered.")
              }
              case (None, Some(user)) => {
                throw new UsernameAlreadyExistsException(s"The username ${user.username} has already been registered.")
              }
              case (Some(userEmail), Some(userUsername)) => {
                throw new EmailAndUsernameAlreadyExistException(s"Both the username ${userUsername.username} and e-mail ${userEmail.email} have already been registered.")
              }
              case _ => {}
            }

            // Create the user object that will be updated into the database, copying
            // data fields if they were provided.
            existingUser.copy(
              version = version,
              username = values.get("username") match {
                case Some(username) => username
                case None => existingUser.username
              },
              email = values.get("email") match {
                case Some(email) => email
                case None => existingUser.email
              },
              passwordHash = values.get("password") match {
                case Some(password) => Some(webcrank.crypt(password))
                case None => existingUser.passwordHash
              },
              givenname = values.get("givenname") match {
                case Some(givenname) => givenname
                case None => existingUser.givenname
              },
              surname = values.get("surname") match {
                case Some(surname) => surname
                case None => existingUser.surname
              }
            )
          }
          updatedUser <- userRepository.update(userToUpdate)
        } yield updatedUser
        updated.recover {
          case exception => {
            throw exception
          }
        }
      }

    /**
     * Deletes a user. This is a VERY DESTRUCTIVE operation.
     */
    override def delete(id: UUID, version: Long): Future[Boolean] = {
      transactional { implicit connection =>
        // delete component notes, task notes, task responses
        // remove roles, remove from courses
        // delete user
        Future.successful(true)
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
     * @param user  The user whose roles should be listed.
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
     * @param id  the name of the Role to find
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
    override def addRole(userId: UUID, roleName: String): Future[\/[Fail, Role]] = {
      transactional { implicit conn =>
        val fUser = userRepository.find(userId)
        val fRole = roleRepository.find(roleName)
        (for {
          user <- lift(fUser)
          role <- lift(fRole)
          roleAdded <- roleRepository.addToUser(user, role)
        }
        yield roleAdded).run
      }
    }


    /**
     * Remove a role from a user.
     *
     * @param userId  the unique id of the user
     * @param roleName  the name of the role
     * @return a boolean indicator if the role was removed
     */
    override def removeRole(userId: UUID, roleName: String): Future[\/[Fail, Role]] = {
      transactional { implicit conn =>
        val fUser = userRepository.find(userId)
        val fRole = roleRepository.find(roleName)
        (for {
          user <- lift(fUser)
          role <- lift(fRole)
          roleRemoved <- roleRepository.removeFromUser(user, role)
        }
        yield roleRemoved).run
      }
    }

    /**
     * Add a role to a given list of users.
     *
     * @param roleId the [[UUID]] of the [[Role]] to be added
     * @param userIds an [[IndexedSeq]] of [[UUID]] listing the users to gain the role
     * @return a boolean indicator if the role was added
     */
    override def addUsers(roleId: UUID, userIds: IndexedSeq[UUID]): Future[Boolean] = {
      for {
        role <- roleRepository.find(roleId).map(_.get)
        userList <- userRepository.list(userIds)
        addedUsers <- roleRepository.addUsers(role, userList)(db.pool)
      }
      yield addedUsers
    }.recover {
      case exception => throw exception
    }

    /**
     * Remove a role from a given list of users.
     *
     * @param roleId the [[UUID]] of the [[Role]] to be removed
     * @param userIds an [[IndexedSeq]] of [[UUID]] listing the users to lose the role
     * @return a boolean indicator if the role was removed
     */
    override def removeUsers(roleId: UUID, userIds: IndexedSeq[UUID]): Future[Boolean] = {
      for {
        role <- roleRepository.find(roleId).map(_.get)
        userList <- userRepository.list(userIds)
        addedUsers <- roleRepository.removeUsers(role, userList)(db.pool)
      }
      yield addedUsers
    }.recover {
      case exception => throw exception
    }
  }
}
