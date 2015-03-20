import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.Connection
import org.scalatest._
import Matchers._
import scala.collection.immutable.TreeMap
import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import scalaz._


class ProjectRepositorySpec
  extends TestEnvironment {

  val partRepository = stub[PartRepository]
  val projectRepository = new ProjectRepositoryPostgres(partRepository)

  "ProjectRepository.list" should {
    inSequence {
      "find all projects" in {
        val testProjectList = TreeMap[Int, Project](
          0 -> TestValues.testProjectA,
          1 -> TestValues.testProjectB,
          2 -> TestValues.testProjectC,
          3 -> TestValues.testProjectE
        )

        val testPartList = TreeMap(
          testProjectList(0).id.toString -> Vector(
            TestValues.testPartA,
            TestValues.testPartB
          ),
          testProjectList(1).id.toString -> Vector(
            TestValues.testPartC
          ),
          testProjectList(2).id.toString -> Vector(),
          testProjectList(3).id.toString -> Vector()
        )

        // Put here parts = Vector(), because after db query Project object is created without parts.
        testProjectList.foreach {
          case (key, project: Project) => {
            (partRepository.list(_: Project)(_: Connection)) when(project.copy(parts = Vector()), *) returns(Future.successful(\/-(testPartList(project.id.toString))))
          }
        }

        val result = projectRepository.list(conn)
        val eitherProjects = Await.result(result, Duration.Inf)
        val \/-(projects) = eitherProjects

        projects.size should be(testProjectList.size)

        testProjectList.foreach {
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
      "find all projects belonging to a given course" in {
        val testCourse = TestValues.testCourseB

        val testProjectList = TreeMap[Int, Project](
          0 -> TestValues.testProjectB,
          1 -> TestValues.testProjectC
        )

        val testPartList = TreeMap(
          testProjectList(0).id.toString -> Vector(
            TestValues.testPartC
          ),
          testProjectList(1).id.toString -> Vector()
        )

        testProjectList.foreach {
          case (key, project: Project) => {
            (partRepository.list(_: Project)(_: Connection)) when(project.copy(parts = Vector()), *) returns (Future.successful(\/-(testPartList(project.id.toString))))
          }
        }

        val result = projectRepository.list(testCourse)
        val eitherProjects = Await.result(result, Duration.Inf)
        val \/-(projects) = eitherProjects

        projects.size should be(testProjectList.size)

        testProjectList.foreach {
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
      "return empty Vector() if there are no projects within indicated course" in {
        val testCourse = TestValues.testCourseD

        val result = projectRepository.list(testCourse)
        Await.result(result, Duration.Inf) should be (\/-(Vector()))
      }
      "return empty Vector() if course doesn't exist" in {
        val testCourse = TestValues.testCourseE

        val result = projectRepository.list(testCourse)
        Await.result(result, Duration.Inf) should be (\/-(Vector()))
      }
    }
  }

    "ProjectRepository.find" should {
      inSequence {
        "find project by ID" in {
          val testProject = TestValues.testProjectA

          val testPartList = Vector(
            TestValues.testPartA,
            TestValues.testPartB
          )

          (partRepository.list(_: Project)(_: Connection)) when(testProject.copy(parts = Vector()), *) returns(Future.successful(\/-(testPartList)))

          val result = projectRepository.find(testProject.id)
          val eitherProject = Await.result(result, Duration.Inf)
          val \/-(project) = eitherProject

          project.id should be(testProject.id)
          project.courseId should be(testProject.courseId)
          project.version should be(testProject.version)
          project.name should be(testProject.name)
          project.slug should be(testProject.slug)
          project.description should be(testProject.description)
          project.availability should be(testProject.availability)
          project.parts should be(testProject.parts)
          project.createdAt.toString should be(testProject.createdAt.toString)
          project.updatedAt.toString should be(testProject.updatedAt.toString)
        }
        "return RepositoryError.NoResults if project wasn't found by ID" in {
          val projectId = UUID("f9aadc67-5e8b-48f3-b0a2-20a0d7d88477")

          val result = projectRepository.find(projectId)
          Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
        }
        "find project by ID and User (teacher)" in {
          val testUser = TestValues.testUserB

          val testProject = TestValues.testProjectB

          val testPartList = Vector(
            TestValues.testPartC
          )

          (partRepository.list(_: Project)(_: Connection)) when(testProject.copy(parts = Vector()), *) returns(Future.successful(\/-(testPartList)))

          val result = projectRepository.find(testProject.id, testUser)
          val eitherProject = Await.result(result, Duration.Inf)
          val \/-(project) = eitherProject

          project.id should be(testProject.id)
          project.courseId should be(testProject.courseId)
          project.version should be(testProject.version)
          project.name should be(testProject.name)
          project.slug should be(testProject.slug)
          project.description should be(testProject.description)
          project.availability should be(testProject.availability)
          project.parts should be(testProject.parts)
          project.createdAt.toString should be(testProject.createdAt.toString)
          project.updatedAt.toString should be(testProject.updatedAt.toString)
        }
        "find project by ID and User (student)" in {
          val testUser = TestValues.testUserC

          val testProject = TestValues.testProjectB

          val testPartList = Vector(
            TestValues.testPartC
          )

          (partRepository.list(_: Project)(_: Connection)) when(testProject.copy(parts = Vector()), *) returns(Future.successful(\/-(testPartList)))

          val result = projectRepository.find(testProject.id, testUser)
          val eitherProject = Await.result(result, Duration.Inf)
          val \/-(project) = eitherProject

          project.id should be(testProject.id)
          project.courseId should be(testProject.courseId)
          project.version should be(testProject.version)
          project.name should be(testProject.name)
          project.slug should be(testProject.slug)
          project.description should be(testProject.description)
          project.availability should be(testProject.availability)
          project.parts should be(testProject.parts)
          project.createdAt.toString should be(testProject.createdAt.toString)
          project.updatedAt.toString should be(testProject.updatedAt.toString)
        }
        // TODO user student
        "return RepositoryError.NoResults if user (teacher) is not connected with a project" in {
          val testUser = TestValues.testUserA

          val testProject = TestValues.testProjectB

          val testPartList = Vector(
            TestValues.testPartC
          )

          (partRepository.list(_: Project)(_: Connection)) when(testProject.copy(parts = Vector()), *) returns(Future.successful(\/-(testPartList)))

          val result = projectRepository.find(testProject.id, testUser)
          Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
        }
        "return RepositoryError.NoResults if user (student) is not connected with a project" in {
          val testUser = TestValues.testUserG

          val testProject = TestValues.testProjectB

          val testPartList = Vector(
            TestValues.testPartC
          )

          (partRepository.list(_: Project)(_: Connection)) when(testProject.copy(parts = Vector()), *) returns(Future.successful(\/-(testPartList)))

          val result = projectRepository.find(testProject.id, testUser)
          Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
        }
        "return RepositoryError.NoResults if project ID is wrong" in {
          val projectId = UUID("f9aadc67-5e8b-48f3-b0a2-20a0d7d88477")
          val testUser = TestValues.testUserB

          val result = projectRepository.find(projectId, testUser)

          Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
        }
        "return RepositoryError.NoResults if User doesn't exist" in {
          val testUser = TestValues.testUserD

          val testProject = TestValues.testProjectB

          val testPartList = Vector(
            TestValues.testPartC
          )

          (partRepository.list(_: Project)(_: Connection)) when(testProject.copy(parts = Vector()), *) returns(Future.successful(\/-(testPartList)))

          val result = projectRepository.find(testProject.id, testUser)
          Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
        }
        "find project by slug" in {
          val testProject = TestValues.testProjectA

          val testPartList = Vector(
            TestValues.testPartA,
            TestValues.testPartB
          )

          (partRepository.list(_: Project)(_: Connection)) when(testProject.copy(parts = Vector()), *) returns(Future.successful(\/-(testPartList)))

          val result = projectRepository.find(testProject.slug)
          val eitherProject = Await.result(result, Duration.Inf)
          val \/-(project) = eitherProject

          project.id should be(testProject.id)
          project.courseId should be(testProject.courseId)
          project.version should be(testProject.version)
          project.name should be(testProject.name)
          project.slug should be(testProject.slug)
          project.description should be(testProject.description)
          project.availability should be(testProject.availability)
          project.parts should be(testProject.parts)
          project.createdAt.toString should be(testProject.createdAt.toString)
          project.updatedAt.toString should be(testProject.updatedAt.toString)
        }
        "return RepositoryError.NoResults if slug doesn't exist" in {
          val projectSlug = "unexisting project slug"

          val result = projectRepository.find(projectSlug)
          Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
        }
      }
    }

    "ProjectRepository.insert" should {
      inSequence {
        "insert new project" in {
          val testProject = TestValues.testProjectD

          val result = projectRepository.insert(testProject)
          val eitherProject = Await.result(result, Duration.Inf)
          val \/-(project) = eitherProject

          project.id should be(testProject.id)
          project.courseId should be(testProject.courseId)
          project.version should be(testProject.version)
          project.name should be(testProject.name)
          project.slug should be(testProject.slug)
          project.description should be(testProject.description)
          project.availability should be(testProject.availability)
          project.parts should be(Vector())
        }
        "return RepositoryError.ForeignKeyConflict if project contains unexisting course id" in {
          val testProject = TestValues.testProjectD.copy(
            courseId = UUID("ad043c17-d552-4744-890a-6ab8a6778e4c")
          )

          val result = projectRepository.insert(testProject)
          Await.result(result, Duration.Inf) should be(-\/(RepositoryError.ForeignKeyConflict("course_id", "projects_course_id_fkey")))
        }
        "return RepositoryError.PrimaryKeyConflict if project already exists" in {
          val testProject = TestValues.testProjectA

          val result = projectRepository.insert(testProject)
          Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
        }
      }
    }


    "ProjectRepository.update" should {
      inSequence {
        "update project" in {
          val testProject = TestValues.testProjectA
          val updatedProject = testProject.copy(
            courseId = TestValues.testCourseB.id,
            name = "updated test project",
            slug = "updated test project slug",
            description = "updated test project description",
            availability = "any"
          )

          val result = projectRepository.update(updatedProject)
          val eitherProject = Await.result(result, Duration.Inf)
          val \/-(project) = eitherProject

          project.id should be(updatedProject.id)
          project.courseId should be(updatedProject.courseId)
          project.version should be(updatedProject.version + 1)
          project.name should be(updatedProject.name)
          project.slug should be(updatedProject.slug)
          project.description should be(updatedProject.description)
          project.availability should be(updatedProject.availability)
          project.parts should be(updatedProject.parts)
          project.createdAt.toString should be(testProject.createdAt.toString)
        }
        "return RepositoryError.NoResults when update an existing Project with wrong version" in {
          val testProject = TestValues.testProjectA
          val updatedProject = testProject.copy(
            name = "updated test project",
            slug = "updated test project slug",
            description = "updated test project description",
            availability = "any",
            version = 99L
          )

          val result = projectRepository.update(updatedProject)
          Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
        }
        "return RepositoryError.NoResults when update an unexisting Project" in {
          val testProject = TestValues.testProjectD
          val updatedProject = testProject.copy(
            name = "updated test project",
            slug = "updated test project slug",
            description = "updated test project description",
            availability = "any",
            version = 99L
          )

          val result = projectRepository.update(updatedProject)
          Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
        }
      }
    }

    "ProjectRepository.delete" should {
      inSequence {
        "delete a project if project doesn't have references in other tables" in {
          val testProject = TestValues.testProjectE

          val result = projectRepository.delete(testProject)
          val eitherProject = Await.result(result, Duration.Inf)
          val \/-(project) = eitherProject

          project.id should be(testProject.id)
          project.courseId should be(testProject.courseId)
          project.version should be(testProject.version)
          project.name should be(testProject.name)
          project.slug should be(testProject.slug)
          project.description should be(testProject.description)
          project.availability should be(testProject.availability)
          project.parts should be(testProject.parts)
          project.createdAt.toString should be(testProject.createdAt.toString)
          project.updatedAt.toString should be(testProject.updatedAt.toString)
        }
        // TODO except if project is conntected with a part and part is connected with a task and the task_id is in the "work" table
        "delete a project if project has references in other tables" in {
          val testProject = TestValues.testProjectC

          val result = projectRepository.delete(testProject)
          val eitherProject = Await.result(result, Duration.Inf)
          val \/-(project) = eitherProject

          project.id should be(testProject.id)
          project.courseId should be(testProject.courseId)
          project.version should be(testProject.version)
          project.name should be(testProject.name)
          project.slug should be(testProject.slug)
          project.description should be(testProject.description)
          project.availability should be(testProject.availability)
          project.parts should be(testProject.parts)
        }
        "return RepositoryError.NoResults if Project hasn't been found" in {
          val testProject = TestValues.testProjectD

          val result = projectRepository.delete(testProject)
          Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
        }
      }
    }
  }
