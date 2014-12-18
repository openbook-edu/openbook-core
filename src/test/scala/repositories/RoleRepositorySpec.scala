import java.awt.Color
import java.io.File
import com.github.mauricio.async.db.RowData

import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.models.{Role, Class, User}
import ca.shiftfocus.krispii.core.repositories.RoleRepositoryPostgresComponent
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import ca.shiftfocus.uuid.UUID
import grizzled.slf4j.Logger
import org.joda.time.{DateTimeZone, DateTime}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{MustMatchers, WordSpec, BeforeAndAfterAll, Suite}
import org.scalatest._
import Matchers._


import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait RoleRepoTestEnvironment
  extends RoleRepositoryPostgresComponent
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

  val testRoleA = Role(
    id = UUID("1430e950-77f9-4b30-baf8-bb226fc7091a"),
    version = 1L,
    name = "test role A",
    createdAt = Option(new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04"))),
    updatedAt = Option(new DateTime(2014, 8, 10, 14, 1, 19, 545, DateTimeZone.forID("-04")))
  )

  val testRoleB = Role(
    id = UUID("a011504c-d118-40cd-b9eb-6e10d5738c67"),
    version = 2L,
    name = "test role B",
    createdAt = Option(new DateTime(2014, 8, 11, 14, 1, 19, 545, DateTimeZone.forID("-04"))),
    updatedAt = Option(new DateTime(2014, 8, 12, 14, 1, 19, 545, DateTimeZone.forID("-04")))
  )

  val testRoleC = Role(
    id = UUID("31a4c2e6-762a-4303-bbb8-e64c24048920"),
    version = 3L,
    name = "test role C",
    createdAt = Option(new DateTime(2014, 8, 13, 14, 1, 19, 545, DateTimeZone.forID("-04"))),
    updatedAt = Option(new DateTime(2014, 8, 14, 14, 1, 19, 545, DateTimeZone.forID("-04")))
  )

  // New role no data in DB
  val testRoleD = Role(
    id = UUID("b82d356d-a1bb-4e07-b28f-d15060fb42c2"),
    name = "test role D"
  )

  // New role no data in DB
  val testRoleE = Role(
    id = UUID("29a84d7b-f90a-4a26-a224-b70631fdfbe4"),
    name = "test role E"
  )

}

class RoleRepositorySpec extends WordSpec
with MustMatchers
with MockFactory
with RoleRepoTestEnvironment {

  "RoleRepository.list" should {
    inSequence {
      "list all roles" in {
        val result = roleRepository.list

        val roles = Await.result(result, Duration.Inf)

        roles should be(Vector(testRoleA, testRoleB, testRoleC))
        Map[Int, Role](0 -> testRoleA, 1 -> testRoleB, 2 -> testRoleC).foreach {
          case (key, role: Role) => {
            roles(key).id should be(role.id)
            roles(key).version should be(role.version)
            roles(key).name should be(role.name)
            roles(key).createdAt.toString should be(role.createdAt.toString)
            roles(key).updatedAt.toString should be(role.updatedAt.toString)
          }
        }
      }
      "list the roles associated with a user" in {
        val result = roleRepository.list(testUserA)

        val roles = Await.result(result, Duration.Inf)

        roles should be(Vector(testRoleA, testRoleB))
        Map[Int, Role](0 -> testRoleA, 1 -> testRoleB).foreach {
          case (key, role: Role) => {
            roles(key).id should be(role.id)
            roles(key).version should be(role.version)
            roles(key).name should be(role.name)
            roles(key).createdAt.toString should be(role.createdAt.toString)
            roles(key).updatedAt.toString should be(role.updatedAt.toString)
          }
        }
      }
      "return empty Vector() if such user doesn't exist" in {
        val result = roleRepository.list(User(
          email = "unexisting_email@example.com",
          username = "unexisting_username",
          givenname = "unexisting_givenname",
          surname = "unexisting_surname"
        ))

        val roles = Await.result(result, Duration.Inf)

        roles should be (Vector())
      }
      "list the roles associated with a users" in {
        val result = roleRepository.list(Vector(testUserA, testUserB))

        val roles = Await.result(result, Duration.Inf)
        roles should be(Map(
          testUserA.id -> Vector(testRoleA, testRoleB),
          testUserB.id -> Vector(testRoleB)
        ))

        // Check for testUserA
        Map[Int, Role](0 -> testRoleA, 1 -> testRoleB).foreach {
          case (key, role: Role) => {
            roles(testUserA.id)(key).id should be(role.id)
            roles(testUserA.id)(key).version should be(role.version)
            roles(testUserA.id)(key).name should be(role.name)
            roles(testUserA.id)(key).createdAt.toString should be(role.createdAt.toString)
            roles(testUserA.id)(key).updatedAt.toString should be(role.updatedAt.toString)
          }
        }

        // Check for testUserB
        Map[Int, Role](0 -> testRoleB).foreach {
          case (key, role: Role) => {
            roles(testUserB.id)(key).id should be(role.id)
            roles(testUserB.id)(key).version should be(role.version)
            roles(testUserB.id)(key).name should be(role.name)
            roles(testUserB.id)(key).createdAt.toString should be(role.createdAt.toString)
            roles(testUserB.id)(key).updatedAt.toString should be(role.updatedAt.toString)
          }
        }
      }
    }
  }

  "RoleRepository.find" should {
    inSequence {
      "find a single entry by ID" in {
        val result = roleRepository.find(testRoleA.id).map(_.get)

        val role = Await.result(result, Duration.Inf)
        role.id should be(testRoleA.id)
        role.version should be(testRoleA.version)
        role.name should be(testRoleA.name)
        role.createdAt.toString should be(testRoleA.createdAt.toString)
        role.updatedAt.toString should be(testRoleA.updatedAt.toString)
      }
      "throw an exception if entry wasn't found by ID" in {
        val result = roleRepository.find(UUID("f9aadc67-5e8b-48f3-b0a2-20a0d7d88477")).map(_.get)

        an[java.util.NoSuchElementException] should be thrownBy Await.result(result, Duration.Inf)
      }
      "find a single entry by name" in {
        val result = roleRepository.find(testRoleA.name).map(_.get)

        val role = Await.result(result, Duration.Inf)
        role.id should be(testRoleA.id)
        role.version should be(testRoleA.version)
        role.name should be(testRoleA.name)
        role.createdAt.toString should be(testRoleA.createdAt.toString)
        role.updatedAt.toString should be(testRoleA.updatedAt.toString)
      }
      "throw an exception if entry wasn't found by name" in {
        val result = roleRepository.find("unexisting_role_name").map(_.get)

        an[java.util.NoSuchElementException] should be thrownBy Await.result(result, Duration.Inf)
      }
    }
  }

  "RoleRepository.addUsers" should {
    inSequence {
      "add roles to users" in {
        val query_result = roleRepository.addUsers(testRoleC, Vector(testUserA, testUserB))

        Await.result(query_result, Duration.Inf) should be (true)

        // Find user roles by user ID
        val find_roles_query = """
          SELECT id, version, roles.name as name, roles.created_at as created_at, updated_at
          FROM roles, users_roles
          WHERE roles.id = users_roles.role_id
            AND users_roles.user_id = ?
                    """

        // Find roles for testUserA
        val resultForUserA = db.pool.sendPreparedStatement(find_roles_query, Array[Any](testUserA.id.bytes)).map { queryResult =>
          val roleList = queryResult.rows.get.map {
            item: RowData => Role(item)
          }
          roleList
        }

        val roleListUserA = Await.result(resultForUserA, Duration.Inf)
        roleListUserA contains testRoleC should be (true)

        // Find roles for testUserA
        val resultForUserB = db.pool.sendPreparedStatement(find_roles_query, Array[Any](testUserB.id.bytes)).map { queryResult =>
          val roleList = queryResult.rows.get.map {
            item: RowData => Role(item)
          }
          roleList
        }

        val roleListUserB = Await.result(resultForUserB, Duration.Inf)
        roleListUserB contains testRoleC should be (true)
      }
    }
    "throw an exception if we add the role to the user that already has this role" in {
      val query_result = roleRepository.addUsers(testRoleB, Vector(testUserA))

      an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy  Await.result(query_result, Duration.Inf)
    }
  }

  "RoleRepository.removeUsers" should {
    inSequence {
      "remove role from users" in {
        val query_result = roleRepository.removeUsers(testRoleB, Vector(testUserA, testUserB))

        Await.result(query_result, Duration.Inf) should be (true)

        // Find user roles by user ID
        val find_roles_query = """
          SELECT id, version, roles.name as name, roles.created_at as created_at, updated_at
          FROM roles, users_roles
          WHERE roles.id = users_roles.role_id
            AND users_roles.user_id = ?
                    """

        // Find roles for testUserA
        val resultForUserA = db.pool.sendPreparedStatement(find_roles_query, Array[Any](testUserA.id.bytes)).map { queryResult =>
          val roleList = queryResult.rows.get.map {
            item: RowData => Role(item)
          }
          roleList
        }

        val roleListUserA = Await.result(resultForUserA, Duration.Inf)
        roleListUserA contains testRoleB should be (false)

        // Find roles for testUserA
        val resultForUserB = db.pool.sendPreparedStatement(find_roles_query, Array[Any](testUserB.id.bytes)).map { queryResult =>
          val roleList = queryResult.rows.get.map {
            item: RowData => Role(item)
          }
          roleList
        }

        val roleListUserB = Await.result(resultForUserB, Duration.Inf)
        roleListUserB contains testRoleB should be (false)
      }
    }
    "return FALSE if the user doesn't have this role" in {
      val query_result = roleRepository.removeUsers(testRoleA, Vector(testUserB))

      val role = Await.result(query_result, Duration.Inf)
      role should be (false)
    }
  }

  "RoleRepository.insert" should {
    inSequence {
      "save a Role row" in {
        val result = roleRepository.insert(Role(
          id = testRoleD.id,
          name = testRoleD.name
        ))

        val role = Await.result(result, Duration.Inf)
        role.id should be(testRoleD.id)
        role.name should be(testRoleD.name)

        // Check Role record
        val checkResult = roleRepository.find(testRoleD.id).map(_.get)

        val checkRole = Await.result(checkResult, Duration.Inf)
        checkRole.id should be(testRoleD.id)
        checkRole.name should be(testRoleD.name)
      }
      "throw an exception if role already exists" in {
        val result = roleRepository.insert(testRoleA)

        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
      }
    }
  }

  "RoleRepository.update" should {
    inSequence {
      "update an existing Role" in {
        val result = roleRepository.update(testRoleC.copy(
          name = "new test role C"
        ))

        val role = Await.result(result, Duration.Inf)
        role.name should be("new test role C")

        // Check Role record
        val checkResult = roleRepository.find(testRoleC.id).map(_.get)

        val checkRole = Await.result(checkResult, Duration.Inf)
        checkRole.name should be("new test role C")
      }
      "throw an exception when update an existing Role with wrong version" in {
        val result = roleRepository.update(testRoleC.copy(
          version = 99L,
          name = "new test role C"
        ))

        an[java.util.NoSuchElementException] should be thrownBy Await.result(result, Duration.Inf)
      }
      "throw an exception when update an unexisting Role" in {
        val result = roleRepository.update(Role(
          name = "test role E"
        ))

        an[java.util.NoSuchElementException] should be thrownBy Await.result(result, Duration.Inf)
      }
    }
  }

  "RoleRepository.delete" should {
    inSequence{
      "delete role if role has no references in other tables" in {
        val result = roleRepository.delete(testRoleB)

        Await.result(result, Duration.Inf) should be (true)

        // Check if role has been deleted
        val result2 = roleRepository.find(testRoleB.id)

        val deleted_role = Await.result(result2, Duration.Inf)
        deleted_role should be (None)
      }
      "throw an exception if role has references in other tables" in {
        val result = roleRepository.delete(testRoleA)

        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
      }
    }
  }

  "RoleRepository.addToUser" should {
    inSequence {
      "associate a role (by object) to a user" in {
        val query_result = roleRepository.addToUser(testUserC, testRoleC)

        Await.result(query_result, Duration.Inf) should be (true)

        // Find user roles by user ID
        val find_roles_query = """
          SELECT id, version, roles.name as name, roles.created_at as created_at, updated_at
          FROM roles, users_roles
          WHERE roles.id = users_roles.role_id
            AND users_roles.user_id = ?
                               """

        // Find roles for testUserC
        val result = db.pool.sendPreparedStatement(find_roles_query, Array[Any](testUserC.id.bytes)).map { queryResult =>
          val roleList = queryResult.rows.get.map {
            item: RowData => Role(item)
          }
          roleList
        }

        val roleList = Await.result(result, Duration.Inf)
        roleList contains testRoleC should be (true)
      }

      "associate a role (by name) to a user" in {
        val query_result = roleRepository.addToUser(testUserC, testRoleA.name)

        Await.result(query_result, Duration.Inf) should be (true)

        // Find user roles by user ID
        val find_roles_query = """
          SELECT id, version, roles.name as name, roles.created_at as created_at, updated_at
          FROM roles, users_roles
          WHERE roles.id = users_roles.role_id
            AND users_roles.user_id = ?
                               """

        // Find roles for testUserC
        val result = db.pool.sendPreparedStatement(find_roles_query, Array[Any](testUserC.id.bytes)).map { queryResult =>
          val roleList = queryResult.rows.get.map {
            item: RowData => Role(item)
          }
          roleList
        }

        val roleList = Await.result(result, Duration.Inf)
        roleList contains testRoleA should be (true)
      }
      "throw an exception if user doesn't exist" in {
        val query_result = roleRepository.addToUser(testUserD, testRoleA)

        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(query_result, Duration.Inf)
      }
      "throw an exception if role (object) doesn't exist" in {
        val query_result = roleRepository.addToUser(testUserA, testRoleE)

        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(query_result, Duration.Inf)
      }
      "return FALSE if role (name) doesn't exist" in {
        val query_result = roleRepository.addToUser(testUserA, testRoleE.name)

        Await.result(query_result, Duration.Inf) should be (false)
      }
    }
  }

  // testRoleA from testUserA
  "RoleRepository.removeFromUser" should {
    inSequence {
      "not finished yet" in {

      }
    }
  }

  "RoleRepository.removeFromAllUsers" should {
    inSequence {
      "not finished yet" in {

      }
    }
  }
}
