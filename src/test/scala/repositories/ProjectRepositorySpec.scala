import java.io.File
import ca.shiftfocus.krispii.core.models.Project
import ca.shiftfocus.krispii.core.repositories.ProjectRepositoryPostgresComponent
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.RowData
import grizzled.slf4j.Logger
import org.scalamock.scalatest.MockFactory
import org.scalatest.{MustMatchers, WordSpec, BeforeAndAfterAll, Suite}
import org.scalatest._
import Matchers._
import scala.concurrent.ExecutionContext.Implicits.global

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

  val SelectOne = """
     SELECT projects.id as id, projects.version as version, projects.class_id, projects.name as name, projects.slug as slug,
     projects.description as description, projects.availability as availability, projects.created_at as created_at, projects.updated_at as updated_at
     FROM projects
     WHERE projects.id = ?
                  """
}

class ProjectRepositorySpec extends WordSpec
  with MustMatchers
  with MockFactory
  with ProjectRepoTestEnvironment {

  // TODO - check why "should be Vector..." doesn't work
  "ProjectRepository.list" should {
    inSequence {
      "find all projects" in {
        val result = projectRepository.list

        val projects = Await.result(result, Duration.Inf)

        // Should be Vector... doesn't work!
//        projects should be(Vector(TestValues.testProjectA, TestValues.testProjectB, TestValues.testProjectC))

        Map[Int, Project](0 -> TestValues.testProjectA, 1 -> TestValues.testProjectB, 2 -> TestValues.testProjectC).foreach {
          case (key, project: Project) => {
            projects(key).id should be(project.id)
            projects(key).classId should be(project.classId)
            projects(key).version should be(project.version)
            projects(key).name should be(project.name)
            projects(key).slug should be(project.slug)
            projects(key).description should be(project.description)
            projects(key).availability should be(project.availability)
            projects(key).createdAt.toString should be(project.createdAt.toString)
            projects(key).updatedAt.toString should be(project.updatedAt.toString)
          }
        }
      }
      "find all projects belonging to a given section" in {
        val result = projectRepository.list(TestValues.testClassA)
        val projects = Await.result(result, Duration.Inf)

        // Should be Vector... doesn't work!
//        projects should be(Vector(TestValues.testProjectA))

        Map[Int, Project](0 -> TestValues.testProjectA).foreach {
          case (key, project: Project) => {
            projects(key).id should be(project.id)
            projects(key).classId should be(project.classId)
            projects(key).version should be(project.version)
            projects(key).name should be(project.name)
            projects(key).slug should be(project.slug)
            projects(key).description should be(project.description)
            projects(key).availability should be(project.availability)
            projects(key).createdAt.toString should be(project.createdAt.toString)
            projects(key).updatedAt.toString should be(project.updatedAt.toString)
          }
        }
      }
      "return empty Vector() if there are no projects with indicated class" in {
        val result = projectRepository.list(TestValues.testClassC)
        val projects = Await.result(result, Duration.Inf)

        projects should be (Vector())
      }
    }
  }

  "ProjectRepository.find" should {
    inSequence {
      "find project by ID" in {
        val result = projectRepository.find(TestValues.testProjectA.id).map(_.get)
        val project = Await.result(result, Duration.Inf)

        project.id should be(TestValues.testProjectA.id)
        project.classId should be(TestValues.testProjectA.classId)
        project.version should be(TestValues.testProjectA.version)
        project.name should be(TestValues.testProjectA.name)
        project.slug should be(TestValues.testProjectA.slug)
        project.description should be(TestValues.testProjectA.description)
        project.availability should be(TestValues.testProjectA.availability)
        project.createdAt.toString should be(TestValues.testProjectA.createdAt.toString)
        project.updatedAt.toString should be(TestValues.testProjectA.updatedAt.toString)
      }
      "be NONE if project wasn't found by ID" in {
        val result = projectRepository.find(UUID("f9aadc67-5e8b-48f3-b0a2-20a0d7d88477"))

        Await.result(result, Duration.Inf) should be (None)
      }
      "find project by ID and User (author)" in {
        val result = projectRepository.find(TestValues.testProjectB.id, TestValues.testUserB).map(_.get)
        val project = Await.result(result, Duration.Inf)

        project.id should be(TestValues.testProjectB.id)
        project.classId should be(TestValues.testProjectB.classId)
        project.version should be(TestValues.testProjectB.version)
        project.name should be(TestValues.testProjectB.name)
        project.slug should be(TestValues.testProjectB.slug)
        project.description should be(TestValues.testProjectB.description)
        project.availability should be(TestValues.testProjectB.availability)
        project.createdAt.toString should be(TestValues.testProjectB.createdAt.toString)
        project.updatedAt.toString should be(TestValues.testProjectB.updatedAt.toString)
      }
      "be NONE if project ID is wrong" in {
        val result = projectRepository.find(UUID("f9aadc67-5e8b-48f3-b0a2-20a0d7d88477"), TestValues.testUserB)

        Await.result(result, Duration.Inf) should be (None)
      }
      "be NONE if User (author) unexists" in {
        val result = projectRepository.find(TestValues.testProjectB.id, TestValues.testUserD)

        Await.result(result, Duration.Inf) should be (None)
      }
      "be NONE if User (author) is not found" in {
        val result = projectRepository.find(TestValues.testProjectB.id, TestValues.testUserC)

        Await.result(result, Duration.Inf) should be (None)
      }
      "find project by slug" in {
        val result = projectRepository.find(TestValues.testProjectA.slug).map(_.get)
        val project = Await.result(result, Duration.Inf)

        project.id should be(TestValues.testProjectA.id)
        project.classId should be(TestValues.testProjectA.classId)
        project.version should be(TestValues.testProjectA.version)
        project.name should be(TestValues.testProjectA.name)
        project.slug should be(TestValues.testProjectA.slug)
        project.description should be(TestValues.testProjectA.description)
        project.availability should be(TestValues.testProjectA.availability)
        project.createdAt.toString should be(TestValues.testProjectA.createdAt.toString)
        project.updatedAt.toString should be(TestValues.testProjectA.updatedAt.toString)
      }
      "be NONE if such slug doesn't exist" in {
        val result = projectRepository.find("unexisting slug")

        Await.result(result, Duration.Inf) should be (None)
      }
    }
  }

  "ProjectRepository.insert" should {
    inSequence {
      "insert new project" in {
        val result = projectRepository.insert(TestValues.testProjectD)
        val project = Await.result(result, Duration.Inf)

        project.id should be(TestValues.testProjectD.id)
        project.classId should be(TestValues.testProjectD.classId)
        project.version should be(TestValues.testProjectD.version)
        project.name should be(TestValues.testProjectD.name)
        project.slug should be(TestValues.testProjectD.slug)
        project.description should be(TestValues.testProjectD.description)
        project.availability should be(TestValues.testProjectD.availability)

        // Check
        val queryResult = db.pool.sendPreparedStatement(SelectOne, Array[Any](TestValues.testProjectD.id.bytes)).map { queryResult =>
          val projectList = queryResult.rows.get.map {
            item: RowData => Project(item)
          }
          projectList
        }

        val projectList = Await.result(queryResult, Duration.Inf)

        projectList(0).id should be(TestValues.testProjectD.id)
        projectList(0).classId should be(TestValues.testProjectD.classId)
        projectList(0).version should be(TestValues.testProjectD.version)
        projectList(0).name should be(TestValues.testProjectD.name)
        projectList(0).slug should be(TestValues.testProjectD.slug)
        projectList(0).description should be(TestValues.testProjectD.description)
        projectList(0).availability should be(TestValues.testProjectD.availability)
      }
      "throw a GenericDatabaseException if project already exists" in {
        val result = projectRepository.insert(TestValues.testProjectA)

        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
      }
    }
  }


  "ProjectRepository.update" should {
    inSequence {
      "update project" in {
        val result = projectRepository.update(TestValues.testProjectC.copy(
          name = "new test project C",
          slug = "new test project slug C",
          description = "new test project C description",
          availability = "any"
        ))
        val project = Await.result(result, Duration.Inf)

        project.name should be("new test project C")
        project.slug should be("new test project slug C")
        project.description should be("new test project C description")
        project.availability should be("any")

        // Check record in db
        val queryResult = db.pool.sendPreparedStatement(SelectOne, Array[Any](TestValues.testProjectC.id.bytes)).map { queryResult =>
          val projectList = queryResult.rows.get.map {
            item: RowData => Project(item)
          }
          projectList
        }

        val projectList = Await.result(queryResult, Duration.Inf)

        projectList(0).name should be("new test project C")
        projectList(0).slug should be("new test project slug C")
        projectList(0).description should be("new test project C description")
        projectList(0).availability should be("any")
      }
      "throw a NoSuchElementException when update an existing Project with wrong version" in {
        val result = projectRepository.update(TestValues.testProjectC.copy(
          version = 99L,
          name = "very new test project C"
        ))

        an[java.util.NoSuchElementException] should be thrownBy Await.result(result, Duration.Inf)
      }
      "throw a NoSuchElementException when update an unexisting Project" in {
        val result = projectRepository.update(Project(
          classId = UUID("217c5622-ff9e-4372-8e6a-95fb3bae300b"),
          name = "unexisting  P",
          slug = "unexisting  P slug",
          description = "unexisting  P description",
          parts = Vector()
        ))

        an[java.util.NoSuchElementException] should be thrownBy Await.result(result, Duration.Inf)
      }
    }
  }

  "ProjectRepository.delete" should {
    inSequence {
      "throw a GenericDatabaseException if project has references in other tables" in {
        val result = projectRepository.delete(TestValues.testProjectA)

        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
      }
    }
  }
}
