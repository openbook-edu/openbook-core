package services

import ca.shiftfocus.krispii.core.models.User
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

import org.scalatest.WordSpec
import org.scalamock.scalatest.MockFactory

trait AuthTestEnvironmentComponent extends
AuthServiceImplComponent with
UserRepositoryComponent with
RoleRepositoryComponent with
SectionRepositoryComponent with
SessionRepositoryComponent with
DB

class AuthSpecComponent
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

  "AuthService.authenticate" should {
    "return some user if it the identifier and password combination are valid" in {
      (userRepository.find(_: String)) when("testUserA") returns(Future.successful(Some(testUserA)))

      val fSomeUser = authService.authenticate(testUserA.username, password)
      val Some(user) = Await.result(fSomeUser, Duration.Inf)
      assert(user == testUserA)
    }
  }
//    "return none if the password was wrong" in {
//      userRepository.find(username).returns(Future.successful(Some(mockUser)))
//
//      val fSomeUser = authService.authenticate(username, "bad password!")
//      fSomeUser must beNone.await
//    }
//    "return none if the user doesn't exist" in {
//      userRepository.find("idonotexist").returns(Future.successful(None))
//      val fSomeUser = authService.authenticate("idonotexist", password)
//      val userOption = Await.result(fSomeUser, Duration.Inf)
//      userOption must beNone
//    }

//  "AuthService.create" should {
//    "return a new user if the email and password are unique and the user was created" in {
//      running(FakeApplication()) {
//        userRepository.find(username).returns(Future.successful(None))
//        userRepository.find(email).returns(Future.successful(None))
//        userRepository.insert(mockUser)(mockConnection).returns(Future.successful(mockUser))
//
//        val fNewUser = authService.create(username, email, password, givenname, surname)
//        await(fNewUser) must be(mockUser)
//      }
//    }
//    "throw an exception if both username and email are not unique" in {
//      userRepository.find(username).returns(Future.successful(Some(mockUser)))
//      userRepository.find(email).returns(Future.successful(Some(mockUser)))
//
//      val fNewUser = authService.create(username, email, password, givenname, surname)
//      val newUser = Await.result(fNewUser, Duration.Inf)
//      newUser must throwA[EmailAndUsernameAlreadyExistException]
//    }
  
}
