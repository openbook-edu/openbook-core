import ca.shiftfocus.krispii.core.models.Project
import ca.shiftfocus.krispii.core.repositories.ProjectRepositoryPostgresComponent
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import grizzled.slf4j.Logger
import org.joda.time.{DateTimeZone, DateTime}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{MustMatchers, WordSpec, BeforeAndAfterAll, Suite}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait ProjectRepoTestEnvironment
  extends ProjectRepositoryPostgresComponent
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

  val testProjectA = Project(
    id = UUID("c9b4cfce-aed4-48fd-94f5-c980763dfddc"),
    classId = UUID("217c5622-ff9e-4372-8e6a-95fb3bae300b"),
    version = 1L,
    name = "test project A",
    slug = "test project slug A",
    description = "test project A description",
    availability = "any",
    parts = Vector(),
    createdAt = Option(new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04"))),
    updatedAt = Option(new DateTime(2014, 8, 10, 14, 1, 19, 545, DateTimeZone.forID("-04")))
  )
}

class ProjectRepositorySpec extends WordSpec
  with MustMatchers
  with MockFactory
  with ProjectRepoTestEnvironment {

  "ProjectRepository.list" should {
    inSequence {
      "list all projects" in {
        val result = projectRepository.list

        val projects = Await.result(result, Duration.Inf)

        projects should be(Vector(testProjectA, testProjectB))
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
}
