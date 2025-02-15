package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.helpers.Token
import ca.shiftfocus.krispii.core.lib.InputUtils
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services.datasource._
import java.util.UUID

import ca.shiftfocus.krispii.core.models.user.User
import com.github.mauricio.async.db.Connection
import org.apache.commons.mail.EmailException
import play.api.Logger
import play.api.libs.mailer.{Email, MailerClient}

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.i18n.{Lang, MessagesApi}

import scala.concurrent.Future
import scalaz.{-\/, \/, \/-}
import webcrank.password._

class AuthServiceDefault(
    val db: DB,
    val mailerClient: MailerClient,
    val accountRepository: AccountRepository,
    val emailChangeRepository: EmailChangeRepository,
    val organizationRepository: OrganizationRepository,
    val paymentLogRepository: PaymentLogRepository,
    val roleRepository: RoleRepository,
    val sessionRepository: SessionRepository,
    val tagRepository: TagRepository,
    val userRepository: UserRepository,
    val userTokenRepository: UserTokenRepository,
    val wordRepository: WordRepository,
    val tagService: TagService
) extends AuthService {

  implicit def conn: Connection = db.pool

  //implicit def cache: ScalaCachePool = scalaCache

  /**
   * token types
   */
  private val password_reset = "password_reset"
  private val activation = "activation"

  private val defaultStudentEmailPrefix = "@student.krispii.com"

  /**
   * List all users (only for top administrators)
   * @return an IndexedSeq of users with their roles
   */
  override def list: Future[\/[ErrorUnion#Fail, IndexedSeq[User]]] = {
    (for {
      users <- lift(userRepository.list(db.pool))
      result <- liftSeq {
        users.map { user =>
          val fRoles = roleRepository.list(user)
          (for {
            roles <- lift(fRoles)
          } yield user.copy(roles = roles)).run
        }
      }
    } yield result).run
  }

  /**
   * List all members of all organizations that the caller is orgAdministrator or member in
   * @param user the caller
   * @return a list of users with their roles, or an error
   */
  override def listColleagues(user: User): Future[\/[ErrorUnion#Fail, IndexedSeq[User]]] =
    for {
      asAdmin <- lift(organizationRepository.listByAdmin(user.email))
      asMember <- lift(organizationRepository.listByMember(user.email))
      organizations = asAdmin ++ asMember
      _ = Logger.debug(s"${user.email} is admin or member in the organizations: ${organizations.map(_.title)}")
      users <- lift(userRepository.listOrganizationMembers(organizations))
    } yield users

  override def listByRange(offset: Int, limit: Int): Future[\/[ErrorUnion#Fail, IndexedSeq[User]]] = {
    (for {
      users <- lift(userRepository.listRange(offset, limit)(db.pool))
    } yield users).run
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
   * List users by tags
   *
   * @param tags (tagName:String, tagLang:String)
   * @param distinct Boolean If true each user should have all listed tags,
   *                 if false user should have at least one listed tag
   */
  def listByTags(tags: IndexedSeq[(String, String)], distinct: Boolean = true): Future[\/[ErrorUnion#Fail, IndexedSeq[User]]] = {
    userRepository.listByTags(tags, distinct)
  }

  def listByTeacher(userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[User]]] = {
    for {
      teacher <- lift(userRepository.find(userId))
      students <- lift(userRepository.list(teacher))
    } yield students
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
            -\/(ServiceError.BadInput("The password was invalid."))
          }
        })
      } yield authUser
    }
  }

  override def authenticateWithoutPassword(identifier: String): Future[\/[ErrorUnion#Fail, User]] = {
    transactional { implicit conn =>
      for {
        user <- lift(userRepository.find(identifier))
        roles <- lift(roleRepository.list(user))
        // _ = Logger.debug(s"Authenticating ${identifier} without password, roles=${roles}")
        userHash = user.hash.getOrElse("")
        authUser = user.copy(roles = roles)
        _ = Logger.info(s"Authenticating ${identifier} without password, account type=${authUser.accountType}, roles=" +
          authUser.roles.map(_.name.toLowerCase()))
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

  override def createSession(userId: UUID, ipAddress: String, userAgent: String): Future[\/[ErrorUnion#Fail, Session]] =
    createSession(userId, ipAddress, userAgent, None, None)

  /**
   * Create a new session.
   *
   * @param userId
   * @param ipAddress
   * @param userAgent
   * @param accessToken: optional Google or Microsoft access token
   * @param refreshToken: optional Microsoft refresh token
   * @return
   */
  override def createSession(userId: UUID, ipAddress: String, userAgent: String,
    accessToken: Option[String], refreshToken: Option[String]): Future[\/[ErrorUnion#Fail, Session]] =
    sessionRepository.create(Session(
      userId = userId,
      ipAddress = ipAddress,
      userAgent = userAgent,
      accessToken = accessToken,
      refreshToken = refreshToken
    ))

  override def updateSession(sessionId: UUID, ipAddress: String, userAgent: String): Future[\/[ErrorUnion#Fail, Session]] =
    updateSession(sessionId, ipAddress, userAgent, None, None)

  /**
   * Update an existing session.
   *
   * @param sessionId
   * @param ipAddress
   * @param userAgent
   * @return
   */
  override def updateSession(sessionId: UUID, ipAddress: String, userAgent: String, accessToken: Option[String], refreshToken: Option[String]): Future[\/[ErrorUnion#Fail, Session]] = {
    val fUpdated = for {
      session <- lift(sessionRepository.find(sessionId))
      updated <- lift(sessionRepository.update(session.copy(
        ipAddress = ipAddress,
        userAgent = userAgent,
        accessToken = accessToken.orElse(session.accessToken),
        refreshToken = refreshToken.orElse(session.refreshToken)
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
  override def find(id: UUID): Future[\/[ErrorUnion#Fail, User]] =
    find(id, includeDeleted = false)
  override def find(id: UUID, includeDeleted: Boolean): Future[\/[ErrorUnion#Fail, User]] =
    for {
      user <- lift(userRepository.find(id, includeDeleted))
      roles <- lift(roleRepository.list(user))
    } yield user.copy(roles = roles)

  /**
   * Find a user by their unique identifier.
   *
   * @param identifier The unique e-mail or username identifying this user.
   * @return a future disjunction containing the user and their information, or a failure
   */
  override def find(identifier: String): Future[\/[ErrorUnion#Fail, User]] =
    for {
      user <- lift(userRepository.find(identifier))
      roles <- lift(roleRepository.list(user))
    } yield user.copy(roles = roles)

  /**
   * Create a new user. Throws exceptions if the e-mail and username aren't unique.
   *
   * @param username  A unique identifier for this user.
   * @param email     The user's unique e-mail address.
   * @param password  The user's password.
   * @param givenname The user's first name.
   * @param surname   The user's family name.
   * @param hostname  Computer where the user is authenticating
   * @param id        The ID to allocate for this user, if left out, it will be random.
   * @return the created user
   */
  override def create(
    username: String,
    email: String,
    password: String,
    givenname: String,
    surname: String,
    hostname: String,
    id: UUID = UUID.randomUUID
  )(messagesApi: MessagesApi, lang: Lang): Future[\/[ErrorUnion#Fail, User]] =
    for {
      validEmail <- lift(validateEmail(email.trim.toLowerCase))
      _ <- lift(validateUsername(username.trim))
      _ <- predicate(InputUtils.isValidPassword(password.trim))(ServiceError.BadInput(
        "core.AuthServiceDefault.password.short"
      ))
      webcrank = Passwords.scrypt()
      passwordHash = Some(webcrank.crypt(password.trim))
      newUser = User(
        id = id,
        username = username.trim,
        email = validEmail,
        hash = passwordHash,
        givenname = givenname.trim,
        surname = surname.trim,
        accountType = "krispii"
      )
      newUser <- lift(userRepository.insert(newUser))
      _ = Logger.info(s"New user ${newUser.email} tentatively created, will now add token and/or tags")
      orgs <- lift(organizationRepository.listByMember(validEmail))
      finalUser <- lift((orgs.nonEmpty, validEmail.contains(defaultStudentEmailPrefix)) match {
        case (true, false) => giveOrgTags(newUser, orgs)
        case (false, false) => createToken(newUser, hostname)(messagesApi, lang)
        case _ => Future successful \/-(newUser)
      })
    } yield finalUser

  /**
   * If deleted user exists then move his account, subscriptions and logs to a new user with the same email
   * @see TagServiceDefault.syncWithDeletedUser()
   *
   * @param newUser User
   * @return in the Future, a krispii Account or an error
   */
  def syncWithDeletedUser(newUser: User): Future[\/[ErrorUnion#Fail, Account]] =
    for {
      // Check if user was deleted and has an account (stripe or group)
      account <- lift(userRepository.findDeleted(newUser.email).flatMap {
        // If deleted user is found
        case \/-(deletedUser) => {
          // We need to check if emails match 100%, because deleted user can be: deleted_1487883998_some.email@example.com,
          // and new user can be email@example.com, which will also match sql LIKE query: '%email@example.com'
          // @see userRepository.delete and userRepository.findDeleted
          // So we need to clean deleted email to compare it
          val oldEmail = deletedUser.email.replaceAll("^deleted_[0-9]{10}_", "")
          val newEmail = newUser.email

          if (oldEmail == newEmail) {
            for {
              // Move account from old user to a new user
              account <- lift(accountRepository.getByUserId(deletedUser.id).flatMap {
                case \/-(account) => accountRepository.update(account.copy(userId = newUser.id))
                case -\/(error) => Future successful -\/(error)
              })
              // Move payment logs from old user to a new user
              paymentLogs <- lift(paymentLogRepository.move(deletedUser.id, newUser.id))
            } yield (account)
          }
          else Future successful -\/(RepositoryError.NoResults("core.AuthServiceDefault.syncWithDeletedUser.no.user"))
        }
        case -\/(error) => Future successful -\/(error)
      })
    } yield account

  /**
   * Creates a new user with the given role.
   * This method sends an email for account activation.
   *
   * @param username Strign
   * @param email String
   * @param password String
   * @param givenname String
   * @param surname String
   * @param role String
   * @return
   */
  override def createWithRole(
    username: String,
    email: String,
    password: String,
    givenname: String,
    surname: String,
    role: String,
    hostname: String
  )(messagesApi: MessagesApi, lang: Lang): Future[\/[ErrorUnion#Fail, User]] =
    for {
      user <- lift(this.create(username, email, password, givenname, surname, hostname)(messagesApi, lang))
      _ = Logger.info(s"User ${user.email} created, will now add role $role")
      _ <- lift(addRole(user.id, role))
      _ = Logger.info(s"Added role $role to ${user.email}")
      _ <- lift(if (user.token.isEmpty) addRole(user.id, "authenticated") else Future successful \/-(role))
    } yield user

  // apparently never used
  override def createOpenIdUser(
    email: String,
    givenname: String,
    surname: String,
    accountType: String,
    hostname: String
  )(messagesApi: MessagesApi, lang: Lang): Future[\/[ErrorUnion#Fail, User]] = {
    val webcrank = Passwords.scrypt()
    val newUser = User(
      id = UUID.randomUUID(),
      username = email.trim,
      email = email.trim.toLowerCase,
      hash = Some(webcrank.crypt(UUID.randomUUID().toString)),
      givenname = givenname,
      surname = surname,
      accountType = accountType
    )
    for {
      newUser <- lift(userRepository.insert(newUser))
      orgs <- lift(organizationRepository.listByMember(email))
      finalUser <- lift((orgs.nonEmpty, email.contains(defaultStudentEmailPrefix)) match {
        case (true, false) => giveOrgTags(newUser, orgs)
        case (false, false) => createToken(newUser, hostname)(messagesApi, lang)
        case _ => Future successful \/-(newUser)
      })
    } yield finalUser
  }

  override def updateUserAccountType(email: String, newAccountType: String): Future[\/[ErrorUnion#Fail, User]] =
    transactional { implicit conn =>
      val fUser = for {
        user <- lift(userRepository.find(email))
        updatedUser <- lift(userRepository.update(user.copy(accountType = newAccountType)))
      } yield updatedUser
      fUser.run
    }

  override def reactivate(email: String, hostname: Option[String])(messagesApi: MessagesApi, lang: Lang): Future[\/[ErrorUnion#Fail, UserToken]] = {
    transactional { implicit conn =>
      val fToken = for {
        user <- lift(userRepository.find(email))
        token <- lift(userTokenRepository.find(user.id, "activation"))
        emailForNew = Email(
          messagesApi("activate.confirm.subject.new")(lang), //subject
          messagesApi("activate.confirm.from")(lang), //from
          Seq(user.givenname + " " + user.surname + " <" + email + ">"), //to
          bodyHtml = Some(messagesApi("activate.confirm.message", hostname.get, user.id.toString, token.token)(lang)) //text
        )
        _ <- lift(sendAsyncEmail(emailForNew))
      } yield token
      fToken.run
    }
  }
  /**
   * Update a user's identifiers.
   *
   * @param id       the unique id of the user
   * @param version  the latest version of the user for O.O.L.
   * @param email    optionally update the e-mail
   * @param username optionally update the username
   * @return a future disjunction containing the updated user, or a failure
   */
  override def update(
    id: UUID,
    version: Long,
    email: Option[String],
    username: Option[String],
    givenname: Option[String],
    surname: Option[String],
    alias: Option[String],
    password: Option[String],
    isDeleted: Option[Boolean]
  ): Future[\/[ErrorUnion#Fail, User]] =
    transactional { implicit conn =>
      val includeDeletedUsers = true
      val updated = for {
        existingUser <- lift(userRepository.find(id, includeDeletedUsers))
        _ <- predicate(existingUser.version == version)(ServiceError.OfflineLockFail)
        u_email <- lift(email.map { someEmail => validateEmail(someEmail.trim.toLowerCase, Some(id)) }.getOrElse(Future.successful(\/-(existingUser.email))))
        u_username <- lift(username.map { someUsername => validateUsername(someUsername, Some(id)) }.getOrElse(Future.successful(\/-(existingUser.username))))
        hash = password.flatMap { pwd =>
          if (!InputUtils.isValidPassword(pwd.trim)) Some("0")
          else {
            val webcrank = Passwords.scrypt()
            Some(webcrank.crypt(pwd.trim))
          }
        }
        _ <- predicate(!(hash.getOrElse("no password") == "0"))(ServiceError.BadInput("Password must be at least 8 characters"))
        userToUpdate = existingUser.copy(
          email = u_email,
          username = u_username,
          givenname = givenname.getOrElse(existingUser.givenname),
          surname = surname.getOrElse(existingUser.surname),
          alias = alias match {
            case Some(userAlias) if !userAlias.trim.isEmpty => Some(userAlias)
            case Some(userAlias) if userAlias.trim.isEmpty => None
            case None => existingUser.alias
          },
          hash = hash,
          isDeleted = isDeleted.getOrElse(existingUser.isDeleted)
        )
        updatedUser <- lift(userRepository.update(userToUpdate))
        roles <- lift(roleRepository.list(updatedUser))
      } yield updatedUser.copy(roles = roles)
      updated.run
    }

  /**
   * Update a user's identifiers.
   *
   * @param id       the unique id of the user
   * @param version  the latest version of the user for O.O.L.
   * @param email    optionally update the e-mail
   * @param username optionally update the username
   * @return a future disjunction containing the updated user, or a failure
   */
  override def updateIdentifier(id: UUID, version: Long, email: Option[String] = None, username: Option[String] = None): Future[\/[ErrorUnion#Fail, User]] =
    transactional { implicit conn =>
      val updated = for {
        existingUser <- lift(userRepository.find(id))
        _ <- predicate(existingUser.version == version)(ServiceError.OfflineLockFail)
        u_email <- lift(email.map { someEmail => validateEmail(someEmail, Some(id)) }.getOrElse(Future.successful(\/-(existingUser.email))))
        u_username <- lift(username.map { someUsername => validateUsername(someUsername, Some(id)) }.getOrElse(Future.successful(\/-(existingUser.username))))
        userToUpdate = existingUser.copy(
          email = u_email,
          username = u_username
        )
        updatedUser <- lift(userRepository.update(userToUpdate))
      } yield updatedUser
      updated.run
    }

  /**
   * Update a user's "non-identifying" information.
   *
   * @param id        the unique id of the user to be updated
   * @param version   the latest version of the user for O.O.L.
   * @param givenname the user's updated given name
   * @param surname   the user's updated family name
   * @return a future disjunction containing the updated user, or a failure
   */
  override def updateInfo(id: UUID, version: Long, givenname: Option[String] = None, surname: Option[String] = None, alias: Option[String] = None): Future[\/[ErrorUnion#Fail, User]] =
    transactional { implicit conn =>
      val updated = for {
        existingUser <- lift(userRepository.find(id))
        _ <- predicate(existingUser.version == version)(ServiceError.OfflineLockFail)
        userToUpdate = existingUser.copy(
          givenname = givenname.getOrElse(existingUser.givenname),
          surname = surname.getOrElse(existingUser.surname),
          alias = alias.orElse(existingUser.alias)
        )
        updatedUser <- lift(userRepository.update(userToUpdate))
      } yield updatedUser
      updated.run
    }

  /**
   * Update the user's password.
   *
   * @param id       the unique id of the user to be updated
   * @param version  the latest version of the user for O.O.L.
   * @param password the new password
   * @return a future disjunction containing the updated user, or a failure
   */
  override def updatePassword(id: UUID, version: Long, password: String): Future[\/[ErrorUnion#Fail, User]] =
    transactional { implicit conn =>
      val wc = Passwords.scrypt()
      val updated = for {
        existingUser <- lift(userRepository.find(id))
        _ <- predicate(existingUser.version == version)(ServiceError.OfflineLockFail)
        _ <- predicate(InputUtils.isValidPassword(password.trim))(ServiceError.BadInput("core.AuthServiceDefault.password.short"))
        u_hash = wc.crypt(password.trim)
        userToUpdate = existingUser.copy(hash = Some(u_hash))
        updatedUser <- lift(userRepository.update(userToUpdate))
      } yield updatedUser
      updated.run
    }

  /**
   * Deletes a user.
   *
   * TODO: delete the user's work
   *
   * @param id      the unique id of the user to be updated
   * @param version the latest version of the user for O.O.L.
   * @return a future disjunction containing the deleted user, or a failure
   */
  override def delete(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, User]] = {
    transactional { implicit conn =>
      for {
        existingUser <- lift(userRepository.find(id))
        _ <- predicate(existingUser.version == version)(ServiceError.OfflineLockFail)
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
  override def listRoles: Future[\/[ErrorUnion#Fail, IndexedSeq[Role]]] =
    roleRepository.list

  /**
   * List all roles for one user.
   *
   * @param userId The user whose roles should be listed.
   * @return an array of this user's Roles
   */
  override def listRoles(userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Role]]] =
    for {
      user <- lift(userRepository.find(userId))
      roles <- lift(roleRepository.list(user))
    } yield roles

  /**
   * Find a specific role by its unique id.
   *
   * @param id the UUID of the Role to find
   * @return an optional Role
   */
  override def findRole(id: UUID): Future[\/[ErrorUnion#Fail, Role]] =
    roleRepository.find(id)

  /**
   * Find a specific role by name
   *
   * @param name the name of the Role to find
   * @return an optional Role
   */
  override def findRole(name: String): Future[\/[ErrorUnion#Fail, Role]] =
    roleRepository.find(name)

  /**
   * Create a new role.
   *
   * @param name the name of the Role to create
   * @return the newly created Role
   */
  override def createRole(name: String, id: UUID = UUID.randomUUID): Future[\/[ErrorUnion#Fail, Role]] =
    transactional { implicit conn =>
      val newRole = Role(name = name, id = id)
      roleRepository.insert(newRole)
    }

  /**
   * Update a Role
   *
   * @param id      the unique id of the Role
   * @param version the version of the Role for optimistic offline lock
   * @param name    the new name to assign this Role
   * @return the newly updated Role
   */
  override def updateRole(id: UUID, version: Long, name: String): Future[\/[ErrorUnion#Fail, Role]] =
    transactional { implicit conn =>
      val result = for {
        existingRole <- lift(roleRepository.find(id))
        _ <- predicate(existingRole.version == version)(ServiceError.OfflineLockFail)
        updatedRole <- lift(roleRepository.update(existingRole.copy(name = name)))
      } yield updatedRole
      result.run
    }

  /**
   * Delete a role.
   *
   * @param id      the unique id of the role
   * @param version the version of the role for optimistic offline lock
   * @return the deleted role
   */
  override def deleteRole(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, Role]] =
    transactional { implicit conn =>
      for {
        role <- lift(roleRepository.find(id))
        _ <- predicate(role.version == version)(ServiceError.OfflineLockFail)
        wasRemovedFromUsers <- lift(roleRepository.removeFromAllUsers(role))
        deletedRole <- lift(roleRepository.delete(role))
      } yield deletedRole
    }

  /**
   * Add a role to a user.
   *
   * @param userId   the unique id of the user
   * @param roleName the name of the role
   * @return the added role if successful, otherwise an error
   */
  override def addRole(userId: UUID, roleName: String): Future[\/[ErrorUnion#Fail, Role]] =
    transactional { implicit conn =>
      for {
        user <- lift(userRepository.find(userId))
        oldRoles <- lift(roleRepository.list(user))
        role <- lift(roleRepository.find(roleName))
        _ <- lift(if (!oldRoles.contains(role)) roleRepository.addToUser(user, role) else Future successful \/-(()))
      } yield role
    }

  /**
   * Add a role to a user.
   *
   * @param userId    the unique id of the user
   * @param roleNames the name of the role
   * @return a boolean indicator if the role was added
   */
  override def addRoles(userId: UUID, roleNames: IndexedSeq[String]): Future[\/[ErrorUnion#Fail, User]] =
    transactional { implicit conn =>
      for {
        user <- lift(userRepository.find(userId))
        _ <- lift(serializedT(roleNames)(roleRepository.addToUser(user, _)))
      } yield user
    }

  /**
   * Remove a role from a user.
   *
   * @param userId   the unique id of the user
   * @param roleName the name of the role
   * @return a boolean indicator if the role was removed
   */
  override def removeRole(userId: UUID, roleName: String): Future[\/[ErrorUnion#Fail, Role]] =
    transactional { implicit conn =>
      val fUser = userRepository.find(userId)
      val fRole = roleRepository.find(roleName)
      for {
        user <- lift(fUser)
        role <- lift(fRole)
        roleRemoved <- lift(roleRepository.removeFromUser(user, role))
      } yield role
    }

  /**
   * Add a role to a given list of users.
   *
   * @param roleId  the UUID of the Role to be added
   * @param userIds an IndexedSeq of UUID listing the users to gain the role
   * @return a boolean indicator if the role was added
   */
  override def addUsers(roleId: UUID, userIds: IndexedSeq[UUID]): Future[\/[ErrorUnion#Fail, Unit]] =
    transactional { implicit conn =>
      val fRole = roleRepository.find(roleId)
      val fUsers = userRepository.list(userIds)

      for {
        role <- lift(fRole)
        userList <- lift(fUsers)
        addedUsers <- lift(roleRepository.addUsers(role, userList))
      } yield addedUsers
    }

  /**
   * Remove a role from a given list of users.
   *
   * @param roleId  the UUID of the Role to be removed
   * @param userIds an IndexedSeq of UUID listing the users to lose the role
   * @return a boolean indicator if the role was removed
   */
  override def removeUsers(roleId: UUID, userIds: IndexedSeq[UUID]): Future[\/[ErrorUnion#Fail, Unit]] = {
    transactional { implicit conn =>
      val fRole = roleRepository.find(roleId)
      val fUsers = userRepository.list(userIds)

      for {
        role <- lift(fRole)
        userList <- lift(fUsers)
        removedUsers <- lift(roleRepository.removeUsers(role, userList))
      } yield removedUsers
    }
  }

  // ---------- private utility methods ----------

  /**
   * Validate whether a given identifier can be used. Checks its format, and then checks
   * whether it is in use by another user. For updates, an existingId can be passed in so that
   * a false positive isn't received for updating an existing user.
   *
   * @param email
   * @param existingId
   * @return
   */
  private def validateEmail(email: String, existingId: Option[UUID] = None): Future[\/[ErrorUnion#Fail, String]] = {
    val existing = for {
      _ <- predicate(InputUtils.isValidEmail(email.trim))(ServiceError.BadInput(s"'$email' is not a valid format"))
      existingUser <- lift(userRepository.find(email.trim))
    } yield existingUser

    existing.run.map {
      case \/-(user) =>
        if (existingId.isEmpty || (existingId.get != user.id)) -\/(RepositoryError.UniqueKeyConflict("email", s"The e-mail address $email is already in use."))
        else \/-(email)
      case -\/(noResults: RepositoryError.NoResults) => \/-(email)
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
  private def validateUsername(username: String, existingId: Option[UUID] = None): Future[\/[ErrorUnion#Fail, String]] = {
    val existing = for {
      _ <- predicate(InputUtils.isValidUsername(username.trim))(ServiceError.BadInput("Your username must be at least 3 characters."))
      existingUser <- lift(userRepository.find(username.trim))
    } yield existingUser

    existing.run.map {
      case \/-(user) =>
        if (existingId.isEmpty || (existingId.get != user.id)) -\/(RepositoryError.UniqueKeyConflict("username", s"The username $username is already in use."))
        else \/-(username)
      case -\/(noResults: RepositoryError.NoResults) => \/-(username)
      case -\/(otherErrors: ErrorUnion#Fail) => -\/(otherErrors)
    }
  }

  /**
   * Activates a user given the userId and the activationCode
   *
   * @param userId         the UUID of the user to be activated
   * @param activationCode the activation code to be verified
   * @return
   */
  override def activate(userId: UUID, activationCode: String): Future[\/[ErrorUnion#Fail, User]] =
    for {
      user <- lift(userRepository.find(userId))
      roles <- lift(roleRepository.list(user))
      _ = Logger.debug(s"Activating ${user.email} with activation code $activationCode")
      token <- lift(userTokenRepository.find(user.id, activation))
      _ <- predicate(activationCode == token.token)(ServiceError.BadInput("Wrong activation code!"))
      _ <- lift(
        if (!roles.exists(_.name == "authenticated")) roleRepository.addToUser(user, "authenticated")
        else Future successful \/-(())
      )
      deleted <- lift(userTokenRepository.delete(userId, activation))
      _ = Logger.debug(s"Token $deleted for ${user.email} has been deleted")
      orgs <- lift(organizationRepository.listByMember(user.email))
      finalUser <- lift(if (orgs.nonEmpty) giveOrgTags(user, orgs) else Future successful \/-(user))
    } yield finalUser

  /**
   * Send an e-mail within a Future.
   *
   * @param email
   * @return
   */
  private def sendAsyncEmail(email: Email): Future[\/[ErrorUnion#Fail, String]] = Future {
    try {
      \/-(mailerClient.send(email))
    }
    catch {
      case emailException: EmailException => {
        -\/(ServiceError.MailerFail("E-mail sending failed.", Some(emailException)))
      }
      case otherException: Throwable => throw otherException
    }
  }

  /**
   * Find user token by nonce
   *
   * @param nonce     nonce duh
   * @param tokenType type of token - password reset or activation
   * @return user token
   */
  override def findUserToken(nonce: String, tokenType: String): Future[\/[ErrorUnion#Fail, UserToken]] = {
    transactional { implicit conn =>
      userTokenRepository.findTokenByNonce(nonce, tokenType)
    }
  }

  /**
   * Delete a token if exists and create a new one and send an email
   *
   * @param user
   * @param host - current host for creating a link
   * @return
   */
  override def createPasswordResetToken(user: User, host: String)(messagesApi: MessagesApi, lang: Lang): Future[\/[ErrorUnion#Fail, UserToken]] = {
    transactional { implicit conn =>
      val nonce = Token.getNext

      (for {
        oldToken <- lift(userTokenRepository.delete(user.id, password_reset).map {
          case \/-(success) => \/-(true)
          case -\/(error: RepositoryError.NoResults) => \/-(true)
          case -\/(error) => -\/(error)
        })
        token <- lift(userTokenRepository.insert(user.id, nonce, password_reset))
        email = Email(
          messagesApi("reset.password.confirm.subject.new")(lang), //subject
          messagesApi("reset.password.confirm.from")(lang), //from
          Seq(user.givenname + " " + user.surname + " <" + user.email + ">"), //to
          bodyHtml = Some(messagesApi("reset.password.confirm.message", host, nonce)(lang)) //text
        )
        mail <- lift(sendAsyncEmail(email))
      } yield token).run
    }
  }

  override def createActivationToken(user: User, host: String)(messagesApi: MessagesApi, lang: Lang): Future[\/[ErrorUnion#Fail, UserToken]] = {
    transactional { implicit conn =>
      val nonce = Token.getNext

      (for {
        oldToken <- lift(userTokenRepository.delete(user.id, activation).map {
          case \/-(success) => \/-(true)
          case -\/(error: RepositoryError.NoResults) => \/-(true)
          case -\/(error) => -\/(error)
        })
        token <- lift(userTokenRepository.insert(user.id, nonce, activation))
        activationEmail = Email(
          messagesApi("activate.confirm.subject.new")(lang), //subject
          messagesApi("activate.confirm.from")(lang), //from
          Seq(user.givenname + " " + user.surname + " <" + user.email + ">"), //to
          bodyHtml = Some(messagesApi("activate.confirm.message", host, user.id.toString, token.token)(lang)) //text
        )
        mail <- lift(sendAsyncEmail(activationEmail))
      } yield token).run
    }
  }

  /**
   * Delete user token
   *
   * @param token token to delete
   * @return
   */
  override def deleteToken(token: UserToken): Future[\/[ErrorUnion#Fail, UserToken]] = {
    transactional { implicit conn =>
      userTokenRepository.delete(token.userId, token.tokenType)
    }
  }

  /**
   * find user by token
   *
   */
  override def findToken(userId: UUID, tokenType: String): Future[\/[ErrorUnion#Fail, UserToken]] = {
    transactional { implicit conn =>
      userTokenRepository.find(userId, tokenType)
    }
  }

  /**
   * List users for autocomplete search
   *
   * @param key the stuff user already typed in
   */
  override def listByKey(key: String, includeDeleted: Boolean = false, limit: Int = 0, offset: Int = 0): Future[\/[ErrorUnion#Fail, IndexedSeq[User]]] = {
    (for {
      users <- lift(userRepository.triagramSearch(key, includeDeleted, limit, offset))
      _ = Logger.info(s"Users matching key ${key}: ${users}")
      result <- liftSeq {
        users.map { user =>
          (for {
            roles <- lift(roleRepository.list(user))
          } yield user.copy(roles = roles)).run
        }
      }
    } yield result).run
  }
  /**
   * Create password reset link for students, if it exists delete it
   */
  override def studentPasswordReset(user: User, lang: String): Future[\/[ErrorUnion#Fail, UserToken]] = {
    transactional { implicit conn =>
      (for {
        nonce <- lift(wordRepository.get(lang))
        token <- lift(userTokenRepository.insert(user.id, nonce.word, password_reset))
      } yield token).run
    }
  }

  override def redeemStudentPasswordReset(token: UserToken): Future[\/[ErrorUnion#Fail, User]] = {
    transactional { implicit conn =>
      for {
        deleted <- lift(userTokenRepository.delete(token.userId, token.tokenType))
        user <- lift(userRepository.find(token.userId))
      } yield user
    }
  }

  //##### EMAIL CHANGE #################################################################################################

  override def findEmailChange(userId: UUID): Future[\/[ErrorUnion#Fail, EmailChangeRequest]] = {
    emailChangeRepository.find(userId)
  }

  /**
   * Change a user's e-mail address.
   *
   * Workflow:
   *   1. Verify that the new e-mail is unique.
   *   2. Create an e-mail change request
   *   3. Send an e-mail to the old address notifying of the change
   *   4. Send an e-mail to the new address, asking them to confirm the change
   *
   * @param user the user for whom the e-mail change is requested
   * @param newEmail the new address the user would like to have
   * @return the created e-mail change request
   */
  override def requestEmailChange(user: User, newEmail: String, host: String)(messagesApi: MessagesApi, lang: Lang): Future[\/[ErrorUnion#Fail, EmailChangeRequest]] = {
    transactional { implicit conn =>
      (for {
        _ <- predicate(InputUtils.isValidEmail(newEmail.trim))(ServiceError.BadInput("core.services.AuthServiceDefault.requestEmailChange.email.bad.format"))
        // Find will also search by username and it can find the same user if newEmail will match username,
        // so we need to check if user id is equal
        emailUnique <- lift(userRepository.find(newEmail).map {
          case \/-(existingUser) => {
            if (existingUser.id == user.id) \/-(true)
            else \/-(false)
          }
          case -\/(error: RepositoryError.NoResults) => \/-(true)
          case -\/(error) => -\/(error)
        })
        requestNotExists <- lift(emailChangeRepository.find(newEmail).map {
          case \/-(request) => \/-(false)
          case -\/(error: RepositoryError.NoResults) => \/-(true)
          case -\/(error) => -\/(error)
        })
        _ <- predicate(emailUnique && requestNotExists)(ServiceError.BadInput("core.services.AuthServiceDefault.requestEmailChange.email.exist"))

        token = Token.getNext
        newRequest <- lift(emailChangeRepository.insert(EmailChangeRequest(user.id, newEmail, token)))

        oldEmailText = messagesApi("emailchange.request.message.old", user.givenname, user.surname, user.email, newEmail)(lang)
        emailForOld = Email(
          messagesApi("emailchange.request.subject.old")(lang), //subject
          messagesApi("emailchange.request.from")(lang), //from
          Seq(user.givenname + " " + user.surname + " <" + user.email + ">"), //to
          bodyHtml = Some(oldEmailText) //text
        )

        newEmailText = messagesApi("emailchange.request.message.new", user.givenname, user.surname, host, user.id, token)(lang)
        emailForNew = Email(
          messagesApi("emailchange.request.subject.new")(lang), //subject
          messagesApi("emailchange.request.from")(lang), //from
          Seq(user.givenname + " " + user.surname + " <" + newEmail + ">"), //to
          bodyHtml = Some(newEmailText) //text
        )

        oldEmailResult <- lift(sendAsyncEmail(emailForOld))
        newEmailResult <- lift(sendAsyncEmail(emailForNew))
      } yield newRequest).run
    }
  }

  override def confirmEmailChange(email: String, token: String)(messagesApi: MessagesApi, lang: Lang): Future[\/[ErrorUnion#Fail, User]] =
    for {
      changeRequest <- lift(emailChangeRepository.find(email))
      _ <- predicate(changeRequest.token == token)(ServiceError.BadInput(
        "core.services.AuthServiceDefault.confirmEmailChange.wrong.token"
      ))

      oldUser <- lift(userRepository.find(changeRequest.userId))
      updatedUser <- lift(userRepository.update(oldUser.copy(email = email)))
      _ <- lift(emailChangeRepository.delete(changeRequest))

      orgs <- lift(organizationRepository.listByMember(email))
      _ <- lift(if (orgs.nonEmpty) giveOrgTags(updatedUser, orgs) else Future successful \/-(updatedUser))

      emailForOld = Email(
        messagesApi("emailchange.confirm.subject.old")(lang), //subject
        messagesApi("emailchange.confirm.from")(lang), //from
        Seq(oldUser.givenname + " " + oldUser.surname + " <" + oldUser.email + ">"), //to
        bodyHtml = Some(messagesApi("emailchange.confirm.message.old", oldUser.givenname, oldUser.surname)(lang)) //text
      )

      emailForNew = Email(
        messagesApi("emailchange.confirm.subject.new")(lang), //subject
        messagesApi("emailchange.confirm.from")(lang), //from
        Seq(updatedUser.givenname + " " + updatedUser.surname + " <" + email + ">"), //to
        bodyHtml = Some(messagesApi("emailchange.confirm.message.new", updatedUser.givenname, updatedUser.surname)(lang)) //text
      )

      _ <- lift(sendAsyncEmail(emailForOld))
      _ <- lift(sendAsyncEmail(emailForNew))

    } yield updatedUser

  override def cancelEmailChange(userId: UUID)(messagesApi: MessagesApi, lang: Lang): Future[\/[ErrorUnion#Fail, EmailChangeRequest]] = {
    for {
      user <- lift(userRepository.find(userId))
      changeRequest <- lift(emailChangeRepository.find(user.id))
      deleted <- lift(emailChangeRepository.delete(changeRequest))

      /*emailForOld = Email(
        messagesApi("emailchange.cancel.subject.old")(lang), //subject
        messagesApi("emailchange.cancel.from")(lang), //from
        Seq(user.givenname + " " + user.surname + " <" + user.email + ">"), //to
        bodyHtml = Some(messagesApi("emailchange.cancel.message.old", user.givenname, user.surname)(lang)) //text
      )

      emailForNew = Email(
        messagesApi("emailchange.cancel.subject.new")(lang), //subject
        messagesApi("emailchange.cancel.from")(lang), //from
        Seq(user.givenname + " " + user.surname + " <" + changeRequest.requestedEmail + ">"), //to
        bodyHtml = Some(messagesApi("emailchange.cancel.message.new", user.givenname, user.surname)(lang)) //text
      )

      _ <- lift(sendAsyncEmail(emailForOld))
      _ <- lift(sendAsyncEmail(emailForNew))*/
    } yield deleted
  }

  /**
   * Create activation token for a new user
   * @param user User
   * @param hostname String (for the email)
   * @return Future containing the new user including the token, or an error
   */
  private def createToken(user: User, hostname: String)(implicit messagesApi: MessagesApi, lang: Lang): Future[ErrorUnion#Fail \/ User] = {
    Logger.info(s"New user ${user.email} tentatively created, will now create token")
    val token = Token.getNext
    for {
      userToken <- lift(userTokenRepository.insert(user.id, token, activation))
      _ = Logger.info(s"Activation token for ${user.email} created")
      emailForNew = Email(
        messagesApi("activate.confirm.subject.new")(lang), //subject
        messagesApi("activate.confirm.from")(lang), //from
        Seq(user.givenname + " " + user.surname + " <" + user.email + ">"), //to
        bodyHtml = Some(messagesApi("activate.confirm.message", hostname, user.id.toString, userToken.token)(lang)) //text
      )
      messageId <- lift(sendAsyncEmail(emailForNew))
      _ = Logger.info(s"Sent activation email to ${user.email}, message ID is $messageId")
    } yield user.copy(token = Some(userToken))
  }

  /**
   * Update a user's admin tags according to the given organizations.
   * @param user User
   * @param orgs IndexedSeq[Organization]
   * @return Future containing an error, or the same User as before (Users are not currently enriched with Tags)
   */
  private def giveOrgTags(user: User, orgs: IndexedSeq[Organization]): Future[ErrorUnion#Fail \/ User] =
    for {
      orgTagList <- lift(serializedT(orgs)(org =>
        tagService.listByEntity(org.id, TaggableEntities.organization)))
      planTags <- lift(accountRepository.getByUserId(user.id).flatMap {
        case \/-(a) if a.activeUntil.isDefined && a.activeUntil.get.isBeforeNow && a.subscriptions.nonEmpty =>
          tagService.listByEntity(a.subscriptions.head.planId, TaggableEntities.plan)
        case \/-(_) | -\/(_: RepositoryError.NoResults) => Future successful \/-(IndexedSeq[Tag]())
        case -\/(error) => Future successful -\/(error)
      })
      goodTags = (orgTagList.flatten ++ planTags).distinct.filter(_.isAdmin) // instead of listAdminByEntity
      _ = Logger.debug(s"giveOrgTags: ${user.email} has the right to admin tags ${goodTags.map(_.name)}")

      allOldTags <- lift(tagService.listByEntity(user.id, TaggableEntities.user))
      oldTags = allOldTags.filter(_.isAdmin) // instead of listAdminByEntity
      _ = Logger.debug(s"giveOrgTags: ${user.email} had admin tags ${oldTags.map(_.name)}")

      tagsToGive = goodTags.diff(oldTags)
      _ = Logger.debug(s"giveOrgTags: ${user.email} will receive admin tags ${tagsToGive.map(_.name)}")

      _ <- lift(serializedT(tagsToGive)(tag =>
        tagService.tag(user.id, TaggableEntities.user, tag.name, tag.lang)))

      tagsToRemove = oldTags.filterNot(_.id.toString == "29479566-3a50-4d36-bc20-f45ff4d4b2d4") // trial tag
        .diff(goodTags)
      _ = Logger.debug(s"giveOrgTags: ${user.email} will lose admin tags ${tagsToRemove.map(_.name)}")
      _ <- lift(serializedT(tagsToRemove)(tag =>
        tagService.untag(user.id, TaggableEntities.user, tag.name, tag.lang, shouldUpdateFrequency = false)))
    } yield user

}
