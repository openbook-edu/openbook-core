//package services

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

import org.scalatest._
import Matchers._
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

  "AuthService.authenticate" should {
    "return some user if it the identifier and password combination are valid" in {
      (userRepository.find(_: String)) when("testUserA") returns(Future.successful(Some(testUserA)))

      val fSomeUser = authService.authenticate(testUserA.username, password)
      val Some(user) = Await.result(fSomeUser, Duration.Inf)
      user should be (testUserA)
    }
    "return none if the password was wrong" in {
      (userRepository.find(_: String)) when("testUserB") returns(Future.successful(Some(testUserA)))

      val fSomeUser = authService.authenticate("testUserB", "bad password!")
      Await.result(fSomeUser, Duration.Inf) should be (None)
    }
    "return none if the user doesn't exist" in {
      (userRepository.find(_: String)) when("testUserC") returns(Future.successful(None))

      val fSomeUser = authService.authenticate("testUserC", password)
      Await.result(fSomeUser, Duration.Inf) should be (None)
    }
  }
}
