//package services //Need to be commented to run the tests
//
//import ca.shiftfocus.krispii.core.error._
//import ca.shiftfocus.uuid.UUID
//import java.awt.Color
//import ca.shiftfocus.krispii.core.models._
//import com.github.mauricio.async.db.Connection
//import scala.concurrent.{Future,ExecutionContext,Await}
//import ExecutionContext.Implicits.global
//import scala.concurrent.duration._
//import scala.concurrent.duration.Duration._
//import ca.shiftfocus.krispii.core.services._
//import ca.shiftfocus.krispii.core.services.datasource._
//import ca.shiftfocus.krispii.core.repositories._
//import grizzled.slf4j.Logger
//import webcrank.password._
//
//import org.scalatest._
//import Matchers._ // Is used for "should be and etc."
//import org.scalamock.scalatest.MockFactory
//import scalaz.{\/, \/-, -\/}
//
//class AuthServiceSpec
//  extends WordSpec
//  with MockFactory {
//
//  val logger = Logger[this.type]
//  val mockConnection = stub[Connection]
//
//  // Create stubs of AuthService's dependencies
//  val userRepository = stub[UserRepository]
//  val roleRepository = stub[RoleRepository]
//  val sessionRepository = stub[SessionRepository]
//
//  // Create a real instance of AuthService for testing
//  val authService = new AuthServiceDefault(mockConnection, userRepository, roleRepository, sessionRepository) {
//    override implicit def conn: Connection = mockConnection
//
//    override def transactional[A](f: Connection => Future[A]): Future[A] = {
//      f(mockConnection)
//    }
//  }
//
//  implicit def conn: Connection = mockConnection
//
//  val webcrank = Passwords.scrypt()
//  val password = "userpass"
//  val passwordHash = webcrank.crypt(password)
//
//  val testUserA = User(
//    email = "testUserA@example.org",
//    username = "testUserA",
//    hash = Some(passwordHash),
//    givenname = "Test",
//    surname = "UserA"
//  )
//
//  val testUserB = User(
//    email = "testUserA@example.org",
//    username = "testUserA",
//    hash = Some(passwordHash),
//    givenname = "Test",
//    surname = "UserA"
//  )
//
//  val testRoleA = Role(
//    name = "Role name A"
//  )
//
//  val testClassA = Course(
//    teacherId = testUserA.id,
//    name = "Class name A",
//    color = new Color(24, 6, 8)
//  )
//
//  "AuthService.authenticate" should {
//    inSequence {
//      "return a user if the identifier and password combination are valid" in {
//        (userRepository.find(_: String)) when (testUserA.username) returns (Future.successful(\/-(testUserA)))
//
//        val fSomeUser = authService.authenticate(testUserA.username, password)
//        val \/-(user) = Await.result(fSomeUser, Duration.Inf)
//        user should be(testUserA)
//      }
//      "return AuthFail if the password was wrong" in {
//        (userRepository.find(_: String)) when (testUserA.username) returns (Future.successful(\/-(testUserA)))
//
//        val fSomeUser = authService.authenticate(testUserA.username, "bad password!")
//        Await.result(fSomeUser, Duration.Inf) should be(-\/(ServiceError.BadPermissions("The password was invalid.")))
//      }
//      "return RepositoryError.NoResults if the user doesn't exist" in {
//        (userRepository.find(_: String)) when (testUserA.username) returns (Future.successful(-\/(RepositoryError.NoResults("Could not find a user."))))
//
//        val fSomeUser = authService.authenticate(testUserA.username, password)
//        Await.result(fSomeUser, Duration.Inf) should be(-\/(RepositoryError.NoResults("Could not find a user.")))
//      }
//    }
//  }
//}
//  "AuthService.create" should {
//    inSequence {
//      "return a new user if the email and password are unique and the user was created" in {
//        (userRepository.find(_: String)) when(testUserA.username) returns(Future.successful(-\/(RepositoryError.NoResults(""))))
//        (userRepository.find(_: String)) when(testUserA.email) returns(Future.successful(-\/(RepositoryError.NoResults(""))))
//        (userRepository.insert(_: User)(_: Connection)) when(testUserA, mockConnection) returns(Future.successful(\/-(testUserA)))
//
//        val fNewUser = authService.create(testUserA.username, testUserA.email, password, testUserA.givenname, testUserA.surname,testUserA.id)
//        val \/-(newUser) = Await.result(fNewUser, Duration.Inf)
//        newUser should be (UserInfo(testUserA, Vector(), Vector()))
//      }
//      "return BadInput if the email is of an invalid format" in {
//        val badEmail = "not@an@email.com"
//        val fNewUser = authService.create(testUserA.username, badEmail, password, testUserA.givenname, testUserA.surname,testUserA.id)
//        Await.result(fNewUser, Duration.Inf) should be(-\/(ServiceError.BadInput(s"$badEmail is not a valid e-mail format.")))
//      }
//      "return BadInput if the username is of an invalid format" in {
//        (userRepository.find(_: String)) when(testUserA.email) returns(Future.successful(-\/(RepositoryError.NoResults(""))))
//
//        val badUsername = "al"
//        val fNewUser = authService.create(badUsername, testUserA.email, password, testUserA.givenname, testUserA.surname,testUserA.id)
//        Await.result(fNewUser, Duration.Inf) should be(-\/(ServiceError.BadInput(s"$badUsername is not a valid format.")))
//      }
//      "return BadInput if the password is shorter than 8 characters" in {
//        (userRepository.find(_: String)) when(testUserA.username) returns(Future.successful(-\/(RepositoryError.NoResults(""))))
//        (userRepository.find(_: String)) when(testUserA.email) returns(Future.successful(-\/(RepositoryError.NoResults(""))))
//        (userRepository.insert(_: User)(_: Connection)) when(testUserA, mockConnection) returns(Future.successful(\/-(testUserA)))
//
//        val fNewUser = authService.create(testUserA.username, testUserA.email, "2short", testUserA.givenname, testUserA.surname,testUserA.id)
//        Await.result(fNewUser, Duration.Inf) should be (-\/(ServiceError.BadInput("The password provided must be at least 8 characters.")))
//      }
//      "return EntityUniqueFieldError if email is not unique" in {
//        (userRepository.find(_: String)) when(testUserA.username) returns(Future.successful(-\/(RepositoryError.NoResults(""))))
//        (userRepository.find(_: String)) when(testUserA.email) returns(Future.successful(\/-(testUserA)))
//
//        val fNewUser = authService.create(testUserA.username, testUserA.email, password, testUserA.givenname, testUserA.surname)
//        Await.result(fNewUser, Duration.Inf) should be (-\/(RepositoryError.UniqueKeyConflict(s"The e-mail address ${testUserA.email} is already in use.")))
//      }
//      "return EntityUniqueFieldError if username is not unique" in {
//        (userRepository.find(_: String)) when(testUserA.username) returns(Future.successful(\/-(testUserA)))
//        (userRepository.find(_: String)) when(testUserA.email) returns(Future.successful(-\/(RepositoryError.NoResults(""))))
//
//        val fNewUser = authService.create(testUserA.username, testUserA.email, password, testUserA.givenname, testUserA.surname)
//        Await.result(fNewUser, Duration.Inf) should be (-\/(RepositoryError.UniqueKeyConflict(s"The username ${testUserA.username} is already in use.")))
//      }
//    }
//  }
////
////  "AuthService.update" should {
////    val indexedRole = Vector(testRoleA)
////    val indexedClass = Vector(testClassA)
////    val values: Map[String, String] = Map("email" -> testUserA.email, "username" -> testUserA.username)
////
////    inSequence {
////      "update user with unique values" in {
////        // Mock authService.find
////        (userRepository.find(_: UUID)) when(testUserA.id) returns(Future.successful(Some(testUserA)))
////        (roleRepository.list(_: User)) when(testUserA) returns(Future.successful(indexedRole))
////        (classRepository.list(_: User, _: Boolean)) when(testUserA, false) returns(Future.successful(indexedClass))
////
////        // Conflicting user
////        (userRepository.find(_: String)) when(testUserA.email) returns(Future.successful(Some(testUserA)))
////        (userRepository.find(_: String)) when(testUserA.username) returns(Future.successful(Some(testUserA)))
////
////        (userRepository.update(_: User)(_: Connection)) when(testUserA, mockConnection) returns(Future.successful(testUserA))
////
////        val fNewUser = authService.update(testUserA.id, testUserA.version, values)
////
////        Await.result(fNewUser, Duration.Inf) should be (testUserA)
////      }
////      "throw an exception if username is not unique" in {
////        // Mock authService.find
////        (userRepository.find(_: UUID)) when(testUserA.id) returns(Future.successful(Some(testUserA)))
////        (roleRepository.list(_: User)) when(testUserA) returns(Future.successful(indexedRole))
////        (classRepository.list(_: User, _: Boolean)) when(testUserA, false) returns(Future.successful(indexedClass))
////
////        // Conflicting user
////        (userRepository.find(_: String)) when(testUserA.email) returns(Future.successful(Some(testUserA)))
////        (userRepository.find(_: String)) when(testUserA.username) returns(Future.successful(Some(testUserB)))
////
////        (userRepository.update(_: User)(_: Connection)) when(testUserA, mockConnection) returns(Future.successful(testUserA))
////
////        val fNewUser = authService.update(testUserA.id, testUserA.version, values)
////
////        an [UsernameAlreadyExistsException] should be thrownBy Await.result(fNewUser, Duration.Inf)
////      }
////      "throw an exception if email is not unique" in {
////        // Mock authService.find
////        (userRepository.find(_: UUID)) when(testUserA.id) returns(Future.successful(Some(testUserA)))
////        (roleRepository.list(_: User)) when(testUserA) returns(Future.successful(indexedRole))
////        (classRepository.list(_: User, _: Boolean)) when(testUserA, false) returns(Future.successful(indexedClass))
////
////        // Conflicting user
////        (userRepository.find(_: String)) when(testUserA.email) returns(Future.successful(Some(testUserB)))
////        (userRepository.find(_: String)) when(testUserA.username) returns(Future.successful(Some(testUserA)))
////
////        (userRepository.update(_: User)(_: Connection)) when(testUserA, mockConnection) returns(Future.successful(testUserA))
////
////        val fNewUser = authService.update(testUserA.id, testUserA.version, values)
////
////        an [EmailAlreadyExistsException] should be thrownBy Await.result(fNewUser, Duration.Inf)
////      }
////      "throw an exception if both username and email are not unique" in {
////        // Mock authService.find
////        (userRepository.find(_: UUID)) when(testUserA.id) returns(Future.successful(Some(testUserA)))
////        (roleRepository.list(_: User)) when(testUserA) returns(Future.successful(indexedRole))
////        (classRepository.list(_: User, _: Boolean)) when(testUserA, false) returns(Future.successful(indexedClass))
////
////        // Conflicting user
////        (userRepository.find(_: String)) when(testUserA.email) returns(Future.successful(Some(testUserB)))
////        (userRepository.find(_: String)) when(testUserA.username) returns(Future.successful(Some(testUserB)))
////
////        (userRepository.update(_: User)(_: Connection)) when(testUserA, mockConnection) returns(Future.successful(testUserA))
////
////        val fNewUser = authService.update(testUserA.id, testUserA.version, values)
////
////        an [EmailAndUsernameAlreadyExistException] should be thrownBy Await.result(fNewUser, Duration.Inf)
////      }
////    }
////  }
////
////  "AuthService.deleteRole" should {
////    inSequence {
////      "throw an exception if role versions don't match" in {
////        (roleRepository.find(_: UUID)) when(testRoleA.id) returns(Future.successful(Option(testRoleA)))
////
////        val fNewUser = authService.deleteRole(testRoleA.id, 123456789L)
////        an [OutOfDateException] should be thrownBy Await.result(fNewUser, Duration.Inf)
////      }
////    }
////  }
//}
