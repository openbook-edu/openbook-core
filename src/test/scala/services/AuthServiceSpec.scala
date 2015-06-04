import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.krispii.core.models.User
import java.util.UUID
import com.github.mauricio.async.db.Connection
import org.joda.time.DateTime
import scala.concurrent.{Future,ExecutionContext,Await}
import scala.concurrent.duration._
import ca.shiftfocus.krispii.core.services._
import ca.shiftfocus.krispii.core.services.datasource._
import ca.shiftfocus.krispii.core.repositories._

import org.scalatest._
import Matchers._ // Is used for "should be and etc."
import scalaz.{-\/, \/, \/-}

class AuthServiceSpec
  extends TestEnvironment(writeToDb = false) {
  // Create stubs of AuthService's dependencies
  val db = stub[DB]
  val mockConnection    = stub[Connection]
  val userRepository    = stub[UserRepository]
  val roleRepository    = stub[RoleRepository]
  val sessionRepository = stub[SessionRepository]

  // Create a real instance of AuthService for testing
  val authService = new AuthServiceDefault(db, cache, userRepository, roleRepository, sessionRepository) {
    override implicit def conn: Connection = mockConnection

    override def transactional[A](f: Connection => Future[A]): Future[A] = {
      f(mockConnection)
    }
  }

  "AuthService.authenticate" should {
    inSequence {
      "return a user if the identifier and password combination are valid" in {
        val testUser = TestValues.testUserA
        val password = "adminpass"
        val testRoleList = Vector(
          TestValues.testRoleA
        )

        (userRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(testUser.username, *, *) returns(Future.successful(\/.right(testUser)))
        (roleRepository.list(_: User)(_: Connection, _: ScalaCachePool)) when(testUser, *, *) returns(Future.successful(\/.right(testRoleList)))

        val fSomeUser = authService.authenticate(testUser.username, password)
        val \/-(user) = Await.result(fSomeUser, Duration.Inf)

        user should be(testUser)
      }
      "return AuthFail if the password was wrong" in {
        val testUser = TestValues.testUserA
        val password = "bad password"
        val testRoleList = Vector(
          TestValues.testRoleA
        )

        (userRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(testUser.username, *, *) returns(Future.successful(\/.right(testUser)))
        (roleRepository.list(_: User)(_: Connection, _: ScalaCachePool)) when(testUser, *, *) returns(Future.successful(\/.right(testRoleList)))

        val fSomeUser = authService.authenticate(testUser.username, password)
        Await.result(fSomeUser, Duration.Inf) should be(-\/(ServiceError.BadPermissions("The password was invalid.")))
      }
      "return RepositoryError.NoResults if the user doesn't exist" in {
        val testUser = TestValues.testUserD
        val password = "adminpass"
        val testRoleList = Vector(
          TestValues.testRoleA
        )

        (userRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(testUser.username, *, *) returns(Future.successful(-\/(RepositoryError.NoResults)))
        (roleRepository.list(_: User)(_: Connection, _: ScalaCachePool)) when(testUser, *, *) returns(Future.successful(\/.right(testRoleList)))

        val fSomeUser = authService.authenticate(testUser.username, password)
        Await.result(fSomeUser, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
    }
  }

  "AuthService.update" should {
    inSequence {
      "update user with unique values (ALL)" in {
        val testUser = TestValues.testUserA.copy(
          hash = None
        )
        val updatedTestUser = testUser.copy(
          version   = testUser.version + 1,
          email     = "updated_user@example.com",
          username  = "updated_username",
          givenname = "updated_givenname",
          surname   = "updated_surname"
        )

        (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testUser.id, *, *) returns(Future.successful(\/-(testUser)))
        (userRepository.update(_: User)(_: Connection, _: ScalaCachePool)) when(updatedTestUser, *, *) returns(Future.successful(\/-(updatedTestUser)))

        // Conflicting user
        (userRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(updatedTestUser.email, *, *) returns(Future.successful(-\/(RepositoryError.NoResults)))
        (userRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(updatedTestUser.username, *, *) returns(Future.successful(-\/(RepositoryError.NoResults)))

        val result = authService.update(testUser.id, testUser.version, Some(updatedTestUser.email), Some(updatedTestUser.username), Some(updatedTestUser.givenname), Some(updatedTestUser.surname))
        val eitherUser = Await.result(result, Duration.Inf)
        val \/-(user) = eitherUser

        user.id should be(updatedTestUser.id)
        user.version should be(updatedTestUser.version)
        user.email should be(updatedTestUser.email)
        user.username should be(updatedTestUser.username)
        user.hash should be(None)
        user.givenname should be(updatedTestUser.givenname)
        user.surname should be(updatedTestUser.surname)
      }
      "update user with unique values (only ginvenname and surname)" in {
        val testUser = TestValues.testUserA.copy(
          hash = None
        )
        val updatedTestUser = testUser.copy(
          version   = testUser.version + 1,
          givenname = "updated_givenname",
          surname   = "updated_surname"
        )

        (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testUser.id, *, *) returns(Future.successful(\/-(testUser)))
        (userRepository.update(_: User)(_: Connection, _: ScalaCachePool)) when(updatedTestUser, *, *) returns(Future.successful(\/-(updatedTestUser)))

        // Conflicting user
        (userRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(updatedTestUser.email, *, *) returns(Future.successful(\/-(testUser)))
        (userRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(updatedTestUser.username, *, *) returns(Future.successful(\/-(testUser)))

        val result = authService.update(testUser.id, testUser.version, Some(updatedTestUser.email), Some(updatedTestUser.username), Some(updatedTestUser.givenname), Some(updatedTestUser.surname))
        val eitherUser = Await.result(result, Duration.Inf)
        val \/-(user) = eitherUser

        user.id should be(updatedTestUser.id)
        user.version should be(updatedTestUser.version)
        user.email should be(updatedTestUser.email)
        user.username should be(updatedTestUser.username)
        user.hash should be(None)
        user.givenname should be(updatedTestUser.givenname)
        user.surname should be(updatedTestUser.surname)
      }
      "return RepositoryError.UniqueKeyConflict if username is not unique" in {
        val testUser = TestValues.testUserA.copy(
          hash = None
        )
        val updatedTestUser = testUser.copy(
          version   = testUser.version + 1,
          email     = "updated_user@example.com",
          username  = "updated_username",
          givenname = "updated_givenname",
          surname   = "updated_surname"
        )
        val conflictingUser = TestValues.testUserB

        (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testUser.id, *, *) returns(Future.successful(\/-(testUser)))
        (userRepository.update(_: User)(_: Connection, _: ScalaCachePool)) when(updatedTestUser, *, *) returns(Future.successful(\/-(updatedTestUser)))

        // Conflicting user
        (userRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(updatedTestUser.email, *, *) returns(Future.successful(-\/(RepositoryError.NoResults)))
        (userRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(updatedTestUser.username, *, *) returns(Future.successful(\/-(conflictingUser)))

        val result = authService.update(testUser.id, testUser.version, Some(updatedTestUser.email), Some(updatedTestUser.username), Some(updatedTestUser.givenname), Some(updatedTestUser.surname))
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.UniqueKeyConflict("username", s"The username ${updatedTestUser.username} is already in use.")))
      }
      "return RepositoryError.UniqueKeyConflict if email is not unique" in {
        val testUser = TestValues.testUserA.copy(
          hash = None
        )
        val updatedTestUser = testUser.copy(
          version   = testUser.version + 1,
          email     = "updated_user@example.com",
          username  = "updated_username",
          givenname = "updated_givenname",
          surname   = "updated_surname"
        )
        val conflictingUser = TestValues.testUserB

        (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testUser.id, *, *) returns(Future.successful(\/-(testUser)))
        (userRepository.update(_: User)(_: Connection, _: ScalaCachePool)) when(updatedTestUser, *, *) returns(Future.successful(\/-(updatedTestUser)))

        // Conflicting user
        (userRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(updatedTestUser.email, *, *) returns(Future.successful(\/-(conflictingUser)))
        (userRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(updatedTestUser.username, *, *) returns(Future.successful(-\/(RepositoryError.NoResults)))

        val result = authService.update(testUser.id, testUser.version, Some(updatedTestUser.email), Some(updatedTestUser.username), Some(updatedTestUser.givenname), Some(updatedTestUser.surname))
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.UniqueKeyConflict("email", s"The e-mail address ${updatedTestUser.email} is already in use.")))
      }
      "return ServiceError.BadInput if email is not valid" in {
        val testUser = TestValues.testUserA.copy(
          hash = None
        )
        val updatedTestUser = testUser.copy(
          version   = testUser.version + 1,
          email     = "updated_userads aj9879example.com",
          username  = "updated_username",
          givenname = "updated_givenname",
          surname   = "updated_surname"
        )

        (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testUser.id, *, *) returns(Future.successful(\/-(testUser)))
        (userRepository.update(_: User)(_: Connection, _: ScalaCachePool)) when(updatedTestUser, *, *) returns(Future.successful(\/-(updatedTestUser)))

        // Conflicting user
        (userRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(updatedTestUser.email, *, *) returns(Future.successful(-\/(RepositoryError.NoResults)))
        (userRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(updatedTestUser.username, *, *) returns(Future.successful(-\/(RepositoryError.NoResults)))

        val result = authService.update(testUser.id, testUser.version, Some(updatedTestUser.email), Some(updatedTestUser.username), Some(updatedTestUser.givenname), Some(updatedTestUser.surname))
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadInput(s"'${updatedTestUser.email}' is not a valid format")))
      }
      "return ServiceError.BadInput if username is not valid (>= 3 caracters)" in {
        val testUser = TestValues.testUserA.copy(
          hash = None
        )
        val updatedTestUser = testUser.copy(
          version   = testUser.version + 1,
          email     = "updated_user@example.com",
          username  = "up",
          givenname = "updated_givenname",
          surname   = "updated_surname"
        )

        (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testUser.id, *, *) returns(Future.successful(\/-(testUser)))
        (userRepository.update(_: User)(_: Connection, _: ScalaCachePool)) when(updatedTestUser, *, *) returns(Future.successful(\/-(updatedTestUser)))

        // Conflicting user
        (userRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(updatedTestUser.email, *, *) returns(Future.successful(-\/(RepositoryError.NoResults)))
        (userRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(updatedTestUser.username, *, *) returns(Future.successful(-\/(RepositoryError.NoResults)))

        val result = authService.update(testUser.id, testUser.version, Some(updatedTestUser.email), Some(updatedTestUser.username), Some(updatedTestUser.givenname), Some(updatedTestUser.surname))
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadInput(s"Your username must be at least 3 characters.")))
      }
      "return ServiceError.OfflineLockFail if version is wrong" in {
        val testUser = TestValues.testUserA.copy(
          hash = None
        )
        val updatedTestUser = testUser.copy(
          version   = testUser.version + 1,
          email     = "updated_user@example.com",
          username  = "updated_username",
          givenname = "updated_givenname",
          surname   = "updated_surname"
        )

        (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testUser.id, *, *) returns(Future.successful(\/-(testUser)))
        (userRepository.update(_: User)(_: Connection, _: ScalaCachePool)) when(updatedTestUser, *, *) returns(Future.successful(\/-(updatedTestUser)))

        // Conflicting user
        (userRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(updatedTestUser.email, *, *) returns(Future.successful(-\/(RepositoryError.NoResults)))
        (userRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(updatedTestUser.username, *, *) returns(Future.successful(-\/(RepositoryError.NoResults)))

        val result = authService.update(testUser.id, 99L, Some(updatedTestUser.email), Some(updatedTestUser.username), Some(updatedTestUser.givenname), Some(updatedTestUser.surname))
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
    }
  }

  "AuthService.updateIdentifier" should {
    inSequence {
      "update user with unique values" in {
        val testUser = TestValues.testUserA.copy(
          hash = None
        )
        val updatedTestUser = testUser.copy(
          version   = testUser.version + 1,
          email     = "updated_user@example.com",
          username  = "updated_username"
        )

        (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testUser.id, *, *) returns(Future.successful(\/-(testUser)))
        (userRepository.update(_: User)(_: Connection, _: ScalaCachePool)) when(updatedTestUser, *, *) returns(Future.successful(\/-(updatedTestUser)))

        // Conflicting user
        (userRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(updatedTestUser.email, *, *) returns(Future.successful(-\/(RepositoryError.NoResults)))
        (userRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(updatedTestUser.username, *, *) returns(Future.successful(-\/(RepositoryError.NoResults)))

        val result = authService.updateIdentifier(testUser.id, testUser.version, Some(updatedTestUser.email), Some(updatedTestUser.username))
        val eitherUser = Await.result(result, Duration.Inf)
        val \/-(user) = eitherUser

        user.id should be(updatedTestUser.id)
        user.version should be(updatedTestUser.version)
        user.email should be(updatedTestUser.email)
        user.username should be(updatedTestUser.username)
        user.hash should be(None)
        user.givenname should be(updatedTestUser.givenname)
        user.surname should be(updatedTestUser.surname)
      }
      "return RepositoryError.UniqueKeyConflict if username is not unique" in {
        val testUser = TestValues.testUserA.copy(
          hash = None
        )
        val updatedTestUser = testUser.copy(
          version   = testUser.version + 1,
          email     = "updated_user@example.com",
          username  = "updated_username"
        )
        val conflictingUser = TestValues.testUserB

        (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testUser.id, *, *) returns(Future.successful(\/-(testUser)))
        (userRepository.update(_: User)(_: Connection, _: ScalaCachePool)) when(updatedTestUser, *, *) returns(Future.successful(\/-(updatedTestUser)))

        // Conflicting user
        (userRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(updatedTestUser.email, *, *) returns(Future.successful(-\/(RepositoryError.NoResults)))
        (userRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(updatedTestUser.username, *, *) returns(Future.successful(\/-(conflictingUser)))

        val result = authService.updateIdentifier(testUser.id, testUser.version, Some(updatedTestUser.email), Some(updatedTestUser.username))
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.UniqueKeyConflict("username", s"The username ${updatedTestUser.username} is already in use.")))
      }
      "return RepositoryError.UniqueKeyConflict if email is not unique" in {
        val testUser = TestValues.testUserA.copy(
          hash = None
        )
        val updatedTestUser = testUser.copy(
          version   = testUser.version + 1,
          email     = "updated_user@example.com",
          username  = "updated_username"
        )
        val conflictingUser = TestValues.testUserB

        (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testUser.id, *, *) returns(Future.successful(\/-(testUser)))
        (userRepository.update(_: User)(_: Connection, _: ScalaCachePool)) when(updatedTestUser, *, *) returns(Future.successful(\/-(updatedTestUser)))

        // Conflicting user
        (userRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(updatedTestUser.email, *, *) returns(Future.successful(\/-(conflictingUser)))
        (userRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(updatedTestUser.username, *, *) returns(Future.successful(-\/(RepositoryError.NoResults)))

        val result = authService.updateIdentifier(testUser.id, testUser.version, Some(updatedTestUser.email), Some(updatedTestUser.username))
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.UniqueKeyConflict("email", s"The e-mail address ${updatedTestUser.email} is already in use.")))
      }
      "return ServiceError.BadInput if email is not valid" in {
        val testUser = TestValues.testUserA.copy(
          hash = None
        )
        val updatedTestUser = testUser.copy(
          version   = testUser.version + 1,
          email     = "updated_userads aj9879example.com",
          username  = "updated_username"
        )

        (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testUser.id, *, *) returns(Future.successful(\/-(testUser)))
        (userRepository.update(_: User)(_: Connection, _: ScalaCachePool)) when(updatedTestUser, *, *) returns(Future.successful(\/-(updatedTestUser)))

        // Conflicting user
        (userRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(updatedTestUser.email, *, *) returns(Future.successful(-\/(RepositoryError.NoResults)))
        (userRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(updatedTestUser.username, *, *) returns(Future.successful(-\/(RepositoryError.NoResults)))

        val result = authService.updateIdentifier(testUser.id, testUser.version, Some(updatedTestUser.email), Some(updatedTestUser.username))
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadInput(s"'${updatedTestUser.email}' is not a valid format")))
      }
      "return ServiceError.BadInput if username is not valid (>= 3 caracters)" in {
        val testUser = TestValues.testUserA.copy(
          hash = None
        )
        val updatedTestUser = testUser.copy(
          version   = testUser.version + 1,
          email     = "updated_user@example.com",
          username  = "up"
        )

        (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testUser.id, *, *) returns(Future.successful(\/-(testUser)))
        (userRepository.update(_: User)(_: Connection, _: ScalaCachePool)) when(updatedTestUser, *, *) returns(Future.successful(\/-(updatedTestUser)))

        // Conflicting user
        (userRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(updatedTestUser.email, *, *) returns(Future.successful(-\/(RepositoryError.NoResults)))
        (userRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(updatedTestUser.username, *, *) returns(Future.successful(-\/(RepositoryError.NoResults)))

        val result = authService.updateIdentifier(testUser.id, testUser.version, Some(updatedTestUser.email), Some(updatedTestUser.username))
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadInput(s"Your username must be at least 3 characters.")))
      }
      "return ServiceError.OfflineLockFail if version is wrong" in {
        val testUser = TestValues.testUserA.copy(
          hash = None
        )
        val updatedTestUser = testUser.copy(
          version   = testUser.version + 1,
          email     = "updated_user@example.com",
          username  = "updated_username"
        )

        (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testUser.id, *, *) returns(Future.successful(\/-(testUser)))
        (userRepository.update(_: User)(_: Connection, _: ScalaCachePool)) when(updatedTestUser, *, *) returns(Future.successful(\/-(updatedTestUser)))

        // Conflicting user
        (userRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(updatedTestUser.email, *, *) returns(Future.successful(-\/(RepositoryError.NoResults)))
        (userRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(updatedTestUser.username, *, *) returns(Future.successful(-\/(RepositoryError.NoResults)))

        val result = authService.updateIdentifier(testUser.id, 99L, Some(updatedTestUser.email), Some(updatedTestUser.username))
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
    }
  }

  "AuthService.updateInfo" should {
    inSequence {
      "update user with values" in {
        val testUser = TestValues.testUserA.copy(
          hash = None
        )
        val updatedTestUser = testUser.copy(
          version   = testUser.version + 1,
          givenname = "updated_givenname",
          surname   = "updated_surname"
        )

        (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testUser.id, *, *) returns(Future.successful(\/-(testUser)))
        (userRepository.update(_: User)(_: Connection, _: ScalaCachePool)) when(updatedTestUser, *, *) returns(Future.successful(\/-(updatedTestUser)))

        val result = authService.updateInfo(testUser.id, testUser.version, Some(updatedTestUser.givenname), Some(updatedTestUser.surname))
        val eitherUser = Await.result(result, Duration.Inf)
        val \/-(user) = eitherUser

        user.id should be(updatedTestUser.id)
        user.version should be(updatedTestUser.version)
        user.email should be(updatedTestUser.email)
        user.username should be(updatedTestUser.username)
        user.hash should be(None)
        user.givenname should be(updatedTestUser.givenname)
        user.surname should be(updatedTestUser.surname)
      }
      "return ServiceError.OfflineLockFail if version is wrong" in {
        val testUser = TestValues.testUserA.copy(
          hash = None
        )
        val updatedTestUser = testUser.copy(
          version   = testUser.version + 1,
          givenname = "updated_givenname",
          surname   = "updated_surname"
        )

        (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testUser.id, *, *) returns(Future.successful(\/-(testUser)))
        (userRepository.update(_: User)(_: Connection, _: ScalaCachePool)) when(updatedTestUser, *, *) returns(Future.successful(\/-(updatedTestUser)))

        val result = authService.updateInfo(testUser.id, 99L, Some(updatedTestUser.givenname), Some(updatedTestUser.surname))
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
    }
  }


  "AuthService.updatePassword" should {
    inSequence {
      "update user with values" in {
        val testUser = TestValues.testUserA.copy(
          hash = None
        )
        val password = "new pass"
        val updatedTestUser = testUser.copy(
          version   = testUser.version + 1
        )

        (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testUser.id, *, *) returns(Future.successful(\/-(testUser)))
        (userRepository.update(_: User)(_: Connection, _: ScalaCachePool)) when(updatedTestUser, *, *) returns(Future.successful(\/-(updatedTestUser)))

        val result = authService.updatePassword(testUser.id, testUser.version, password)
        val eitherUser = Await.result(result, Duration.Inf)
        val \/-(user) = eitherUser

        user.id should be(updatedTestUser.id)
        user.version should be(updatedTestUser.version)
        user.email should be(updatedTestUser.email)
        user.username should be(updatedTestUser.username)
        user.hash should be(None)
        user.givenname should be(updatedTestUser.givenname)
        user.surname should be(updatedTestUser.surname)
      }
      "return ServiceError.OfflineLockFail if version is wrong" in {
        val testUser = TestValues.testUserA.copy(
          hash = None
        )
        val password = "nes pass"
        val updatedTestUser = testUser.copy(
          version   = testUser.version + 1
        )

        (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testUser.id, *, *) returns(Future.successful(\/-(testUser)))
        (userRepository.update(_: User)(_: Connection, _: ScalaCachePool)) when(updatedTestUser, *, *) returns(Future.successful(\/-(updatedTestUser)))

        val result = authService.updatePassword(testUser.id, 99L, password)
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
      "return ServiceError.OfflineLockFail if password is short" in {
        val testUser = TestValues.testUserA.copy(
          hash = None
        )
        val password = "pass ss"
        val updatedTestUser = testUser.copy(
          version   = testUser.version + 1
        )

        (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testUser.id, *, *) returns(Future.successful(\/-(testUser)))
        (userRepository.update(_: User)(_: Connection, _: ScalaCachePool)) when(updatedTestUser, *, *) returns(Future.successful(\/-(updatedTestUser)))

        val result = authService.updatePassword(testUser.id, testUser.version, password)
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadInput(s"The password provided must be at least 8 characters.")))
      }
    }
  }

  "AuthService.delete" should {
    inSequence {
      "return ServiceError.OfflineLockFail if versions don't match" in {
        val testUser = TestValues.testUserA

        (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testUser.id, *, *) returns(Future.successful(\/-(testUser)))

        val result = authService.delete(testUser.id, 99L)
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
    }
  }

  "AuthService.updateRole" should {
    inSequence {
      "return ServiceError.OfflineLockFail if versions don't match" in {
        val testRole = TestValues.testRoleA

        (roleRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testRole.id, *, *) returns(Future.successful(\/-(testRole)))

        val result = authService.updateRole(testRole.id, 99L, testRole.name)
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
    }
  }

  "AuthService.deleteRole" should {
    inSequence {
      "return ServiceError.OfflineLockFail if versions don't match" in {
        val testRole = TestValues.testRoleA

        (roleRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testRole.id, *, *) returns(Future.successful(\/-(testRole)))

        val result = authService.deleteRole(testRole.id, 99L)
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
    }
  }
}
