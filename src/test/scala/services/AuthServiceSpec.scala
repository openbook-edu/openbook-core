package services

import ca.shiftfocus.krispii.core.models.User
import com.github.mauricio.async.db.Connection
import scala.concurrent.{Future,ExecutionContext,Await}
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.duration.Duration._
import org.specs2.mutable.Specification
import org.scalamock.specs2.MockContext
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
MockContext {
  val mockConnection = stub[Connection]
  override def transactional[A](f : Connection => Future[A]): Future[A] = {
    f(mockConnection)
  }
  override val userRepository = mock[UserRepository]
  override val roleRepository = mock[RoleRepository]
  override val sectionRepository = mock[SectionRepository]
  override val sessionRepository = mock[SessionRepository]
  override val db = stub[DBSettings]
  (db.pool _).when().returns(mockConnection)

  val webcrank = Passwords.scrypt()
  val username = "sample_user1"
  val email = "sampleuser1@example.org"
  val password = "userpass"
  val passwordHash = webcrank.crypt("userpass")
  val givenname = "Frank"
  val surname = "Studently"

  val bad_username = "idontexist"
  val bad_email = "nonexistant@example.org"
  val bad_password = "imabadpassword"

  val fakeUser = User(
    username = username,
    email = email,
    passwordHash = Some(passwordHash),
    givenname = givenname,
    surname = surname
  )

  (userRepository.find(_: String)).when(username).returns(Future.successful(Some(fakeUser)))
  (userRepository.find(_: String)).when(email).returns(Future.successful(Some(fakeUser)))
  (userRepository.find(_: String)).when(bad_username).returns(Future.successful(None))
  (userRepository.find(_: String)).when(bad_email).returns(Future.successful(None))
}

class AuthSpecComponent
  extends Specification
  with org.specs2.specification.Snippets
  with AuthTestEnvironmentComponent {

  "AuthService.authenticate" should {
    "return some user if it the identifier and password combination are valid" in {
      val fSomeUser = authService.authenticate(username, password)
      val someUser = Await.result(fSomeUser, Duration.Inf)
      someUser must beSome(fakeUser)
    }
    "return none if the password was wrong" in {
      val fSomeUser = authService.authenticate(username, "bad password!")
      val someUser = Await.result(fSomeUser, Duration.Inf)
      someUser must beNone
    }
    "return none if the user doesn't exist" in {
      val fSomeUser = authService.authenticate(bad_username, password)
      val someUser = Await.result(fSomeUser, Duration.Inf)
      someUser must beNone
    }
  }

  "AuthService.create" should {
    "return a new user if the email and password are unique and the user was created" in {
      val new_username = "a_new_user"
      val new_email = "newemail@example.org"
      val new_password = "newpassword"
      val new_passwordHash = webcrank.crypt(new_password)
      val new_givenname = "New"
      val new_surname = "User"
      val new_fakeUser = User(
        username = new_username,
        email = new_email,
        passwordHash = Some(new_passwordHash),
        givenname = new_givenname,
        surname = new_surname
      )

      userRepository.find(new_username).returns(Future.successful(None))
      userRepository.find(new_email).returns(Future.successful(None))
      userRepository.insert(argThat(===(new_fakeUser)))(argThat(===(implicitly[Connection]))).returns(Future.successful(fakeUser))

      val fNewUser = authService.create(new_username, new_email, new_password, new_givenname, new_surname)
      val newUser = Await.result(fNewUser, Duration.Inf)
      newUser.email must be(new_fakeUser.email)
      newUser.username must be(new_fakeUser.username)
    }
  }
}
