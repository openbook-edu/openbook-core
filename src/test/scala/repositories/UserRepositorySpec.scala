import java.awt.Color
import java.io.File

import ca.shiftfocus.krispii.core.models.{Class, User}
import ca.shiftfocus.uuid.UUID
import org.joda.time.{DateTimeZone, DateTime}
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

  val testUserA = User(
    id = UUID("36c8c0ca-50aa-4806-afa5-916a5e33a81f"),
    version = 1L,
    email = "testUserA@example.com",
    username = "testUserA",
    passwordHash = Some("$s0$100801$SIZ9lgHz0kPMgtLB37Uyhw==$wyKhNrg/MmUvlYuVygDctBE5LHBjLB91nyaiTpjbeyM="),
    givenname = "TestA",
    surname = "UserA",
    createdAt = Option(new DateTime(2014, 8, 1, 14, 1, 19, 545, DateTimeZone.forID("-04"))),
    updatedAt = Option(new DateTime(2014, 8, 2, 14, 1, 19, 545, DateTimeZone.forID("-04")))
  )

  val testUserB = User(
    id = UUID("6c0e29bd-d05b-4b29-8115-6be93e936c59"),
    version = 2L,
    email = "testUserB@example.com",
    username = "testUserB",
    passwordHash = Some("$s0$100801$84r2edPRqM/8xFCe+G1PPw==$p7dTGjBJpGUMoyQ1Nqat1i4SBV6aT6BX7h1WU6cLRnc="),
    givenname = "TestB",
    surname = "UserB",
    createdAt = Option(new DateTime(2014, 8, 3, 14, 1, 19, 545, DateTimeZone.forID("-04"))),
    updatedAt = Option(new DateTime(2014, 8, 4, 14, 1, 19, 545, DateTimeZone.forID("-04")))
  )

  // User has no references in other tables
  val testUserC = User(
    id = UUID("f5f98407-3a0b-4ea5-952a-575886e90586"),
    version = 3L,
    email = "testUserC@example.com",
    username = "testUserC",
    passwordHash = Some("$s0$100801$LmS/oJ7gIulUSr4qJ9by2A==$c91t4yMA594s092V4LB89topw5Deo10BXowjW3WmWjo="),
    givenname = "TestC",
    surname = "UserC",
    createdAt = Option(new DateTime(2014, 8, 5, 14, 1, 19, 545, DateTimeZone.forID("-04"))),
    updatedAt = Option(new DateTime(2014, 8, 6, 14, 1, 19, 545, DateTimeZone.forID("-04")))
  )

  // New user no data in DB
  val testUserD = User(
    id = UUID("4d97f26c-df3f-4866-8919-11f51f14e9c4"),
    email = "testUserD@example.com",
    username = "testUserD",
    passwordHash = Some("$s0$100801$LmS/oJ7gIulUSr4qJ9by2A==$c91t4yMA594s092V4LB89topw5Deo10BXowjW3W1234="),
    givenname = "TestD",
    surname = "UserD"
  )

  val testClassA = Class(
    id = UUID("217c5622-ff9e-4372-8e6a-95fb3bae300b"),
    teacherId = Option(testUserA.id),
    name = "test class A",
    color = new Color(24, 6, 8)
  )

  val testClassB = Class(
    id = UUID("404c800a-5385-4e6b-867e-365a1e6b00de"),
    teacherId = Option(testUserB.id),
    name = "test class B",
    color = new Color(34, 8, 16)
  )
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

        users should be(Vector(testUserA, testUserB, testUserC))
        Map[Int, User](0 -> testUserA, 1 -> testUserB, 2 -> testUserC).foreach {
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
        val result = userRepository.list(Vector(testUserC.id, testUserA.id))

        val users = Await.result(result, Duration.Inf)

        users should be(Vector(testUserC, testUserA))
        Map[Int, User](0 -> testUserC, 1 -> testUserA).foreach {
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
      "list users in a given section(class)" in {
        val result = userRepository.list(testClassA)
        val result2 = userRepository.list(testClassB)

        val users = Await.result(result, Duration.Inf)
        val users2 = Await.result(result2, Duration.Inf)

        users should be(Vector(testUserA))
        users2 should be(Vector(testUserB))

        Map[Int, User](0 -> testUserA).foreach {
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

        Map[Int, User](0 -> testUserB).foreach {
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
    }
  }

  "UserRepository.listForSections" should {
    inSequence{
      "list the users belonging to a set of classes" in {
        val result = userRepository.listForSections(Vector(testClassA, testClassB))

        val users = Await.result(result, Duration.Inf)
        users should be (Vector(testUserA, testUserB))
        Map[Int, User](0 -> testUserA, 1 -> testUserB).foreach {
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
    }
  }

  "UserRepository.listForRoles" should {
    inSequence{
      "list the users one of a set of roles" in {
        val result = userRepository.listForRoles(Vector("test role A", "test role B"))

        val users = Await.result(result, Duration.Inf)
        users should be (Vector(testUserA, testUserB))
        Map[Int, User](0 -> testUserA, 1 -> testUserB).foreach {
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
    }
  }

  "UserRepository.listForRolesAndSections" should {
    inSequence{
      "list users filtering by both roles and classes" in {
        val result = userRepository.listForRolesAndSections(Vector("test role A", "test role B"), Vector("test class A", "test class B"))

        val users = Await.result(result, Duration.Inf)
        users should be (Vector(testUserA, testUserB))
        Map[Int, User](0 -> testUserA, 1 -> testUserB).foreach {
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
    }
  }

  "UserRepository.find" should {
    inSequence {
      "find a user by ID" in {
        val result = userRepository.find(testUserA.id).map(_.get)

        val user = Await.result(result, Duration.Inf)
        user.id should be(testUserA.id)
        user.version should be(testUserA.version)
        user.email should be(testUserA.email)
        user.username should be(testUserA.username)
        user.givenname should be(testUserA.givenname)
        user.surname should be(testUserA.surname)
        user.createdAt.toString() should be (testUserA.createdAt.toString())
        user.updatedAt.toString() should be (testUserA.updatedAt.toString())
      }
      "throw an exception if user wasn't found by ID" in {
        val result = userRepository.find(UUID("f9aadc67-5e8b-48f3-b0a2-20a0d7d88477")).map(_.get)

        an [java.util.NoSuchElementException] should be thrownBy Await.result(result, Duration.Inf)
      }
      "find a user by their identifiers - email" in {
        val result = userRepository.find(testUserA.email).map(_.get)

        val user = Await.result(result, Duration.Inf)

        user.id should be(testUserA.id)
        user.version should be(testUserA.version)
        user.email should be(testUserA.email)
        user.username should be(testUserA.username)
        user.givenname should be(testUserA.givenname)
        user.surname should be(testUserA.surname)
        user.createdAt.toString should be(testUserA.createdAt.toString)
        user.updatedAt.toString should be(testUserA.updatedAt.toString)
      }
      "find a user by their identifiers - username" in {
        val result = userRepository.find(testUserB.username).map(_.get)

        val user = Await.result(result, Duration.Inf)

        user.id should be(testUserB.id)
        user.version should be(testUserB.version)
        user.email should be(testUserB.email)
        user.username should be(testUserB.username)
        user.givenname should be(testUserB.givenname)
        user.surname should be(testUserB.surname)
        user.createdAt.toString should be(testUserB.createdAt.toString)
        user.updatedAt.toString should be(testUserB.updatedAt.toString)
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
        val result = userRepository.find(testUserC.email).map(_.get)

        val user = Await.result(result, Duration.Inf)

        user.id should be (testUserC.id)
        user.version should be (testUserC.version)
        user.email should be (testUserC.email)
        user.username should be (testUserC.username)
        user.givenname should be (testUserC.givenname)
        user.surname should be (testUserC.surname)
        user.createdAt.toString should be(testUserC.createdAt.toString)
        user.updatedAt.toString should be(testUserC.updatedAt.toString)
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

        val result = userRepository.update(testUserA.copy(
          email = "newtestUserA@example.com",
          username = "newtestUserA",
          givenname = "newTestA",
          surname = "newUserA"
        ))

        val user = Await.result(result, Duration.Inf)

        user.id should be (testUserA.id)
        user.version should be (testUserA.version + 1)
        user.email should be ("newtestUserA@example.com")
        user.username should be ("newtestUserA")
        user.givenname should be ("newTestA")
        user.surname should be ("newUserA")
        user.createdAt.toString should be(testUserA.createdAt.toString)
        user.updatedAt.toString should not be(testUserA.updatedAt.toString)

        // Find updated user and check
        val result2 = userRepository.find("newtestUserA@example.com").map(_.get)

        val updated_user = Await.result(result2, Duration.Inf)

        updated_user.id should be (testUserA.id)
        updated_user.version should be (testUserA.version + 1)
        updated_user.email should be ("newtestUserA@example.com")
        updated_user.username should be ("newtestUserA")
        updated_user.givenname should be ("newTestA")
        updated_user.surname should be ("newUserA")
        user.createdAt.toString should be(testUserA.createdAt.toString)
        updated_user.updatedAt.toString() should not be (testUserA.updatedAt.toString())
      }
      "throw an exception when update an existing user with wrong version" in {
        val result = userRepository.update(testUserB.copy(
          version = 99L,
          username = "newtestUserB",
          givenname = "newTestB",
          surname = "newUserB"
        ))

        an [java.util.NoSuchElementException] should be thrownBy Await.result(result, Duration.Inf)
      }
      "throw an exception when update an unexisting user" in {
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
        val result = userRepository.insert(testUserD)

        val user = Await.result(result, Duration.Inf)

        user.id should be (testUserD.id)
        user.version should be (testUserD.version)
        user.email should be (testUserD.email)
        user.username should be (testUserD.username)
        user.givenname should be (testUserD.givenname)
        user.surname should be (testUserD.surname)

        // Find new user and check
        val result2 = userRepository.find(testUserD.email).map(_.get)

        val new_user = Await.result(result2, Duration.Inf)

        new_user.id should be (testUserD.id)
        new_user.version should be (testUserD.version)
        new_user.email should be (testUserD.email)
        new_user.username should be (testUserD.username)
        new_user.givenname should be (testUserD.givenname)
        new_user.surname should be (testUserD.surname)
      }
      "throw an exception if user already exists" in {
        val result = userRepository.insert(testUserA)

        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
      }
    }
  }

  "UserRepository.delete" should {
    inSequence {
      "delete a user from the database if user has no references in other tables" in {
        val result = userRepository.delete(testUserC)

        val is_deleted = Await.result(result, Duration.Inf)
        is_deleted should be (true)

        // Check if user has been deleted
        val result2 = userRepository.find(testUserC.email)

        val deleted_user = Await.result(result2, Duration.Inf)
        deleted_user should be (None)
      }
      "throw an exception if user has references in other tables" in {
        val result = userRepository.delete(testUserB)
        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
      }
    }
  }
}



