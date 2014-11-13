package ca.shiftfocus.krispii.core.services

import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services.datasource._
import ca.shiftfocus.krispii.core.lib.UUID
import play.api.Logger
import scala.concurrent.Future
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
        SectionRepositoryComponent with
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
    override def list: Future[IndexedSeq[UserInfo]] = {
      userRepository.list.flatMap { users =>
        Future.sequence(users.map { user =>
          val fRoles = roleRepository.list(user)
          val fSections = sectionRepository.list(user)

          for {
            roles <- fRoles
            sections <- fSections
          }
          yield UserInfo(user, roles, sections)
        })
      }
    }

    /**
     * List users with filter for roles and sections.
     *
     * @param rolesFilter an optional list of roles to filter by
     * @param sectionsFilter an optional list of sections to filter by
     * @return an [[IndexedSeq]] of [[UserInfo]]
     */
    override def list(rolesFilter: Option[IndexedSeq[String]],
                      sectionsFilter: Option[IndexedSeq[UUID]]): Future[IndexedSeq[UserInfo]] = {
      // First build a future returning a list of users
      val fUsers = (rolesFilter, sectionsFilter) match {
        case (Some(roles), Some(sections)) => userRepository.listForRolesAndSections(roles, sections.map(_.string))
        case (Some(roles), None) => userRepository.listForRoles(roles)
        case (None, Some(sectionIds)) => {
          val users = Future.sequence(sectionIds.map { sectionId => sectionRepository.find(sectionId).map(_.get) }).flatMap {
            sections => userRepository.listForSections(sections)
          }
          users
        }
        case (None, None) => {
          userRepository.list
        }
      }

      // Next find the roles and sections for those users.
      fUsers.flatMap { users =>
        val fRoles = roleRepository.list(users)
        val fSections = sectionRepository.list(users)

        for {
          roles <- fRoles
          sections <- fSections
        }
        yield {
          // Now pair each user with both their roles and sections
          val userInfoList = users.map { user =>
            UserInfo(
              user,
              roles.getOrElse(user.id, IndexedSeq()),
              sections.getOrElse(user.id, IndexedSeq())
            )
          }
          userInfoList
        }
      }.recover {
        case exception => throw exception
      }
    }

    /**
     * Authenticates a given identifier/password combination.
     *
     * @param email
     * @param password
     * @return Some(user) if valid, otherwise None.
     */
    override def authenticate(identifier: String, password: String): Future[Option[User]] = {
      transactional { implicit conn =>
        userRepository.find(identifier.trim().toLowerCase()).map {
          case Some(user) => {
            user.passwordHash match {
              case Some(hash) => {
                if (Passwords.scrypt().verify(password.trim(), hash)) {
                  Some(user)
                }
                else { None }
              }
              case None => None
            }
          }
          case None => None
        }
      }
    }

    /**
     * List the active sessions for one user.
     *
     * @param userId
     * @return
     */
    override def listSessions(userId: UUID): Future[IndexedSeq[Session]] = {
      sessionRepository.list(userId)
    }

    /**
     * Find one session by its ID.
     *
     * @param sessionId
     * @return
     */
    override def findSession(sessionId: UUID): Future[Option[Session]] = {
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
    override def createSession(userId: UUID, ipAddress: String, userAgent: String): Future[Session] = {
      sessionRepository.create(Session(
        userId = userId,
        ipAddress = ipAddress,
        userAgent = userAgent
      )).recover { case exception => throw exception }
    }

    /**
     * Update an existing session.
     *
     * @param sessionId
     * @param ipAddress
     * @param userAgent
     * @return
     */
    override def updateSession(sessionId: UUID, ipAddress: String, userAgent: String): Future[Session] = {
      val fUpdated = for {
        session <- sessionRepository.find(sessionId).map(_.get)
        updated <- sessionRepository.update(session.copy(
          ipAddress = ipAddress,
          userAgent = userAgent
        ))
      } yield updated

      fUpdated.recover { case exception => throw exception }
    }

    /**
     * Delete a session.
     *
     * @param sessionId
     * @return
     */
    override def deleteSession(sessionId: UUID): Future[Session] = {
      val fDeleted = for {
        session <- sessionRepository.find(sessionId).map(_.get)
        deleted <- sessionRepository.delete(session)
      } yield deleted

      fDeleted.recover { case exception => throw exception }
    }

    /**
     * Find a user by their UUID.
     *
     * @param id  The user's universally unique identifier.
     * @return if found, returns some UserInfo including their roles and sections.
     */
    override def find(id: UUID): Future[Option[UserInfo]] = {
      Logger.debug(s"authService.find(${id.string})")
      userRepository.find(id).flatMap {
        case Some(user) => {
          Logger.debug(s"authService.find(${id.string}) - found user")
          val fRoles = roleRepository.list(user)
          val fSections = sectionRepository.list(user)
          for {
            roles <- fRoles
            sections <- fSections
          } yield Some(UserInfo(user, roles, sections))
        }
        case None => {
          Logger.debug(s"authService.find(${id.string}) - not found")
          Future.successful(None)
        }
      }
    }

    /**
     * Find a user by their unique identifier.
     *
     * @param identifier  The unique e-mail or username identifying this user.
     * @return if found, returns some UserInfo including their roles and sections.
     */
    override def find(identifier: String): Future[Option[UserInfo]] = {
      userRepository.find(identifier).flatMap {
        case Some(user) => {
          val fRoles = roleRepository.list(user)
          val fSections = sectionRepository.list(user)
          for {
            roles <- fRoles
            sections <- fSections
          } yield Some(UserInfo(user, roles, sections))
        }
        case None => Future.successful(None)
      }
    }

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
    override def create(username: String, email: String, password: String, givenname: String, surname: String): Future[User] = {
      transactional { implicit conn =>
        // Before we do anything, we need to verify that the username and email are
        // unique. Throw a temper tantrum if they aren't.
        val fExistingEmail = userRepository.find(email)
        val fExistingUsername = userRepository.find(username)
        for {
          existingEmailOption <- fExistingEmail
          existingUsernameOption <- fExistingUsername
          createdUser <- { (existingEmailOption, existingUsernameOption) match {
            case (Some(email), None) => {
              throw new EmailAlreadyExistsException(s"The e-mail $email has already been registered.")
            }
            case (None, Some(username)) => {
              throw new UsernameAlreadyExistsException(s"The username $username has already been registered.")
            }
            case (Some(email), Some(username)) => {
              throw new EmailAndUsernameAlreadyExistException("Both the username $username and e-mail $email have already been registered.")
            }
            case (None, None) => {
              val webcrank = Passwords.scrypt()
              val passwordHash = Some(webcrank.crypt(password))
              val newUser = User(
                username = username,
                email = email,
                passwordHash = passwordHash,
                givenname = givenname,
                surname = surname
              )
              val fCreatedUser = userRepository.insert(newUser).recover {
                case exception => {
                  println("What went wrong? No exception???")
                  throw exception
                }
              }
              println(conn.toString())
              fCreatedUser.map { user =>
                println(s"created user ${user.username}")
              }
              fCreatedUser
            }
          }
        }}
        yield createdUser
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
                throw new EmailAndUsernameAlreadyExistException("Both the username ${userUsername.username} and e-mail ${userEmail.email} have already been registered.")
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
        // remove roles, remove from sections
        // delete user
        Future.successful(true)
      }
    }

    /**
     * List all roles.
     *
     * @return an array of Roles
     */
    override def listRoles: Future[IndexedSeq[Role]] = {
      roleRepository.list
    }

    /**
     * List all roles for one user.
     *
     * @param user  The user whose roles should be listed.
     * @return an array of this user's Roles
     */
    override def listRoles(userId: UUID): Future[IndexedSeq[Role]] = {
      for {
        userOption <- userRepository.find(userId)
        roles <- roleRepository.list(userOption.get)
      } yield roles
    }

    /**
     * Find a specific role by its unique id.
     *
     * @param id  the UUID of the Role to find
     * @return an optional Role
     */
    override def findRole(id: UUID): Future[Option[Role]] = {
      roleRepository.find(id)
    }

    /**
     * Find a specific role by name
     *
     * @param id  the name of the Role to find
     * @return an optional Role
     */
    override def findRole(name: String): Future[Option[Role]] = {
      roleRepository.find(name)
    }

    /**
     * Create a new role.
     *
     * @param name  the name of the Role to create
     * @return the newly created Role
     */
    override def createRole(name: String): Future[Role] = {
      val newRole = Role(name = name)
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
    override def updateRole(id: UUID, version: Long, name: String): Future[Role] = {
      transactional { implicit conn =>
        for {
          existingRole <- roleRepository.find(id)
          updatedRole <- roleRepository.update(Role(
            id = id,
            version = version,
            name = name
          ))
        }
        yield updatedRole
      }
    }

    /**
     *  Delete a role.
     *
     *  @param id  the unique id of the role
     *  @param version  the version of the role for optimistic offline lock
     *  @return the deleted role
     */
    override def deleteRole(id: UUID, version: Long): Future[Role] = {
      transactional { implicit conn =>
        for {
          role <- roleRepository.find(id).map { roleOption =>
            if (roleOption.get.version != version) throw new OutOfDateException(s"Role ${id.string} has been updated since version $version, it's now at version ${roleOption.get.version}.")
            roleOption.get
          }
          wasRemovedFromUsers <- roleRepository.removeFromAllUsers(role)
          wasDeleted <- roleRepository.delete(role)
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
    override def addRole(userId: UUID, roleName: String) = transactional { implicit conn =>
      val fUserOption = userRepository.find(userId)
      val fRoleOption = roleRepository.find(roleName)
      for {
        userOption <- fUserOption
        roleOption <- fRoleOption
        roleAdded <- roleRepository.addToUser(userOption.get, roleOption.get)
      }
      yield roleAdded
    }


    /**
     * Remove a role from a user.
     *
     * @param userId  the unique id of the user
     * @param roleName  the name of the role
     * @return a boolean indicator if the role was removed
     */
    override def removeRole(userId: UUID, roleName: String) = transactional { implicit conn =>
      val fUserOption = userRepository.find(userId)
      val fRoleOption = roleRepository.find(roleName)
      for {
        userOption <- fUserOption
        roleOption <- fRoleOption
        roleRemoved <- roleRepository.removeFromUser(userOption.get, roleOption.get)
      }
      yield roleRemoved
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
