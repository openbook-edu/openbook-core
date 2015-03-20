import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.{Role, User}
import ca.shiftfocus.uuid.UUID
import scala.collection.immutable.TreeMap
import scala.concurrent.Await
import scala.concurrent.duration._
import ca.shiftfocus.krispii.core.repositories.UserRepositoryPostgres
import org.scalatest._
import Matchers._
import collection.breakOut
import scalaz.{-\/, \/-}


class UserRepositorySpec
  extends TestEnvironment
{
  val userRepository = new UserRepositoryPostgres
  val users_table = userRepository.Table
  val users_fields = userRepository.Fields

  "UserRepository.list" should {
    inSequence {
      "list all users" in {
        val testUserList = TreeMap[Int, User](
          0 -> TestValues.testUserA.copy(hash = None),
          1 -> TestValues.testUserB.copy(hash = None),
          2 -> TestValues.testUserC.copy(hash = None),
          3 -> TestValues.testUserE.copy(hash = None),
          4 -> TestValues.testUserF.copy(hash = None),
          5 -> TestValues.testUserG.copy(hash = None),
          6 -> TestValues.testUserH.copy(hash = None)
        )

        val result = userRepository.list(conn)
        val eitherUsers = Await.result(result, Duration.Inf)
        val \/-(users) = eitherUsers

        testUserList.foreach {
          case (key, user: User) => {
            users(key).id should be(user.id)
            users(key).version should be(user.version)
            users(key).email should be(user.email)
            users(key).username should be(user.username)
            users(key).hash should be(None)
            users(key).givenname should be(user.givenname)
            users(key).surname should be(user.surname)
            users(key).createdAt.toString should be(user.createdAt.toString)
            users(key).updatedAt.toString should be(user.updatedAt.toString)
          }
        }

        users.size should be(testUserList.size)
      }
      "list users with a specified set of user Ids" in {
        val testUserList = TreeMap[Int, User](
          0 -> TestValues.testUserC.copy(hash = None),
          1 -> TestValues.testUserA.copy(hash = None)
        )

        val result = userRepository.list(testUserList.map(_._2.id)(breakOut))
        val eitherUsers = Await.result(result, Duration.Inf)
        val \/-(users) = eitherUsers

        testUserList.foreach {
          case (key, user: User) => {
            users(key).id should be(user.id)
            users(key).version should be(user.version)
            users(key).email should be(user.email)
            users(key).username should be(user.username)
            users(key).hash should be(None)
            users(key).givenname should be(user.givenname)
            users(key).surname should be(user.surname)
            users(key).createdAt.toString should be(user.createdAt.toString)
            users(key).updatedAt.toString should be(user.updatedAt.toString)
          }
        }

        users.size should be(testUserList.size)
      }
      "return RepositoryError.NoResults if set contains unexisting user ID" in {
        val ids = Vector(
          TestValues.testUserA.id,
          UUID("a5caac60-8fd7-4ecc-8fd3-f84dc11355f1")
        )

        val result = userRepository.list(ids)

        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
      "List all users who have a role" in {
        val testRole = TestValues.testRoleA

        val testUserList = TreeMap[Int, User](
          0 -> TestValues.testUserA.copy(hash = None),
          1 -> TestValues.testUserB.copy(hash = None)
        )

        val result = userRepository.list(testRole)
        val eitherUsers = Await.result(result, Duration.Inf)
        val \/-(users) = eitherUsers

        testUserList.foreach {
          case (key, user: User) => {
            users(key).id should be(user.id)
            users(key).version should be(user.version)
            users(key).email should be(user.email)
            users(key).username should be(user.username)
            users(key).hash should be(None)
            users(key).givenname should be(user.givenname)
            users(key).surname should be(user.surname)
            users(key).createdAt.toString should be(user.createdAt.toString)
            users(key).updatedAt.toString should be(user.updatedAt.toString)
          }
        }

        users.size should be(testUserList.size)
      }
      "return empty Vector() if role doesn't exist" in {
        val unexistingRole = Role(
          name = "unexisting role name"
        )

        val result = userRepository.list(unexistingRole)
        Await.result(result, Duration.Inf) should be (\/- (Vector()))
      }
      "return empty Vector() if there are no users that have this role" in {
        val testRole = TestValues.testRoleH

        val result = userRepository.list(testRole)
        Await.result(result, Duration.Inf) should be (\/- (Vector()))
      }
      "list users in a given course" in {
        val testCourse = TestValues.testCourseB

        val testUserList = TreeMap[Int, User](
          0 -> TestValues.testUserC.copy(hash = None),
          1 -> TestValues.testUserE.copy(hash = None),
          2 -> TestValues.testUserG.copy(hash = None),
          3 -> TestValues.testUserH.copy(hash = None)
        )

        val result = userRepository.list(testCourse)
        val eitherUsers = Await.result(result, Duration.Inf)
        val \/-(users) = eitherUsers

        testUserList.foreach {
          case (key, user: User) => {
            users(key).id should be(user.id)
            users(key).version should be(user.version)
            users(key).email should be(user.email)
            users(key).username should be(user.username)
            users(key).hash should be(None)
            users(key).givenname should be(user.givenname)
            users(key).surname should be(user.surname)
            users(key).createdAt.toString should be(user.createdAt.toString)
            users(key).updatedAt.toString should be(user.updatedAt.toString)
          }
        }

        users.size should be(testUserList.size)
      }
      "return empty Vector() if course doesn't exist" in {
        val testCourse = TestValues.testCourseC

        val result = userRepository.list(testCourse)
        Await.result(result, Duration.Inf) should be(\/- (Vector()))
      }
      "return empty Vector() if there are no users in the course" in {
        val testCourse = TestValues.testCourseG

        val result = userRepository.list(testCourse)
        Await.result(result, Duration.Inf) should be(\/- (Vector()))
      }
    }
  }

  "UserRepository.find" should {
    inSequence {
      "find a user by ID" in {
        val testUser = TestValues.testUserA

        val result = userRepository.find(testUser.id)
        val eitherUser = Await.result(result, Duration.Inf)
        val \/-(user) = eitherUser

        user.id should be(testUser.id)
        user.version should be(testUser.version)
        user.email should be(testUser.email)
        user.username should be(testUser.username)
        user.hash should be(testUser.hash)
        user.givenname should be(testUser.givenname)
        user.surname should be(testUser.surname)
        user.createdAt.toString() should be (testUser.createdAt.toString())
        user.updatedAt.toString() should be (testUser.updatedAt.toString())
      }
      "return RepositoryError.NoResults if user wasn't found by ID" in {
        val id = UUID("f9aadc67-5e8b-48f3-b0a2-20a0d7d88477")

        val result = userRepository.find(id)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
      "find a user by their identifier - email" in {
        val testUser = TestValues.testUserA

        val result = userRepository.find(testUser.email)
        val eitherUser = Await.result(result, Duration.Inf)
        val \/-(user) = eitherUser

        user.id should be(testUser.id)
        user.version should be(testUser.version)
        user.email should be(testUser.email)
        user.username should be(testUser.username)
        user.hash should be(testUser.hash)
        user.givenname should be(testUser.givenname)
        user.surname should be(testUser.surname)
        user.createdAt.toString() should be (testUser.createdAt.toString())
        user.updatedAt.toString() should be (testUser.updatedAt.toString())
      }
      "find a user by their identifier - username" in {
        val testUser = TestValues.testUserB

        val result = userRepository.find(testUser.username)
        val eitherUser = Await.result(result, Duration.Inf)
        val \/-(user) = eitherUser

        user.id should be(testUser.id)
        user.version should be(testUser.version)
        user.email should be(testUser.email)
        user.username should be(testUser.username)
        user.hash should be(testUser.hash)
        user.givenname should be(testUser.givenname)
        user.surname should be(testUser.surname)
        user.createdAt.toString() should be (testUser.createdAt.toString())
        user.updatedAt.toString() should be (testUser.updatedAt.toString())
      }
      "reutrn RepositoryError.NoResults if identifier - email that doesn't exist" in {
        val email = "unexisting_email@example.com"

        val result = userRepository.find(email)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
      "reutrn RepositoryError.NoResults if identifier - username that doesn't exist" in {
        val username = "unexisting_username"

        val result = userRepository.find(username)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
    }
  }

  "UserRepository.update" should {
    inSequence {
      "update an existing user with pass" in {
        val testUser        = TestValues.testUserA
        val updatedTestUser = testUser.copy(
          email     = "updated_user@example.com",
          username  = "updated_username",
          hash      = Some("$s0$100801$SIZ9lgHz0kPMgtLB37Uyhw==$wyKhNrg/MmUvlYuVygDctBE5LHBjLB91nyaiTpjbeyL="), // New hash
          givenname = "updated_givenname",
          surname   = "updated_surname"
        )

        val result = userRepository.update(updatedTestUser)
        val eitherUser = Await.result(result, Duration.Inf)
        val \/-(user) = eitherUser

        user.id should be(updatedTestUser.id)
        user.version should be(updatedTestUser.version + 1)
        user.email should be(updatedTestUser.email)
        user.username should be(updatedTestUser.username)
        user.hash should be(None)
        user.givenname should be(updatedTestUser.givenname)
        user.surname should be(updatedTestUser.surname)
        user.createdAt.toString() should be (updatedTestUser.createdAt.toString())
        user.updatedAt.toString() should not be (updatedTestUser.updatedAt.toString())
      }
      "update an existing user without pass" in {
        val testUser        = TestValues.testUserA
        val updatedTestUser = testUser.copy(
          email     = "updated_user@example.com",
          username  = "updated_username",
          givenname = "updated_givenname",
          surname   = "updated_surname",
          hash      = None
        )

        val result = userRepository.update(updatedTestUser)
        val eitherUser = Await.result(result, Duration.Inf)
        val \/-(user) = eitherUser

        user.id should be(updatedTestUser.id)
        user.version should be(updatedTestUser.version + 1)
        user.email should be(updatedTestUser.email)
        user.username should be(updatedTestUser.username)
        user.hash should be(None)
        user.givenname should be(updatedTestUser.givenname)
        user.surname should be(updatedTestUser.surname)
        user.createdAt.toString() should be (updatedTestUser.createdAt.toString())
        user.updatedAt.toString() should not be (updatedTestUser.updatedAt.toString())
      }
      "reutrn RepositoryError.NoResults when update an existing user with wrong version" in {
        val testUser        = TestValues.testUserA
        val updatedTestUser = testUser.copy(
          email     = "updated_user@example.com",
          username  = "updated_username",
          givenname = "updated_givenname",
          surname   = "updated_surname",
          version   = 99L
        )

        val result = userRepository.update(updatedTestUser)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
      "reutrn RepositoryError.UniqueKeyConflict when update an existing user with username that already exists" in {
        val testUser        = TestValues.testUserA
        val updatedTestUser = testUser.copy(
          username = TestValues.testUserB.username
        )

        val result = userRepository.update(updatedTestUser)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.UniqueKeyConflict("username", "users_username_key")))
      }
      "reutrn RepositoryError.UniqueKeyConflict when update an existing user with email that already exists" in {
        val testUser        = TestValues.testUserA
        val updatedTestUser = testUser.copy(
          email = TestValues.testUserB.email
        )

        val result = userRepository.update(updatedTestUser)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.UniqueKeyConflict("email", "users_email_key")))
      }
      "reutrn RepositoryError.NoResults when update an unexisting user" in {
        val unexistingUser = User(
          email     = "unexisting_email@example.com",
          username  = "unexisting_username",
          givenname = "unexisting_givenname",
          surname   = "unexisting_surname"
        )

        val result = userRepository.update(unexistingUser)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
    }
  }

  "UserRepository.insert" should {
    inSequence {
      "save a new User" in {
        val testUser = TestValues.testUserD

        val result = userRepository.insert(testUser)
        val eitherUser = Await.result(result, Duration.Inf)
        val \/-(user) = eitherUser

        user.id should be(testUser.id)
        user.version should be(testUser.version)
        user.email should be(testUser.email)
        user.username should be(testUser.username)
        user.hash should be(None)
        user.givenname should be(testUser.givenname)
        user.surname should be(testUser.surname)
      }
      "reutrn RepositoryError.PrimaryKeyConflict if user already exists" in {
        val testUser = TestValues.testUserA
        val result = userRepository.insert(TestValues.testUserA)

        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
      }
      "reutrn RepositoryError.UniqueKeyConflict if username already exists" in {
        val testUser = TestValues.testUserD.copy(
          username = TestValues.testUserA.username
        )
        val result = userRepository.insert(testUser)

        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.UniqueKeyConflict("username", "users_username_key")))
      }
      "reutrn RepositoryError.UniqueKeyConflict if email already exists" in {
        val testUser = TestValues.testUserD.copy(
          email = TestValues.testUserA.email
        )
        val result = userRepository.insert(testUser)

        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.UniqueKeyConflict("email", "users_email_key")))
      }
    }
  }

  "UserRepository.delete" should {
    inSequence {
      "delete a user from the database if user has no references in other tables" in {
        val testUser = TestValues.testUserC

        val result = userRepository.delete(testUser)
        val eitherUser = Await.result(result, Duration.Inf)
        val \/-(user) = eitherUser

        user.id should be(testUser.id)
        user.version should be(testUser.version)
        user.email should be(testUser.email)
        user.username should be(testUser.username)
        user.hash should be(testUser.hash)
        user.givenname should be(testUser.givenname)
        user.surname should be(testUser.surname)
        user.createdAt.toString() should be (testUser.createdAt.toString())
        user.updatedAt.toString() should be (testUser.updatedAt.toString())
      }
      "return ForeignKeyConflict if user is teacher and has references in other tables" in {
        val result = userRepository.delete(TestValues.testUserB)

        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.ForeignKeyConflict("teacher_id", "courses_teacher_id_fkey")))
      }
      "return RepositoryError.NoResults if User hasn't been found" in {
        val unexistingUser = User(
          email     = "unexisting_email@example.com",
          username  = "unexisting_username",
          givenname = "unexisting_givenname",
          surname   = "unexisting_surname"
        )

        val result = userRepository.delete(unexistingUser)

        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
    }
  }
}
