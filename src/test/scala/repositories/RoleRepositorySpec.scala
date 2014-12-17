import java.awt.Color
import java.io.File

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
  val data_schema_path = s"${project_path}/src/test/resources/schemas/roles/data_schema.sql"

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
  }
}
