import java.io.File
import ca.shiftfocus.krispii.core.models.Project
import ca.shiftfocus.krispii.core.repositories.{PartRepositoryComponent, ProjectRepositoryPostgresComponent}
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.RowData
import grizzled.slf4j.Logger
import org.scalamock.scalatest.MockFactory
import org.scalatest.{MustMatchers, WordSpec, BeforeAndAfterAll, Suite}
import org.scalatest._
import Matchers._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration

trait ProjectRepoTestEnvironment
  extends ProjectRepositoryPostgresComponent
  with PartRepositoryComponent
  with Suite
  with BeforeAndAfterAll
  with MustMatchers
  with MockFactory
  with PostgresDB {
  val logger = Logger[this.type]

  /* START MOCK */
  override val partRepository = stub[PartRepository]
  /* END MOCK */

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
     SELECT *
     FROM projects
     WHERE projects.id = ?
                  """
}

class ProjectRepositorySpec
  extends WordSpec
  with ProjectRepoTestEnvironment {



  "ProjectRepository.list" should {
    inSequence {
      "find all projects" in {
        // Put here parts = Vector(), because after db query Project object is created without parts.
        (partRepository.list(_: Project)) when(TestValues.testProjectA.copy(parts = Vector())) returns(Future.successful(Vector(TestValues.testPartA, TestValues.testPartB)))
        (partRepository.list(_: Project)) when(TestValues.testProjectB.copy(parts = Vector())) returns(Future.successful(Vector(TestValues.testPartC)))
        (partRepository.list(_: Project)) when(TestValues.testProjectC.copy(parts = Vector())) returns(Future.successful(Vector()))

        val result = projectRepository.list

        val projects = Await.result(result, Duration.Inf)
        projects.toString should be(Vector(TestValues.testProjectA, TestValues.testProjectB, TestValues.testProjectC).toString)

        Map[Int, Project](0 -> TestValues.testProjectA, 1 -> TestValues.testProjectB, 2 -> TestValues.testProjectC).foreach {
          case (key, project: Project) => {
            projects(key).id should be(project.id)
            projects(key).courseId should be(project.courseId)
            projects(key).version should be(project.version)
            projects(key).name should be(project.name)
            projects(key).slug should be(project.slug)
            projects(key).description should be(project.description)
            projects(key).availability should be(project.availability)
            projects(key).parts should be(project.parts)
            projects(key).createdAt.toString should be(project.createdAt.toString)
            projects(key).updatedAt.toString should be(project.updatedAt.toString)
          }
        }
      }
      "find all projects belonging to a given section" in {
        // Put here parts = Vector(), because after db query Project object is created without parts.
        (partRepository.list(_: Project)) when(TestValues.testProjectA.copy(parts = Vector())) returns(Future.successful(Vector(TestValues.testPartA, TestValues.testPartB)))

        val result = projectRepository.list(TestValues.testCourseA)
        val projects = Await.result(result, Duration.Inf)

        projects.toString should be(Vector(TestValues.testProjectA).toString)

        Map[Int, Project](0 -> TestValues.testProjectA).foreach {
          case (key, project: Project) => {
            projects(key).id should be(project.id)
            projects(key).courseId should be(project.courseId)
            projects(key).version should be(project.version)
            projects(key).name should be(project.name)
            projects(key).slug should be(project.slug)
            projects(key).description should be(project.description)
            projects(key).availability should be(project.availability)
            projects(key).parts should be(project.parts)
            projects(key).createdAt.toString should be(project.createdAt.toString)
            projects(key).updatedAt.toString should be(project.updatedAt.toString)
          }
        }
      }
      "return empty Vector() if there are no projects with indicated course" in {
        val result = projectRepository.list(TestValues.testCourseC)
        val projects = Await.result(result, Duration.Inf)

        projects should be (Vector())
      }
    }
  }

  "ProjectRepository.find" should {
    inSequence {
      "find project by ID" in {
        // Put here parts = Vector(), because after db query Project object is created without parts.
        (partRepository.list(_: Project)) when(TestValues.testProjectA.copy(parts = Vector())) returns(Future.successful(Vector(TestValues.testPartA, TestValues.testPartB)))

        val result = projectRepository.find(TestValues.testProjectA.id).map(_.get)
        val project = Await.result(result, Duration.Inf)

        project.id should be(TestValues.testProjectA.id)
        project.courseId should be(TestValues.testProjectA.courseId)
        project.version should be(TestValues.testProjectA.version)
        project.name should be(TestValues.testProjectA.name)
        project.slug should be(TestValues.testProjectA.slug)
        project.description should be(TestValues.testProjectA.description)
        project.availability should be(TestValues.testProjectA.availability)
        project.parts should be(TestValues.testProjectA.parts)
        project.createdAt.toString should be(TestValues.testProjectA.createdAt.toString)
        project.updatedAt.toString should be(TestValues.testProjectA.updatedAt.toString)
      }
      "be NONE if project wasn't found by ID" in {
        val result = projectRepository.find(UUID("f9aadc67-5e8b-48f3-b0a2-20a0d7d88477"))

        Await.result(result, Duration.Inf) should be (None)
      }
      "find project by ID and User (teacher)" in {
        // Put here parts = Vector(), because after db query Project object is created without parts.
        (partRepository.list(_: Project)) when(TestValues.testProjectB.copy(parts = Vector())) returns(Future.successful(Vector(TestValues.testPartC)))

        val result = projectRepository.find(TestValues.testProjectB.id, TestValues.testUserB).map(_.get)
        val project = Await.result(result, Duration.Inf)

        project.id should be(TestValues.testProjectB.id)
        project.courseId should be(TestValues.testProjectB.courseId)
        project.version should be(TestValues.testProjectB.version)
        project.name should be(TestValues.testProjectB.name)
        project.slug should be(TestValues.testProjectB.slug)
        project.description should be(TestValues.testProjectB.description)
        project.availability should be(TestValues.testProjectB.availability)
        project.createdAt.toString should be(TestValues.testProjectB.createdAt.toString)
        project.updatedAt.toString should be(TestValues.testProjectB.updatedAt.toString)
      }
      "find project by ID and User (student)" in {
        // Put here parts = Vector(), because after db query Project object is created without parts.
        (partRepository.list(_: Project)) when(TestValues.testProjectB.copy(parts = Vector())) returns(Future.successful(Vector(TestValues.testPartC)))

        val result = projectRepository.find(TestValues.testProjectB.id, TestValues.testUserG).map(_.get)
        val project = Await.result(result, Duration.Inf)

        project.id should be(TestValues.testProjectB.id)
        project.courseId should be(TestValues.testProjectB.courseId)
        project.version should be(TestValues.testProjectB.version)
        project.name should be(TestValues.testProjectB.name)
        project.slug should be(TestValues.testProjectB.slug)
        project.description should be(TestValues.testProjectB.description)
        project.availability should be(TestValues.testProjectB.availability)
        project.createdAt.toString should be(TestValues.testProjectB.createdAt.toString)
        project.updatedAt.toString should be(TestValues.testProjectB.updatedAt.toString)
      }
      "be NONE if user is not connected with a project" in {
        // Put here parts = Vector(), because after db query Project object is created without parts.
        (partRepository.list(_: Project)) when(TestValues.testProjectB.copy(parts = Vector())) returns(Future.successful(Vector(TestValues.testPartC)))

        val result = projectRepository.find(TestValues.testProjectB.id, TestValues.testUserA)

        Await.result(result, Duration.Inf) should be (None)
      }
      "be NONE if project ID is wrong" in {
        val result = projectRepository.find(UUID("f9aadc67-5e8b-48f3-b0a2-20a0d7d88477"), TestValues.testUserB)

        Await.result(result, Duration.Inf) should be (None)
      }
      "be NONE if User (author) unexists" in {
        // Put here parts = Vector(), because after db query Project object is created without parts.
        (partRepository.list(_: Project)) when(TestValues.testProjectB.copy(parts = Vector())) returns(Future.successful(Vector(TestValues.testPartC)))

        val result = projectRepository.find(TestValues.testProjectB.id, TestValues.testUserD)

        Await.result(result, Duration.Inf) should be (None)
      }
      "be NONE if User (author) is not found" in {
        // Put here parts = Vector(), because after db query Project object is created without parts.
        (partRepository.list(_: Project)) when(TestValues.testProjectB.copy(parts = Vector())) returns(Future.successful(Vector(TestValues.testPartC)))

        val result = projectRepository.find(TestValues.testProjectB.id, TestValues.testUserC)

        Await.result(result, Duration.Inf) should be (None)
      }
      "find project by slug" in {
        // Put here parts = Vector(), because after db query Project object is created without parts.
        (partRepository.list(_: Project)) when(TestValues.testProjectA.copy(parts = Vector())) returns(Future.successful(Vector(TestValues.testPartA, TestValues.testPartB)))

        val result = projectRepository.find(TestValues.testProjectA.slug).map(_.get)
        val project = Await.result(result, Duration.Inf)

        project.id should be(TestValues.testProjectA.id)
        project.courseId should be(TestValues.testProjectA.courseId)
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
        project.courseId should be(TestValues.testProjectD.courseId)
        project.version should be(1L)
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
        projectList(0).courseId should be(TestValues.testProjectD.courseId)
        projectList(0).version should be(1L)
        projectList(0).name should be(TestValues.testProjectD.name)
        projectList(0).slug should be(TestValues.testProjectD.slug)
        projectList(0).description should be(TestValues.testProjectD.description)
        projectList(0).availability should be(TestValues.testProjectD.availability)
      }
      "throw a GenericDatabaseException if project contains unexisting course id" in {
        val result = projectRepository.insert(TestValues.testProjectE.copy(
         courseId = UUID("ad043c17-d552-4744-890a-6ab8a6778e4c")
        ))

        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
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
        val result = projectRepository.update(TestValues.testProjectA.copy(
          name = "new test project A",
          slug = "new test project slug A",
          description = "new test project A description",
          availability = "any"
        ))
        val project = Await.result(result, Duration.Inf)

        project.name should be("new test project A")
        project.slug should be("new test project slug A")
        project.description should be("new test project A description")
        project.availability should be("any")

        // Check record in db
        val queryResult = db.pool.sendPreparedStatement(SelectOne, Array[Any](TestValues.testProjectA.id.bytes)).map { queryResult =>
          val projectList = queryResult.rows.get.map {
            item: RowData => Project(item)
          }
          projectList
        }

        val projectList = Await.result(queryResult, Duration.Inf)

        projectList(0).name should be("new test project A")
        projectList(0).slug should be("new test project slug A")
        projectList(0).description should be("new test project A description")
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
        val result = projectRepository.update(TestValues.testProjectE)

        an[java.util.NoSuchElementException] should be thrownBy Await.result(result, Duration.Inf)
      }
    }
  }

  "ProjectRepository.delete" should {
    inSequence {
      "delete a project if project doesn't have references in other tables" in {
        val result = projectRepository.delete(TestValues.testProjectC)

        Await.result(result, Duration.Inf) should be (true)

        // Check
        val queryResult = db.pool.sendPreparedStatement(SelectOne, Array[Any](TestValues.testProjectC.id.bytes)).map { queryResult =>
          val projectList = queryResult.rows.get.map {
            item: RowData => Project(item)
          }
          projectList
        }

        Await.result(queryResult, Duration.Inf) should be (Vector())
      }
      "delete a project if project has references in other tables" in {
        val result = projectRepository.delete(TestValues.testProjectB)

        Await.result(result, Duration.Inf) should be (true)

        // Check
        val queryResult = db.pool.sendPreparedStatement(SelectOne, Array[Any](TestValues.testProjectB.id.bytes)).map { queryResult =>
          val projectList = queryResult.rows.get.map {
            item: RowData => Project(item)
          }
          projectList
        }

        Await.result(queryResult, Duration.Inf) should be (Vector())
      }
      "return FALSE if Project hasn't been found" in {
        val result = projectRepository.delete(TestValues.testProjectE)

        Await.result(result, Duration.Inf) should be(false)
      }
    }
  }
}
