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
    }
  }

  "RoleRepository.addUsers" should {
    inSequence {
      "add users" in {
        val result = roleRepository.addUsers(testRoleC, Vector(testUserA, testUserB))

        val query = """
          SELECT id, version, roles.name as name, roles.created_at as created_at, updated_at
          FROM roles, users_roles
          WHERE roles.id = users_roles.role_id
            AND users_roles.user_id = ?
                    """

        db.pool.sendPreparedStatement(query, Array[Any](testUserA.id.bytes)).map { queryResult =>
          val roleList = queryResult.rows.get.map {
            item: RowData => Role(item)
          }
        }
      }
    }
  }
}
