import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.krispii.core.models.User
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.Connection
import scala.concurrent.{Future,ExecutionContext,Await}
import scala.concurrent.duration._
import ca.shiftfocus.krispii.core.services._
import ca.shiftfocus.krispii.core.services.datasource._
import ca.shiftfocus.krispii.core.repositories._

import org.scalatest._
import Matchers._ // Is used for "should be and etc."
import scalaz.{-\/, \/, \/-}

class AuthServiceSpec
  extends TestEnvironment {
  // Create stubs of AuthService's dependencies
  val db = stub[DB]
  val mockConnection = stub[Connection]
  val userRepository = stub[UserRepository]
  val roleRepository = stub[RoleRepository]
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

//  "AuthService.update" should {
//    inSequence {
//      "update user with unique values" in {
//        val testUser = TestValues.testUserA
//        val testRoleList = Vector(
//          TestValues.testRoleA
//        )
//        val values = Map(
//          "email" -> testUser.email,
//          "username" -> testUser.username
//        )
//        // Mock authService.find
//        (userRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(testUser.username, *, *) returns(Future.successful(-\/(RepositoryError.NoResults)))
//        (roleRepository.list(_: User)(_: Connection, _: ScalaCachePool)) when(testUser, *, *) returns(Future.successful(\/.right(testRoleList)))
//
//        // Conflicting user
//        (userRepository.find(_: String)) when(testUserA.email) returns(Future.successful(Some(testUserA)))
//        (userRepository.find(_: String)) when(testUserA.username) returns(Future.successful(Some(testUserA)))
//
//        (userRepository.update(_: User)(_: Connection)) when(testUserA, mockConnection) returns(Future.successful(testUserA))
//
//        val fNewUser = authService.update(testUserA.id, testUserA.version, values)
//
//        Await.result(fNewUser, Duration.Inf) should be (testUserA)
//      }
//      "throw an exception if username is not unique" in {
//        // Mock authService.find
//        (userRepository.find(_: UUID)) when(testUserA.id) returns(Future.successful(Some(testUserA)))
//        (roleRepository.list(_: User)) when(testUserA) returns(Future.successful(indexedRole))
//        (classRepository.list(_: User, _: Boolean)) when(testUserA, false) returns(Future.successful(indexedClass))
//
//        // Conflicting user
//        (userRepository.find(_: String)) when(testUserA.email) returns(Future.successful(Some(testUserA)))
//        (userRepository.find(_: String)) when(testUserA.username) returns(Future.successful(Some(testUserB)))
//
//        (userRepository.update(_: User)(_: Connection)) when(testUserA, mockConnection) returns(Future.successful(testUserA))
//
//        val fNewUser = authService.update(testUserA.id, testUserA.version, values)
//
//        an [UsernameAlreadyExistsException] should be thrownBy Await.result(fNewUser, Duration.Inf)
//      }
//      "throw an exception if email is not unique" in {
//        // Mock authService.find
//        (userRepository.find(_: UUID)) when(testUserA.id) returns(Future.successful(Some(testUserA)))
//        (roleRepository.list(_: User)) when(testUserA) returns(Future.successful(indexedRole))
//        (classRepository.list(_: User, _: Boolean)) when(testUserA, false) returns(Future.successful(indexedClass))
//
//        // Conflicting user
//        (userRepository.find(_: String)) when(testUserA.email) returns(Future.successful(Some(testUserB)))
//        (userRepository.find(_: String)) when(testUserA.username) returns(Future.successful(Some(testUserA)))
//
//        (userRepository.update(_: User)(_: Connection)) when(testUserA, mockConnection) returns(Future.successful(testUserA))
//
//        val fNewUser = authService.update(testUserA.id, testUserA.version, values)
//
//        an [EmailAlreadyExistsException] should be thrownBy Await.result(fNewUser, Duration.Inf)
//      }
//      "return if email is not valid" in {
//
//      }
//      "return if username is not valid (>= 3 caracters)" in {
//
//      }
//      "return if User id to update is not matching" in {
//
//      }
//    }
//  }
//
//  "AuthService.deleteRole" should {
//    inSequence {
//      "throw an exception if role versions don't match" in {
//        (roleRepository.find(_: UUID)) when(testRoleA.id) returns(Future.successful(Option(testRoleA)))
//
//        val fNewUser = authService.deleteRole(testRoleA.id, 123456789L)
//        an [OutOfDateException] should be thrownBy Await.result(fNewUser, Duration.Inf)
//      }
//    }
//  }
}
