import java.io.File
import com.github.mauricio.async.db.RowData

import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.models.{Role, User}
import ca.shiftfocus.krispii.core.repositories.RoleRepositoryPostgresComponent
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import ca.shiftfocus.uuid.UUID
import grizzled.slf4j.Logger
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

  // Find user roles by user ID
  val find_roles_query = """
    SELECT id, version, roles.name as name, roles.created_at as created_at, updated_at
    FROM roles, users_roles
    WHERE roles.id = users_roles.role_id
      AND users_roles.user_id = ?
                   """

  val SelectOne = """
      SELECT id, version, roles.name as name, roles.created_at as created_at, updated_at
      FROM roles
      WHERE id = ?
    """
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

        roles should be(Vector(TestValues.testRoleA, TestValues.testRoleB, TestValues.testRoleC, TestValues.testRoleF, TestValues.testRoleG))
        Map[Int, Role](0 -> TestValues.testRoleA, 1 -> TestValues.testRoleB, 2 -> TestValues.testRoleC, 3 -> TestValues.testRoleF, 4 -> TestValues.testRoleG).foreach {
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
        val result = roleRepository.list(TestValues.testUserA)

        val roles = Await.result(result, Duration.Inf)

        roles should be(Vector(TestValues.testRoleA, TestValues.testRoleB, TestValues.testRoleF, TestValues.testRoleG))
        Map[Int, Role](0 -> TestValues.testRoleA, 1 -> TestValues.testRoleB, 2 -> TestValues.testRoleF, 3 -> TestValues.testRoleG).foreach {
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
        val result = roleRepository.list(Vector(TestValues.testUserA, TestValues.testUserB))

        val roles = Await.result(result, Duration.Inf)
        roles should be(Map(
          TestValues.testUserA.id -> Vector(TestValues.testRoleA, TestValues.testRoleB, TestValues.testRoleF, TestValues.testRoleG),
          TestValues.testUserB.id -> Vector(TestValues.testRoleA, TestValues.testRoleB, TestValues.testRoleF, TestValues.testRoleG)
        ))

        // Check for TestValues.testUserA
        Map[Int, Role](0 -> TestValues.testRoleA, 1 -> TestValues.testRoleB, 2 -> TestValues.testRoleF, 3 -> TestValues.testRoleG).foreach {
          case (key, role: Role) => {
            roles(TestValues.testUserA.id)(key).id should be(role.id)
            roles(TestValues.testUserA.id)(key).version should be(role.version)
            roles(TestValues.testUserA.id)(key).name should be(role.name)
            roles(TestValues.testUserA.id)(key).createdAt.toString should be(role.createdAt.toString)
            roles(TestValues.testUserA.id)(key).updatedAt.toString should be(role.updatedAt.toString)
          }
        }

        // Check for TestValues.testUserB
        Map[Int, Role](0 -> TestValues.testRoleA, 1 -> TestValues.testRoleB, 2 -> TestValues.testRoleF, 3 -> TestValues.testRoleG).foreach {
          case (key, role: Role) => {
            roles(TestValues.testUserB.id)(key).id should be(role.id)
            roles(TestValues.testUserB.id)(key).version should be(role.version)
            roles(TestValues.testUserB.id)(key).name should be(role.name)
            roles(TestValues.testUserB.id)(key).createdAt.toString should be(role.createdAt.toString)
            roles(TestValues.testUserB.id)(key).updatedAt.toString should be(role.updatedAt.toString)
          }
        }
      }
      "return empty Vector() if such users doesn't exist" in {
        val result = roleRepository.list(Vector(
          User(
          id = UUID("a626c45c-8de2-45ea-a361-c64ced9ac58e"),
          email = "unexisting_email@example.com",
          username = "unexisting_username",
          givenname = "unexisting_givenname",
          surname = "unexisting_surname"
          ),
          User(
          id = UUID("054834b9-424e-493d-835b-9e5f49172cad"),
          email = "unexisting_email2@example.com",
          username = "unexisting_username2",
          givenname = "unexisting_givenname2",
          surname = "unexisting_surname2"
          )
        ))

        val roles = Await.result(result, Duration.Inf)

        roles(UUID("a626c45c-8de2-45ea-a361-c64ced9ac58e")) should be (Vector())
        roles(UUID("054834b9-424e-493d-835b-9e5f49172cad")) should be (Vector())
      }
    }
  }

  "RoleRepository.find" should {
    inSequence {
      "find a single entry by ID" in {
        val result = roleRepository.find(TestValues.testRoleA.id).map(_.get)

        val role = Await.result(result, Duration.Inf)
        role.id should be(TestValues.testRoleA.id)
        role.version should be(TestValues.testRoleA.version)
        role.name should be(TestValues.testRoleA.name)
        role.createdAt.toString should be(TestValues.testRoleA.createdAt.toString)
        role.updatedAt.toString should be(TestValues.testRoleA.updatedAt.toString)
      }
      "be NONE if entry wasn't found by ID" in {
        val result = roleRepository.find(UUID("f9aadc67-5e8b-48f3-b0a2-20a0d7d88477"))

        Await.result(result, Duration.Inf) should be (None)
      }
      "find a single entry by name" in {
        val result = roleRepository.find(TestValues.testRoleA.name).map(_.get)

        val role = Await.result(result, Duration.Inf)
        role.id should be(TestValues.testRoleA.id)
        role.version should be(TestValues.testRoleA.version)
        role.name should be(TestValues.testRoleA.name)
        role.createdAt.toString should be(TestValues.testRoleA.createdAt.toString)
        role.updatedAt.toString should be(TestValues.testRoleA.updatedAt.toString)
      }
      "be NONE if entry wasn't found by name" in {
        val result = roleRepository.find("unexisting_role_name")

        Await.result(result, Duration.Inf) should be (None)
      }
    }
  }

  "RoleRepository.addUsers" + Console.RED + Console.BOLD + " (NOTE: Please check Javadoc for this method) " + Console.RESET should {
    inSequence {
      "add roles to users" in {
        val query_result = roleRepository.addUsers(TestValues.testRoleC, Vector(TestValues.testUserA, TestValues.testUserB))

        Await.result(query_result, Duration.Inf) should be (true)

        // Find roles for TestValues.testUserA
        val resultForUserA = db.pool.sendPreparedStatement(find_roles_query, Array[Any](TestValues.testUserA.id.bytes)).map { queryResult =>
          val roleList = queryResult.rows.get.map {
            item: RowData => Role(item)
          }
          roleList
        }

        val roleListUserA = Await.result(resultForUserA, Duration.Inf)
        roleListUserA contains TestValues.testRoleC should be (true)

        // Find roles for TestValues.testUserB
        val resultForUserB = db.pool.sendPreparedStatement(find_roles_query, Array[Any](TestValues.testUserB.id.bytes)).map { queryResult =>
          val roleList = queryResult.rows.get.map {
            item: RowData => Role(item)
          }
          roleList
        }

        val roleListUserB = Await.result(resultForUserB, Duration.Inf)
        roleListUserB contains TestValues.testRoleC should be (true)
      }
    }
    "throw a GenericDatabaseException if we add a role to the user that already has this role" in {
      val query_result = roleRepository.addUsers(TestValues.testRoleB, Vector(TestValues.testUserA))

      an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy  Await.result(query_result, Duration.Inf)
    }
    "throw a GenericDatabaseException if we add a role to unexisting user" in {
      val query_result = roleRepository.addUsers(TestValues.testRoleB, Vector(TestValues.testUserD))

      an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy  Await.result(query_result, Duration.Inf)
    }
  }

  "RoleRepository.removeUsers" + Console.RED + Console.BOLD + " (NOTE: Please check Javadoc for this method) " + Console.RESET should {
    inSequence {
      "remove role from users" in {
        val query_result = roleRepository.removeUsers(TestValues.testRoleB, Vector(TestValues.testUserA, TestValues.testUserB))

        Await.result(query_result, Duration.Inf) should be (true)

        // Find roles for TestValues.testUserA
        val resultForUserA = db.pool.sendPreparedStatement(find_roles_query, Array[Any](TestValues.testUserA.id.bytes)).map { queryResult =>
          val roleList = queryResult.rows.get.map {
            item: RowData => Role(item)
          }
          roleList
        }

        val roleListUserA = Await.result(resultForUserA, Duration.Inf)
        roleListUserA contains TestValues.testRoleB should be (false)

        // Find roles for TestValues.testUserB
        val resultForUserB = db.pool.sendPreparedStatement(find_roles_query, Array[Any](TestValues.testUserB.id.bytes)).map { queryResult =>
          val roleList = queryResult.rows.get.map {
            item: RowData => Role(item)
          }
          roleList
        }

        val roleListUserB = Await.result(resultForUserB, Duration.Inf)
        roleListUserB contains TestValues.testRoleB should be (false)
      }
    }
    "return FALSE if the user doesn't have this role" in {
      val query_result = roleRepository.removeUsers(TestValues.testRoleA, Vector(TestValues.testUserC))

      val role = Await.result(query_result, Duration.Inf)
      role should be (false)
    }
  }

  "RoleRepository.insert" should {
    inSequence {
      "save a Role row" in {
        val result = roleRepository.insert(Role(
          id = TestValues.testRoleD.id,
          name = TestValues.testRoleD.name
        ))

        val role = Await.result(result, Duration.Inf)
        role.id should be(TestValues.testRoleD.id)
        role.name should be(TestValues.testRoleD.name)
        role.version should be(1L)

        // Check Role record
        val queryResult = db.pool.sendPreparedStatement(SelectOne, Array[Any](TestValues.testRoleD.id.bytes)).map { queryResult =>
          val roleList = queryResult.rows.get.map {
            item: RowData => Role(item)
          }
          roleList
        }

        val roleList = Await.result(queryResult, Duration.Inf)
        roleList(0).id should be (TestValues.testRoleD.id)
        roleList(0).version should be (1L)
        roleList(0).name should be (TestValues.testRoleD.name)
      }
      "throw a GenericDatabaseException if role already exists" in {
        val result = roleRepository.insert(TestValues.testRoleA)

        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
      }
    }
  }

  "RoleRepository.update" + Console.RED + Console.BOLD + " (NOTE: Please check Javadoc for this method) " + Console.RESET should {
    inSequence {
      "update an existing Role" in {
        val result = roleRepository.update(TestValues.testRoleC.copy(
          name = "new test role C"
        ))

        val role = Await.result(result, Duration.Inf)
        role.name should be("new test role C")
        role.version should be(TestValues.testRoleC.version + 1)
        role.createdAt.toString should be (TestValues.testRoleC.createdAt.toString)
        role.updatedAt.toString should not be (TestValues.testRoleC.updatedAt.toString)

        // Check Role record
        val queryResult = db.pool.sendPreparedStatement(SelectOne, Array[Any](TestValues.testRoleC.id.bytes)).map { queryResult =>
          val roleList = queryResult.rows.get.map {
            item: RowData => Role(item)
          }
          roleList
        }

        val roleList = Await.result(queryResult, Duration.Inf)

        roleList(0).name should be("new test role C")
        roleList(0).version should be(TestValues.testRoleC.version + 1)
      }
      "throw a NoSuchElementException when update an existing Role with wrong version" in {
        val result = roleRepository.update(TestValues.testRoleC.copy(
          version = 99L,
          name = "new test role C"
        ))

        an[java.util.NoSuchElementException] should be thrownBy Await.result(result, Duration.Inf)
      }
      "throw a NoSuchElementException when update an unexisting Role" in {
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
        val result = roleRepository.delete(TestValues.testRoleB)

        Await.result(result, Duration.Inf) should be (true)

        // Check if role has been deleted
        val queryResult = db.pool.sendPreparedStatement(SelectOne, Array[Any](TestValues.testRoleB.id.bytes)).map { queryResult =>
          val roleList = queryResult.rows.get.map {
            item: RowData => Role(item)
          }
          roleList
        }

        Await.result(queryResult, Duration.Inf) should be (Vector())
      }
      "throw a GenericDatabaseException if role has references in other tables" in {
        val result = roleRepository.delete(TestValues.testRoleA)

        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
      }
      "return FALSE if Role hasn't been found" in {
        val result = roleRepository.delete(Role(
          name = "unexisting role"
        ))

        Await.result(result, Duration.Inf) should be(false)
      }
    }
  }

  "RoleRepository.addToUser" should {
    inSequence {
      "associate a role (by object) to a user" in {
        val query_result = roleRepository.addToUser(TestValues.testUserC, TestValues.testRoleC)

        Await.result(query_result, Duration.Inf) should be (true)

        // Find roles for TestValues.testUserC
        val result = db.pool.sendPreparedStatement(find_roles_query, Array[Any](TestValues.testUserC.id.bytes)).map { queryResult =>
          val roleList = queryResult.rows.get.map {
            item: RowData => Role(item)
          }
          roleList
        }

        val roleList = Await.result(result, Duration.Inf)
        roleList contains TestValues.testRoleC should be (true)
      }

      "associate a role (by name) to a user" in {
        val query_result = roleRepository.addToUser(TestValues.testUserC, TestValues.testRoleA.name)

        Await.result(query_result, Duration.Inf) should be (true)

        // Find roles for TestValues.testUserC
        val result = db.pool.sendPreparedStatement(find_roles_query, Array[Any](TestValues.testUserC.id.bytes)).map { queryResult =>
          val roleList = queryResult.rows.get.map {
            item: RowData => Role(item)
          }
          roleList
        }

        val roleList = Await.result(result, Duration.Inf)
        roleList contains TestValues.testRoleA should be (true)
      }
      "throw a GenericDatabaseException exception if user doesn't exist" in {
        val query_result = roleRepository.addToUser(TestValues.testUserD, TestValues.testRoleA)

        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(query_result, Duration.Inf)
      }
      "throw a GenericDatabaseException exception if role (object) doesn't exist" in {
        val query_result = roleRepository.addToUser(TestValues.testUserA, TestValues.testRoleE)

        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(query_result, Duration.Inf)
      }
      "return FALSE if role (name) doesn't exist" in {
        val query_result = roleRepository.addToUser(TestValues.testUserA, TestValues.testRoleE.name)

        Await.result(query_result, Duration.Inf) should be (false)
      }
    }
  }

  "RoleRepository.removeFromUser" should {
    inSequence {
      "remove role from user when role is object" in {
        val query_result = roleRepository.removeFromUser(TestValues.testUserA, TestValues.testRoleA)

        Await.result(query_result, Duration.Inf) should be (true)

        // Find roles for TestValues.testUserA
        val result = db.pool.sendPreparedStatement(find_roles_query, Array[Any](TestValues.testUserA.id.bytes)).map { queryResult =>
          val roleList = queryResult.rows.get.map {
            item: RowData => Role(item)
          }
          roleList
        }

        val roleList = Await.result(result, Duration.Inf)
        roleList contains TestValues.testRoleA should be (false)
      }
      "remove role from user by role name" + Console.RED + Console.BOLD + " (NOTE: Please check Javadoc for this method) " + Console.RESET in {
        val query_result = roleRepository.removeFromUser(TestValues.testUserB, TestValues.testRoleA.name)

        Await.result(query_result, Duration.Inf) should be (true)

        // Find roles for TestValues.testUserB
        val result = db.pool.sendPreparedStatement(find_roles_query, Array[Any](TestValues.testUserB.id.bytes)).map { queryResult =>
          val roleList = queryResult.rows.get.map {
            item: RowData => Role(item)
          }
          roleList
        }

        val roleList = Await.result(result, Duration.Inf)
        roleList contains TestValues.testRoleA should be (false)
      }
      "return FALSE if role (object) doesn't exist" in {
        val query_result = roleRepository.removeFromUser(TestValues.testUserA, TestValues.testRoleE)

        Await.result(query_result, Duration.Inf) should be (false)
      }
      "return FALSE if role (name) doesn't exist" in {
        val query_result = roleRepository.removeFromUser(TestValues.testUserA, TestValues.testRoleE.name)

        Await.result(query_result, Duration.Inf) should be (false)
      }
      "return FALSE if user doesn't exist" in {
        val query_result = roleRepository.removeFromUser(TestValues.testUserD, TestValues.testRoleA)

        Await.result(query_result, Duration.Inf) should be (false)
      }
    }
  }

  "RoleRepository.removeFromAllUsers" should {
    inSequence {
      "remove role from all users when role is object"  in {
        val query_result = roleRepository.removeFromAllUsers(TestValues.testRoleF)

        Await.result(query_result, Duration.Inf) should be (true)

        // Find roles for TestValues.testUserA
        val resultForUserA = db.pool.sendPreparedStatement(find_roles_query, Array[Any](TestValues.testUserA.id.bytes)).map { queryResult =>
          val roleList = queryResult.rows.get.map {
            item: RowData => Role(item)
          }
          roleList
        }

        val roleListUserA = Await.result(resultForUserA, Duration.Inf)
        roleListUserA contains TestValues.testRoleF should be (false)

        // Find roles for TestValues.testUserB
        val resultForUserB = db.pool.sendPreparedStatement(find_roles_query, Array[Any](TestValues.testUserB.id.bytes)).map { queryResult =>
          val roleList = queryResult.rows.get.map {
            item: RowData => Role(item)
          }
          roleList
        }

        val roleListUserB = Await.result(resultForUserB, Duration.Inf)
        roleListUserB contains TestValues.testRoleF should be (false)
      }
      "remove role from all users by role name" + Console.RED + Console.BOLD + " (NOTE: Please check Javadoc for this method) " + Console.RESET in {
        val query_result = roleRepository.removeFromAllUsers(TestValues.testRoleG.name)

        Await.result(query_result, Duration.Inf) should be (true)

        // Find roles for TestValues.testUserA
        val resultForUserA = db.pool.sendPreparedStatement(find_roles_query, Array[Any](TestValues.testUserA.id.bytes)).map { queryResult =>
          val roleList = queryResult.rows.get.map {
            item: RowData => Role(item)
          }
          roleList
        }

        val roleListUserA = Await.result(resultForUserA, Duration.Inf)
        roleListUserA contains TestValues.testRoleG should be (false)

        // Find roles for TestValues.testUserB
        val resultForUserB = db.pool.sendPreparedStatement(find_roles_query, Array[Any](TestValues.testUserB.id.bytes)).map { queryResult =>
          val roleList = queryResult.rows.get.map {
            item: RowData => Role(item)
          }
          roleList
        }

        val roleListUserB = Await.result(resultForUserB, Duration.Inf)
        roleListUserB contains TestValues.testRoleG should be (false)
      }
      "return FALSE if role (object) doesn't exist" in {
        val query_result = roleRepository.removeFromAllUsers(TestValues.testRoleE)

        Await.result(query_result, Duration.Inf) should be (false)
      }
      "return FALSE if role (name) doesn't exist" in {
        val query_result = roleRepository.removeFromAllUsers(TestValues.testRoleE.name)

        Await.result(query_result, Duration.Inf) should be (false)
      }
    }
  }
}
