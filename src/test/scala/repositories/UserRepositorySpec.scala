import java.io.File

import ca.shiftfocus.krispii.core.models.User
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.RowData
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._
import ca.shiftfocus.krispii.core.repositories.UserRepositoryPostgresComponent
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import grizzled.slf4j.Logger
import org.scalamock.scalatest.MockFactory
import org.scalatest._
import Matchers._

trait UserRepoTestEnvironment
extends UserRepositoryPostgresComponent
with Suite
with BeforeAndAfterAll
with PostgresDB {
  val logger = Logger[this.type]

  implicit val connection = db.pool

  val project_path = new File(".").getAbsolutePath()
  val create_schema_path = s"${project_path}/src/test/resources/schemas/create_schema.sql"
  val drop_schema_path = s"${project_path}/src/test/resources/schemas/drop_schema.sql"
  val data_schema_path = s"${project_path}/src/test/resources/schemas/data_schema.sql"

  /**
   * Implements query from schema file
   * @param path Path to schema file
   */
  def load_schema(path: String): Unit = {
    val sql_schema_file = scala.io.Source.fromFile(path)
    val query = sql_schema_file.getLines().mkString
    sql_schema_file.close()
    val result = db.pool.sendQuery(query)
    Await.result(result, Duration.Inf)
  }

  // Before test
  override def beforeAll(): Unit = {
    // DROP tables
    load_schema(drop_schema_path)
    // CREATE tables
    load_schema(create_schema_path)
    // Insert data into tables
    load_schema(data_schema_path)
  }

  // After test
  override def afterAll(): Unit = {
    // DROP tables
    load_schema(drop_schema_path)
  }

  val SelectOneByIdentifier = """
      SELECT *
      FROM users
      WHERE (email = ? OR username = ?)
      LIMIT 1
    """
}

class UserRepositorySpec
  extends WordSpec
  with MustMatchers
  with MockFactory
  with UserRepoTestEnvironment {

  "UserRepository.list" should {
    inSequence {
      "list all users" in {
        val result = userRepository.list

        val users = Await.result(result, Duration.Inf)

        users should be(Vector(
          TestValues.testUserA,
          TestValues.testUserB,
          TestValues.testUserC,
          TestValues.testUserE,
          TestValues.testUserF,
          TestValues.testUserG,
          TestValues.testUserH
        ))
        Map[Int, User](
          0 -> TestValues.testUserA,
          1 -> TestValues.testUserB,
          2 -> TestValues.testUserC,
          3 -> TestValues.testUserE,
          4 -> TestValues.testUserF,
          5 -> TestValues.testUserG,
          6 -> TestValues.testUserH).foreach {
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
        val result = userRepository.list(Vector(TestValues.testUserC.id, TestValues.testUserA.id))

        val users = Await.result(result, Duration.Inf)

        users should be(Vector(TestValues.testUserC, TestValues.testUserA))
        Map[Int, User](0 -> TestValues.testUserC, 1 -> TestValues.testUserA).foreach {
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
      "throw an NoSuchElementException if set contains unexisting user ID" in {
        val result = userRepository.list(Vector(TestValues.testUserD.id))

        an [java.util.NoSuchElementException] should be thrownBy Await.result(result, Duration.Inf)
      }
      "list users in a given course" in {
        val result = userRepository.list(TestValues.testCourseA)
        val result2 = userRepository.list(TestValues.testCourseB)

        val users = Await.result(result, Duration.Inf)
        val users2 = Await.result(result2, Duration.Inf)

        users should be(Vector(TestValues.testUserA))
        users2 should be(Vector(TestValues.testUserB, TestValues.testUserE,TestValues.testUserG, TestValues.testUserH))

        Map[Int, User](0 -> TestValues.testUserA).foreach {
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

        Map[Int, User](0 -> TestValues.testUserB).foreach {
          case (key, user: User) => {
            users2(key).id should be(user.id)
            users2(key).version should be(user.version)
            users2(key).email should be(user.email)
            users2(key).username should be(user.username)
            users2(key).givenname should be(user.givenname)
            users2(key).surname should be(user.surname)
            users2(key).createdAt.toString should be(user.createdAt.toString)
            users2(key).updatedAt.toString should be(user.updatedAt.toString)
          }
        }
      }
      "return empty Vector() if course unexists" in {
        val result = userRepository.list(TestValues.testCourseC)

        Await.result(result, Duration.Inf) should be (Vector())
      }
    }
  }

  "UserRepository.listForSections" should {
    inSequence{
      "list the users belonging to a set of courses" in {
        val result = userRepository.listForCourses(Vector(TestValues.testCourseA, TestValues.testCourseB))

        val users = Await.result(result, Duration.Inf)

        users should be (Vector(
          TestValues.testUserA,
          TestValues.testUserB,
          TestValues.testUserE,
          TestValues.testUserG,
          TestValues.testUserH
        ))

        Map[Int, User](
          0 -> TestValues.testUserA,
          1 -> TestValues.testUserB,
          2 -> TestValues.testUserE,
          3 -> TestValues.testUserG,
          4 -> TestValues.testUserH).foreach {
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
      "return empty Vector() if set of courses contains unexisting course" in {
        val result = userRepository.listForCourses(Vector(TestValues.testCourseC))

        Await.result(result, Duration.Inf) should be (Vector())
      }
    }
  }

  "UserRepository.listForRoles" should {
    inSequence{
      "list the users one of a set of roles" in {
        val result = userRepository.listForRoles(Vector("test role A", "test role B"))

        val users = Await.result(result, Duration.Inf)
        users should be (Vector(TestValues.testUserA, TestValues.testUserB))
        Map[Int, User](0 -> TestValues.testUserA, 1 -> TestValues.testUserB).foreach {
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
      "return empty Vector() if set of roles contains unexisting role name" in {
        val result = userRepository.listForRoles(Vector("unexisting role"))

        Await.result(result, Duration.Inf) should be (Vector())
      }
    }
  }

  "UserRepository.listForRolesAndSections" should {
    inSequence{
      "list users filtering by both roles and courses" in {
        val result = userRepository.listForRolesAndCourses(Vector(TestValues.testRoleA.name, TestValues.testRoleB.name), Vector(TestValues.testCourseA.name, TestValues.testCourseB.name))

        val users = Await.result(result, Duration.Inf)
        users should be (Vector(TestValues.testUserA, TestValues.testUserB))
        Map[Int, User](0 -> TestValues.testUserA, 1 -> TestValues.testUserB).foreach {
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
      "return only one user if set of roles contains more elements than set of courses" in {
        val result = userRepository.listForRolesAndCourses(Vector(TestValues.testRoleA.name, TestValues.testRoleB.name), Vector(TestValues.testCourseA.name))

        val users = Await.result(result, Duration.Inf)
        users should be (Vector(TestValues.testUserA))
        Map[Int, User](0 -> TestValues.testUserA).foreach {
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
      "return empty Vector() if set of roles contains only unexisting role name" in {
        val result = userRepository.listForRolesAndCourses(Vector("unexisting role"), Vector(TestValues.testCourseA.name, TestValues.testCourseB.name))

        Await.result(result, Duration.Inf) should be (Vector())
      }
      "return empty Vector() if set of courses contains unexisting course name" in {
        val result = userRepository.listForRolesAndCourses(Vector(TestValues.testRoleA.name, TestValues.testRoleB.name), Vector("unexisting course"))

        Await.result(result, Duration.Inf) should be (Vector())
      }
    }
  }

  "UserRepository.find" should {
    inSequence {
      "find a user by ID" in {
        val result = userRepository.find(TestValues.testUserA.id).map(_.get)

        val user = Await.result(result, Duration.Inf)
        user.id should be(TestValues.testUserA.id)
        user.version should be(TestValues.testUserA.version)
        user.email should be(TestValues.testUserA.email)
        user.username should be(TestValues.testUserA.username)
        user.givenname should be(TestValues.testUserA.givenname)
        user.surname should be(TestValues.testUserA.surname)
        user.createdAt.toString() should be (TestValues.testUserA.createdAt.toString())
        user.updatedAt.toString() should be (TestValues.testUserA.updatedAt.toString())
      }
      "be None if user wasn't found by ID" in {
        val result = userRepository.find(UUID("f9aadc67-5e8b-48f3-b0a2-20a0d7d88477"))

        Await.result(result, Duration.Inf) should be (None)
      }
      "find a user by their identifiers - email" in {
        val result = userRepository.find(TestValues.testUserA.email).map(_.get)

        val user = Await.result(result, Duration.Inf)

        user.id should be(TestValues.testUserA.id)
        user.version should be(TestValues.testUserA.version)
        user.email should be(TestValues.testUserA.email)
        user.username should be(TestValues.testUserA.username)
        user.givenname should be(TestValues.testUserA.givenname)
        user.surname should be(TestValues.testUserA.surname)
        user.createdAt.toString should be(TestValues.testUserA.createdAt.toString)
        user.updatedAt.toString should be(TestValues.testUserA.updatedAt.toString)
      }
      "find a user by their identifiers - username" in {
        val result = userRepository.find(TestValues.testUserB.username).map(_.get)

        val user = Await.result(result, Duration.Inf)

        user.id should be(TestValues.testUserB.id)
        user.version should be(TestValues.testUserB.version)
        user.email should be(TestValues.testUserB.email)
        user.username should be(TestValues.testUserB.username)
        user.givenname should be(TestValues.testUserB.givenname)
        user.surname should be(TestValues.testUserB.surname)
        user.createdAt.toString should be(TestValues.testUserB.createdAt.toString)
        user.updatedAt.toString should be(TestValues.testUserB.updatedAt.toString)
      }
      "not find a user by their identifiers - email (unexisting email)" in {
        val result = userRepository.find("unexisting_email@example.com")

        val user = Await.result(result, Duration.Inf)

        user should be (None)
      }
      "not find a user by their identifiers - username (unexisting username)" in {
        val result = userRepository.find("unexisting_username")

        val user = Await.result(result, Duration.Inf)

        user should be (None)
      }
    }
  }

  "UserRepository.findByEmail" should {
    inSequence {
      "find a user by e-mail address" in {
        val result = userRepository.find(TestValues.testUserC.email).map(_.get)

        val user = Await.result(result, Duration.Inf)

        user.id should be (TestValues.testUserC.id)
        user.version should be (TestValues.testUserC.version)
        user.email should be (TestValues.testUserC.email)
        user.username should be (TestValues.testUserC.username)
        user.givenname should be (TestValues.testUserC.givenname)
        user.surname should be (TestValues.testUserC.surname)
        user.createdAt.toString should be(TestValues.testUserC.createdAt.toString)
        user.updatedAt.toString should be(TestValues.testUserC.updatedAt.toString)
      }
      "not find a user by unexisting e-mail address" in {
        val result = userRepository.find("unexisting_email@example.com")

        val user = Await.result(result, Duration.Inf)

        user should be (None)
      }
    }
  }

  "UserRepository.update" should {
    inSequence {
      "update an existing user" in {

        val result = userRepository.update(TestValues.testUserA.copy(
          email = "newtestUserA@example.com",
          username = "newtestUserA",
          givenname = "newTestA",
          surname = "newUserA"
        ))

        val user = Await.result(result, Duration.Inf)

        user.id should be (TestValues.testUserA.id)
        user.version should be (TestValues.testUserA.version + 1)
        user.email should be ("newtestUserA@example.com")
        user.username should be ("newtestUserA")
        user.givenname should be ("newTestA")
        user.surname should be ("newUserA")
        user.createdAt.toString should be(TestValues.testUserA.createdAt.toString)
        user.updatedAt.toString should not be(TestValues.testUserA.updatedAt.toString)

        // Find updated user and check
        val queryResult = db.pool.sendPreparedStatement(SelectOneByIdentifier, Array[Any]("newtestUserA@example.com", "newtestUserA@example.com")).map { queryResult =>
          val userList = queryResult.rows.get.map {
            item: RowData => User(item)
          }
          userList
        }

        val userList = Await.result(queryResult, Duration.Inf)
        val updated_user = userList(0)

        updated_user.id should be (TestValues.testUserA.id)
        updated_user.version should be (TestValues.testUserA.version + 1)
        updated_user.email should be ("newtestUserA@example.com")
        updated_user.username should be ("newtestUserA")
        updated_user.givenname should be ("newTestA")
        updated_user.surname should be ("newUserA")
        user.createdAt.toString should be(TestValues.testUserA.createdAt.toString)
        updated_user.updatedAt.toString() should not be (TestValues.testUserA.updatedAt.toString())
      }
      "throw a NoSuchElementException when update an existing user with wrong version" in {
        val result = userRepository.update(TestValues.testUserB.copy(
          version = 99L,
          username = "newtestUserB",
          givenname = "newTestB",
          surname = "newUserB"
        ))

        an [java.util.NoSuchElementException] should be thrownBy Await.result(result, Duration.Inf)
      }
      "throw a GenericDatabaseException when update an existing user with username that already exists" in {
        val result = userRepository.update(TestValues.testUserB.copy(
          username = TestValues.testUserC.username
        ))

        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
      }
      "throw a GenericDatabaseException when update an existing user with email that already exists" in {
        val result = userRepository.update(TestValues.testUserB.copy(
          email = TestValues.testUserC.email
        ))

        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
      }
      "throw a NoSuchElementException when update an unexisting user" in {
        an [java.util.NoSuchElementException] should be thrownBy userRepository.update(User(
          email = "unexisting_email@example.com",
          username = "unexisting_username",
          givenname = "unexisting_givenname",
          surname = "unexisting_surname"
        ))
      }
    }
  }

  "UserRepository.insert" should {
    inSequence {
      "save a new User" in {
        val result = userRepository.insert(TestValues.testUserD)

        val user = Await.result(result, Duration.Inf)

        user.id should be (TestValues.testUserD.id)
        user.version should be (1L)
        user.email should be (TestValues.testUserD.email)
        user.username should be (TestValues.testUserD.username)
        user.givenname should be (TestValues.testUserD.givenname)
        user.surname should be (TestValues.testUserD.surname)

        // Find new user and check
        val queryResult = db.pool.sendPreparedStatement(SelectOneByIdentifier, Array[Any](TestValues.testUserD.email, TestValues.testUserD.email)).map { queryResult =>
          val userList = queryResult.rows.get.map {
            item: RowData => User(item)
          }
          userList
        }

        val userList = Await.result(queryResult, Duration.Inf)
        val new_user = userList(0)

        new_user.id should be (TestValues.testUserD.id)
        new_user.version should be (1L)
        new_user.email should be (TestValues.testUserD.email)
        new_user.username should be (TestValues.testUserD.username)
        new_user.givenname should be (TestValues.testUserD.givenname)
        new_user.surname should be (TestValues.testUserD.surname)
      }
      "throw a GenericDatabaseException if user already exists" in {
        val result = userRepository.insert(TestValues.testUserA)

        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
      }
      "throw a GenericDatabaseException if username already exists" in {
        val result = userRepository.insert(User(
          email = "unexistinguser@example.com",
          username = TestValues.testUserB.username,
          passwordHash = Some("$s0$100801$LmS/oJ7gIulUSr4qJ9by2A==$c91t4yMA594s092V4LB89topw5Deo10BXowjW3W1234="),
          givenname = "unexisting",
          surname = "user"
        ))

        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
      }
      "throw a GenericDatabaseException if email already exists" in {
        val result = userRepository.insert(User(
          email = TestValues.testUserB.email,
          username = "unexisting user",
          passwordHash = Some("$s0$100801$LmS/oJ7gIulUSr4qJ9by2A==$c91t4yMA594s092V4LB89topw5Deo10BXowjW3W1234="),
          givenname = "unexisting",
          surname = "user"
        ))

        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
      }
    }
  }

  "UserRepository.delete" should {
    inSequence {
      "delete a user from the database if user has no references in other tables" in {
        val result = userRepository.delete(TestValues.testUserC)

        val is_deleted = Await.result(result, Duration.Inf)
        is_deleted should be (true)

        // Check if user has been deleted
        val queryResult = db.pool.sendPreparedStatement(SelectOneByIdentifier, Array[Any](TestValues.testUserC.email, TestValues.testUserC.email)).map { queryResult =>
          val userList = queryResult.rows.get.map {
            item: RowData => User(item)
          }
          userList
        }

        Await.result(queryResult, Duration.Inf) should be (Vector())
      }
      "throw a GenericDatabaseException if user is teacher and has references in other tables" in {
        val result = userRepository.delete(TestValues.testUserB)

        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
      }
      "return FALSE if User hasn't been found" in {
        val result = userRepository.delete(User(
          email = "unexisting_email@example.com",
          username = "unexisting_username",
          givenname = "unexisting_givenname",
          surname = "unexisting_surname"
        ))

        Await.result(result, Duration.Inf) should be(false)
      }
    }
  }
}



