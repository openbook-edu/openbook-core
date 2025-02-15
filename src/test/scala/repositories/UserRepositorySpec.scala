//import java.util.UUID
//
//import ca.shiftfocus.krispii.core.error.RepositoryError
//import ca.shiftfocus.krispii.core.models.{ Organization, Role, User }
//import ca.shiftfocus.krispii.core.repositories.{ TagRepositoryPostgres, UserRepositoryPostgres }
//import org.scalatest.Matchers._
//import org.scalatest._
//
//import scala.collection.breakOut
//import scala.collection.immutable.TreeMap
//import scala.concurrent.duration._
//import scala.concurrent.{ Await, Future }
//import scalaz.{ -\/, \/- }
//import play.api.Logger

//class UserRepositorySpec
//    extends TestEnvironment {
//  val tagRepository = new TagRepositoryPostgres()
//  val userRepository = new UserRepositoryPostgres(tagRepository)
//
//  "UserRepository.organizationTeammateSearch" should {
//    inSequence {
//      "find teammates by key" in {
//        val key = "Test"
//        val testUserList = TreeMap[Int, User](
//          0 -> TestValues.testUserA,
//          1 -> TestValues.testUserB
//        )
//        val orgList = IndexedSeq(
//          TestValues.testOrganizationA
//        )
//
//        val result = userRepository.searchOrganizationMembers(key, orgList)
//        val eitherUsers = Await.result(result, Duration.Inf)
//        val \/-(users) = eitherUsers
//
//        println(Console.GREEN + users + Console.RESET)
//
//        users.size should be(testUserList.size)
//
//        testUserList.foreach {
//          case (key, user: User) => {
//            users(key).id should be(user.id)
//            users(key).version should be(user.version)
//            users(key).email should be(user.email)
//            users(key).username should be(user.username)
//            users(key).hash should be(None)
//            users(key).givenname should be(user.givenname)
//            users(key).surname should be(user.surname)
//            users(key).createdAt.toString should be(user.createdAt.toString)
//            users(key).updatedAt.toString should be(user.updatedAt.toString)
//          }
//        }
//      }
//    }
//  }

//  "UserRepository.findDeleted" should {
//    inSequence {
//      "find Deleted user" in {
//        val testUser = TestValues.testUserK
//        val email = testUser.email.replaceAll("^deleted_[1-9]{10}_", "")
//
//        val result = userRepository.findDeleted(email)
//        val eitherUser = Await.result(result, Duration.Inf)
//        val \/-(user) = eitherUser
//
//        user.id should be(testUser.id)
//        user.version should be(testUser.version)
//        user.email should be(testUser.email)
//        user.username should be(testUser.username)
//        user.hash should be(testUser.hash)
//        user.givenname should be(testUser.givenname)
//        user.surname should be(testUser.surname)
//        user.createdAt.toString() should be(testUser.createdAt.toString())
//        user.updatedAt.toString() should be(testUser.updatedAt.toString())
//      }
//    }
//  }

//
//  "UserRepository.list" should {
//    inSequence {
//      "list all users" in {
//        val testUserList = TreeMap[Int, User](
//          0 -> TestValues.testUserA.copy(hash = None),
//          1 -> TestValues.testUserB.copy(hash = None),
//          2 -> TestValues.testUserC.copy(hash = None),
//          3 -> TestValues.testUserE.copy(hash = None),
//          4 -> TestValues.testUserF.copy(hash = None),
//          5 -> TestValues.testUserG.copy(hash = None),
//          6 -> TestValues.testUserH.copy(hash = None),
//          7 -> TestValues.testUserJ.copy(hash = None),
//          8 -> TestValues.testUserI.copy(hash = None),
//          9 -> TestValues.testUserD.copy(hash = None)
//        )
//
//        val result = userRepository.list(conn)
//        val eitherUsers = Await.result(result, Duration.Inf)
//        val \/-(users) = eitherUsers
//
//        users.size should be(testUserList.size)
//
//        testUserList.foreach {
//          case (key, user: User) => {
//            users(key).id should be(user.id)
//            users(key).version should be(user.version)
//            users(key).email should be(user.email)
//            users(key).username should be(user.username)
//            users(key).hash should be(None)
//            users(key).givenname should be(user.givenname)
//            users(key).surname should be(user.surname)
//            users(key).createdAt.toString should be(user.createdAt.toString)
//            users(key).updatedAt.toString should be(user.updatedAt.toString)
//          }
//        }
//      }
//      "not list deleted users" in {
//        val testUser = TestValues.testUserX
//
//        val result = userRepository.list(conn)
//        val eitherUsers = Await.result(result, Duration.Inf)
//        val \/-(users) = eitherUsers
//
//        users.foreach {
//          case (user: User) => {
//            testUser.id should not be (user.id)
//            testUser.email should not be (user.email)
//            testUser.username should not be (user.username)
//            testUser.givenname should not be (user.givenname)
//            testUser.surname should not be (user.surname)
//          }
//        }
//      }
//      "list users with a specified set of user Ids" in {
//        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
//        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
//
//        val testUserList = TreeMap[Int, User](
//          0 -> TestValues.testUserC.copy(hash = None),
//          1 -> TestValues.testUserA.copy(hash = None)
//        )
//
//        val result = userRepository.list(testUserList.map(_._2.id)(breakOut))
//        val eitherUsers = Await.result(result, Duration.Inf)
//        val \/-(users) = eitherUsers
//
//        users.size should be(testUserList.size)
//
//        testUserList.foreach {
//          case (key, user: User) => {
//            users(key).id should be(user.id)
//            users(key).version should be(user.version)
//            users(key).email should be(user.email)
//            users(key).username should be(user.username)
//            users(key).hash should be(None)
//            users(key).givenname should be(user.givenname)
//            users(key).surname should be(user.surname)
//            users(key).createdAt.toString should be(user.createdAt.toString)
//            users(key).updatedAt.toString should be(user.updatedAt.toString)
//          }
//        }
//      }
//      "return RepositoryError.NoResults if set contains unexisting user ID" in {
//        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
//        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
//
//        val ids = Vector(
//          TestValues.testUserA.id,
//          UUID.fromString("a5caac60-8fd7-4ecc-8fd3-f84dc11355f1")
//        )
//
//        val result = userRepository.list(ids)
//
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type User")))
//      }
//      "List all users who have a role" in {
//        val testRole = TestValues.testRoleA
//
//        val testUserList = TreeMap[Int, User](
//          0 -> TestValues.testUserA.copy(hash = None),
//          1 -> TestValues.testUserB.copy(hash = None)
//        )
//
//        val result = userRepository.list(testRole)
//        val eitherUsers = Await.result(result, Duration.Inf)
//        val \/-(users) = eitherUsers
//
//        users.size should be(testUserList.size)
//
//        testUserList.foreach {
//          case (key, user: User) => {
//            users(key).id should be(user.id)
//            users(key).version should be(user.version)
//            users(key).email should be(user.email)
//            users(key).username should be(user.username)
//            users(key).hash should be(None)
//            users(key).givenname should be(user.givenname)
//            users(key).surname should be(user.surname)
//            users(key).createdAt.toString should be(user.createdAt.toString)
//            users(key).updatedAt.toString should be(user.updatedAt.toString)
//          }
//        }
//      }
//      "return empty Vector() if role doesn't exist" in {
//        val unexistingRole = Role(
//          name = "unexisting role name"
//        )
//
//        val result = userRepository.list(unexistingRole)
//        Await.result(result, Duration.Inf) should be(\/-(Vector()))
//      }
//      "return empty Vector() if there are no users that have this role" in {
//        val testRole = TestValues.testRoleH
//
//        val result = userRepository.list(testRole)
//        Await.result(result, Duration.Inf) should be(\/-(Vector()))
//      }
//      "list users in a given group" in {
//        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
//        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
//
//        val testCourse = TestValues.testCourseB
//
//        val testUserList = TreeMap[Int, User](
//          0 -> TestValues.testUserC.copy(hash = None),
//          1 -> TestValues.testUserE.copy(hash = None)
//        )
//
//        val result = userRepository.list(testCourse)
//        val eitherUsers = Await.result(result, Duration.Inf)
//        val \/-(users) = eitherUsers
//
//        users.size should be(testUserList.size)
//
//        testUserList.foreach {
//          case (key, user: User) => {
//            users(key).id should be(user.id)
//            users(key).version should be(user.version)
//            users(key).email should be(user.email)
//            users(key).username should be(user.username)
//            users(key).hash should be(None)
//            users(key).givenname should be(user.givenname)
//            users(key).surname should be(user.surname)
//            users(key).createdAt.toString should be(user.createdAt.toString)
//            users(key).updatedAt.toString should be(user.updatedAt.toString)
//          }
//        }
//      }
//      "return empty Vector() if group doesn't exist" in {
//        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
//        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
//
//        val testCourse = TestValues.testCourseC
//
//        val result = userRepository.list(testCourse)
//        Await.result(result, Duration.Inf) should be(\/-(Vector()))
//      }
//      "return empty Vector() if there are no users in the group" in {
//        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
//        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
//
//        val testCourse = TestValues.testCourseG
//
//        val result = userRepository.list(testCourse)
//        Await.result(result, Duration.Inf) should be(\/-(Vector()))
//      }
//    }
//  }
//
//  "UserRepository.find" should {
//    inSequence {
//      "find a user by ID" in {
//        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
//        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
//
//        val testUser = TestValues.testUserA
//
//        val result = userRepository.find(testUser.id)
//        val eitherUser = Await.result(result, Duration.Inf)
//        val \/-(user) = eitherUser
//
//        user.id should be(testUser.id)
//        user.version should be(testUser.version)
//        user.email should be(testUser.email)
//        user.username should be(testUser.username)
//        user.hash should be(testUser.hash)
//        user.givenname should be(testUser.givenname)
//        user.surname should be(testUser.surname)
//        user.createdAt.toString() should be(testUser.createdAt.toString())
//        user.updatedAt.toString() should be(testUser.updatedAt.toString())
//      }
//      "not return deleted user" in {
//        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
//        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
//
//        val testUser = TestValues.testUserX
//
//        val result = userRepository.find(testUser.id)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type User")))
//      }
//      "return RepositoryError.NoResults if user wasn't found by ID" in {
//        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
//        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
//
//        val id = UUID.fromString("f9aadc67-5e8b-48f3-b0a2-20a0d7d88477")
//
//        val result = userRepository.find(id)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type User")))
//      }
//      "find a user by their identifier - email" in {
//        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
//        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
//
//        val testUser = TestValues.testUserA
//
//        val result = userRepository.find(testUser.email)
//        val eitherUser = Await.result(result, Duration.Inf)
//        val \/-(user) = eitherUser
//
//        user.id should be(testUser.id)
//        user.version should be(testUser.version)
//        user.email should be(testUser.email)
//        user.username should be(testUser.username)
//        user.hash should be(testUser.hash)
//        user.givenname should be(testUser.givenname)
//        user.surname should be(testUser.surname)
//        user.createdAt.toString() should be(testUser.createdAt.toString())
//        user.updatedAt.toString() should be(testUser.updatedAt.toString())
//      }
//      "find a user by their identifier - username" in {
//        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
//        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
//
//        val testUser = TestValues.testUserB
//
//        val result = userRepository.find(testUser.username)
//        val eitherUser = Await.result(result, Duration.Inf)
//        val \/-(user) = eitherUser
//
//        user.id should be(testUser.id)
//        user.version should be(testUser.version)
//        user.email should be(testUser.email)
//        user.username should be(testUser.username)
//        user.hash should be(testUser.hash)
//        user.givenname should be(testUser.givenname)
//        user.surname should be(testUser.surname)
//        user.createdAt.toString() should be(testUser.createdAt.toString())
//        user.updatedAt.toString() should be(testUser.updatedAt.toString())
//      }
//      "reutrn RepositoryError.NoResults if identifier - email that doesn't exist" in {
//        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
//        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
//
//        val email = "unexisting_email@example.com"
//
//        val result = userRepository.find(email)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type User")))
//      }
//      "reutrn RepositoryError.NoResults if identifier - username that doesn't exist" in {
//        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
//        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
//
//        val username = "unexisting_username"
//
//        val result = userRepository.find(username)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type User")))
//      }
//    }
//  }
//
//  "UserRepository.update" should {
//    inSequence {
//      "update an existing user with pass" in {
//        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))
//
//        val testUser = TestValues.testUserA
//        val updatedTestUser = testUser.copy(
//          email = "updated_user@example.com",
//          username = "updated_username",
//          hash = Some("$s0$100801$SIZ9lgHz0kPMgtLB37Uyhw==$wyKhNrg/MmUvlYuVygDctBE5LHBjLB91nyaiTpjbeyL="), // New hash
//          givenname = "updated_givenname",
//          surname = "updated_surname"
//        )
//
//        val result = userRepository.update(updatedTestUser)
//        val eitherUser = Await.result(result, Duration.Inf)
//        val \/-(user) = eitherUser
//
//        user.id should be(updatedTestUser.id)
//        user.version should be(updatedTestUser.version + 1)
//        user.email should be(updatedTestUser.email)
//        user.username should be(updatedTestUser.username)
//        user.hash should be(None)
//        user.givenname should be(updatedTestUser.givenname)
//        user.surname should be(updatedTestUser.surname)
//        user.createdAt.toString() should be(updatedTestUser.createdAt.toString())
//        user.updatedAt.toString() should not be (updatedTestUser.updatedAt.toString())
//      }
//      "update an existing user without pass" in {
//        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))
//
//        val testUser = TestValues.testUserA
//        val updatedTestUser = testUser.copy(
//          email = "updated_user@example.com",
//          username = "updated_username",
//          givenname = "updated_givenname",
//          surname = "updated_surname",
//          hash = None
//        )
//
//        val result = userRepository.update(updatedTestUser)
//        val eitherUser = Await.result(result, Duration.Inf)
//        val \/-(user) = eitherUser
//
//        user.id should be(updatedTestUser.id)
//        user.version should be(updatedTestUser.version + 1)
//        user.email should be(updatedTestUser.email)
//        user.username should be(updatedTestUser.username)
//        user.hash should be(None)
//        user.givenname should be(updatedTestUser.givenname)
//        user.surname should be(updatedTestUser.surname)
//        user.createdAt.toString() should be(updatedTestUser.createdAt.toString())
//        user.updatedAt.toString() should not be (updatedTestUser.updatedAt.toString())
//      }
//      "reutrn RepositoryError.NoResults when update an existing user with wrong version" in {
//        val testUser = TestValues.testUserA
//        val updatedTestUser = testUser.copy(
//          email = "updated_user@example.com",
//          username = "updated_username",
//          givenname = "updated_givenname",
//          surname = "updated_surname",
//          version = 99L
//        )
//
//        val result = userRepository.update(updatedTestUser)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type User")))
//      }
//      "reutrn RepositoryError.UniqueKeyConflict when update an existing user with username that already exists" in {
//        val testUser = TestValues.testUserA
//        val updatedTestUser = testUser.copy(
//          username = TestValues.testUserB.username
//        )
//
//        val result = userRepository.update(updatedTestUser)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.UniqueKeyConflict("username", "users_username_key")))
//      }
//      "reutrn RepositoryError.UniqueKeyConflict when update an existing user with email that already exists" in {
//        val testUser = TestValues.testUserA
//        val updatedTestUser = testUser.copy(
//          email = TestValues.testUserB.email
//        )
//
//        val result = userRepository.update(updatedTestUser)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.UniqueKeyConflict("email", "users_email_key")))
//      }
//      "return RepositoryError.NoResults when update an unexisting user" in {
//        val unexistingUser = User(
//          email = "unexisting_email@example.com",
//          username = "unexisting_username",
//          givenname = "unexisting_givenname",
//          surname = "unexisting_surname",
//          accountType = "google"
//        )
//
//        val result = userRepository.update(unexistingUser)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type User")))
//      }
//    }
//  }
//
//  "UserRepository.insert" should {
//    inSequence {
//      "save a new User" in {
//        val testUser = TestValues.testUserD
//
//        val result = userRepository.insert(testUser)
//        val eitherUser = Await.result(result, Duration.Inf)
//        val \/-(user) = eitherUser
//
//        user.id should be(testUser.id)
//        user.version should be(testUser.version)
//        user.email should be(testUser.email)
//        user.username should be(testUser.username)
//        user.hash should be(None)
//        user.givenname should be(testUser.givenname)
//        user.surname should be(testUser.surname)
//      }
//      "save a new User with empty Password Hash" in {
//        val testUser = TestValues.testUserD.copy(hash = None)
//
//        val result = userRepository.insert(testUser)
//        val eitherUser = Await.result(result, Duration.Inf)
//        val \/-(user) = eitherUser
//
//        user.id should be(testUser.id)
//        user.version should be(testUser.version)
//        user.email should be(testUser.email)
//        user.username should be(testUser.username)
//        user.hash should be(None)
//        user.givenname should be(testUser.givenname)
//        user.surname should be(testUser.surname)
//      }
//      "reutrn RepositoryError.PrimaryKeyConflict if user already exists" in {
//        val testUser = TestValues.testUserA
//
//        val result = userRepository.insert(testUser)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
//      }
//      "reutrn RepositoryError.UniqueKeyConflict if username already exists" in {
//        val testUser = TestValues.testUserD.copy(
//          username = TestValues.testUserA.username
//        )
//
//        val result = userRepository.insert(testUser)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.UniqueKeyConflict("username", "users_username_key")))
//      }
//      "reutrn RepositoryError.UniqueKeyConflict if email already exists" in {
//        val testUser = TestValues.testUserD.copy(
//          email = TestValues.testUserA.email
//        )
//
//        val result = userRepository.insert(testUser)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.UniqueKeyConflict("email", "users_email_key")))
//      }
//    }
//  }
//
//  "UserRepository.delete" should {
//    inSequence {
//      "delete a user from the database if user has no references in other tables" in {
//        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))
//
//        val testUser = TestValues.testUserH.copy(hash = None)
//
//        val result = userRepository.delete(testUser)
//        val eitherUser = Await.result(result, Duration.Inf)
//        val \/-(user) = eitherUser
//
//        user.id should be(testUser.id)
//        user.version should be(testUser.version)
//        user.email should be(testUser.email)
//        user.username should be(testUser.username)
//        user.hash should be(testUser.hash)
//        user.givenname should be(testUser.givenname)
//        user.surname should be(testUser.surname)
//        user.createdAt.toString() should be(testUser.createdAt.toString())
//        user.updatedAt.toString() should be(testUser.updatedAt.toString())
//      }
//      //      "return ForeignKeyConflict if user is teacher and has references in other tables" in {
//      //        val testUser = TestValues.testUserB
//      //
//      //        val result = userRepository.delete(testUser)
//      //        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.ForeignKeyConflict("teacher_id", "courses_teacher_id_fkey")))
//      //      }
//      "return RepositoryError.NoResults if User hasn't been found" in {
//        val unexistingUser = User(
//          email = "unexisting_email@example.com",
//          username = "unexisting_username",
//          givenname = "unexisting_givenname",
//          surname = "unexisting_surname",
//          accountType = "krispii"
//        )
//
//        val result = userRepository.delete(unexistingUser)
//
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type User")))
//      }
//      "return RepositoryError.NoResults if User version is wrong" in {
//        val testUser = TestValues.testUserH.copy(
//          version = 99L
//        )
//
//        val result = userRepository.delete(testUser)
//
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type User")))
//      }
//    }
//  }
//
//  "UserRepository.triagramSearch" should {
//    inSequence {
//      "List all the users who has a given string in their email, even deleted ones" in {
//        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
//        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
//
//        val testUserList = TreeMap[Int, User](
//          0 -> TestValues.testUserI.copy(hash = None),
//          1 -> TestValues.testUserJ.copy(hash = None),
//          2 -> TestValues.testUserX.copy(hash = None)
//
//        )
//
//        val result = userRepository.triagramSearch("kri", false)
//        val eitherUsers = Await.result(result, Duration.Inf)
//        val \/-(users) = eitherUsers
//
//        users.size should be(testUserList.size)
//        users.foreach {
//          case (user: User) => {
//            user.email.contains("kri") should be(true)
//          }
//        }
//      }
//      "List all the users who has a given string in their email, even deleted ones with 2 letter sequence" in {
//        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
//        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
//
//        val testUserList = TreeMap[Int, User](
//          0 -> TestValues.testUserI.copy(hash = None),
//          1 -> TestValues.testUserJ.copy(hash = None),
//          2 -> TestValues.testUserX.copy(hash = None)
//
//        )
//
//        val result = userRepository.triagramSearch("kri", false)
//        val eitherUsers = Await.result(result, Duration.Inf)
//        val \/-(users) = eitherUsers
//
//        Logger.debug(users.toString)
//        users.foreach {
//          case (user: User) => {
//            user.email.contains("kr") should be(true)
//          }
//        }
//      }
//
//      "Return error is nothing is there" in {
//        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
//        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
//
//        val testUserList = TreeMap[Int, User](
//          0 -> TestValues.testUserI.copy(hash = None),
//          1 -> TestValues.testUserJ.copy(hash = None),
//          2 -> TestValues.testUserX.copy(hash = None)
//
//        )
//
//        val result = userRepository.triagramSearch("shmebulock", false)
//        val eitherUsers = Await.result(result, Duration.Inf)
//        eitherUsers should be(\/-(Vector()))
//      }
//    }
//  }
//}
