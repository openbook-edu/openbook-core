import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks.{ DocumentTask, Task }
import ca.shiftfocus.krispii.core.repositories._
import com.github.mauricio.async.db.Connection
import org.scalatest.Matchers._
import org.scalatest._
import play.Logger

import scala.collection.immutable.TreeMap
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }
import scalaz._

class ProjectRepositorySpec
    extends TestEnvironment {

  val partRepository = stub[PartRepository]
  val componentRepository = stub[ComponentRepository]
  val taskRepository = stub[TaskRepository]
  val projectRepository = new ProjectRepositoryPostgres(partRepository, taskRepository, componentRepository)

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
            TestValues.testPartB,
            TestValues.testPartG
          ),
          testProjectList(1).id.toString -> Vector(
            TestValues.testPartC
          ),
          testProjectList(2).id.toString -> Vector(
            TestValues.testPartE,
            TestValues.testPartF,
            TestValues.testPartH
          ),
          testProjectList(3).id.toString -> Vector()
        )

        // Put here parts = Vector(), because after db query Project object is created without parts.
        testProjectList.foreach {
          case (key, project: Project) => {
            (partRepository.list(_: Project)(_: Connection, _: ScalaCachePool)) when (project.copy(parts = Vector()), *, *) returns (Future.successful(\/-(testPartList(project.id.toString))))
          }
        }

        val result = projectRepository.list()
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
            projects(key).projectType should be(project.projectType)
            projects(key).parts should be(project.parts)
            projects(key).createdAt.toString should be(project.createdAt.toString)
            projects(key).updatedAt.toString should be(project.updatedAt.toString)
          }
        }
      }
      "find all projects belonging to a given course" in {
        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

        val testCourse = TestValues.testCourseB

        val testProjectList = TreeMap[Int, Project](
          0 -> TestValues.testProjectC,
          1 -> TestValues.testProjectB
        )

        val testPartList = TreeMap(
          testProjectList(1).id.toString -> Vector(
            TestValues.testPartC
          ),
          testProjectList(0).id.toString -> Vector(
            TestValues.testPartE,
            TestValues.testPartF,
            TestValues.testPartH
          )
        )

        testProjectList.foreach {
          case (key, project: Project) => {
            (partRepository.list(_: Project)(_: Connection, _: ScalaCachePool)) when (project.copy(parts = Vector()), *, *) returns (Future.successful(\/-(testPartList(project.id.toString))))
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
            projects(key).projectType should be(project.projectType)
            projects(key).parts should be(project.parts)
            projects(key).createdAt.toString should be(project.createdAt.toString)
            projects(key).updatedAt.toString should be(project.updatedAt.toString)
          }
        }
      }
      "return empty Vector() if there are no projects within indicated course" in {
        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

        val testCourse = TestValues.testCourseD

        val result = projectRepository.list(testCourse)
        Await.result(result, Duration.Inf) should be(\/-(Vector()))
      }
      "return empty Vector() if course doesn't exist" in {
        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

        val testCourse = TestValues.testCourseE

        val result = projectRepository.list(testCourse)
        Await.result(result, Duration.Inf) should be(\/-(Vector()))
      }
      "find all master projects" in {
        val testProjectList = TreeMap[Int, Project](
          0 -> TestValues.testProjectF.copy(parts = Vector(TestValues.testPartI)),
          1 -> TestValues.testProjectG.copy(parts = Vector(TestValues.testPartI)),
          2 -> TestValues.testProjectH
        )

        // Put here parts = Vector(), because after db query Project object is created without parts.
        (partRepository.list(_: Project)(_: Connection, _: ScalaCachePool)) when (*, *, *) returns (Future.successful(\/-(Vector(TestValues.testPartI))))

        val result = projectRepository.list(Some(true))
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
            projects(key).projectType should be(project.projectType)
            projects(key).parts should be(project.parts)
            projects(key).createdAt.toString should be(project.createdAt.toString)
            projects(key).updatedAt.toString should be(project.updatedAt.toString)
          }
        }
      }
      "find all projects by key" in {
        val testProjectList = TreeMap[Int, Project](
          0 -> TestValues.testProjectA,
          1 -> TestValues.testProjectB
        )

        // Put here parts = Vector(), because after db query Project object is created without parts.

        val result = projectRepository.trigramSearch("pacific")
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
            projects(key).projectType should be(project.projectType)
            projects(key).createdAt.toString should be(project.createdAt.toString)
            projects(key).updatedAt.toString should be(project.updatedAt.toString)
          }
        }
      }
      "find projects by key no projects available" in {
        val result = projectRepository.trigramSearch("seductive")
        val eitherProjects = Await.result(result, Duration.Inf)
        val \/-(projects) = eitherProjects

        projects.size should be(0)
      }
    }
  }

  "ProjectRepository.find" should {
    inSequence {
      "find project by ID" in {
        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

        val testProject = TestValues.testProjectA

        val testPartList = Vector(
          TestValues.testPartA,
          TestValues.testPartB,
          TestValues.testPartG
        )

        (partRepository.list(_: Project, _: Boolean, _: Boolean)(_: Connection, _: ScalaCachePool)) when (testProject.copy(parts = Vector()), true, false, *, *) returns (Future.successful(\/-(testPartList)))

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
        project.projectType should be(testProject.projectType)
        project.parts should be(testProject.parts)
        project.createdAt.toString should be(testProject.createdAt.toString)
        project.updatedAt.toString should be(testProject.updatedAt.toString)
      }
      "return RepositoryError.NoResults if project wasn't found by ID" in {
        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

        val projectId = UUID.fromString("f9aadc67-5e8b-48f3-b0a2-20a0d7d88477")

        val result = projectRepository.find(projectId)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Project")))
      }
      "find project by ID and User (teacher)" in {
        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

        val testUser = TestValues.testUserB

        val testProject = TestValues.testProjectB

        val testPartList = Vector(
          TestValues.testPartC
        )

        (partRepository.list(_: Project)(_: Connection, _: ScalaCachePool)) when (testProject.copy(parts = Vector()), *, *) returns (Future.successful(\/-(testPartList)))

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
        project.projectType should be(testProject.projectType)
        project.parts should be(testProject.parts)
        project.createdAt.toString should be(testProject.createdAt.toString)
        project.updatedAt.toString should be(testProject.updatedAt.toString)
      }
      "find project by ID and User (student)" in {
        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

        val testUser = TestValues.testUserC

        val testProject = TestValues.testProjectB

        val testPartList = Vector(
          TestValues.testPartC
        )

        (partRepository.list(_: Project)(_: Connection, _: ScalaCachePool)) when (testProject.copy(parts = Vector()), *, *) returns (Future.successful(\/-(testPartList)))

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
        project.projectType should be(testProject.projectType)
        project.parts should be(testProject.parts)
        project.createdAt.toString should be(testProject.createdAt.toString)
        project.updatedAt.toString should be(testProject.updatedAt.toString)
      }
      "return RepositoryError.NoResults if user (teacher) is not connected with a project" in {
        val testUser = TestValues.testUserA

        val testProject = TestValues.testProjectB

        val testPartList = Vector(
          TestValues.testPartC
        )

        (partRepository.list(_: Project)(_: Connection, _: ScalaCachePool)) when (testProject.copy(parts = Vector()), *, *) returns (Future.successful(\/-(testPartList)))

        val result = projectRepository.find(testProject.id, testUser)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Project")))
      }
      "return RepositoryError.NoResults if user (student) is not connected with a project" in {
        val testUser = TestValues.testUserG

        val testProject = TestValues.testProjectB

        val testPartList = Vector(
          TestValues.testPartC
        )

        (partRepository.list(_: Project)(_: Connection, _: ScalaCachePool)) when (testProject.copy(parts = Vector()), *, *) returns (Future.successful(\/-(testPartList)))

        val result = projectRepository.find(testProject.id, testUser)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Project")))
      }
      "return RepositoryError.NoResults if project ID is wrong" in {
        val projectId = UUID.fromString("f9aadc67-5e8b-48f3-b0a2-20a0d7d88477")
        val testUser = TestValues.testUserB

        val result = projectRepository.find(projectId, testUser)

        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Project")))
      }
      "return RepositoryError.NoResults if User doesn't exist" in {
        val testUser = TestValues.testUserD

        val testProject = TestValues.testProjectB

        val testPartList = Vector(
          TestValues.testPartC
        )

        (partRepository.list(_: Project)(_: Connection, _: ScalaCachePool)) when (testProject.copy(parts = Vector()), *, *) returns (Future.successful(\/-(testPartList)))

        val result = projectRepository.find(testProject.id, testUser)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Project")))
      }
      "find project by slug" in {
        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

        val testProject = TestValues.testProjectA

        val testPartList = Vector(
          TestValues.testPartA,
          TestValues.testPartB,
          TestValues.testPartG
        )

        (partRepository.list(_: Project)(_: Connection, _: ScalaCachePool)) when (testProject.copy(parts = Vector()), *, *) returns (Future.successful(\/-(testPartList)))

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
        project.projectType should be(testProject.projectType)
        project.parts should be(testProject.parts)
        project.createdAt.toString should be(testProject.createdAt.toString)
        project.updatedAt.toString should be(testProject.updatedAt.toString)
      }
      "return RepositoryError.NoResults if slug doesn't exist" in {
        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

        val projectSlug = "unexisting project slug"

        val result = projectRepository.find(projectSlug)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Project")))
      }
    }
  }

  "ProjectRepository.insert" should {
    inSequence {
      "insert new project" in {
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

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
        project.projectType should be(testProject.projectType)
        project.parts should be(Vector())
      }
      "insert new master project" in {
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val testProject = TestValues.testProjectH.copy(id = UUID.randomUUID, slug = "test-project-slug-H-1")

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
        project.projectType should be(testProject.projectType)
        project.parts should be(Vector())
      }
      "insert new project with slug + '-1' if slug exists" in {
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))
        val existingSlug = TestValues.testProjectA.slug
        val testProject = TestValues.testProjectD.copy(
          slug = existingSlug
        )

        val result = projectRepository.insert(testProject)
        val eitherProject = Await.result(result, Duration.Inf)
        val \/-(project) = eitherProject

        project.id should be(testProject.id)
        project.courseId should be(testProject.courseId)
        project.version should be(testProject.version)
        project.name should be(testProject.name)
        project.slug should be(testProject.slug + "-1")
        project.description should be(testProject.description)
        project.availability should be(testProject.availability)
        project.projectType should be(testProject.projectType)
        project.parts should be(Vector())
      }
      "return RepositoryError.ForeignKeyConflict if project contains unexisting course id" in {
        val testProject = TestValues.testProjectD.copy(
          courseId = UUID.fromString("ad043c17-d552-4744-890a-6ab8a6778e4c")
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
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

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
        project.projectType should be(updatedProject.projectType)
        project.parts should be(updatedProject.parts)
        project.createdAt.toString should be(testProject.createdAt.toString)
        project.updatedAt.toString should not be (testProject.updatedAt.toString)
      }
      "update to master project" in {
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val testProject = TestValues.testProjectA
        val updatedProject = testProject.copy(
          courseId = TestValues.testCourseB.id,
          name = "updated test project",
          slug = "updated test project slug",
          isMaster = true,
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
        project.isMaster should be(updatedProject.isMaster)
        project.projectType should be(updatedProject.projectType)
        project.parts should be(updatedProject.parts)
        project.createdAt.toString should be(testProject.createdAt.toString)
        project.updatedAt.toString should not be (testProject.updatedAt.toString)
      }
      "update project with slug + '-1' if slug exists" in {
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))
        val existingSlug = TestValues.testProjectB.slug
        val testProject = TestValues.testProjectA
        val updatedProject = testProject.copy(
          courseId = TestValues.testCourseB.id,
          name = "updated test project",
          slug = existingSlug,
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
        project.slug should be(updatedProject.slug + "-1")
        project.description should be(updatedProject.description)
        project.availability should be(updatedProject.availability)
        project.projectType should be(updatedProject.projectType)
        project.parts should be(updatedProject.parts)
        project.createdAt.toString should be(testProject.createdAt.toString)
        project.updatedAt.toString should not be (testProject.updatedAt.toString)
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
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Project")))
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
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Project")))
      }
    }
  }

  "ProjectRepository.delete" should {
    inSequence {
      "delete a project if project doesn't have references in other tables" in {
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

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
        project.projectType should be(testProject.projectType)
        project.parts should be(testProject.parts)
        project.createdAt.toString should be(testProject.createdAt.toString)
        project.updatedAt.toString should be(testProject.updatedAt.toString)
      }
      // TODO except if project is conntected with a part and part is connected with a task and the task_id is in the "work" table
      "delete a project if project has references in other tables" in {
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

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
        project.projectType should be(testProject.projectType)
        project.parts should be(testProject.parts)
      }
      "return RepositoryError.NoResults if Project hasn't been found" in {
        val testProject = TestValues.testProjectD

        val result = projectRepository.delete(testProject)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Project")))
      }
      "return RepositoryError.NoResults if Project version is wrong" in {
        val testProject = TestValues.testProjectE.copy(
          version = 99L
        )

        val result = projectRepository.delete(testProject)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Project")))
      }
    }
  }

  "ProjectRepository.cloneProjects" should {
    inSequence {
      "clone a components of a project" in {

        val testComponents = Vector(
          TestValues.testAudioComponentC,
          TestValues.testAudioComponentE,
          TestValues.testTextComponentA,
          TestValues.testVideoComponentB
        )
        val testUserId = UUID.randomUUID
        val result = projectRepository.cloneComponents(testComponents, testUserId)

        for (i <- 0 to testComponents.size - 1) {
          testComponents(i).id should not be (result(i).id)
          testComponents(i).title should be(result(i).title)
          testComponents(i).version should be(result(i).version)
          testComponents(i).questions should be(result(i).questions)
          testUserId should be(result(i).ownerId)
          testComponents(i).thingsToThinkAbout should be(result(i).thingsToThinkAbout)
          testComponents(i).createdAt should not be (result(i).createdAt)
          testComponents(i).updatedAt should not be (result(i).updatedAt)
        }
      }
      "clone tasks of a project" in {
        val testTaskList = IndexedSeq[Task](
          TestValues.testLongAnswerTaskA,
          TestValues.testLongAnswerTaskF,
          TestValues.testLongAnswerTaskN,
          TestValues.testLongAnswerTaskO
        ).sortBy(t => t.position)

        val result = projectRepository.cloneTasks(testTaskList, UUID.fromString("5cd214be-6bba-47fa-9f35-0eb8bafec397"))

        for (i <- 0 to testTaskList.size - 1) {
          testTaskList(i).id should not be (result(i).id)
          testTaskList(i).version should be(result(i).version)
          testTaskList(i).partId should be(result(i).partId)
          testTaskList(i).taskType should be(result(i).taskType)
          testTaskList(i).position should be(result(i).position)
          testTaskList(i).settings should be(result(i).settings)
          testTaskList(i).createdAt should not be (result(i).createdAt)
          testTaskList(i).updatedAt should not be (result(i).updatedAt)
        }
        val taskADependencyId = result(0).asInstanceOf[DocumentTask].dependencyId
        val taskFDependencyId = result(1).asInstanceOf[DocumentTask].dependencyId
        val taskNDependencyId = result(2).asInstanceOf[DocumentTask].dependencyId
        val taskODependencyId = result(3).asInstanceOf[DocumentTask].dependencyId
        taskADependencyId should be(None)
        taskFDependencyId.get should be(result(0).asInstanceOf[DocumentTask].id)
        taskNDependencyId should be(None)
        taskODependencyId.get should be(result(2).asInstanceOf[DocumentTask].id)
      }
      "clone a project" in {
        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
        val result = projectRepository.cloneProject(TestValues.testProjectA.id, TestValues.testCourseB.id)
        val testPartList = Vector(
          TestValues.testPartA,
          TestValues.testPartB,
          TestValues.testPartG
        )
        (partRepository.list(_: Project, _: Boolean, _: Boolean)(_: Connection, _: ScalaCachePool)) when (TestValues.testProjectA.copy(parts = Vector()), true, false, *, *) returns (Future.successful(\/-(testPartList)))
        val eitherProject = Await.result(result, Duration.Inf)
        val \/-(project) = eitherProject
        Logger.error(project.slug)
        project.id should not be (TestValues.testProjectA.id)
        project.courseId should be(TestValues.testCourseB.id)
        project.version should be(TestValues.testProjectA.version)
        project.name should be(TestValues.testProjectA.name)
        project.slug should be("test-project-slug-A")
        project.description should be(TestValues.testProjectA.description)
        project.availability should be(TestValues.testProjectA.availability)
        project.projectType should be(TestValues.testProjectA.projectType)
        project.parts.zipWithIndex.foreach {
          case (part: Part, i: Int) =>
            part.name should be(testPartList(i).name)
          //            part.id should not be(testPartList(i).id)
          //            part.updatedAt should not be(testPartList(i).updatedAt)
          //            part.createdAt should not be(testPartList(i).createdAt)
          //            part.projectId should be(project.id)
        }
      }
    }
  }
}
