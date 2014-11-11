package services

import com.github.mauricio.async.db.Connection
import scala.concurrent.{Future,ExecutionContext,Await}
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.duration.Duration._
import org.specs2.mutable._
import org.specs2.matcher._
import org.specs2.mock._
import ca.shiftfocus.krispii.core.services._
import ca.shiftfocus.krispii.core.services.datasource._
import ca.shiftfocus.krispii.core.repositories._
import webcrank.password._

trait AuthTestEnvironmentComponent extends
AuthServiceImplComponent with
UserRepositoryComponent with
RoleRepositoryComponent with
SectionRepositoryComponent with
SessionRepositoryComponent with
DB with
Mockito {
  val mockConnection = mock[Connection]
  override def transactional[A](f : Connection => Future[A]): Future[A] = {
    f(mockConnection)
  }
  override val userRepository = mock[UserRepository]
  override val roleRepository = mock[RoleRepository]
  override val sectionRepository = mock[SectionRepository]
  override val sessionRepository = mock[SessionRepository]
  override val db = mock[DBSettings]
  db.pool.returns(mockConnection)

  val webcrank = Passwords.scrypt()
  val username = "sample_user1"
  val email = "sampleuser1@example.org"
  val password = "userpass"
  val passwordHash = webcrank.crypt("userpass")
  val givenname = "Frank"
  val surname = "Studently"

  val mockUser = mock[ca.shiftfocus.krispii.core.models.User]
  mockUser.username.returns(username)
  mockUser.email.returns(email)
  mockUser.passwordHash.returns(Some(passwordHash))
  mockUser.givenname.returns(givenname)
  mockUser.surname.returns(surname)
}

class AuthSpecComponent
  extends Specification
  with org.specs2.specification.Snippets
  with AuthTestEnvironmentComponent
  with Mockito {

  "AuthService.authenticate" should {
    "return some user if it the identifier and password combination are valid" in {
      userRepository.find(username).returns(Future.successful(Some(mockUser)))

      val fSomeUser = authService.authenticate(username, password)
      fSomeUser must beSome(mockUser).await
    }
    "return none if the password was wrong" in {
      userRepository.find(username).returns(Future.successful(Some(mockUser)))

      val fSomeUser = authService.authenticate(username, "bad password!")
      fSomeUser must beNone.await
    }
    "return none if the user doesn't exist" in {
      userRepository.find(username).returns(Future.successful(None))

      val fSomeUser = authService.authenticate(username, password)
      fSomeUser must beNone.await
    }
  }

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
//  }
}
