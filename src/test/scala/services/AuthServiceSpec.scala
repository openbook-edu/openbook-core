package services

import ca.shiftfocus.krispii.core.models._
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
import grizzled.slf4j.Logger
import webcrank.password._

trait AuthTestEnvironmentComponent extends
AuthServiceImplComponent with
UserRepositoryComponent with
RoleRepositoryComponent with
SectionRepositoryComponent with
SessionRepositoryComponent with
DB with
Mockito {
  val logger = Logger[this.type]
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

  val mockRole = mock[ca.shiftfocus.krispii.core.models.Role]
  val mockSection = mock[ca.shiftfocus.krispii.core.models.Section]

  val indexedRole: IndexedSeq[Role] = for { s <- mockRole } yield mockRole
  val indexedSection: IndexedSeq[Section] = for { s <- mockSection } yield mockSection

}

class AuthSpecComponent
  extends Specification
  with org.specs2.specification.Snippets
  with AuthTestEnvironmentComponent
  with Mockito {

//  "AuthService.authenticate" should {
//    "return some user if it the identifier and password combination are valid" in {
//      userRepository.find(username).returns(Future.successful(Some(mockUser)))
//
//      val fSomeUser = authService.authenticate(username, password)
//      fSomeUser must beSome(mockUser).await
//    }
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
//  }

//  "AuthService.create" should {
//    "return a new user if the email and password are unique and the user was created" in {
//      userRepository.find(username).returns(Future.successful(None))
//      userRepository.find(email).returns(Future.successful(None))
//      userRepository.insert(mockUser)(mockConnection).returns(Future.successful(mockUser))
//
//      val fNewUser = authService.create(username, email, password, givenname, surname)
//      val newUser = Await.result(fNewUser, Duration.Inf)
//      newUser must be(mockUser)
//    }
//    "throw an exception if email is not unique" in {
//      userRepository.find("bla1").returns(Future.successful(None))
//      userRepository.find("email1").returns(Future.successful(Some(mockUser)))
//
//      val fNewUser = authService.create("bla1", "email1", password, givenname, surname)
//      Await.result(fNewUser, Duration.Inf) must throwA[EmailAlreadyExistsException]
//    }
//    "throw an exception if username is not unique" in {
//      userRepository.find("bla2").returns(Future.successful(Some(mockUser)))
//      userRepository.find("email2").returns(Future.successful(None))
//
//      val fNewUser = authService.create("bla2", "email2", password, givenname, surname)
//      Await.result(fNewUser, Duration.Inf) must throwA[UsernameAlreadyExistsException]
//    }
//    "throw an exception if both username and email are not unique" in {
//      userRepository.find("bla3").returns(Future.successful(Some(mockUser)))
//      userRepository.find("email3").returns(Future.successful(Some(mockUser)))
//
//      val fNewUser = authService.create("bla3", "email3", password, givenname, surname)
//      Await.result(fNewUser, Duration.Inf) must throwA[EmailAndUsernameAlreadyExistException]
//    }
//  }

  "AuthService.update" should {
    "update user" in {
      // mock AuthServiceImplComponent.find
      userRepository.find(mockUser.id).returns(Future(Option(mockUser)))
      roleRepository.list(mockUser).returns(Future.successful(indexedRole))
      sectionRepository.list(mockUser).returns(Future.successful(indexedSection))

      authService.find(mockUser.id).returns(Future.successful(Some(UserInfo(mockUser, indexedRole, indexedSection))))

//      var value:Map[String, String] = Map("email" -> mockUser.email)
      var value:Map[String, String] = Map()
      value += ("email" -> mockUser.email)
      val fNewUser = authService.update(mockUser.id, mockUser.version, value)

      Await.result(fNewUser, Duration.Inf) must throwA[EmailAndUsernameAlreadyExistException]
    }
  }

  "AuthService.delete" should {
    "delete user" in {
      val fuserDelete = authService.delete(mockUser.id, mockUser.version)
      Await.result(fuserDelete, Duration.Inf) must beTrue
    }
  }

  "AuthService.listRoles" should {
    "return Future[IndexedSeq[Role]]" in {
      userRepository.find(mockUser.id).returns(Future(Option(mockUser)))
      roleRepository.list(mockUser).returns(Future.successful(indexedRole))

      val fuserRoles = authService.listRoles(mockUser.id)
//      Await.result(fuserRoles, Duration.Inf) must // ?? Future[IndexedSeq[Role]]
    }
  }

  "AuthService.findRole" should {
    "return Future[IndexedSeq[Role]]" in {
      roleRepository.find(mockUser.id).returns(Future[Option[mockRole]]) // Why Future[Option[Any]]?

      val fuserRoles = authService.listRoles(mockUser.id)
//      Await.result(fuserRoles, Duration.Inf) must // ?? Future[IndexedSeq[Role]]
    }
  }
}
