//package services //Need to be commented to run the tests

import ca.shiftfocus.krispii.core.lib.UUID
import ca.shiftfocus.krispii.core.models._
import com.github.mauricio.async.db.Connection
import scala.concurrent.{Future,ExecutionContext,Await}
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.duration.Duration._
import ca.shiftfocus.krispii.core.services._
import ca.shiftfocus.krispii.core.services.datasource._
import ca.shiftfocus.krispii.core.repositories._
import grizzled.slf4j.Logger
import webcrank.password._

import org.scalatest._
import Matchers._ // Is used for "should be and etc."
import org.scalamock.scalatest.MockFactory

trait AuthTestEnvironmentComponent extends
AuthServiceImplComponent with
UserRepositoryComponent with
RoleRepositoryComponent with
SectionRepositoryComponent with
SessionRepositoryComponent with
DB

class AuthServiceSpec
  extends WordSpec
  with MockFactory
  with AuthTestEnvironmentComponent {

  val logger = Logger[this.type]
  val mockConnection = stub[Connection]
  override def transactional[A](f : Connection => Future[A]): Future[A] = {
    f(mockConnection)
  }

  override val userRepository = stub[UserRepository]
  override val roleRepository = stub[RoleRepository]
  override val sectionRepository = stub[SectionRepository]
  override val sessionRepository = stub[SessionRepository]

  override val db = stub[DBSettings]

  (db.pool _) when() returns(mockConnection)

  val webcrank = Passwords.scrypt()
  val password = "userpass"
  val passwordHash = webcrank.crypt("userpass")

  val testUserA = User(
    email = "testUserA@example.org",
    username = "testUserA",
    passwordHash = Some(passwordHash),
    givenname = "Test",
    surname = "UserA"
  )

  val testUserB = User(
    email = "testUserA@example.org",
    username = "testUserA",
    passwordHash = Some(passwordHash),
    givenname = "Test",
    surname = "UserA"
  )

  val testRoleA = Role(
    name = "Role name A"
  )

  val testSectionA = Section(
    courseId = testUserA.id,
    teacherId = Option(testUserA.id),
    name = "Role name A"
  )

  "AuthService.authenticate" should {
    inSequence {
      "return some user if it the identifier and password combination are valid" in {
        (userRepository.find(_: String)) when(testUserA.username) returns(Future.successful(Some(testUserA)))

        val fSomeUser = authService.authenticate(testUserA.username, password)
        val Some(user) = Await.result(fSomeUser, Duration.Inf)
        user should be (testUserA)
      }
      "return none if the password was wrong" in {
        (userRepository.find(_: String)) when(testUserA.username) returns(Future.successful(Some(testUserA)))

        val fSomeUser = authService.authenticate(testUserA.username, "bad password!")
        Await.result(fSomeUser, Duration.Inf) should be (None)
      }
      "return none if the user doesn't exist" in {
        (userRepository.find(_: String)) when(testUserA.username) returns(Future.successful(None))

        val fSomeUser = authService.authenticate(testUserA.username, password)
        Await.result(fSomeUser, Duration.Inf) should be (None)
      }
    }
  }

  "AuthService.create" should {
    inSequence {
      "return a new user if the email and password are unique and the user was created" in {
        (userRepository.find(_: String)) when(testUserA.username) returns(Future.successful(None))
        (userRepository.find(_: String)) when(testUserA.email) returns(Future.successful(None))
        (userRepository.insert(_: User)(_: Connection)) when(testUserA, mockConnection) returns(Future.successful(testUserA))

        val fNewUser = authService.create(testUserA.username, testUserA.email, password, testUserA.givenname, testUserA.surname)
        Await.result(fNewUser, Duration.Inf) should be (testUserA)
      }
      "throw an exception if email is not unique" in {
        (userRepository.find(_: String)) when(testUserA.username) returns(Future.successful(None))
        (userRepository.find(_: String)) when(testUserA.email) returns(Future.successful(Some(testUserA)))

        val fNewUser = authService.create(testUserA.username, testUserA.email, password, testUserA.givenname, testUserA.surname)
        an [EmailAlreadyExistsException] should be thrownBy Await.result(fNewUser, Duration.Inf)
      }
      "throw an exception if username is not unique" in {
        (userRepository.find(_: String)) when(testUserA.username) returns(Future.successful(Some(testUserA)))
        (userRepository.find(_: String)) when(testUserA.email) returns(Future.successful(None))

        val fNewUser = authService.create(testUserA.username, testUserA.email, password, testUserA.givenname, testUserA.surname)
        an [UsernameAlreadyExistsException] should be thrownBy Await.result(fNewUser, Duration.Inf)
      }
      "throw an exception if both username and email are not unique" in {
        (userRepository.find(_: String)) when(testUserA.username) returns(Future.successful(Some(testUserA)))
        (userRepository.find(_: String)) when(testUserA.email) returns(Future.successful(Some(testUserA)))

        val fNewUser = authService.create(testUserA.username, testUserA.email, password, testUserA.givenname, testUserA.surname)
        an [EmailAndUsernameAlreadyExistException] should be thrownBy Await.result(fNewUser, Duration.Inf)
      }
    }
  }

  "AuthService.update" should {
    val indexedRole = Vector(testRoleA)
    val indexedSection = Vector(testSectionA)
    val values: Map[String, String] = Map("email" -> testUserA.email, "username" -> testUserA.username)

    inSequence {
      "update user with unique values" in {
        // Mock authService.find
        (userRepository.find(_: UUID)) when(testUserA.id) returns(Future.successful(Some(testUserA)))
        (roleRepository.list(_: User)) when(testUserA) returns(Future.successful(indexedRole))
        (sectionRepository.list(_: User, _: Boolean)) when(testUserA, false) returns(Future.successful(indexedSection))

        // Conflicting user
        (userRepository.find(_: String)) when(testUserA.email) returns(Future.successful(Some(testUserA)))
        (userRepository.find(_: String)) when(testUserA.username) returns(Future.successful(Some(testUserA)))

        (userRepository.update(_: User)(_: Connection)) when(testUserA, mockConnection) returns(Future.successful(testUserA))

        val fNewUser = authService.update(testUserA.id, testUserA.version, values)

        Await.result(fNewUser, Duration.Inf) should be (testUserA)
      }
      "throw an exception if username is not unique" in {
        // Mock authService.find
        (userRepository.find(_: UUID)) when(testUserA.id) returns(Future.successful(Some(testUserA)))
        (roleRepository.list(_: User)) when(testUserA) returns(Future.successful(indexedRole))
        (sectionRepository.list(_: User, _: Boolean)) when(testUserA, false) returns(Future.successful(indexedSection))

        // Conflicting user
        (userRepository.find(_: String)) when(testUserA.email) returns(Future.successful(Some(testUserA)))
        (userRepository.find(_: String)) when(testUserA.username) returns(Future.successful(Some(testUserB)))

        (userRepository.update(_: User)(_: Connection)) when(testUserA, mockConnection) returns(Future.successful(testUserA))

        val fNewUser = authService.update(testUserA.id, testUserA.version, values)

        an [UsernameAlreadyExistsException] should be thrownBy Await.result(fNewUser, Duration.Inf)
      }
      "throw an exception if email is not unique" in {
        // Mock authService.find
        (userRepository.find(_: UUID)) when(testUserA.id) returns(Future.successful(Some(testUserA)))
        (roleRepository.list(_: User)) when(testUserA) returns(Future.successful(indexedRole))
        (sectionRepository.list(_: User, _: Boolean)) when(testUserA, false) returns(Future.successful(indexedSection))

        // Conflicting user
        (userRepository.find(_: String)) when(testUserA.email) returns(Future.successful(Some(testUserB)))
        (userRepository.find(_: String)) when(testUserA.username) returns(Future.successful(Some(testUserA)))

        (userRepository.update(_: User)(_: Connection)) when(testUserA, mockConnection) returns(Future.successful(testUserA))

        val fNewUser = authService.update(testUserA.id, testUserA.version, values)

        an [EmailAlreadyExistsException] should be thrownBy Await.result(fNewUser, Duration.Inf)
      }
      "throw an exception if both username and email are not unique" in {
        // Mock authService.find
        (userRepository.find(_: UUID)) when(testUserA.id) returns(Future.successful(Some(testUserA)))
        (roleRepository.list(_: User)) when(testUserA) returns(Future.successful(indexedRole))
        (sectionRepository.list(_: User, _: Boolean)) when(testUserA, false) returns(Future.successful(indexedSection))

        // Conflicting user
        (userRepository.find(_: String)) when(testUserA.email) returns(Future.successful(Some(testUserB)))
        (userRepository.find(_: String)) when(testUserA.username) returns(Future.successful(Some(testUserB)))

        (userRepository.update(_: User)(_: Connection)) when(testUserA, mockConnection) returns(Future.successful(testUserA))

        val fNewUser = authService.update(testUserA.id, testUserA.version, values)

        an [EmailAndUsernameAlreadyExistException] should be thrownBy Await.result(fNewUser, Duration.Inf)
      }
    }
  }

  "AuthService.deleteRole" should {
    inSequence {
      "throw an exception if role versions don't match" in {
        (roleRepository.find(_: UUID)) when(testRoleA.id) returns(Future.successful(Option(testRoleA)))

        val fNewUser = authService.deleteRole(testRoleA.id, 123456789L)
        an [OutOfDateException] should be thrownBy Await.result(fNewUser, Duration.Inf)
      }
    }
  }
}
