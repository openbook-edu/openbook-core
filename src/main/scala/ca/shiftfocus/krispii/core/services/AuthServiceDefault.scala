package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.helpers.Token
import ca.shiftfocus.krispii.core.lib.{ InputUtils, ScalaCachePool }
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services.datasource._
import java.util.UUID
import ca.shiftfocus.lib.exceptions.ExceptionWriter
import com.github.mauricio.async.db.Connection
import org.apache.commons.mail.EmailException
import play.api.Logger
import play.api.libs.mailer.{ Email, MailerClient }
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.i18n.{ I18nSupport, MessagesApi, Messages }
import scala.concurrent.Future
import scalacache.ScalaCache
import scalaz.{ -\/, \/-, \/, EitherT }
import webcrank.password._

class AuthServiceDefault(
    val db: DB,
    val scalaCache: ScalaCachePool,
    val userRepository: UserRepository,
    val roleRepository: RoleRepository,
    val userTokenRepository: UserTokenRepository,
    val sessionRepository: SessionRepository,
    val mailerClient: MailerClient,
    val wordRepository: WordRepository,
    override val messagesApi: MessagesApi
) extends AuthService with I18nSupport {

  implicit def conn: Connection = db.pool

  implicit def cache: ScalaCachePool = scalaCache

  /**
   * token types
   */
  val password_reset = "password_reset"
  val activation = "activation"

  /**
   * List all users.
   *
   * @return an IndexedSeq of user
   */
  override def list: Future[\/[ErrorUnion#Fail, IndexedSeq[User]]] = {
    for {
      users <- lift(userRepository.list(db.pool))
      result <- liftSeq {
        users.map { user =>
          val fRoles = roleRepository.list(user)(db.pool, cache)
          (for {
            roles <- lift(fRoles)
          } yield user.copy(roles = roles)).run
        }
      }
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
   * @param identifier The unique e-mail or username identifying this user.
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
   * @param email     The user's unique e-mail address.
   * @param password  The user's password.
   * @param givenname The user's first name.
   * @param surname   The user's family name.
   * @param id        The ID to allocate for this user, if left out, it will be random.
   * @return the created user
   */
  override def create(
    username: String,
    email: String,
    password: String,
    givenname: String,
    surname: String,
    id: UUID = UUID.randomUUID
  ): Future[\/[ErrorUnion#Fail, User]] = {
    transactional { implicit conn =>
      val webcrank = Passwords.scrypt()
      val token = Token.getNext
      for {
        validEmail <- lift(validateEmail(email.trim))
        validUsername <- lift(validateUsername(username.trim))
        _ <- predicate(InputUtils.isValidPassword(password.trim))(ServiceError.BadInput("The password provided must be at least 8 characters."))
        passwordHash = Some(webcrank.crypt(password.trim))
        newUser <- lift {
          val newUser = User(
            id = id,
            username = username.trim,
            email = email.trim,
            hash = passwordHash,
            givenname = givenname.trim,
            surname = surname.trim
          )
          userRepository.insert(newUser)
        }
        user = newUser.copy(token = Some(UserToken(id, token, activation)))
        _ <- lift(userTokenRepository.insert(newUser.id, token, activation))
      } yield user
    }
  }

  /**
   * Creates a new user with the given role.
   * This method sends an email for account activation.
   *
   * @param username
   * @param email
   * @param password
   * @param givenname
   * @param surname
   * @param role
   * @return
   */
  override def createWithRole(
    username: String,
    email: String,
    password: String,
    givenname: String,
    surname: String,
    role: String,
    hostname: Option[String]
  )(messagesApi: MessagesApi): Future[\/[ErrorUnion#Fail, User]] = {
    val messages = messagesApi
    val fUser = for {
      user <- lift(this.create(username, email, password, givenname, surname))
      _ <- lift(addRole(user.id, role))
      emailForNew = Email(
        messages("activate.confirm.subject.new"), //subject
        messages("activate.confirm.from"), //from
        Seq(givenname + " " + surname + " <" + email + ">"), //to
        bodyHtml = Some(messages("activate.confirm.message", hostname.get, user.id.toString, user.token.get.token)) //text
      )
      _ <- lift(sendAsyncEmail(emailForNew))
    } yield user
    fUser.run
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
    password: Option[String]
  ): Future[\/[ErrorUnion#Fail, User]] = {
    transactional { implicit conn =>
      val updated = for {
        existingUser <- lift(userRepository.find(id))
        _ <- predicate(existingUser.version == version)(ServiceError.OfflineLockFail)
        u_email <- lift(email.map { someEmail => validateEmail(someEmail, Some(id)) }.getOrElse(Future.successful(\/-(existingUser.email))))
        u_username <- lift(username.map { someUsername => validateUsername(someUsername, Some(id)) }.getOrElse(Future.successful(\/-(existingUser.username))))
        hash = password match {
          case Some(pwd) => {
            if (!InputUtils.isValidPassword(pwd.trim)) {
              Some("0")
            }
            else {
              val webcrank = Passwords.scrypt()
              Some(webcrank.crypt(pwd.trim))
            }
          }
          case None => None
        }
        _ <- predicate(!(hash.getOrElse("no password") == "0"))(ServiceError.BadInput("Password must be at least 8 characters"))
        userToUpdate = existingUser.copy(
          email = u_email,
          username = u_username,
          givenname = givenname.getOrElse(existingUser.givenname),
          surname = surname.getOrElse(existingUser.surname),
          hash = hash
        )
        updatedUser <- lift(userRepository.update(userToUpdate))
      } yield updatedUser
      updated.run
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
  override def updateIdentifier(id: UUID, version: Long, email: Option[String] = None, username: Option[String] = None): Future[\/[ErrorUnion#Fail, User]] = {
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
  override def updateInfo(id: UUID, version: Long, givenname: Option[String] = None, surname: Option[String] = None): Future[\/[ErrorUnion#Fail, User]] = {
    transactional { implicit conn =>
      val updated = for {
        existingUser <- lift(userRepository.find(id))
        _ <- predicate(existingUser.version == version)(ServiceError.OfflineLockFail)
        userToUpdate = existingUser.copy(
          givenname = givenname.getOrElse(existingUser.givenname),
          surname = surname.getOrElse(existingUser.surname)
        )
        updatedUser <- lift(userRepository.update(userToUpdate))
      } yield updatedUser
      updated.run
    }
  }

  /**
   * Update the user's password.
   *
   * @param id       the unique id of the user to be updated
   * @param version  the latest version of the user for O.O.L.
   * @param password the new password
   * @return a future disjunction containing the updated user, or a failure
   */
  override def updatePassword(id: UUID, version: Long, password: String): Future[\/[ErrorUnion#Fail, User]] = {
    transactional { implicit conn =>
      val wc = Passwords.scrypt()
      val updated = for {
        existingUser <- lift(userRepository.find(id))
        _ <- predicate(existingUser.version == version)(ServiceError.OfflineLockFail)
        _ <- predicate(InputUtils.isValidPassword(password.trim))(ServiceError.BadInput("The password provided must be at least 8 characters."))
        u_hash = wc.crypt(password.trim)
        userToUpdate = existingUser.copy(hash = Some(u_hash))
        updatedUser <- lift(userRepository.update(userToUpdate))
      } yield updatedUser
      updated.run
    }
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
  override def listRoles: Future[\/[ErrorUnion#Fail, IndexedSeq[Role]]] = {
    roleRepository.list
  }

  /**
   * List all roles for one user.
   *
   * @param userId The user whose roles should be listed.
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
   * @param id the UUID of the Role to find
   * @return an optional Role
   */
  override def findRole(id: UUID): Future[\/[ErrorUnion#Fail, Role]] = {
    roleRepository.find(id)
  }

  /**
   * Find a specific role by name
   *
   * @param name the name of the Role to find
   * @return an optional Role
   */
  override def findRole(name: String): Future[\/[ErrorUnion#Fail, Role]] = {
    roleRepository.find(name)
  }

  /**
   * Create a new role.
   *
   * @param name the name of the Role to create
   * @return the newly created Role
   */
  override def createRole(name: String, id: UUID = UUID.randomUUID): Future[\/[ErrorUnion#Fail, Role]] = {
    transactional { implicit conn =>
      val newRole = Role(name = name, id = id)
      roleRepository.insert(newRole)
    }
  }

  /**
   * Update a Role
   *
   * @param id      the unique id of the Role
   * @param version the version of the Role for optimistic offline lock
   * @param name    the new name to assign this Role
   * @return the newly updated Role
   */
  override def updateRole(id: UUID, version: Long, name: String): Future[\/[ErrorUnion#Fail, Role]] = {
    transactional { implicit conn =>
      val result = for {
        existingRole <- lift(roleRepository.find(id))
        _ <- predicate(existingRole.version == version)(ServiceError.OfflineLockFail)
        updatedRole <- lift(roleRepository.update(existingRole.copy(name = name)))
      } yield updatedRole
      result.run
    }
  }

  /**
   * Delete a role.
   *
   * @param id      the unique id of the role
   * @param version the version of the role for optimistic offline lock
   * @return the deleted role
   */
  override def deleteRole(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, Role]] = {
    transactional { implicit conn =>
      for {
        role <- lift(roleRepository.find(id))
        _ <- predicate(role.version == version)(ServiceError.OfflineLockFail)
        wasRemovedFromUsers <- lift(roleRepository.removeFromAllUsers(role))
        deletedRole <- lift(roleRepository.delete(role))
      } yield deletedRole
    }
  }

  /**
   * Add a role to a user.
   *
   * @param userId   the unique id of the user
   * @param roleName the name of the role
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
      } yield userInfo
    }
  }

  /**
   * Add a role to a user.
   *
   * @param userId    the unique id of the user
   * @param roleNames the name of the role
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
   * @param userId   the unique id of the user
   * @param roleName the name of the role
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
      } yield userInfo
    }
  }

  /**
   * Add a role to a given list of users.
   *
   * @param roleId  the UUID of the Role to be added
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
      } yield addedUsers
    }
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
      val fRole = roleRepository.find(roleId)(db.pool, cache)
      val fUsers = userRepository.list(userIds)(db.pool, cache)

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
  private def validateEmail(email: String, existingId: Option[UUID] = None)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, String]] = {
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
  private def validateUsername(username: String, existingId: Option[UUID] = None)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, String]] = {
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
  override def activate(userId: UUID, activationCode: String): Future[\/[ErrorUnion#Fail, User]] = {
    transactional { implicit conn =>
      val fUser = userRepository.find(userId)(db.pool, cache)
      val result = for {
        user <- lift(fUser)

        roles <- lift(roleRepository.list(user))
        token <- lift(userTokenRepository.find(user.id, activation))
        _ <- predicate(activationCode == token.token)(ServiceError.BadInput("Wrong activation code!"))
        _ <- lift(roleRepository.addToUser(user, "authenticated"))
        deleted <- lift(userTokenRepository.delete(userId, activation))

      } yield (user, token, roles, deleted)

      result.run.map {
        case \/-((user, token, roles, deleted)) => \/-(user)
        case -\/(otherErrors: ErrorUnion#Fail) => -\/(otherErrors)
      }
    }

  }

  /**
   * Get the user by activation token, verify and send him to his profile page after in api
   *
   * @param token
   * @return
   */
  //  def resetPassword(token: String): Future[\/[ErrorUnion#Fail, User]] = {
  //
  //  }

  /**
   * @inheritdoc
   */
  //  def resendActivation(email: String)(messagesApi: MessagesApi): Future[\/[ErrorUnion#Fail, User]] = {
  //    for {
  //      user <- userRepository.find(email)(db.pool, cache)
  //      nonce <- userTokenRepository.find(user.id)(db.pool, cache)
  //      messages = messagesApi.preferred(Seq(user.languagePref))
  //      message = messages("activate.email.body", (configuration.getString("general.base_url").get + controllers.routes.Application.activateUser(user.id)), nonce.token)
  //      email = Email(
  //        messages("activate.email.subject"), //subject
  //        messages("activate.email.from"), //from
  //        Seq(user.givenname + " " + user.surname + " <" + user.email + ">"), //to
  //        bodyText = Some(message) //text
  //      )
  //      mail <- sendAsyncEmail(email)
  //    } yield user
  //  }

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
  override def createPasswordResetToken(user: User, host: String): Future[\/[ErrorUnion#Fail, UserToken]] = {
    transactional { implicit conn =>
      var nonce = Token.getNext
      var fToken = for {
        oldToken <- lift(userTokenRepository.delete(user.id, password_reset).map {
          case \/-(success) => \/-(true)
          case -\/(error: RepositoryError.NoResults) => \/-(true)
          case -\/(error) => -\/(error)
        })
        token <- lift(userTokenRepository.insert(user.id, nonce, password_reset))
        email = Email(
          "reset your password", //subject
          "vz@shiftfocus.ca", //from
          Seq(user.givenname + " " + user.surname + " <" + user.email + ">"), //to
          bodyText = Some(host + "/api/reset/" + nonce.toString) //text
        )
        mail <- lift(sendAsyncEmail(email))
      } yield token
      fToken.run
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
   * @param conn
   */
  override def listByKey(key: String): Future[\/[RepositoryError.Fail, IndexedSeq[User]]] = {
    transactional { implicit conn =>
      userRepository.triagramSearch(key)
    }
  }
  /**
   * Create password reset link for students, if it exists delete it
   */
  override def studentPasswordReset(user: User, lang: String): Future[\/[ErrorUnion#Fail, UserToken]] = {
    transactional { implicit conn =>
      var fToken = for {
        nonce <- lift(wordRepository.get(lang))
        token <- lift(userTokenRepository.insert(user.id, nonce.word, password_reset))
      } yield token
      fToken.run
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

}