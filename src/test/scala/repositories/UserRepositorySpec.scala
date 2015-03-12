import java.io.File

import ca.shiftfocus.krispii.core.models.User
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.pool.PoolConfiguration
import com.github.mauricio.async.db.{Connection, Configuration, RowData}
import com.typesafe.config.ConfigFactory
import scala.collection.immutable.TreeMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._
import ca.shiftfocus.krispii.core.repositories.UserRepositoryPostgres
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import grizzled.slf4j.Logger
import org.scalamock.scalatest.MockFactory
import org.scalatest._
import Matchers._
import collection.breakOut
import scalaz.\/-


class UserRepositorySpec
  extends TestEnvironment
{
  val userRepository = new UserRepositoryPostgres

  "UserRepository.list" should {
    inSequence {
      "list all users" in {
        val testUserList = TreeMap[Int, User](
          0 -> TestValues.testUserA,
          1 -> TestValues.testUserB,
          2 -> TestValues.testUserC,
          3 -> TestValues.testUserE,
          4 -> TestValues.testUserF,
          5 -> TestValues.testUserG,
          6 -> TestValues.testUserH
        )

        val result = userRepository.list(conn)
        val eitherUsers = Await.result(result, Duration.Inf)
        val \/-(users) = eitherUsers

        eitherUsers.toString should be(\/-(testUserList.map(_._2.toString)(breakOut)).toString)

        testUserList.foreach {
          case (key, user: User) => {
            users(key).id should be(user.id)
            users(key).version should be(user.version)
            users(key).email should be(user.email)
            users(key).username should be(user.username)
            users(key).givenname should be(user.givenname)
            users(key).surname should be(user.surname)
            users(key).createdAt.toString should be(user.createdAt.toString)
            users(key).updatedAt.toString should be(user.updatedAt.toString)
          }
        }
      }
      "list users with a specified set of user Ids" in {
        val testUserList = TreeMap[Int, User](
          0 -> TestValues.testUserC,
          1 -> TestValues.testUserA
        )

        val result = userRepository.list(Vector(testUserList(0).id, testUserList(1).id))
        val eitherUsers = Await.result(result, Duration.Inf)
        val \/-(users) = eitherUsers

        eitherUsers.toString should be(\/-(testUserList.map(_._2.toString)(breakOut)).toString)

        testUserList.foreach {
          case (key, user: User) => {
            users(key).id should be(user.id)
            users(key).version should be(user.version)
            users(key).email should be(user.email)
            users(key).username should be(user.username)
            users(key).givenname should be(user.givenname)
            users(key).surname should be(user.surname)
            users(key).createdAt.toString should be(user.createdAt.toString)
            users(key).updatedAt.toString should be(user.updatedAt.toString)
          }
        }
      }
      // TODO add this Exception to the class of Exceptions
      "throw an NoSuchElementException if set contains unexisting user ID" in {
        val result = userRepository.list(Vector(UUID("a5caac60-8fd7-4ecc-8fd3-f84dc11355f1")))

        an [java.util.NoSuchElementException] should be thrownBy Await.result(result, Duration.Inf)
      }
      "List all users who have a role" in {
        val testUserList = TreeMap[Int, User](
          0 -> TestValues.testUserA,
          1 -> TestValues.testUserB
        )

        val result = userRepository.list(TestValues.testRoleA)
        val eitherUsers = Await.result(result, Duration.Inf)
        val \/-(users) = eitherUsers

        eitherUsers.toString should be(\/-(testUserList.map(_._2.toString)(breakOut)).toString)

        testUserList.foreach {
          case (key, user: User) => {
            users(key).id should be(user.id)
            users(key).version should be(user.version)
            users(key).email should be(user.email)
            users(key).username should be(user.username)
            users(key).givenname should be(user.givenname)
            users(key).surname should be(user.surname)
            users(key).createdAt.toString should be(user.createdAt.toString)
            users(key).updatedAt.toString should be(user.updatedAt.toString)
          }
        }
      }
      "list users in a given course" in {
        val testUserList = TreeMap[Int, User](
          0 -> TestValues.testUserB,
          1 -> TestValues.testUserE,
          2 -> TestValues.testUserG,
          3 -> TestValues.testUserH
        )

        val result = userRepository.list(TestValues.testCourseB)
        val eitherUsers = Await.result(result, Duration.Inf)
        val \/-(users) = eitherUsers

        eitherUsers.toString should be(\/-(testUserList.map(_._2.toString)(breakOut)).toString)

        testUserList.foreach {
          case (key, user: User) => {
            users(key).id should be(user.id)
            users(key).version should be(user.version)
            users(key).email should be(user.email)
            users(key).username should be(user.username)
            users(key).givenname should be(user.givenname)
            users(key).surname should be(user.surname)
            users(key).createdAt.toString should be(user.createdAt.toString)
            users(key).updatedAt.toString should be(user.updatedAt.toString)
          }
        }
      }
      "return empty Vector() if course unexists" in {
        val result = userRepository.list(TestValues.testCourseC)

        Await.result(result, Duration.Inf) should be (\/- (Vector()))
      }
    }
  }




//
//  "UserRepository.find" should {
//    inSequence {
//      "find a user by ID" in {
//        val result = userRepository.find(TestValues.testUserA.id).map(_.get)
//
//        val user = Await.result(result, Duration.Inf)
//        user.id should be(TestValues.testUserA.id)
//        user.version should be(TestValues.testUserA.version)
//        user.email should be(TestValues.testUserA.email)
//        user.username should be(TestValues.testUserA.username)
//        user.givenname should be(TestValues.testUserA.givenname)
//        user.surname should be(TestValues.testUserA.surname)
//        user.createdAt.toString() should be (TestValues.testUserA.createdAt.toString())
//        user.updatedAt.toString() should be (TestValues.testUserA.updatedAt.toString())
//      }
//      "be None if user wasn't found by ID" in {
//        val result = userRepository.find(UUID("f9aadc67-5e8b-48f3-b0a2-20a0d7d88477"))
//
//        Await.result(result, Duration.Inf) should be (None)
//      }
//      "find a user by their identifiers - email" in {
//        val result = userRepository.find(TestValues.testUserA.email).map(_.get)
//
//        val user = Await.result(result, Duration.Inf)
//
//        user.id should be(TestValues.testUserA.id)
//        user.version should be(TestValues.testUserA.version)
//        user.email should be(TestValues.testUserA.email)
//        user.username should be(TestValues.testUserA.username)
//        user.givenname should be(TestValues.testUserA.givenname)
//        user.surname should be(TestValues.testUserA.surname)
//        user.createdAt.toString should be(TestValues.testUserA.createdAt.toString)
//        user.updatedAt.toString should be(TestValues.testUserA.updatedAt.toString)
//      }
//      "find a user by their identifiers - username" in {
//        val result = userRepository.find(TestValues.testUserB.username).map(_.get)
//
//        val user = Await.result(result, Duration.Inf)
//
//        user.id should be(TestValues.testUserB.id)
//        user.version should be(TestValues.testUserB.version)
//        user.email should be(TestValues.testUserB.email)
//        user.username should be(TestValues.testUserB.username)
//        user.givenname should be(TestValues.testUserB.givenname)
//        user.surname should be(TestValues.testUserB.surname)
//        user.createdAt.toString should be(TestValues.testUserB.createdAt.toString)
//        user.updatedAt.toString should be(TestValues.testUserB.updatedAt.toString)
//      }
//      "not find a user by their identifiers - email (unexisting email)" in {
//        val result = userRepository.find("unexisting_email@example.com")
//
//        val user = Await.result(result, Duration.Inf)
//
//        user should be (None)
//      }
//      "not find a user by their identifiers - username (unexisting username)" in {
//        val result = userRepository.find("unexisting_username")
//
//        val user = Await.result(result, Duration.Inf)
//
//        user should be (None)
//      }
//    }
//  }
//
//  "UserRepository.findByEmail" should {
//    inSequence {
//      "find a user by e-mail address" in {
//        val result = userRepository.find(TestValues.testUserC.email).map(_.get)
//
//        val user = Await.result(result, Duration.Inf)
//
//        user.id should be (TestValues.testUserC.id)
//        user.version should be (TestValues.testUserC.version)
//        user.email should be (TestValues.testUserC.email)
//        user.username should be (TestValues.testUserC.username)
//        user.givenname should be (TestValues.testUserC.givenname)
//        user.surname should be (TestValues.testUserC.surname)
//        user.createdAt.toString should be(TestValues.testUserC.createdAt.toString)
//        user.updatedAt.toString should be(TestValues.testUserC.updatedAt.toString)
//      }
//      "not find a user by unexisting e-mail address" in {
//        val result = userRepository.find("unexisting_email@example.com")
//
//        val user = Await.result(result, Duration.Inf)
//
//        user should be (None)
//      }
//    }
//  }
//
//  "UserRepository.update" should {
//    inSequence {
//      "update an existing user" in {
//
//        val result = userRepository.update(TestValues.testUserA.copy(
//          email = "newtestUserA@example.com",
//          username = "newtestUserA",
//          givenname = "newTestA",
//          surname = "newUserA"
//        ))
//
//        val user = Await.result(result, Duration.Inf)
//
//        user.id should be (TestValues.testUserA.id)
//        user.version should be (TestValues.testUserA.version + 1)
//        user.email should be ("newtestUserA@example.com")
//        user.username should be ("newtestUserA")
//        user.givenname should be ("newTestA")
//        user.surname should be ("newUserA")
//        user.createdAt.toString should be(TestValues.testUserA.createdAt.toString)
//        user.updatedAt.toString should not be(TestValues.testUserA.updatedAt.toString)
//
//        // Find updated user and check
//        val queryResult = db.pool.sendPreparedStatement(SelectOneByIdentifier, Array[Any]("newtestUserA@example.com", "newtestUserA@example.com")).map { queryResult =>
//          val userList = queryResult.rows.get.map {
//            item: RowData => User(item)
//          }
//          userList
//        }
//
//        val userList = Await.result(queryResult, Duration.Inf)
//        val updated_user = userList(0)
//
//        updated_user.id should be (TestValues.testUserA.id)
//        updated_user.version should be (TestValues.testUserA.version + 1)
//        updated_user.email should be ("newtestUserA@example.com")
//        updated_user.username should be ("newtestUserA")
//        updated_user.givenname should be ("newTestA")
//        updated_user.surname should be ("newUserA")
//        user.createdAt.toString should be(TestValues.testUserA.createdAt.toString)
//        updated_user.updatedAt.toString() should not be (TestValues.testUserA.updatedAt.toString())
//      }
//      "throw a NoSuchElementException when update an existing user with wrong version" in {
//        val result = userRepository.update(TestValues.testUserB.copy(
//          version = 99L,
//          username = "newtestUserB",
//          givenname = "newTestB",
//          surname = "newUserB"
//        ))
//
//        an [java.util.NoSuchElementException] should be thrownBy Await.result(result, Duration.Inf)
//      }
//      "throw a GenericDatabaseException when update an existing user with username that already exists" in {
//        val result = userRepository.update(TestValues.testUserB.copy(
//          username = TestValues.testUserC.username
//        ))
//
//        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
//      }
//      "throw a GenericDatabaseException when update an existing user with email that already exists" in {
//        val result = userRepository.update(TestValues.testUserB.copy(
//          email = TestValues.testUserC.email
//        ))
//
//        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
//      }
//      "throw a NoSuchElementException when update an unexisting user" in {
//        an [java.util.NoSuchElementException] should be thrownBy userRepository.update(User(
//          email = "unexisting_email@example.com",
//          username = "unexisting_username",
//          givenname = "unexisting_givenname",
//          surname = "unexisting_surname"
//        ))
//      }
//    }
//  }
//
//  "UserRepository.insert" should {
//    inSequence {
//      "save a new User" in {
//        val result = userRepository.insert(TestValues.testUserD)
//
//        val user = Await.result(result, Duration.Inf)
//
//        user.id should be (TestValues.testUserD.id)
//        user.version should be (1L)
//        user.email should be (TestValues.testUserD.email)
//        user.username should be (TestValues.testUserD.username)
//        user.givenname should be (TestValues.testUserD.givenname)
//        user.surname should be (TestValues.testUserD.surname)
//
//        // Find new user and check
//        val queryResult = db.pool.sendPreparedStatement(SelectOneByIdentifier, Array[Any](TestValues.testUserD.email, TestValues.testUserD.email)).map { queryResult =>
//          val userList = queryResult.rows.get.map {
//            item: RowData => User(item)
//          }
//          userList
//        }
//
//        val userList = Await.result(queryResult, Duration.Inf)
//        val new_user = userList(0)
//
//        new_user.id should be (TestValues.testUserD.id)
//        new_user.version should be (1L)
//        new_user.email should be (TestValues.testUserD.email)
//        new_user.username should be (TestValues.testUserD.username)
//        new_user.givenname should be (TestValues.testUserD.givenname)
//        new_user.surname should be (TestValues.testUserD.surname)
//      }
//      "throw a GenericDatabaseException if user already exists" in {
//        val result = userRepository.insert(TestValues.testUserA)
//
//        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
//      }
//      "throw a GenericDatabaseException if username already exists" in {
//        val result = userRepository.insert(User(
//          email = "unexistinguser@example.com",
//          username = TestValues.testUserB.username,
//          passwordHash = Some("$s0$100801$LmS/oJ7gIulUSr4qJ9by2A==$c91t4yMA594s092V4LB89topw5Deo10BXowjW3W1234="),
//          givenname = "unexisting",
//          surname = "user"
//        ))
//
//        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
//      }
//      "throw a GenericDatabaseException if email already exists" in {
//        val result = userRepository.insert(User(
//          email = TestValues.testUserB.email,
//          username = "unexisting user",
//          passwordHash = Some("$s0$100801$LmS/oJ7gIulUSr4qJ9by2A==$c91t4yMA594s092V4LB89topw5Deo10BXowjW3W1234="),
//          givenname = "unexisting",
//          surname = "user"
//        ))
//
//        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
//      }
//    }
//  }
//
//  "UserRepository.delete" should {
//    inSequence {
//      "delete a user from the database if user has no references in other tables" in {
//        val result = userRepository.delete(TestValues.testUserC)
//
//        val is_deleted = Await.result(result, Duration.Inf)
//        is_deleted should be (true)
//
//        // Check if user has been deleted
//        val queryResult = db.pool.sendPreparedStatement(SelectOneByIdentifier, Array[Any](TestValues.testUserC.email, TestValues.testUserC.email)).map { queryResult =>
//          val userList = queryResult.rows.get.map {
//            item: RowData => User(item)
//          }
//          userList
//        }
//
//        Await.result(queryResult, Duration.Inf) should be (Vector())
//      }
//      "throw a GenericDatabaseException if user is teacher and has references in other tables" in {
//        val result = userRepository.delete(TestValues.testUserB)
//
//        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
//      }
//      "return FALSE if User hasn't been found" in {
//        val result = userRepository.delete(User(
//          email = "unexisting_email@example.com",
//          username = "unexisting_username",
//          givenname = "unexisting_givenname",
//          surname = "unexisting_surname"
//        ))
//
//        Await.result(result, Duration.Inf) should be(false)
//      }
//    }
//  }
}



