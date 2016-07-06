import ca.shiftfocus.krispii.core.error.{ RepositoryError, ServiceError }
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks._
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services._
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.github.mauricio.async.db.Connection
import java.util.UUID

import org.scalatest._
import Matchers._

import scala.collection.immutable.TreeMap
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }
import scalaz.{ -\/, \/- }

class ProjectServiceSpec
    extends TestEnvironment(writeToDb = false) {

  val db = stub[DB]
  val mockConnection = stub[Connection]
  val authService = stub[AuthService]
  val schoolService = stub[SchoolService]
  val courseRepository = stub[CourseRepository]
  val projectRepository = stub[ProjectRepository]
  val partRepository = stub[PartRepository]
  val taskRepository = stub[TaskRepository]
  val componentRepository = stub[ComponentRepository]

  val projectService = new ProjectServiceDefault(db, cache, authService, schoolService, courseRepository, projectRepository, partRepository, taskRepository, componentRepository) {
    override implicit def conn: Connection = mockConnection

    override def transactional[A](f: Connection => Future[A]): Future[A] = {
      f(mockConnection)
    }
  }

  "ProjectService.create" should {
    inSequence {
      "return new project" in {
        val testCourse = TestValues.testCourseA

        val testProject = TestValues.testProjectA
        val emptyProject = Project(
          id = testProject.id,
          courseId = testCourse.id,
          name = testProject.name,
          slug = testProject.slug,
          description = testProject.description,
          longDescription = testProject.longDescription,
          availability = testProject.availability,
          projectType = testProject.projectType,
          parts = IndexedSeq.empty[Part]
        )

        val emptyPart = Part(
          projectId = testProject.id,
          name = ""
        )

        val emptyTask = DocumentTask(
          partId = emptyPart.id,
          position = 1,
          maxGrade = "0"
        )

        val resultProject = emptyProject

        (projectRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when (emptyProject.slug, *, *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (projectRepository.insert(_: Project)(_: Connection, _: ScalaCachePool)) when (*, *, *) returns (Future.successful(\/-(emptyProject)))
        (partRepository.insert(_: Part)(_: Connection, _: ScalaCachePool)) when (*, *, *) returns (Future.successful(\/-(emptyPart)))
        (taskRepository.insert(_: Task)(_: Connection, _: ScalaCachePool)) when (*, *, *) returns (Future.successful(\/-(emptyTask)))

        val result = projectService.create(testCourse.id, emptyProject.name, emptyProject.slug, emptyProject.description, emptyProject.longDescription, emptyProject.availability, enabled = true, projectType = emptyProject.projectType)
        val eitherProject = Await.result(result, Duration.Inf)
        val \/-(project) = eitherProject

        project.id should be(resultProject.id)
        project.courseId should be(resultProject.courseId)
        project.version should be(resultProject.version)
        project.name should be(resultProject.name)
        project.slug should be(resultProject.slug)
        project.description should be(resultProject.description)
        project.longDescription should be(resultProject.longDescription)
        project.availability should be(resultProject.availability)
        project.parts should be(resultProject.parts)
        project.createdAt.toString should be(resultProject.createdAt.toString)
        project.updatedAt.toString should be(resultProject.updatedAt.toString)

      }
      "return ServiceError.BadInput if slug has bad format" in {
        val testCourse = TestValues.testCourseA

        val testProject = TestValues.testProjectA
        val emptyProject = Project(
          id = testProject.id,
          courseId = testCourse.id,
          name = testProject.name,
          slug = "bad slug format A",
          description = testProject.description,
          longDescription = testProject.longDescription,
          availability = testProject.availability,
          projectType = testProject.projectType,
          parts = IndexedSeq.empty[Part]
        )

        val emptyPart = Part(
          projectId = testProject.id,
          name = ""
        )

        val emptyTask = DocumentTask(
          partId = emptyPart.id,
          position = 1,
          maxGrade = "0"
        )

        val resultProject = emptyProject.copy(
          parts = IndexedSeq(
            emptyPart.copy(
              tasks = IndexedSeq(
                emptyTask
              )
            )
          )
        )

        (projectRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when (emptyProject.slug, *, *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (projectRepository.insert(_: Project)(_: Connection, _: ScalaCachePool)) when (*, *, *) returns (Future.successful(\/-(emptyProject)))
        (partRepository.insert(_: Part)(_: Connection, _: ScalaCachePool)) when (*, *, *) returns (Future.successful(\/-(emptyPart)))
        (taskRepository.insert(_: Task)(_: Connection, _: ScalaCachePool)) when (*, *, *) returns (Future.successful(\/-(emptyTask)))

        val result = projectService.create(
          testCourse.id,
          emptyProject.name,
          emptyProject.slug,
          emptyProject.description,
          emptyProject.longDescription,
          emptyProject.availability,
          emptyProject.parentId,
          emptyProject.isMaster,
          emptyProject.enabled,
          emptyProject.projectType
        )
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadInput(s"${emptyProject.slug} is not a valid slug format.")))
      }
      "return a new project with slug + '-1' if slug is already in use" in {
        val testCourse = TestValues.testCourseA

        val testProject = TestValues.testProjectA
        val emptyProject = Project(
          id = testProject.id,
          courseId = testCourse.id,
          name = testProject.name,
          slug = testProject.slug,
          description = testProject.description,
          longDescription = testProject.longDescription,
          availability = testProject.availability,
          projectType = testProject.projectType,
          parts = IndexedSeq.empty[Part]
        )

        val emptyPart = Part(
          projectId = testProject.id,
          name = ""
        )

        val emptyTask = DocumentTask(
          partId = emptyPart.id,
          position = 1,
          maxGrade = "0"
        )

        val resultProject = emptyProject.copy(
          slug = emptyProject.slug + "-1",
          parts = IndexedSeq(
            emptyPart.copy(
              tasks = IndexedSeq(
                emptyTask
              )
            )
          )
        )

        (projectRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when (emptyProject.slug, *, *) returns (Future.successful(\/-(testProject)))
        (projectRepository.insert(_: Project)(_: Connection, _: ScalaCachePool)) when (*, *, *) returns (Future.successful(\/-(resultProject)))
        (partRepository.insert(_: Part)(_: Connection, _: ScalaCachePool)) when (*, *, *) returns (Future.successful(\/-(emptyPart)))
        (taskRepository.insert(_: Task)(_: Connection, _: ScalaCachePool)) when (*, *, *) returns (Future.successful(\/-(emptyTask)))

        val result = projectService.create(testCourse.id, emptyProject.name, emptyProject.slug, emptyProject.description, emptyProject.longDescription, emptyProject.availability, enabled = true, projectType = emptyProject.projectType)
        val eitherProject = Await.result(result, Duration.Inf)
        val \/-(project) = eitherProject

        project.id should be(resultProject.id)
        project.courseId should be(resultProject.courseId)
        project.version should be(resultProject.version)
        project.name should be(resultProject.name)
        project.slug should be(resultProject.slug)
        project.description should be(resultProject.description)
        project.longDescription should be(resultProject.longDescription)
        project.availability should be(resultProject.availability)
        project.parts should be(resultProject.parts)
        project.createdAt.toString should be(resultProject.createdAt.toString)
        project.updatedAt.toString should be(resultProject.updatedAt.toString)
      }
    }
  }

  "ProjectService.updateInfo" should {
    inSequence {
      "return ServiceError.OfflineLockFail if versions don't match" in {
        val testProject = TestValues.testProjectA

        (projectRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testProject.id, *, *) returns (Future.successful(\/-(testProject)))

        val result = projectService.updateInfo(
          testProject.id,
          99L, Some(testProject.courseId),
          Some(testProject.name),
          Some(testProject.slug),
          Some(testProject.description),
          Some(testProject.longDescription),
          Some(testProject.availability),
          Some(testProject.enabled),
          Some(testProject.projectType)
        )
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
      "return RepositoryError.NoResults if project doesn't exist" in {
        val testProject = TestValues.testProjectD

        (projectRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testProject.id, *, *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))

        val result = projectService.updateInfo(testProject.id, testProject.version, Some(testProject.courseId), Some(testProject.name), Some(testProject.slug), Some(testProject.description), Some(testProject.longDescription), Some(testProject.availability), Some(testProject.enabled), Some(testProject.projectType))
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("")))
      }
    }
  }

  "ProjectService.updateSlug" should {
    inSequence {
      "update slug" in {
        val testProject = TestValues.testProjectA
        val updatedProject = testProject.copy(
          slug = "updated-" + testProject.slug
        )

        (projectRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testProject.id, *, *) returns (Future.successful(\/-(testProject)))
        (projectRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when (updatedProject.slug, *, *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (projectRepository.update(_: Project)(_: Connection, _: ScalaCachePool)) when (updatedProject, *, *) returns (Future.successful(\/-(updatedProject)))

        val result = projectService.updateSlug(testProject.id, testProject.version, updatedProject.slug)
        Await.result(result, Duration.Inf) should be(\/-(updatedProject))
      }
      "return ServiceError.BadInput if slug has bad format" in {
        val testProject = TestValues.testProjectA
        val updatedProject = testProject.copy(
          slug = "updated ;( " + testProject.slug
        )

        (projectRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testProject.id, *, *) returns (Future.successful(\/-(testProject)))
        (projectRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when (updatedProject.slug, *, *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (projectRepository.update(_: Project)(_: Connection, _: ScalaCachePool)) when (updatedProject, *, *) returns (Future.successful(\/-(updatedProject)))

        val result = projectService.updateSlug(testProject.id, testProject.version, updatedProject.slug)
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadInput(s"${updatedProject.slug} is not a valid slug format.")))
      }
      "return updated project with slug + '-1'  if slug is already in use" in {
        val testProject = TestValues.testProjectA
        val updatedProject = testProject.copy(
          slug = "updated-" + testProject.slug
        )

        val resultProject = updatedProject.copy(
          slug = updatedProject.slug + "-1"
        )

        (projectRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testProject.id, *, *) returns (Future.successful(\/-(testProject)))
        (projectRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when (updatedProject.slug, *, *) returns (Future.successful(\/-(testProject)))
        (projectRepository.update(_: Project)(_: Connection, _: ScalaCachePool)) when (updatedProject, *, *) returns (Future.successful(\/-(resultProject)))

        val result = projectService.updateSlug(testProject.id, testProject.version, updatedProject.slug)
        val eitherProject = Await.result(result, Duration.Inf)
        val \/-(project) = eitherProject

        project.id should be(resultProject.id)
        project.courseId should be(resultProject.courseId)
        project.version should be(resultProject.version)
        project.name should be(resultProject.name)
        project.slug should be(resultProject.slug)
        project.description should be(resultProject.description)
        project.longDescription should be(resultProject.longDescription)
        project.availability should be(resultProject.availability)
        project.parts should be(resultProject.parts)
        project.createdAt.toString should be(resultProject.createdAt.toString)
        project.updatedAt.toString should be(resultProject.updatedAt.toString)
      }
      "return ServiceError.OfflineLockFail if versions don't match" in {
        val testProject = TestValues.testProjectA
        val updatedProject = testProject.copy(
          slug = "updated-" + testProject.slug
        )

        (projectRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testProject.id, *, *) returns (Future.successful(\/-(testProject)))

        val result = projectService.updateSlug(testProject.id, 99L, updatedProject.slug)
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
      "return RepositoryError.NoResults if project doesn't exist" in {
        val testProject = TestValues.testProjectD
        val updatedProject = testProject.copy(
          slug = "updated-" + testProject.slug
        )

        (projectRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testProject.id, *, *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))

        val result = projectService.updateSlug(testProject.id, testProject.version, updatedProject.slug)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("")))
      }
    }
  }

  "ProjectService.delete" should {
    inSequence {
      "return ServiceError.OfflineLockFail if versions don't match" in {
        val testProject = TestValues.testProjectA

        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when (testProject.id, false, *, *) returns (Future.successful(\/-(testProject)))

        val result = projectService.delete(testProject.id, 99L)
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
      "return RepositoryError.NoResults if project doesn't exist" in {
        val testProject = TestValues.testProjectD

        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when (testProject.id, false, *, *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))

        val result = projectService.delete(testProject.id, testProject.version)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("")))
      }
    }
  }

  "ProjectService.createPart" should {
    inSequence {
      // Mocking of the partRepository.update method let us check if part positions were increased.
      // Also we should compare part.position in part.equals method.
      "create new part and update existing parts if new part position = min position" in {
        val testProject = TestValues.testProjectA.copy(parts = Vector(
          TestValues.testPartA,
          TestValues.testPartB,
          TestValues.testPartC
        ))
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val newPart = Part(
          projectId = testProject.id,
          name = "new part name",
          position = testPartList.head.position
        )

        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when (testProject.id, false, *, *) returns (Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean, _: Boolean)(_: Connection, _: ScalaCachePool)) when (noPartsProject, false, false, *, *) returns (Future.successful(\/-(testPartList)))

        for (i <- testPartList.indices) {
          (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(i).copy(position = i + 2), *, *) returns (Future.successful(\/-(testPartList(i).copy(position = i + 2))))
        }

        (partRepository.insert(_: Part)(_: Connection, _: ScalaCachePool)) when (newPart.copy(position = 1), *, *) returns (Future.successful(\/-(newPart.copy(position = 1))))

        val result = projectService.createPart(testProject.id, newPart.name, newPart.position, newPart.id)
        val \/-(part) = Await.result(result, Duration.Inf)

        part.projectId should be(newPart.projectId)
        part.name should be(newPart.name)
        part.position should be(1)
      }
      "create new part and put it as first element and move all over parts position if new part position < min position" in {
        val testProject = TestValues.testProjectA.copy(parts = Vector(
          TestValues.testPartA,
          TestValues.testPartB,
          TestValues.testPartC
        ))
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val newPart = Part(
          projectId = testProject.id,
          name = "new part name",
          position = testPartList.head.position - 99
        )

        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when (testProject.id, false, *, *) returns (Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean, _: Boolean)(_: Connection, _: ScalaCachePool)) when (noPartsProject, false, false, *, *) returns (Future.successful(\/-(testPartList)))

        for (i <- testPartList.indices) {
          (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(i).copy(position = i + 2), *, *) returns (Future.successful(\/-(testPartList(i).copy(position = i + 2))))
        }

        (partRepository.insert(_: Part)(_: Connection, _: ScalaCachePool)) when (newPart.copy(position = 1), *, *) returns (Future.successful(\/-(newPart.copy(position = 1))))

        val result = projectService.createPart(testProject.id, newPart.name, newPart.position, newPart.id)
        val \/-(part) = Await.result(result, Duration.Inf)

        part.projectId should be(newPart.projectId)
        part.name should be(newPart.name)
        part.position should be(1)
      }
      "create new part and update position of all element if new part position = max position" in {
        val testProject = TestValues.testProjectA.copy(parts = Vector(
          TestValues.testPartA,
          TestValues.testPartB,
          TestValues.testPartC
        ))
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val newPart = Part(
          projectId = testProject.id,
          name = "new part name",
          position = testPartList.last.position
        )

        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when (testProject.id, false, *, *) returns (Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean, _: Boolean)(_: Connection, _: ScalaCachePool)) when (noPartsProject, false, false, *, *) returns (Future.successful(\/-(testPartList)))

        for (i <- testPartList.indices) {
          if (i + 1 != testPartList.length)
            (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(i).copy(position = i + 1), *, *) returns (Future.successful(\/-(testPartList(i).copy(position = i + 1))))
        }

        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList.last.copy(position = testPartList.length + 1), *, *) returns (Future.successful(\/-(testPartList.last.copy(position = testPartList.length + 1))))

        (partRepository.insert(_: Part)(_: Connection, _: ScalaCachePool)) when (newPart.copy(position = testPartList.length), *, *) returns (Future.successful(\/-(newPart.copy(position = testPartList.length))))

        val result = projectService.createPart(testProject.id, newPart.name, newPart.position, newPart.id)
        val \/-(part) = Await.result(result, Duration.Inf)

        part.projectId should be(newPart.projectId)
        part.name should be(newPart.name)
        part.position should be(testPartList.length)
      }
      "create new part and insert between elements and update positions of all elements even if they are unsorted " in {
        val testProject = TestValues.testProjectA.copy(parts = Vector(
          TestValues.testPartA.copy(position = 15),
          TestValues.testPartB.copy(position = 11),
          TestValues.testPartC.copy(position = 19)
        ))
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val newPart = Part(
          projectId = testProject.id,
          name = "new part name",
          position = testPartList(0).position
        )
        val resultNewPartPosition = testPartList.sortWith(_.position < _.position).indexOf(testPartList(0))

        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when (testProject.id, false, *, *) returns (Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean, _: Boolean)(_: Connection, _: ScalaCachePool)) when (noPartsProject, false, false, *, *) returns (Future.successful(\/-(testPartList)))

        // mock partA
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(0).copy(position = 3), *, *) returns (Future.successful(\/-(testPartList(0).copy(position = 3))))
        // mock PartB
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(1).copy(position = 1), *, *) returns (Future.successful(\/-(testPartList(1).copy(position = 1))))
        // mock partC
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(2).copy(position = 4), *, *) returns (Future.successful(\/-(testPartList(2).copy(position = 4))))
        // mock newPart
        (partRepository.insert(_: Part)(_: Connection, _: ScalaCachePool)) when (newPart.copy(position = 2), *, *) returns (Future.successful(\/-(newPart.copy(position = 2))))

        val result = projectService.createPart(testProject.id, newPart.name, newPart.position, newPart.id)
        val \/-(part) = Await.result(result, Duration.Inf)

        part.projectId should be(newPart.projectId)
        part.name should be(newPart.name)
        part.position should be(2)
      }
      "create new part and give it position of last element + 1 if new part position > max position" in {
        val testProject = TestValues.testProjectA.copy(parts = Vector(
          TestValues.testPartA,
          TestValues.testPartB,
          TestValues.testPartC
        ))
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val newPart = Part(
          projectId = testProject.id,
          name = "new part name",
          position = testPartList.last.position + 99
        )

        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when (testProject.id, false, *, *) returns (Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean, _: Boolean)(_: Connection, _: ScalaCachePool)) when (noPartsProject, false, false, *, *) returns (Future.successful(\/-(testPartList)))

        for (i <- testPartList.indices) {
          (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(i).copy(position = i + 1), *, *) returns (Future.successful(\/-(testPartList(i).copy(position = i + 1))))
        }

        (partRepository.insert(_: Part)(_: Connection, _: ScalaCachePool)) when (newPart.copy(position = testPartList.length + 1), *, *) returns (Future.successful(\/-(newPart.copy(position = testPartList.length + 1))))

        val result = projectService.createPart(testProject.id, newPart.name, newPart.position, newPart.id)
        val \/-(part) = Await.result(result, Duration.Inf)

        part.projectId should be(newPart.projectId)
        part.name should be(newPart.name)
        part.position should be(testPartList.length + 1)
      }
      "create new part and give it position 1 if partList is empty" in {
        val testProject = TestValues.testProjectA.copy(parts = Vector())
        val testPartList = testProject.parts
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val newPart = Part(
          projectId = testProject.id,
          name = "new part name",
          position = 99
        )

        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when (testProject.id, false, *, *) returns (Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean, _: Boolean)(_: Connection, _: ScalaCachePool)) when (noPartsProject, false, false, *, *) returns (Future.successful(\/-(testPartList)))

        (partRepository.insert(_: Part)(_: Connection, _: ScalaCachePool)) when (newPart.copy(position = 1), *, *) returns (Future.successful(\/-(newPart.copy(position = 1))))

        val result = projectService.createPart(testProject.id, newPart.name, newPart.position, newPart.id)
        val \/-(part) = Await.result(result, Duration.Inf)

        part.projectId should be(newPart.projectId)
        part.name should be(newPart.name)
        part.position should be(1)
      }
      "create new part and give it position 1 if partList is not empty and partList positions start with 0 and we assign position 1 to newPart" in {
        val testProject = TestValues.testProjectA.copy(parts = Vector(
          TestValues.testPartA.copy(position = 0),
          TestValues.testPartB.copy(position = 1),
          TestValues.testPartC.copy(position = 2)
        ))
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val newPart = Part(
          projectId = testProject.id,
          name = "new part name",
          position = 1
        )

        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when (testProject.id, false, *, *) returns (Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean, _: Boolean)(_: Connection, _: ScalaCachePool)) when (noPartsProject, false, false, *, *) returns (Future.successful(\/-(testPartList)))

        for (i <- testPartList.indices) {
          (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(i).copy(position = i + 2), *, *) returns (Future.successful(\/-(testPartList(i).copy(position = i + 2))))
        }

        (partRepository.insert(_: Part)(_: Connection, _: ScalaCachePool)) when (newPart, *, *) returns (Future.successful(\/-(newPart)))

        val result = projectService.createPart(testProject.id, newPart.name, newPart.position, newPart.id)
        val \/-(part) = Await.result(result, Duration.Inf)

        part.projectId should be(newPart.projectId)
        part.name should be(newPart.name)
        part.position should be(newPart.position)
      }
      "create new part add it to the end of list and do not update partList positions if they are correct (1, 2, ...)" in {
        val testProject = TestValues.testProjectA.copy(parts = Vector(
          TestValues.testPartA.copy(position = 1),
          TestValues.testPartB.copy(position = 2),
          TestValues.testPartC.copy(position = 3)
        ))
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val newPart = Part(
          projectId = testProject.id,
          name = "new part name",
          position = testPartList.length + 1
        )

        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when (testProject.id, false, *, *) returns (Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean, _: Boolean)(_: Connection, _: ScalaCachePool)) when (noPartsProject, false, false, *, *) returns (Future.successful(\/-(testPartList)))

        (partRepository.insert(_: Part)(_: Connection, _: ScalaCachePool)) when (newPart, *, *) returns (Future.successful(\/-(newPart)))

        val result = projectService.createPart(testProject.id, newPart.name, newPart.position, newPart.id)
        val \/-(part) = Await.result(result, Duration.Inf)

        part.projectId should be(newPart.projectId)
        part.name should be(newPart.name)
        part.position should be(newPart.position)
      }
      "return RepositoryError.NoResults if project doesn't exist" in {
        val testProject = TestValues.testProjectD.copy(parts = Vector(
          TestValues.testPartA.copy(position = 1),
          TestValues.testPartB.copy(position = 2),
          TestValues.testPartC.copy(position = 3)
        ))
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val newPart = Part(
          projectId = testProject.id,
          name = "new part name",
          position = testPartList.length + 1
        )

        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when (testProject.id, false, *, *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (partRepository.list(_: Project, _: Boolean, _: Boolean)(_: Connection, _: ScalaCachePool)) when (noPartsProject, false, false, *, *) returns (Future.successful(\/-(testPartList)))

        (partRepository.insert(_: Part)(_: Connection, _: ScalaCachePool)) when (newPart, *, *) returns (Future.successful(\/-(newPart)))

        val result = projectService.createPart(testProject.id, newPart.name, newPart.position, newPart.id)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("")))
      }
    }
  }

  "ProjectService.updatePart" + (Console.YELLOW + Console.BOLD + " - These tests include also ProjectService.movePart method verification" + Console.RESET) should {
    inSequence {
      "update part and update existing parts if new part position = min position" in {
        val testProject = TestValues.testProjectA.copy(parts = Vector(
          TestValues.testPartA,
          TestValues.testPartB,
          TestValues.testPartC
        ))
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val updatedPart = testPartList(1).copy(
          name = "updated " + testPartList(1).name,
          enabled = !testPartList(1).enabled,
          position = testPartList.head.position
        )
        val filteredPartList = testPartList.filter(_.id != updatedPart.id)

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPartList(1).id, *, *) returns (Future.successful(\/-(testPartList(1))))
        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when (updatedPart.projectId, false, *, *) returns (Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean, _: Boolean)(_: Connection, _: ScalaCachePool)) when (noPartsProject, false, false, *, *) returns (Future.successful(\/-(testPartList)))

        for (i <- filteredPartList.indices) {
          (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (filteredPartList(i).copy(position = i + 2), *, *) returns (Future.successful(\/-(filteredPartList(i).copy(position = i + 2))))
        }

        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(1).copy(position = 1), *, *) returns (Future.successful(\/-(testPartList(1).copy(position = 1))))
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (updatedPart.copy(position = 1), *, *) returns (Future.successful(\/-(updatedPart.copy(position = 1))))

        val result = projectService.updatePart(updatedPart.id, updatedPart.version, Some(updatedPart.name), Some(updatedPart.position), Some(updatedPart.enabled))
        Await.result(result, Duration.Inf) should be(\/-(updatedPart.copy(position = 1)))
      }
      "update part and update existing parts if new part position < min position" in {
        val testProject = TestValues.testProjectA.copy(parts = Vector(
          TestValues.testPartA,
          TestValues.testPartB,
          TestValues.testPartC
        ))
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val updatedPart = testPartList(1).copy(
          name = "updated " + testPartList(1).name,
          enabled = !testPartList(1).enabled,
          position = testPartList.head.position - 99
        )
        val filteredPartList = testPartList.filter(_.id != updatedPart.id)

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPartList(1).id, *, *) returns (Future.successful(\/-(testPartList(1))))
        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when (updatedPart.projectId, false, *, *) returns (Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean, _: Boolean)(_: Connection, _: ScalaCachePool)) when (noPartsProject, false, false, *, *) returns (Future.successful(\/-(testPartList)))

        for (i <- filteredPartList.indices) {
          (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (filteredPartList(i).copy(position = i + 2), *, *) returns (Future.successful(\/-(filteredPartList(i).copy(position = i + 2))))
        }

        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(1).copy(position = 1), *, *) returns (Future.successful(\/-(testPartList(1).copy(position = 1))))
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (updatedPart.copy(position = 1), *, *) returns (Future.successful(\/-(updatedPart.copy(position = 1))))

        val result = projectService.updatePart(updatedPart.id, updatedPart.version, Some(updatedPart.name), Some(updatedPart.position), Some(updatedPart.enabled))
        Await.result(result, Duration.Inf) should be(\/-(updatedPart.copy(position = 1)))
      }
      "update part and preserve position if newPosition != oldPosition, but position is next part position (partList positions are unsorted)" in {
        val testProject = TestValues.testProjectA.copy(parts = Vector(
          TestValues.testPartA.copy(position = 15),
          TestValues.testPartB.copy(position = 11),
          TestValues.testPartC.copy(position = 19),
          TestValues.testPartE.copy(position = 22)
        ))
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val updatedPart = testPartList(0).copy(
          name = "updated " + testPartList(0).name,
          enabled = !testPartList(0).enabled,
          position = testPartList(2).position
        )

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPartList(0).id, *, *) returns (Future.successful(\/-(testPartList(0))))
        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when (updatedPart.projectId, false, *, *) returns (Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean, _: Boolean)(_: Connection, _: ScalaCachePool)) when (noPartsProject, false, false, *, *) returns (Future.successful(\/-(testPartList)))

        // partA
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(0).copy(position = 2), *, *) returns (Future.successful(\/-(testPartList(0).copy(position = 2))))
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (updatedPart.copy(position = 2), *, *) returns (Future.successful(\/-(updatedPart.copy(position = 2))))
        // partB
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(1).copy(position = 1), *, *) returns (Future.successful(\/-(testPartList(1).copy(position = 1))))
        // partC
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(2).copy(position = 3), *, *) returns (Future.successful(\/-(testPartList(2).copy(position = 3))))
        // partE
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(3).copy(position = 4), *, *) returns (Future.successful(\/-(testPartList(3).copy(position = 4))))

        val result = projectService.updatePart(updatedPart.id, updatedPart.version, Some(updatedPart.name), Some(updatedPart.position), Some(updatedPart.enabled))
        Await.result(result, Duration.Inf) should be(\/-(updatedPart.copy(position = 2)))
      }
      "update part and update position if newPosition != oldPosition and position is next part + 1 position" in {
        val testProject = TestValues.testProjectA.copy(parts = Vector(
          TestValues.testPartA.copy(position = 15),
          TestValues.testPartB.copy(position = 11),
          TestValues.testPartC.copy(position = 19),
          TestValues.testPartE.copy(position = 22)
        ))
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val updatedPart = testPartList(0).copy(
          name = "updated " + testPartList(0).name,
          enabled = !testPartList(0).enabled,
          position = testPartList(3).position
        )

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPartList(0).id, *, *) returns (Future.successful(\/-(testPartList(0))))
        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when (updatedPart.projectId, false, *, *) returns (Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean, _: Boolean)(_: Connection, _: ScalaCachePool)) when (noPartsProject, false, false, *, *) returns (Future.successful(\/-(testPartList)))

        // partA
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(0).copy(position = 3), *, *) returns (Future.successful(\/-(testPartList(0).copy(position = 3))))
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (updatedPart.copy(position = 3), *, *) returns (Future.successful(\/-(updatedPart.copy(position = 3))))
        // partB
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(1).copy(position = 1), *, *) returns (Future.successful(\/-(testPartList(1).copy(position = 1))))
        // partC
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(2).copy(position = 2), *, *) returns (Future.successful(\/-(testPartList(2).copy(position = 2))))
        // partE
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(3).copy(position = 4), *, *) returns (Future.successful(\/-(testPartList(3).copy(position = 4))))

        val result = projectService.updatePart(updatedPart.id, updatedPart.version, Some(updatedPart.name), Some(updatedPart.position), Some(updatedPart.enabled))
        Await.result(result, Duration.Inf) should be(\/-(updatedPart.copy(position = 3)))
      }
      "update part and give it max position - 1, if newPosition != oldPosition and position = max position" in {
        val testProject = TestValues.testProjectA.copy(parts = Vector(
          TestValues.testPartA,
          TestValues.testPartB,
          TestValues.testPartC
        ))
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val updatedPart = testPartList(0).copy(
          name = "updated " + testPartList(0).name,
          enabled = !testPartList(0).enabled,
          position = testPartList.last.position
        )
        val filteredPartList = testPartList.filter(_.id != updatedPart.id)

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPartList(0).id, *, *) returns (Future.successful(\/-(testPartList(0))))
        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when (updatedPart.projectId, false, *, *) returns (Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean, _: Boolean)(_: Connection, _: ScalaCachePool)) when (noPartsProject, false, false, *, *) returns (Future.successful(\/-(testPartList)))

        // partA
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(0).copy(position = 2), *, *) returns (Future.successful(\/-(testPartList(0).copy(position = 2))))
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (updatedPart.copy(position = 2), *, *) returns (Future.successful(\/-(updatedPart.copy(position = 2))))
        // partB
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(1).copy(position = 1), *, *) returns (Future.successful(\/-(testPartList(1).copy(position = 1))))
        // partC
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(2).copy(position = 3), *, *) returns (Future.successful(\/-(testPartList(2).copy(position = 3))))

        val result = projectService.updatePart(updatedPart.id, updatedPart.version, Some(updatedPart.name), Some(updatedPart.position), Some(updatedPart.enabled))
        Await.result(result, Duration.Inf) should be(\/-(updatedPart.copy(position = 2)))
      }
      "update part if newPosition != oldPosition and position > max position" in {
        val testProject = TestValues.testProjectA.copy(parts = Vector(
          TestValues.testPartA,
          TestValues.testPartB,
          TestValues.testPartC
        ))
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val updatedPart = testPartList(0).copy(
          name = "updated " + testPartList(0).name,
          enabled = !testPartList(0).enabled,
          position = testPartList.last.position + 99
        )
        val filteredPartList = testPartList.filter(_.id != updatedPart.id)

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPartList(0).id, *, *) returns (Future.successful(\/-(testPartList(0))))
        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when (updatedPart.projectId, false, *, *) returns (Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean, _: Boolean)(_: Connection, _: ScalaCachePool)) when (noPartsProject, false, false, *, *) returns (Future.successful(\/-(testPartList)))

        // partA
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(0).copy(position = 3), *, *) returns (Future.successful(\/-(testPartList(0).copy(position = 3))))
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (updatedPart.copy(position = 3), *, *) returns (Future.successful(\/-(updatedPart.copy(position = 3))))
        // partB
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(1).copy(position = 1), *, *) returns (Future.successful(\/-(testPartList(1).copy(position = 1))))
        // partC
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(2).copy(position = 2), *, *) returns (Future.successful(\/-(testPartList(2).copy(position = 2))))

        val result = projectService.updatePart(updatedPart.id, updatedPart.version, Some(updatedPart.name), Some(updatedPart.position), Some(updatedPart.enabled))
        Await.result(result, Duration.Inf) should be(\/-(updatedPart.copy(position = testPartList.length)))
      }
      "update part if newPosition == oldPosition" in {
        val testProject = TestValues.testProjectA.copy(parts = Vector(
          TestValues.testPartA,
          TestValues.testPartB,
          TestValues.testPartC
        ))
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val updatedPart = testPartList(0).copy(
          name = "updated " + testPartList(0).name,
          enabled = !testPartList(0).enabled
        )

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPartList(0).id, *, *) returns (Future.successful(\/-(testPartList(0))))
        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when (updatedPart.projectId, false, *, *) returns (Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean, _: Boolean)(_: Connection, _: ScalaCachePool)) when (noPartsProject, false, false, *, *) returns (Future.successful(\/-(testPartList)))

        // partA
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(0).copy(position = 1), *, *) returns (Future.successful(\/-(testPartList(0).copy(position = 1))))
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (updatedPart.copy(position = 1), *, *) returns (Future.successful(\/-(updatedPart.copy(position = 1))))
        // partB
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(1).copy(position = 2), *, *) returns (Future.successful(\/-(testPartList(1).copy(position = 2))))
        // partC
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(2).copy(position = 3), *, *) returns (Future.successful(\/-(testPartList(2).copy(position = 3))))

        val result = projectService.updatePart(updatedPart.id, updatedPart.version, Some(updatedPart.name), Some(updatedPart.position), Some(updatedPart.enabled))
        Await.result(result, Duration.Inf) should be(\/-(updatedPart.copy(position = 1)))
      }
      "update part and put it as first element if newPosition == 1 and part list has positions that start from 0" in {
        val testProject = TestValues.testProjectA.copy(parts = Vector(
          TestValues.testPartC.copy(position = 0),
          TestValues.testPartA.copy(position = 1),
          TestValues.testPartB.copy(position = 2)

        ))
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val updatedPart = testPartList(2).copy(
          name = "updated " + testPartList(2).name,
          enabled = !testPartList(2).enabled,
          position = 1
        )

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPartList(2).id, *, *) returns (Future.successful(\/-(testPartList(2))))
        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when (updatedPart.projectId, false, *, *) returns (Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean, _: Boolean)(_: Connection, _: ScalaCachePool)) when (noPartsProject, false, false, *, *) returns (Future.successful(\/-(testPartList)))

        // partA
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(1).copy(position = 3), *, *) returns (Future.successful(\/-(testPartList(1).copy(position = 3))))
        // partB
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(2).copy(position = updatedPart.position), *, *) returns (Future.successful(\/-(testPartList(2).copy(position = updatedPart.position))))
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (updatedPart, *, *) returns (Future.successful(\/-(updatedPart)))
        // partC
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(0).copy(position = 2), *, *) returns (Future.successful(\/-(testPartList(0).copy(position = 2))))

        val result = projectService.updatePart(updatedPart.id, updatedPart.version, Some(updatedPart.name), Some(updatedPart.position), Some(updatedPart.enabled))
        Await.result(result, Duration.Inf) should be(\/-(updatedPart))
      }
      "update only one indicated part if newPosition == oldPosition, and position of other parts is correct (starts with 1)" in {
        val testProject = TestValues.testProjectA.copy(parts = Vector(
          TestValues.testPartA.copy(position = 1),
          TestValues.testPartB.copy(position = 2),
          TestValues.testPartC.copy(position = 3)
        ))
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val updatedPart = testPartList(1).copy(
          name = "updated " + testPartList(1).name,
          enabled = !testPartList(1).enabled
        )

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (updatedPart.id, *, *) returns (Future.successful(\/-(testPartList(1))))
        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when (updatedPart.projectId, false, *, *) returns (Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean, _: Boolean)(_: Connection, _: ScalaCachePool)) when (noPartsProject, false, false, *, *) returns (Future.successful(\/-(testPartList)))

        // partB
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (updatedPart, *, *) returns (Future.successful(\/-(updatedPart)))

        val result = projectService.updatePart(updatedPart.id, updatedPart.version, Some(updatedPart.name), Some(updatedPart.position), Some(updatedPart.enabled))
        Await.result(result, Duration.Inf) should be(\/-(updatedPart))
      }
      "return ServiceError.OfflineLockFail if versions don't match" in {
        val testProject = TestValues.testProjectA.copy(parts = Vector(
          TestValues.testPartA,
          TestValues.testPartB,
          TestValues.testPartC
        ))
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val updatedPart = testPartList(0).copy(
          name = "new " + testPartList(0).name,
          enabled = !testPartList(0).enabled
        )

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (updatedPart.id, *, *) returns (Future.successful(\/-(updatedPart)))

        val result = projectService.updatePart(updatedPart.id, 99L, Some(updatedPart.name), Some(updatedPart.position), Some(updatedPart.enabled))
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
      "return ServiceError.BusinessLogicFail if part list is empty and we change position" in {
        val testProject = TestValues.testProjectA.copy(parts = Vector(
          TestValues.testPartA.copy(position = 1),
          TestValues.testPartB.copy(position = 2),
          TestValues.testPartC.copy(position = 3)
        ))
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val updatedPart = testPartList(1).copy(
          name = "updated " + testPartList(1).name,
          enabled = !testPartList(1).enabled,
          position = 1
        )

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (updatedPart.id, *, *) returns (Future.successful(\/-(testPartList(1))))
        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when (updatedPart.projectId, false, *, *) returns (Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean, _: Boolean)(_: Connection, _: ScalaCachePool)) when (noPartsProject, false, false, *, *) returns (Future.successful(\/-(IndexedSeq.empty[Part])))

        // partB
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (updatedPart, *, *) returns (Future.successful(\/-(updatedPart)))

        val result = projectService.updatePart(updatedPart.id, updatedPart.version, Some(updatedPart.name), Some(updatedPart.position), Some(updatedPart.enabled))
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BusinessLogicFail("Weird, part list shouldn't be empty!")))
      }
      "return RepositoryError.NoResults if project doesn't exist and we change position" in {
        val testProject = TestValues.testProjectD.copy(parts = Vector(
          TestValues.testPartA.copy(position = 1),
          TestValues.testPartB.copy(position = 2),
          TestValues.testPartC.copy(position = 3)
        ))
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val updatedPart = testPartList(1).copy(
          name = "updated " + testPartList(1).name,
          enabled = !testPartList(1).enabled,
          position = 1
        )

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (updatedPart.id, *, *) returns (Future.successful(\/-(testPartList(1))))
        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when (updatedPart.projectId, false, *, *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (partRepository.list(_: Project, _: Boolean, _: Boolean)(_: Connection, _: ScalaCachePool)) when (noPartsProject, false, false, *, *) returns (Future.successful(\/-(testPartList)))

        // partB
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (updatedPart, *, *) returns (Future.successful(\/-(updatedPart)))

        val result = projectService.updatePart(updatedPart.id, updatedPart.version, Some(updatedPart.name), Some(updatedPart.position), Some(updatedPart.enabled))
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("")))
      }
      "return RepositoryError.NoResults if part doesn't exist" in {
        val testProject = TestValues.testProjectA.copy(parts = Vector(
          TestValues.testPartA.copy(position = 1),
          TestValues.testPartD.copy(position = 2),
          TestValues.testPartC.copy(position = 3)
        ))
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val updatedPart = testPartList(1).copy(
          name = "updated " + testPartList(1).name,
          enabled = !testPartList(1).enabled
        )

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (updatedPart.id, *, *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when (updatedPart.projectId, false, *, *) returns (Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean, _: Boolean)(_: Connection, _: ScalaCachePool)) when (noPartsProject, false, false, *, *) returns (Future.successful(\/-(testPartList)))

        // partB
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (updatedPart, *, *) returns (Future.successful(\/-(updatedPart)))

        val result = projectService.updatePart(updatedPart.id, updatedPart.version, Some(updatedPart.name), Some(updatedPart.position), Some(updatedPart.enabled))
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("")))
      }
    }
  }

  "ProjectService.deletePart" should {
    inSequence {
      // Mock appart all parts with changed positions, to test if positions were changed within methods
      "delete part between two parts" in {
        val testProject = TestValues.testProjectA.copy(parts = Vector(
          TestValues.testPartA,
          TestValues.testPartB,
          TestValues.testPartC
        ))
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val deletedPart = testPartList(1)

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (deletedPart.id, *, *) returns (Future.successful(\/-(deletedPart)))
        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when (testProject.id, false, *, *) returns (Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean, _: Boolean)(_: Connection, _: ScalaCachePool)) when (noPartsProject, false, false, *, *) returns (Future.successful(\/-(testPartList)))

        // mock partA with changed position
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(0).copy(position = 1), *, *) returns (Future.successful(\/-(testPartList(0).copy(position = 1))))
        // mock partC with changed position
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(2).copy(position = 2), *, *) returns (Future.successful(\/-(testPartList(2).copy(position = 2))))

        (taskRepository.delete(_: Part)(_: Connection, _: ScalaCachePool)) when (deletedPart, *, *) returns (Future.successful(\/-(IndexedSeq())))
        (partRepository.delete(_: Part)(_: Connection, _: ScalaCachePool)) when (deletedPart, *, *) returns (Future.successful(\/-(deletedPart)))

        val result = projectService.deletePart(deletedPart.id, deletedPart.version)
        Await.result(result, Duration.Inf) should be(\/-(deletedPart))
      }
      "delete part in the beginning" in {
        val testProject = TestValues.testProjectA.copy(parts = Vector(
          TestValues.testPartA,
          TestValues.testPartB,
          TestValues.testPartC
        ))
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val deletedPart = testPartList(0)

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (deletedPart.id, *, *) returns (Future.successful(\/-(deletedPart)))
        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when (deletedPart.projectId, false, *, *) returns (Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean, _: Boolean)(_: Connection, _: ScalaCachePool)) when (noPartsProject, false, false, *, *) returns (Future.successful(\/-(testPartList)))

        // mock partB with changed position
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(1).copy(position = 1), *, *) returns (Future.successful(\/-(testPartList(1).copy(position = 1))))

        // mock partC with changed position
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(2).copy(position = 2), *, *) returns (Future.successful(\/-(testPartList(2).copy(position = 2))))

        (taskRepository.delete(_: Part)(_: Connection, _: ScalaCachePool)) when (deletedPart, *, *) returns (Future.successful(\/-(IndexedSeq())))
        (partRepository.delete(_: Part)(_: Connection, _: ScalaCachePool)) when (deletedPart, *, *) returns (Future.successful(\/-(deletedPart)))

        val result = projectService.deletePart(deletedPart.id, deletedPart.version)
        Await.result(result, Duration.Inf) should be(\/-(deletedPart))
      }
      "delete the part between and change the positions only of the parts that go after, because positions start with 1" in {
        val testProject = TestValues.testProjectA.copy(parts = Vector(
          TestValues.testPartC.copy(position = 1),
          TestValues.testPartA.copy(position = 2),
          TestValues.testPartB.copy(position = 3)
        ))
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val deletedPart = testPartList(1)

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (deletedPart.id, *, *) returns (Future.successful(\/-(deletedPart)))
        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when (deletedPart.projectId, false, *, *) returns (Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean, _: Boolean)(_: Connection, _: ScalaCachePool)) when (noPartsProject, false, false, *, *) returns (Future.successful(\/-(testPartList)))

        // mock partB with changed position
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(2).copy(position = 2), *, *) returns (Future.successful(\/-(testPartList(2).copy(position = 2))))

        (taskRepository.delete(_: Part)(_: Connection, _: ScalaCachePool)) when (deletedPart, *, *) returns (Future.successful(\/-(IndexedSeq())))
        (partRepository.delete(_: Part)(_: Connection, _: ScalaCachePool)) when (deletedPart, *, *) returns (Future.successful(\/-(deletedPart)))

        val result = projectService.deletePart(deletedPart.id, deletedPart.version)
        Await.result(result, Duration.Inf) should be(\/-(deletedPart))
      }
      "delete part between and change all parts positions if position starts with 0" in {
        val testProject = TestValues.testProjectA.copy(parts = Vector(
          TestValues.testPartC.copy(position = 0),
          TestValues.testPartA.copy(position = 1),
          TestValues.testPartB.copy(position = 2)
        ))
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val deletedPart = testPartList(1)

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (deletedPart.id, *, *) returns (Future.successful(\/-(deletedPart)))
        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when (deletedPart.projectId, false, *, *) returns (Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean, _: Boolean)(_: Connection, _: ScalaCachePool)) when (noPartsProject, false, false, *, *) returns (Future.successful(\/-(testPartList)))

        // mock partC with changed position
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(0).copy(position = 1), *, *) returns (Future.successful(\/-(testPartList(0).copy(position = 1))))
        // mock partB with changed position
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(2).copy(position = 2), *, *) returns (Future.successful(\/-(testPartList(2).copy(position = 2))))

        (taskRepository.delete(_: Part)(_: Connection, _: ScalaCachePool)) when (deletedPart, *, *) returns (Future.successful(\/-(IndexedSeq())))
        (partRepository.delete(_: Part)(_: Connection, _: ScalaCachePool)) when (deletedPart, *, *) returns (Future.successful(\/-(deletedPart)))

        val result = projectService.deletePart(deletedPart.id, deletedPart.version)
        Await.result(result, Duration.Inf) should be(\/-(deletedPart))
      }
      "delete part in the end" in {
        val testProject = TestValues.testProjectA.copy(parts = Vector(
          TestValues.testPartA,
          TestValues.testPartB
        ))
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val deletedPart = testPartList(1)

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (deletedPart.id, *, *) returns (Future.successful(\/-(deletedPart)))
        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when (deletedPart.projectId, false, *, *) returns (Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean, _: Boolean)(_: Connection, _: ScalaCachePool)) when (noPartsProject, false, false, *, *) returns (Future.successful(\/-(testPartList)))

        // mock partA with changed position
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(0).copy(position = 1), *, *) returns (Future.successful(\/-(testPartList(0).copy(position = 1))))

        (taskRepository.delete(_: Part)(_: Connection, _: ScalaCachePool)) when (deletedPart, *, *) returns (Future.successful(\/-(IndexedSeq())))
        (partRepository.delete(_: Part)(_: Connection, _: ScalaCachePool)) when (deletedPart, *, *) returns (Future.successful(\/-(deletedPart)))

        val result = projectService.deletePart(deletedPart.id, deletedPart.version)
        Await.result(result, Duration.Inf) should be(\/-(deletedPart))
      }
      "delete part in the end and do not update rest parts position if position starts with 1" in {
        val testProject = TestValues.testProjectA.copy(parts = Vector(
          TestValues.testPartA.copy(position = 1),
          TestValues.testPartB.copy(position = 2)
        ))
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val deletedPart = testPartList(1)

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (deletedPart.id, *, *) returns (Future.successful(\/-(deletedPart)))
        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when (deletedPart.projectId, false, *, *) returns (Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean, _: Boolean)(_: Connection, _: ScalaCachePool)) when (noPartsProject, false, false, *, *) returns (Future.successful(\/-(testPartList)))

        (taskRepository.delete(_: Part)(_: Connection, _: ScalaCachePool)) when (deletedPart, *, *) returns (Future.successful(\/-(IndexedSeq())))
        (partRepository.delete(_: Part)(_: Connection, _: ScalaCachePool)) when (deletedPart, *, *) returns (Future.successful(\/-(deletedPart)))

        val result = projectService.deletePart(deletedPart.id, deletedPart.version)
        Await.result(result, Duration.Inf) should be(\/-(deletedPart))
      }
      "return ServiceError.OfflineLockFail if versions don't match" in {
        val testProject = TestValues.testProjectA.copy(parts = Vector(
          TestValues.testPartA,
          TestValues.testPartB,
          TestValues.testPartC
        ))
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPartList(0).id, *, *) returns (Future.successful(\/-(testPartList(0))))

        val result = projectService.deletePart(testPartList(0).id, 99L)
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
      "return ServiceError.BusinessLogicFail if part list is empty" in {
        val testProject = TestValues.testProjectA.copy(parts = Vector(
          TestValues.testPartA,
          TestValues.testPartB,
          TestValues.testPartC
        ))
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val deletedPart = testPartList(0)

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (deletedPart.id, *, *) returns (Future.successful(\/-(deletedPart)))
        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when (deletedPart.projectId, false, *, *) returns (Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean, _: Boolean)(_: Connection, _: ScalaCachePool)) when (noPartsProject, false, false, *, *) returns (Future.successful(\/-(IndexedSeq.empty[Part])))

        // mock partB with changed position
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(1).copy(position = 1), *, *) returns (Future.successful(\/-(testPartList(1).copy(position = 1))))

        // mock partC with changed position
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPartList(2).copy(position = 2), *, *) returns (Future.successful(\/-(testPartList(2).copy(position = 2))))

        (taskRepository.delete(_: Part)(_: Connection, _: ScalaCachePool)) when (deletedPart, *, *) returns (Future.successful(\/-(IndexedSeq())))
        (partRepository.delete(_: Part)(_: Connection, _: ScalaCachePool)) when (deletedPart, *, *) returns (Future.successful(\/-(deletedPart)))

        val result = projectService.deletePart(deletedPart.id, deletedPart.version)
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BusinessLogicFail("Weird, part list shouldn't be empty!")))
      }
      "return RepositoryError.NoResults if project doesn't exist" in {
        val testProject = TestValues.testProjectD.copy(parts = Vector(
          TestValues.testPartA.copy(position = 1),
          TestValues.testPartB.copy(position = 2)
        ))
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val deletedPart = testPartList(1)

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (deletedPart.id, *, *) returns (Future.successful(\/-(deletedPart)))
        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when (deletedPart.projectId, false, *, *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (partRepository.list(_: Project, _: Boolean, _: Boolean)(_: Connection, _: ScalaCachePool)) when (noPartsProject, false, false, *, *) returns (Future.successful(\/-(testPartList)))

        (taskRepository.delete(_: Part)(_: Connection, _: ScalaCachePool)) when (deletedPart, *, *) returns (Future.successful(\/-(IndexedSeq())))
        (partRepository.delete(_: Part)(_: Connection, _: ScalaCachePool)) when (deletedPart, *, *) returns (Future.successful(\/-(deletedPart)))

        val result = projectService.deletePart(deletedPart.id, deletedPart.version)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("")))
      }
      "return RepositoryError.NoResults if part doesn't exist" in {
        val testProject = TestValues.testProjectA.copy(parts = Vector(
          TestValues.testPartA.copy(position = 1),
          TestValues.testPartD.copy(position = 2)
        ))
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val deletedPart = testPartList(1)

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (deletedPart.id, *, *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when (deletedPart.projectId, false, *, *) returns (Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean, _: Boolean)(_: Connection, _: ScalaCachePool)) when (noPartsProject, false, false, *, *) returns (Future.successful(\/-(testPartList)))

        (taskRepository.delete(_: Part)(_: Connection, _: ScalaCachePool)) when (deletedPart, *, *) returns (Future.successful(\/-(IndexedSeq())))
        (partRepository.delete(_: Part)(_: Connection, _: ScalaCachePool)) when (deletedPart, *, *) returns (Future.successful(\/-(deletedPart)))

        val result = projectService.deletePart(deletedPart.id, deletedPart.version)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("")))
      }
    }
  }

  "ProjectService.togglePart" should {
    inSequence {
      "enable a disabled part" in {
        val testPart = TestValues.testPartA.copy(enabled = false)

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPart.copy(enabled = !testPart.enabled), *, *) returns (Future.successful(\/-(testPart.copy(enabled = !testPart.enabled))))

        val result = projectService.togglePart(testPart.id, testPart.version)
        Await.result(result, Duration.Inf) should be(\/-(testPart.copy(enabled = !testPart.enabled)))
      }
      "disable an enabled part" in {
        val testPart = TestValues.testPartA.copy(enabled = true)

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPart.copy(enabled = !testPart.enabled), *, *) returns (Future.successful(\/-(testPart.copy(enabled = !testPart.enabled))))

        val result = projectService.togglePart(testPart.id, testPart.version)
        Await.result(result, Duration.Inf) should be(\/-(testPart.copy(enabled = !testPart.enabled)))
      }
      "return ServiceError.OfflineLockFail if versions don't match" in {
        val testPart = TestValues.testPartA

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        val result = projectService.togglePart(testPart.id, 99L)
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
      "return RepositoryError.NoResults if part doesn't exist" in {
        val testPart = TestValues.testPartD.copy(enabled = true)

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when (testPart.copy(enabled = !testPart.enabled), *, *) returns (Future.successful(\/-(testPart.copy(enabled = !testPart.enabled))))

        val result = projectService.togglePart(testPart.id, testPart.version)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("")))
      }
    }
  }

  "ProjectService.createTask" should {
    inSequence {
      "create new task and update existing tasks if new task position = min position" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 14),
          TestValues.testMultipleChoiceTaskC.copy(position = 16)
        ))
        val testTaskList = testPart.tasks
        val newTask = DocumentTask(
          partId = testPart.id,
          position = testTaskList.head.position,
          settings = CommonTaskSettings(
            title = "New task name",
            description = "New task description"
          ),
          maxGrade = "0"
        )

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        for (i <- testTaskList.indices) {
          testTaskList(i) match {
            case task: DocumentTask => (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (task.copy(position = i + 2), *, *, *) returns (Future.successful(\/-(task.copy(position = i + 2))))
            case task: QuestionTask => (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (task.copy(position = i + 2), *, *, *) returns (Future.successful(\/-(task.copy(position = i + 2))))
          }
        }

        (taskRepository.insert(_: Task)(_: Connection, _: ScalaCachePool)) when (newTask.copy(position = 1), *, *) returns (Future.successful(\/-(newTask.copy(position = 1))))

        val result = projectService.createTask(newTask.partId, newTask.taskType, newTask.settings.title, newTask.settings.description, newTask.position, newTask.id)
        val \/-(task: Task) = Await.result(result, Duration.Inf)

        task.partId should be(newTask.partId)
        task.settings.title should be(newTask.settings.title)
        task.settings.description should be(newTask.settings.description)
        task.position should be(1)
      }
      "create new task and put it as first element and move all over tasks position if new task position < min position" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 14),
          TestValues.testMultipleChoiceTaskC.copy(position = 16)
        ))
        val testTaskList = testPart.tasks
        val newTask = DocumentTask(
          partId = testPart.id,
          position = testTaskList.head.position - 99,
          settings = CommonTaskSettings(
            title = "New task name",
            description = "New task description"
          ),
          maxGrade = "0"
        )

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        for (i <- testTaskList.indices) {
          testTaskList(i) match {
            case task: DocumentTask => (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (task.copy(position = i + 2), *, *, *) returns (Future.successful(\/-(task.copy(position = i + 2))))
            case task: QuestionTask => (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (task.copy(position = i + 2), *, *, *) returns (Future.successful(\/-(task.copy(position = i + 2))))
          }
        }

        (taskRepository.insert(_: Task)(_: Connection, _: ScalaCachePool)) when (newTask.copy(position = 1), *, *) returns (Future.successful(\/-(newTask.copy(position = 1))))

        val result = projectService.createTask(newTask.partId, newTask.taskType, newTask.settings.title, newTask.settings.description, newTask.position, newTask.id)
        val \/-(task: Task) = Await.result(result, Duration.Inf)

        task.partId should be(newTask.partId)
        task.settings.title should be(newTask.settings.title)
        task.settings.description should be(newTask.settings.description)
        task.position should be(1)
      }
      "create new task and update position of all elements if new task position = max position" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 14),
          TestValues.testMultipleChoiceTaskC.copy(position = 16)
        ))
        val testTaskList = testPart.tasks
        val newTask = DocumentTask(
          partId = testPart.id,
          position = testTaskList.last.position,
          settings = CommonTaskSettings(
            title = "New task name",
            description = "New task description"
          ),
          maxGrade = "0"
        )

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(0).asInstanceOf[DocumentTask].copy(position = 1), *, *, *) returns (Future.successful(\/-(testTaskList(0).asInstanceOf[DocumentTask].copy(position = 1))))
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 2))))
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(2).asInstanceOf[QuestionTask].copy(position = testTaskList.length + 1), *, *, *) returns (Future.successful(\/-(testTaskList(2).asInstanceOf[QuestionTask].copy(position = testTaskList.length + 1))))
        (taskRepository.insert(_: Task)(_: Connection, _: ScalaCachePool)) when (newTask.copy(position = testTaskList.length), *, *) returns (Future.successful(\/-(newTask.copy(position = testTaskList.length))))

        val result = projectService.createTask(newTask.partId, newTask.taskType, newTask.settings.title, newTask.settings.description, newTask.position, newTask.id)
        val \/-(task: Task) = Await.result(result, Duration.Inf)

        task.partId should be(newTask.partId)
        task.settings.title should be(newTask.settings.title)
        task.settings.description should be(newTask.settings.description)
        task.position should be(testTaskList.length)
      }
      "create new task and insert between elements and update positions of all elements even if they are unsorted " in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 15),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 19)
        ))
        val testTaskList = testPart.tasks
        val newTask = DocumentTask(
          partId = testPart.id,
          position = testTaskList(0).position,
          settings = CommonTaskSettings(
            title = "New task name",
            description = "New task description"
          ),
          maxGrade = "0"
        )

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(0).asInstanceOf[DocumentTask].copy(position = 3), *, *, *) returns (Future.successful(\/-(testTaskList(0).asInstanceOf[DocumentTask].copy(position = 3))))
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 1), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 1))))
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(2).asInstanceOf[QuestionTask].copy(position = 4), *, *, *) returns (Future.successful(\/-(testTaskList(2).asInstanceOf[QuestionTask].copy(position = 4))))
        (taskRepository.insert(_: Task)(_: Connection, _: ScalaCachePool)) when (newTask.copy(position = 2), *, *) returns (Future.successful(\/-(newTask.copy(position = 2))))

        val result = projectService.createTask(newTask.partId, newTask.taskType, newTask.settings.title, newTask.settings.description, newTask.position, newTask.id)
        val \/-(task: Task) = Await.result(result, Duration.Inf)

        task.partId should be(newTask.partId)
        task.settings.title should be(newTask.settings.title)
        task.settings.description should be(newTask.settings.description)
        task.position should be(2)
      }
      "create new task and give it position of last element + 1 if new task position > max position" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 14),
          TestValues.testMultipleChoiceTaskC.copy(position = 16)
        ))
        val testTaskList = testPart.tasks
        val newTask = DocumentTask(
          partId = testPart.id,
          position = testTaskList.last.position + 99,
          settings = CommonTaskSettings(
            title = "New task name",
            description = "New task description"
          ),
          maxGrade = "0"
        )

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(0).asInstanceOf[DocumentTask].copy(position = 1), *, *, *) returns (Future.successful(\/-(testTaskList(0).asInstanceOf[DocumentTask].copy(position = 1))))
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 2))))
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(2).asInstanceOf[QuestionTask].copy(position = 3), *, *, *) returns (Future.successful(\/-(testTaskList(2).asInstanceOf[QuestionTask].copy(position = 3))))
        (taskRepository.insert(_: Task)(_: Connection, _: ScalaCachePool)) when (newTask.copy(position = 4), *, *) returns (Future.successful(\/-(newTask.copy(position = 4))))

        val result = projectService.createTask(newTask.partId, newTask.taskType, newTask.settings.title, newTask.settings.description, newTask.position, newTask.id)
        val \/-(task: Task) = Await.result(result, Duration.Inf)

        task.partId should be(newTask.partId)
        task.settings.title should be(newTask.settings.title)
        task.settings.description should be(newTask.settings.description)
        task.position should be(testTaskList.length + 1)
      }
      "create new task and give it position 1 if taskList is empty" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector())
        val testTaskList = testPart.tasks
        val newTask = DocumentTask(
          partId = testPart.id,
          position = 99,
          settings = CommonTaskSettings(
            title = "New task name",
            description = "New task description"
          ),
          maxGrade = "0"
        )

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        (taskRepository.insert(_: Task)(_: Connection, _: ScalaCachePool)) when (newTask.copy(position = 1), *, *) returns (Future.successful(\/-(newTask.copy(position = 1))))

        val result = projectService.createTask(newTask.partId, newTask.taskType, newTask.settings.title, newTask.settings.description, newTask.position, newTask.id)
        val \/-(task: Task) = Await.result(result, Duration.Inf)

        task.partId should be(newTask.partId)
        task.settings.title should be(newTask.settings.title)
        task.settings.description should be(newTask.settings.description)
        task.position should be(1)
      }
      "create new task and give it position 1 if taskList is not empty and taskList positions start with 0 and we assign position 1 to newTask" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 0),
          TestValues.testShortAnswerTaskB.copy(position = 1),
          TestValues.testMultipleChoiceTaskC.copy(position = 2)
        ))
        val testTaskList = testPart.tasks
        val newTask = DocumentTask(
          partId = testPart.id,
          position = 1,
          settings = CommonTaskSettings(
            title = "New task name",
            description = "New task description"
          ),
          maxGrade = "0"
        )

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(0).asInstanceOf[DocumentTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(0).asInstanceOf[DocumentTask].copy(position = 2))))
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 3), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 3))))
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(2).asInstanceOf[QuestionTask].copy(position = 4), *, *, *) returns (Future.successful(\/-(testTaskList(2).asInstanceOf[QuestionTask].copy(position = 4))))
        (taskRepository.insert(_: Task)(_: Connection, _: ScalaCachePool)) when (newTask.copy(position = 1), *, *) returns (Future.successful(\/-(newTask.copy(position = 1))))

        val result = projectService.createTask(newTask.partId, newTask.taskType, newTask.settings.title, newTask.settings.description, newTask.position, newTask.id)
        val \/-(task: Task) = Await.result(result, Duration.Inf)

        task.partId should be(newTask.partId)
        task.settings.title should be(newTask.settings.title)
        task.settings.description should be(newTask.settings.description)
        task.position should be(1)
      }
      "create new task and add it to the end of list and do not update taskList positions if they are correct (1, 2, ...)" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 1),
          TestValues.testShortAnswerTaskB.copy(position = 2),
          TestValues.testMultipleChoiceTaskC.copy(position = 3)
        ))
        val testTaskList = testPart.tasks
        val newTask = DocumentTask(
          partId = testPart.id,
          position = testTaskList.length + 1,
          settings = CommonTaskSettings(
            title = "New task name",
            description = "New task description"
          ),
          maxGrade = "0"
        )

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        (taskRepository.insert(_: Task)(_: Connection, _: ScalaCachePool)) when (newTask.copy(position = testTaskList.length + 1), *, *) returns (Future.successful(\/-(newTask.copy(position = testTaskList.length + 1))))

        val result = projectService.createTask(newTask.partId, newTask.taskType, newTask.settings.title, newTask.settings.description, newTask.position, newTask.id)
        val \/-(task: Task) = Await.result(result, Duration.Inf)

        task.partId should be(newTask.partId)
        task.settings.title should be(newTask.settings.title)
        task.settings.description should be(newTask.settings.description)
        task.position should be(testTaskList.length + 1)
      }
      "return RepositoryError.NoResults if part doesn't exist" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 1),
          TestValues.testShortAnswerTaskB.copy(position = 2),
          TestValues.testMultipleChoiceTaskC.copy(position = 3)
        ))
        val testTaskList = testPart.tasks
        val newTask = DocumentTask(
          partId = testPart.id,
          position = testTaskList.length + 1,
          settings = CommonTaskSettings(
            title = "New task name",
            description = "New task description"
          ),
          maxGrade = "0"
        )

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))

        (taskRepository.insert(_: Task)(_: Connection, _: ScalaCachePool)) when (newTask.copy(position = testTaskList.length + 1), *, *) returns (Future.successful(\/-(newTask.copy(position = testTaskList.length + 1))))

        val result = projectService.createTask(newTask.partId, newTask.taskType, newTask.settings.title, newTask.settings.description, newTask.position, newTask.id)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("")))
      }
    }
  }

  "ProjectService.updateDocumentTask (LongAnswerTask)" should {
    inSequence {
      "update DocumentTask and update existing tasks if new part position = min position within same part" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 12)
        ))
        val testTaskList = testPart.tasks
        val originTask = testTaskList(0).asInstanceOf[DocumentTask]
        val updatedTask = originTask.copy(
          position = testTaskList.map(_.position).min,
          settings = originTask.settings.copy(
            title = "New task name",
            help = "New task help",
            description = "New task description",
            notesAllowed = !originTask.settings.notesAllowed,
            notesTitle = Some("new notes title"),
            responseTitle = Some("new response title")
          ),
          dependencyId = Some(originTask.id)
        )
        val commonTaskArgs = new projectService.CommonTaskArgs(
          taskId = updatedTask.id,
          version = updatedTask.version,
          name = Some(updatedTask.settings.title),
          help = Some(updatedTask.settings.help),
          description = Some(updatedTask.settings.description),
          position = Some(updatedTask.position),
          notesAllowed = Some(updatedTask.settings.notesAllowed),
          partId = Some(updatedTask.partId),
          maxGrade = Some(updatedTask.maxGrade),
          notesTitle = Some(updatedTask.settings.notesTitle),
          responseTitle = Some(updatedTask.settings.responseTitle)
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(originTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        // Move
        // task A
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (originTask.copy(position = 1), *, *, *) returns (Future.successful(\/-(originTask.copy(position = 1))))
        // task B
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 2))))
        // task C
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(2).asInstanceOf[QuestionTask].copy(position = 3), *, *, *) returns (Future.successful(\/-(testTaskList(2).asInstanceOf[QuestionTask].copy(position = 3))))

        // update task A
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (updatedTask.copy(position = 1), *, *, *) returns (Future.successful(\/-(updatedTask.copy(position = 1))))

        val result = projectService.updateDocumentTask(commonTaskArgs, Some(updatedTask.dependencyId))
        Await.result(result, Duration.Inf) should be(\/-(updatedTask.copy(position = 1)))
      }
      "update DocumentTask and update dependencyId, notesTitle, responseTitle to None (if commonTaskArgs value is Some(None))(aka erase)" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskF.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 12)
        ))
        val testTaskList = testPart.tasks
        val originTask = testTaskList(0).asInstanceOf[DocumentTask]
        val updatedTask = originTask.copy(
          position = testTaskList.map(_.position).min,
          settings = originTask.settings.copy(
            title = "New task name",
            help = "New task help",
            description = "New task description",
            notesAllowed = !originTask.settings.notesAllowed,
            notesTitle = None,
            responseTitle = None
          ),
          dependencyId = None
        )
        val commonTaskArgs = new projectService.CommonTaskArgs(
          taskId = updatedTask.id,
          version = updatedTask.version,
          name = Some(updatedTask.settings.title),
          help = Some(updatedTask.settings.help),
          description = Some(updatedTask.settings.description),
          position = Some(updatedTask.position),
          notesAllowed = Some(updatedTask.settings.notesAllowed),
          partId = Some(updatedTask.partId),
          maxGrade = Some(updatedTask.maxGrade),
          notesTitle = Some(updatedTask.settings.notesTitle),
          responseTitle = Some(updatedTask.settings.responseTitle)
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(originTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        // Move
        // task A
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (originTask.copy(position = 1), *, *, *) returns (Future.successful(\/-(originTask.copy(position = 1))))
        // task B
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 2))))
        // task C
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(2).asInstanceOf[QuestionTask].copy(position = 3), *, *, *) returns (Future.successful(\/-(testTaskList(2).asInstanceOf[QuestionTask].copy(position = 3))))

        // update task A
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (updatedTask.copy(position = 1), *, *, *) returns (Future.successful(\/-(updatedTask.copy(position = 1))))

        val result = projectService.updateDocumentTask(commonTaskArgs, Some(None))
        originTask.dependencyId should not be (None)
        Await.result(result, Duration.Inf) should be(\/-(updatedTask.copy(position = 1)))
      }
      "update DocumentTask and don't update dependencyId, notesTitle, responseTitle (if commonTaskArgs value is None)" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskF.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 12)
        ))
        val testTaskList = testPart.tasks
        val originTask = testTaskList(0).asInstanceOf[DocumentTask]
        val updatedTask = originTask.copy(
          position = testTaskList.map(_.position).min,
          settings = originTask.settings.copy(
            title = "New task name",
            help = "New task help",
            description = "New task description",
            notesAllowed = !originTask.settings.notesAllowed,
            notesTitle = originTask.settings.notesTitle,
            responseTitle = originTask.settings.responseTitle
          ),
          dependencyId = originTask.dependencyId
        )
        val commonTaskArgs = new projectService.CommonTaskArgs(
          taskId = updatedTask.id,
          version = updatedTask.version,
          name = Some(updatedTask.settings.title),
          help = Some(updatedTask.settings.help),
          description = Some(updatedTask.settings.description),
          position = Some(updatedTask.position),
          maxGrade = Some(updatedTask.maxGrade),
          notesAllowed = Some(updatedTask.settings.notesAllowed),
          partId = Some(updatedTask.partId),
          notesTitle = None,
          responseTitle = None
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(originTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        // Move
        // task A
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (originTask.copy(position = 1), *, *, *) returns (Future.successful(\/-(originTask.copy(position = 1))))
        // task B
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 2))))
        // task C
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(2).asInstanceOf[QuestionTask].copy(position = 3), *, *, *) returns (Future.successful(\/-(testTaskList(2).asInstanceOf[QuestionTask].copy(position = 3))))

        // update task A
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (updatedTask.copy(position = 1), *, *, *) returns (Future.successful(\/-(updatedTask.copy(position = 1))))

        val result = projectService.updateDocumentTask(commonTaskArgs, None)
        originTask.dependencyId should not be (None)
        Await.result(result, Duration.Inf) should be(\/-(updatedTask.copy(position = 1)))
      }
      "return ServiceError.BadInput if task has wrong type" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskF.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 12)
        ))
        val testTaskList = testPart.tasks
        val originTask = testTaskList(0).asInstanceOf[DocumentTask]
        val updatedTask = originTask.copy(
          position = testTaskList.map(_.position).min,
          settings = originTask.settings.copy(
            title = "New task name",
            help = "New task help",
            description = "New task description",
            notesAllowed = !originTask.settings.notesAllowed,
            notesTitle = None,
            responseTitle = None
          ),
          dependencyId = None
        )
        val commonTaskArgs = new projectService.CommonTaskArgs(
          taskId = updatedTask.id,
          version = updatedTask.version,
          name = Some(updatedTask.settings.title),
          help = Some(updatedTask.settings.help),
          description = Some(updatedTask.settings.description),
          position = Some(updatedTask.position),
          maxGrade = Some(updatedTask.maxGrade),
          notesAllowed = Some(updatedTask.settings.notesAllowed),
          partId = Some(updatedTask.partId),
          notesTitle = Some(updatedTask.settings.notesTitle),
          responseTitle = Some(updatedTask.settings.responseTitle)
        )

        originTask.dependencyId should not be (None)

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(testTaskList(1))))

        val result = projectService.updateDocumentTask(commonTaskArgs)
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadInput("services.ProjectService.updateDocumentTask.wrongTaskType")))
      }
      "return ServiceError.OfflineLockFail if version is wrong" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskF.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 12)
        ))
        val testTaskList = testPart.tasks
        val originTask = testTaskList(0).asInstanceOf[DocumentTask]
        val updatedTask = originTask.copy(
          position = testTaskList.map(_.position).min,
          settings = originTask.settings.copy(
            title = "New task name",
            help = "New task help",
            description = "New task description",
            notesAllowed = !originTask.settings.notesAllowed,
            notesTitle = None,
            responseTitle = None
          ),
          dependencyId = None
        )
        val commonTaskArgs = new projectService.CommonTaskArgs(
          taskId = updatedTask.id,
          version = updatedTask.version + 1,
          name = Some(updatedTask.settings.title),
          help = Some(updatedTask.settings.help),
          description = Some(updatedTask.settings.description),
          position = Some(updatedTask.position),
          maxGrade = Some(updatedTask.maxGrade),
          notesAllowed = Some(updatedTask.settings.notesAllowed),
          partId = Some(updatedTask.partId),
          notesTitle = Some(updatedTask.settings.notesTitle),
          responseTitle = Some(updatedTask.settings.responseTitle)
        )

        originTask.dependencyId should not be (None)

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(originTask)))

        val result = projectService.updateDocumentTask(commonTaskArgs)
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
    }
  }

  "ProjectService.updateQuestionTask (ShortAnswerTask)" should {
    inSequence {
      "update QuestionTask and update existing tasks if new part position = min position (within same part)" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 12)
        ))
        val testTaskList = testPart.tasks
        val updatedTask = testTaskList(1).asInstanceOf[QuestionTask].copy(
          position = testTaskList.map(_.position).min,
          settings = testTaskList(1).settings.copy(
            title = "New task name",
            help = "New task help",
            description = "New task description",
            notesAllowed = !testTaskList(1).settings.notesAllowed,
            notesTitle = Some("new notes title"),
            responseTitle = Some("new response title")
          ),
          questions = IndexedSeq(
            TestValues.testMultipleChoiceQuestionB,
            TestValues.testOrderingQuestionC
          )
        )
        val commonTaskArgs = new projectService.CommonTaskArgs(
          taskId = updatedTask.id,
          version = updatedTask.version,
          name = Some(updatedTask.settings.title),
          help = Some(updatedTask.settings.help),
          description = Some(updatedTask.settings.description),
          position = Some(updatedTask.position),
          maxGrade = Some(updatedTask.maxGrade),
          notesAllowed = Some(updatedTask.settings.notesAllowed),
          partId = Some(updatedTask.partId),
          notesTitle = Some(updatedTask.settings.notesTitle),
          responseTitle = Some(updatedTask.settings.responseTitle)
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testTaskList(1).id, *, *) returns (Future.successful(\/-(testTaskList(1))))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(0).asInstanceOf[DocumentTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(0).asInstanceOf[DocumentTask].copy(position = 2, version = testTaskList(0).version + 1))))
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 1), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 1, version = testTaskList(1).version + 1))))
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(2).asInstanceOf[QuestionTask].copy(position = 3), *, *, *) returns (Future.successful(\/-(testTaskList(2).asInstanceOf[QuestionTask].copy(position = 3, version = testTaskList(2).version + 1))))
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (updatedTask.copy(position = 1, version = updatedTask.version + 1), *, *, *) returns (Future.successful(\/-(updatedTask.copy(position = 1, version = updatedTask.version + 2))))

        val result = projectService.updateQuestionTask(commonTaskArgs, Some(updatedTask.questions))
        Await.result(result, Duration.Inf) should be(\/-(updatedTask.copy(position = 1, version = updatedTask.version + 2)))
      }
      "update QuestionTask and leave specific values unchanged if we don't pass them as parameters" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 12)
        ))
        val testTaskList = testPart.tasks
        val updatedTask = testTaskList(1).asInstanceOf[QuestionTask].copy(
          position = testTaskList.map(_.position).min,
          settings = testTaskList(1).settings.copy(
            title = "New task name",
            help = "New task help",
            description = "New task description",
            notesAllowed = !testTaskList(1).settings.notesAllowed,
            notesTitle = Some("new notes title"),
            responseTitle = Some("new response title")
          )
        )
        val commonTaskArgs = new projectService.CommonTaskArgs(
          taskId = updatedTask.id,
          version = updatedTask.version,
          name = Some(updatedTask.settings.title),
          help = Some(updatedTask.settings.help),
          description = Some(updatedTask.settings.description),
          position = Some(updatedTask.position),
          maxGrade = Some(updatedTask.maxGrade),
          notesAllowed = Some(updatedTask.settings.notesAllowed),
          partId = Some(updatedTask.partId),
          notesTitle = Some(updatedTask.settings.notesTitle),
          responseTitle = Some(updatedTask.settings.responseTitle)
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testTaskList(1).id, *, *) returns (Future.successful(\/-(testTaskList(1))))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(0).asInstanceOf[DocumentTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(0).asInstanceOf[DocumentTask].copy(position = 2, version = testTaskList(0).version + 1))))
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 1), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 1, version = testTaskList(1).version + 1))))
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(2).asInstanceOf[QuestionTask].copy(position = 3), *, *, *) returns (Future.successful(\/-(testTaskList(2).asInstanceOf[QuestionTask].copy(position = 3, version = testTaskList(2).version + 1))))
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (updatedTask.copy(position = 1, version = updatedTask.version + 1), *, *, *) returns (Future.successful(\/-(updatedTask.copy(position = 1, version = updatedTask.version + 2))))

        val result = projectService.updateQuestionTask(commonTaskArgs)
        Await.result(result, Duration.Inf) should be(\/-(updatedTask.copy(position = 1, version = updatedTask.version + 2)))
      }
      "update QuestionTask and update notesTitle, responseTitle to None (aka erase)" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 12)
        ))
        val testTaskList = testPart.tasks
        val originTask = testTaskList(1).asInstanceOf[QuestionTask]
        val updatedTask = originTask.copy(
          position = testTaskList.map(_.position).min,
          settings = originTask.settings.copy(
            title = "New task name",
            help = "New task help",
            description = "New task description",
            notesAllowed = !testTaskList(1).settings.notesAllowed,
            notesTitle = None,
            responseTitle = None
          )
        )
        val commonTaskArgs = new projectService.CommonTaskArgs(
          taskId = updatedTask.id,
          version = updatedTask.version,
          name = Some(updatedTask.settings.title),
          help = Some(updatedTask.settings.help),
          description = Some(updatedTask.settings.description),
          position = Some(updatedTask.position),
          maxGrade = Some(updatedTask.maxGrade),
          notesAllowed = Some(updatedTask.settings.notesAllowed),
          partId = Some(updatedTask.partId),
          notesTitle = Some(updatedTask.settings.notesTitle),
          responseTitle = Some(updatedTask.settings.responseTitle)
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(originTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(0).asInstanceOf[DocumentTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(0).asInstanceOf[DocumentTask].copy(position = 2, version = testTaskList(0).version + 1))))
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 1), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 1, version = testTaskList(1).version + 1))))
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(2).asInstanceOf[QuestionTask].copy(position = 3), *, *, *) returns (Future.successful(\/-(testTaskList(2).asInstanceOf[QuestionTask].copy(position = 3, version = testTaskList(2).version + 1))))
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (updatedTask.copy(position = 1, version = updatedTask.version + 1), *, *, *) returns (Future.successful(\/-(updatedTask.copy(position = 1, version = updatedTask.version + 2))))

        val result = projectService.updateQuestionTask(commonTaskArgs, Some(updatedTask.questions))
        Await.result(result, Duration.Inf) should be(\/-(updatedTask.copy(position = 1, version = updatedTask.version + 2)))
      }
      "return ServiceError.BadInput if task has wrong type" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 12)
        ))
        val testTaskList = testPart.tasks
        val originTask = testTaskList(1).asInstanceOf[QuestionTask]
        val updatedTask = originTask.copy(
          position = testTaskList.map(_.position).min,
          settings = originTask.settings.copy(
            title = "New task name",
            help = "New task help",
            description = "New task description",
            notesAllowed = !testTaskList(1).settings.notesAllowed,
            notesTitle = None,
            responseTitle = None
          )
        )
        val commonTaskArgs = new projectService.CommonTaskArgs(
          taskId = updatedTask.id,
          version = updatedTask.version,
          name = Some(updatedTask.settings.title),
          help = Some(updatedTask.settings.help),
          description = Some(updatedTask.settings.description),
          position = Some(updatedTask.position),
          maxGrade = Some(updatedTask.maxGrade),
          notesAllowed = Some(updatedTask.settings.notesAllowed),
          partId = Some(updatedTask.partId),
          notesTitle = Some(updatedTask.settings.notesTitle),
          responseTitle = Some(updatedTask.settings.responseTitle)
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(testTaskList(0))))

        val result = projectService.updateQuestionTask(commonTaskArgs, Some(updatedTask.questions))
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadInput("services.ProjectService.updateQuestionTask.wrongTaskType")))
      }
      "return ServiceError.OfflineLockFail if version is wrong" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 12)
        ))
        val testTaskList = testPart.tasks
        val originTask = testTaskList(1).asInstanceOf[QuestionTask]
        val updatedTask = originTask.copy(
          position = testTaskList.map(_.position).min,
          settings = originTask.settings.copy(
            title = "New task name",
            help = "New task help",
            description = "New task description",
            notesAllowed = !testTaskList(1).settings.notesAllowed,
            notesTitle = None,
            responseTitle = None
          )
        )
        val commonTaskArgs = new projectService.CommonTaskArgs(
          taskId = updatedTask.id,
          version = updatedTask.version + 1,
          name = Some(updatedTask.settings.title),
          help = Some(updatedTask.settings.help),
          description = Some(updatedTask.settings.description),
          position = Some(updatedTask.position),
          maxGrade = Some(updatedTask.maxGrade),
          notesAllowed = Some(updatedTask.settings.notesAllowed),
          partId = Some(updatedTask.partId),
          notesTitle = Some(updatedTask.settings.notesTitle),
          responseTitle = Some(updatedTask.settings.responseTitle)
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(originTask)))

        val result = projectService.updateQuestionTask(commonTaskArgs, Some(updatedTask.questions))
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
    }
  }

  "ProjectService.updateQuestionTask (MultipleChoiceTask)" should {
    inSequence {
      "update QuestionTask and update existing tasks if new part position = min position within same part" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 12)
        ))
        val testTaskList = testPart.tasks
        val originTask = testTaskList(2).asInstanceOf[QuestionTask]
        val updatedTask = originTask.copy(
          position = testTaskList.map(_.position).min,
          settings = originTask.settings.copy(
            title = "New task name",
            help = "New task help",
            description = "New task description",
            notesAllowed = !originTask.settings.notesAllowed,
            notesTitle = Some("new notes title"),
            responseTitle = Some("new response title")
          ),
          questions = IndexedSeq(
            TestValues.testMultipleChoiceQuestionB,
            TestValues.testOrderingQuestionC
          )
        )
        val commonTaskArgs = new projectService.CommonTaskArgs(
          taskId = updatedTask.id,
          version = updatedTask.version,
          name = Some(updatedTask.settings.title),
          help = Some(updatedTask.settings.help),
          description = Some(updatedTask.settings.description),
          position = Some(updatedTask.position),
          maxGrade = Some(updatedTask.maxGrade),
          notesAllowed = Some(updatedTask.settings.notesAllowed),
          partId = Some(updatedTask.partId),
          notesTitle = Some(updatedTask.settings.notesTitle),
          responseTitle = Some(updatedTask.settings.responseTitle)
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(originTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        // Move
        // task A
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(0).asInstanceOf[DocumentTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(0).asInstanceOf[DocumentTask].copy(position = 2, version = testTaskList(0).version + 1))))
        // task B
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 3), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 3, version = testTaskList(1).version + 1))))
        // task C
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (originTask.copy(position = 1), *, *, *) returns (Future.successful(\/-(originTask.copy(position = 1, version = originTask.version + 1))))

        // update task C
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (updatedTask.copy(position = 1, version = updatedTask.version + 1), *, *, *) returns (Future.successful(\/-(updatedTask.copy(position = 1, version = updatedTask.version + 2))))

        val result = projectService.updateQuestionTask(commonTaskArgs, Some(updatedTask.questions))
        Await.result(result, Duration.Inf) should be(\/-(updatedTask.copy(position = 1, version = updatedTask.version + 2)))
      }
      "update QuestionTask and leave specific values unchanged if we don't pass them as parameters" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 12)
        ))
        val testTaskList = testPart.tasks
        val originTask = testTaskList(2).asInstanceOf[QuestionTask]
        val updatedTask = originTask.copy(
          position = testTaskList.map(_.position).min,
          settings = originTask.settings.copy(
            title = "New task name",
            help = "New task help",
            description = "New task description",
            notesAllowed = !originTask.settings.notesAllowed,
            notesTitle = Some("new notes title"),
            responseTitle = Some("new response title")
          )
        )
        val commonTaskArgs = new projectService.CommonTaskArgs(
          taskId = updatedTask.id,
          version = updatedTask.version,
          name = Some(updatedTask.settings.title),
          help = Some(updatedTask.settings.help),
          description = Some(updatedTask.settings.description),
          position = Some(updatedTask.position),
          maxGrade = Some(updatedTask.maxGrade),
          notesAllowed = Some(updatedTask.settings.notesAllowed),
          partId = Some(updatedTask.partId),
          notesTitle = Some(updatedTask.settings.notesTitle),
          responseTitle = Some(updatedTask.settings.responseTitle)
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(originTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        // Move
        // task A
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(0).asInstanceOf[DocumentTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(0).asInstanceOf[DocumentTask].copy(position = 2, version = testTaskList(0).version + 1))))
        // task B
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 3), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 3, version = testTaskList(1).version + 1))))
        // task C
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (originTask.copy(position = 1), *, *, *) returns (Future.successful(\/-(originTask.copy(position = 1, version = originTask.version + 1))))

        // update task C
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (updatedTask.copy(position = 1, version = updatedTask.version + 1), *, *, *) returns (Future.successful(\/-(updatedTask.copy(position = 1, version = updatedTask.version + 2))))

        val result = projectService.updateQuestionTask(commonTaskArgs)
        Await.result(result, Duration.Inf) should be(\/-(updatedTask.copy(position = 1, version = updatedTask.version + 2)))
      }
      "update QuestionTask and update notesTitle, responseTitle to None (aka erase)" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskF.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 12)
        ))
        val testTaskList = testPart.tasks
        val originTask = testTaskList(2).asInstanceOf[QuestionTask]
        val updatedTask = originTask.copy(
          position = testTaskList.map(_.position).min,
          settings = originTask.settings.copy(
            title = "New task name",
            help = "New task help",
            description = "New task description",
            notesAllowed = !originTask.settings.notesAllowed,
            notesTitle = None,
            responseTitle = None
          )
        )
        val commonTaskArgs = new projectService.CommonTaskArgs(
          taskId = updatedTask.id,
          version = updatedTask.version,
          name = Some(updatedTask.settings.title),
          help = Some(updatedTask.settings.help),
          description = Some(updatedTask.settings.description),
          maxGrade = Some(updatedTask.maxGrade),
          position = Some(updatedTask.position),
          notesAllowed = Some(updatedTask.settings.notesAllowed),
          partId = Some(updatedTask.partId),
          notesTitle = Some(updatedTask.settings.notesTitle),
          responseTitle = Some(updatedTask.settings.responseTitle)
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(originTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        // Move
        // task A
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(0).asInstanceOf[DocumentTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(0).asInstanceOf[DocumentTask].copy(position = 2, version = testTaskList(0).version + 1))))
        // task B
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 3), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 3, version = testTaskList(1).version + 1))))
        // task C
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (originTask.copy(position = 1), *, *, *) returns (Future.successful(\/-(originTask.copy(position = 1, version = originTask.version + 1))))

        // update task C
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (updatedTask.copy(position = 1, version = updatedTask.version + 1), *, *, *) returns (Future.successful(\/-(updatedTask.copy(position = 1, version = updatedTask.version + 2))))

        val result = projectService.updateQuestionTask(commonTaskArgs, Some(updatedTask.questions))
        Await.result(result, Duration.Inf) should be(\/-(updatedTask.copy(position = 1, version = updatedTask.version + 2)))
      }
      "return ServiceError.BadInput if task has wrong type" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskF.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 12)
        ))
        val testTaskList = testPart.tasks
        val originTask = testTaskList(2).asInstanceOf[QuestionTask]
        val updatedTask = originTask.copy(
          position = testTaskList.map(_.position).min,
          settings = originTask.settings.copy(
            title = "New task name",
            help = "New task help",
            description = "New task description",
            notesAllowed = !originTask.settings.notesAllowed,
            notesTitle = None,
            responseTitle = None
          )
        )
        val commonTaskArgs = new projectService.CommonTaskArgs(
          taskId = updatedTask.id,
          version = updatedTask.version,
          name = Some(updatedTask.settings.title),
          help = Some(updatedTask.settings.help),
          description = Some(updatedTask.settings.description),
          position = Some(updatedTask.position),
          maxGrade = Some(updatedTask.maxGrade),
          notesAllowed = Some(updatedTask.settings.notesAllowed),
          partId = Some(updatedTask.partId),
          notesTitle = Some(updatedTask.settings.notesTitle),
          responseTitle = Some(updatedTask.settings.responseTitle)
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(testTaskList(0))))

        val result = projectService.updateQuestionTask(commonTaskArgs, Some(updatedTask.questions))
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadInput("services.ProjectService.updateQuestionTask.wrongTaskType")))
      }
      "return ServiceError.OfflineLockFail if version is wrong" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskF.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 12)
        ))
        val testTaskList = testPart.tasks
        val originTask = testTaskList(2).asInstanceOf[QuestionTask]
        val updatedTask = originTask.copy(
          position = testTaskList.map(_.position).min,
          settings = originTask.settings.copy(
            title = "New task name",
            help = "New task help",
            description = "New task description",
            notesAllowed = !originTask.settings.notesAllowed,
            notesTitle = None,
            responseTitle = None
          )
        )
        val commonTaskArgs = new projectService.CommonTaskArgs(
          taskId = updatedTask.id,
          version = updatedTask.version + 1,
          name = Some(updatedTask.settings.title),
          help = Some(updatedTask.settings.help),
          maxGrade = Some(updatedTask.maxGrade),
          description = Some(updatedTask.settings.description),
          position = Some(updatedTask.position),
          notesAllowed = Some(updatedTask.settings.notesAllowed),
          partId = Some(updatedTask.partId),
          notesTitle = Some(updatedTask.settings.notesTitle),
          responseTitle = Some(updatedTask.settings.responseTitle)
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(originTask)))

        val result = projectService.updateQuestionTask(commonTaskArgs, Some(updatedTask.questions))
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
    }
  }

  "ProjectService.updateQuestionTask (OrderingTask)" should {
    inSequence {
      "update OrderingTask and update existing tasks if new part position = min position within same part" in {
        val testPart = TestValues.testPartB.copy(tasks = Vector(
          TestValues.testOrderingTaskD.copy(position = 10),
          TestValues.testMatchingTaskK.copy(position = 11),
          TestValues.testOrderingTaskL.copy(position = 12)
        ))
        val testTaskList = testPart.tasks
        val originTask = testTaskList(2).asInstanceOf[QuestionTask]
        val updatedTask = originTask.copy(
          position = testTaskList.map(_.position).min,
          settings = originTask.settings.copy(
            title = "New task name",
            help = "New task help",
            description = "New task description",
            notesAllowed = !originTask.settings.notesAllowed,
            notesTitle = Some("new notes title"),
            responseTitle = Some("new response title")
          ),
          questions = IndexedSeq(
            TestValues.testMultipleChoiceQuestionB,
            TestValues.testOrderingQuestionC
          )
        )
        val commonTaskArgs = new projectService.CommonTaskArgs(
          taskId = updatedTask.id,
          version = updatedTask.version,
          name = Some(updatedTask.settings.title),
          help = Some(updatedTask.settings.help),
          description = Some(updatedTask.settings.description),
          maxGrade = Some(updatedTask.maxGrade),
          position = Some(updatedTask.position),
          notesAllowed = Some(updatedTask.settings.notesAllowed),
          partId = Some(updatedTask.partId),
          notesTitle = Some(updatedTask.settings.notesTitle),
          responseTitle = Some(updatedTask.settings.responseTitle)
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(originTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        // Move
        // task D
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(0).asInstanceOf[QuestionTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(0).asInstanceOf[QuestionTask].copy(position = 2, version = testTaskList(0).version + 1))))
        // task K
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 3), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 3, version = testTaskList(1).version + 1))))
        // task L
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (originTask.copy(position = 1), *, *, *) returns (Future.successful(\/-(originTask.copy(position = 1, version = originTask.version + 1))))

        // update task L
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (updatedTask.copy(position = 1, version = updatedTask.version + 1), *, *, *) returns (Future.successful(\/-(updatedTask.copy(position = 1, version = updatedTask.version + 2))))

        val result = projectService.updateQuestionTask(commonTaskArgs, Some(updatedTask.questions))
        Await.result(result, Duration.Inf) should be(\/-(updatedTask.copy(position = 1, version = updatedTask.version + 2)))
      }
      "update OrderingTask and leave specific values unchanged if we don't pass them as parameters" in {
        val testPart = TestValues.testPartB.copy(tasks = Vector(
          TestValues.testOrderingTaskD.copy(position = 10),
          TestValues.testMatchingTaskK.copy(position = 11),
          TestValues.testOrderingTaskL.copy(position = 12)
        ))
        val testTaskList = testPart.tasks
        val originTask = testTaskList(2).asInstanceOf[QuestionTask]
        val updatedTask = originTask.copy(
          position = testTaskList.map(_.position).min,
          settings = originTask.settings.copy(
            title = "New task name",
            help = "New task help",
            description = "New task description",
            notesAllowed = !originTask.settings.notesAllowed,
            notesTitle = Some("new notes title"),
            responseTitle = Some("new response title")
          )
        )
        val commonTaskArgs = new projectService.CommonTaskArgs(
          taskId = updatedTask.id,
          version = updatedTask.version,
          name = Some(updatedTask.settings.title),
          help = Some(updatedTask.settings.help),
          description = Some(updatedTask.settings.description),
          maxGrade = Some(updatedTask.maxGrade),
          position = Some(updatedTask.position),
          notesAllowed = Some(updatedTask.settings.notesAllowed),
          partId = Some(updatedTask.partId),
          notesTitle = Some(updatedTask.settings.notesTitle),
          responseTitle = Some(updatedTask.settings.responseTitle)
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(originTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        /// Move
        // task D
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(0).asInstanceOf[QuestionTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(0).asInstanceOf[QuestionTask].copy(position = 2, version = testTaskList(0).version + 1))))
        // task K
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 3), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 3, version = testTaskList(1).version + 1))))
        // task L
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (originTask.copy(position = 1), *, *, *) returns (Future.successful(\/-(originTask.copy(position = 1, version = originTask.version + 1))))

        // update task L
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (updatedTask.copy(position = 1, version = updatedTask.version + 1), *, *, *) returns (Future.successful(\/-(updatedTask.copy(position = 1, version = updatedTask.version + 2))))

        val result = projectService.updateQuestionTask(commonTaskArgs)
        Await.result(result, Duration.Inf) should be(\/-(updatedTask.copy(position = 1, version = updatedTask.version + 2)))
      }
      "update OrderingTask and update notesTitle, responseTitle to None (aka erase)" in {
        val testPart = TestValues.testPartB.copy(tasks = Vector(
          TestValues.testOrderingTaskD.copy(position = 10),
          TestValues.testMatchingTaskK.copy(position = 11),
          TestValues.testOrderingTaskL.copy(position = 12)
        ))
        val testTaskList = testPart.tasks
        val originTask = testTaskList(2).asInstanceOf[QuestionTask]
        val updatedTask = originTask.copy(
          position = testTaskList.map(_.position).min,
          settings = originTask.settings.copy(
            title = "New task name",
            help = "New task help",
            description = "New task description",
            notesAllowed = !originTask.settings.notesAllowed,
            notesTitle = None,
            responseTitle = None
          )
        )
        val commonTaskArgs = new projectService.CommonTaskArgs(
          taskId = updatedTask.id,
          version = updatedTask.version,
          name = Some(updatedTask.settings.title),
          help = Some(updatedTask.settings.help),
          description = Some(updatedTask.settings.description),
          position = Some(updatedTask.position),
          maxGrade = Some(updatedTask.maxGrade),
          notesAllowed = Some(updatedTask.settings.notesAllowed),
          partId = Some(updatedTask.partId),
          notesTitle = Some(updatedTask.settings.notesTitle),
          responseTitle = Some(updatedTask.settings.responseTitle)
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(originTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        // Move
        // task D
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(0).asInstanceOf[QuestionTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(0).asInstanceOf[QuestionTask].copy(position = 2, version = testTaskList(0).version + 1))))
        // task K
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 3), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 3, version = testTaskList(1).version + 1))))
        // task L
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (originTask.copy(position = 1), *, *, *) returns (Future.successful(\/-(originTask.copy(position = 1, version = originTask.version + 1))))

        // update task L
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (updatedTask.copy(position = 1, version = updatedTask.version + 1), *, *, *) returns (Future.successful(\/-(updatedTask.copy(position = 1, version = updatedTask.version + 2))))

        val result = projectService.updateQuestionTask(commonTaskArgs, Some(updatedTask.questions))
        Await.result(result, Duration.Inf) should be(\/-(updatedTask.copy(position = 1, version = updatedTask.version + 2)))
      }
      "return ServiceError.BadInput if task has wrong type" in {
        val testPart = TestValues.testPartB.copy(tasks = Vector(
          TestValues.testOrderingTaskD.copy(position = 10),
          TestValues.testMatchingTaskK.copy(position = 11),
          TestValues.testOrderingTaskL.copy(position = 12)
        ))
        val wrongTypeTask = TestValues.testLongAnswerTaskF
        val testTaskList = testPart.tasks
        val originTask = testTaskList(2).asInstanceOf[QuestionTask]
        val updatedTask = originTask.copy(
          position = testTaskList.map(_.position).min,
          settings = originTask.settings.copy(
            title = "New task name",
            help = "New task help",
            description = "New task description",
            notesAllowed = !originTask.settings.notesAllowed,
            notesTitle = None,
            responseTitle = None
          )
        )
        val commonTaskArgs = new projectService.CommonTaskArgs(
          taskId = updatedTask.id,
          version = updatedTask.version,
          name = Some(updatedTask.settings.title),
          help = Some(updatedTask.settings.help),
          maxGrade = Some(updatedTask.maxGrade),
          description = Some(updatedTask.settings.description),
          position = Some(updatedTask.position),
          notesAllowed = Some(updatedTask.settings.notesAllowed),
          partId = Some(updatedTask.partId),
          notesTitle = Some(updatedTask.settings.notesTitle),
          responseTitle = Some(updatedTask.settings.responseTitle)
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(wrongTypeTask)))

        val result = projectService.updateQuestionTask(commonTaskArgs, Some(updatedTask.questions))
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadInput("services.ProjectService.updateQuestionTask.wrongTaskType")))
      }
      "return ServiceError.OfflineLockFail if version is wrong" in {
        val testPart = TestValues.testPartB.copy(tasks = Vector(
          TestValues.testOrderingTaskD.copy(position = 10),
          TestValues.testMatchingTaskK.copy(position = 11),
          TestValues.testOrderingTaskL.copy(position = 12)
        ))
        val testTaskList = testPart.tasks
        val originTask = testTaskList(2).asInstanceOf[QuestionTask]
        val updatedTask = originTask.copy(
          position = testTaskList.map(_.position).min,
          settings = originTask.settings.copy(
            title = "New task name",
            help = "New task help",
            description = "New task description",
            notesAllowed = !originTask.settings.notesAllowed,
            notesTitle = None,
            responseTitle = None
          )
        )
        val commonTaskArgs = new projectService.CommonTaskArgs(
          taskId = updatedTask.id,
          version = updatedTask.version + 1,
          name = Some(updatedTask.settings.title),
          help = Some(updatedTask.settings.help),
          maxGrade = Some(updatedTask.maxGrade),
          description = Some(updatedTask.settings.description),
          position = Some(updatedTask.position),
          notesAllowed = Some(updatedTask.settings.notesAllowed),
          partId = Some(updatedTask.partId),
          notesTitle = Some(updatedTask.settings.notesTitle),
          responseTitle = Some(updatedTask.settings.responseTitle)
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(originTask)))

        val result = projectService.updateQuestionTask(commonTaskArgs, Some(updatedTask.questions))
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
    }
  }

  // TODO - update Matching Tasks and update Blanks Tasks are included in UpdateQuestionTask

  "ProjectService.moveTask" should {
    inSequence {
      "move QuestionTask and update existing tasks positions if task new position = min position within same part" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 12)
        ))
        val testTaskList = testPart.tasks
        val originTask = testTaskList(2).asInstanceOf[QuestionTask]
        val updatedTask = originTask.copy(
          position = testTaskList.map(_.position).min
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(originTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        // Move
        // task A
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(0).asInstanceOf[DocumentTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(0).asInstanceOf[DocumentTask].copy(position = 2, version = testTaskList(0).version + 1))))
        // task B
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 3), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 3, version = testTaskList(1).version + 1))))
        // task C
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (originTask.copy(position = 1), *, *, *) returns (Future.successful(\/-(originTask.copy(position = 1, version = originTask.version + 1))))

        val result = projectService.moveTask(updatedTask.id, updatedTask.version, updatedTask.position)
        Await.result(result, Duration.Inf) should be(\/-(originTask.copy(position = 1, version = originTask.version + 1)))
      }
      "move QuestionTask and update old part tasks positions and new part tasks positions if task new position = min position within another part" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 12)
        ))
        val testAnotherPart = TestValues.testPartB.copy(tasks = Vector(
          TestValues.testOrderingTaskD.copy(position = 10),
          TestValues.testMatchingTaskK.copy(position = 11)
        ))
        val testTaskList = testPart.tasks
        val testAnotherTaskList = testAnotherPart.tasks
        val originTask = testTaskList(2).asInstanceOf[QuestionTask]
        val updatedTask = originTask.copy(
          partId = testAnotherPart.id,
          position = testAnotherTaskList.map(_.position).min
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(originTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testAnotherPart.id, *, *) returns (Future.successful(\/-(testAnotherPart)))

        // Move part A
        // task A
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(0).asInstanceOf[DocumentTask].copy(position = 1), *, *, *) returns (Future.successful(\/-(testTaskList(0).asInstanceOf[DocumentTask].copy(position = 1, version = testTaskList(0).version + 1))))
        // task B
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 2, version = testTaskList(1).version + 1))))

        // Move part B
        // task C
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (updatedTask.copy(position = 1), *, *, *) returns (Future.successful(\/-(updatedTask.copy(position = 1, version = updatedTask.version + 1))))
        // task D
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testAnotherTaskList(0).asInstanceOf[QuestionTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testAnotherTaskList(0).asInstanceOf[QuestionTask].copy(position = 2, version = testAnotherTaskList(0).version + 1))))
        // task K
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testAnotherTaskList(1).asInstanceOf[QuestionTask].copy(position = 3), *, *, *) returns (Future.successful(\/-(testAnotherTaskList(1).asInstanceOf[QuestionTask].copy(position = 3, version = testAnotherTaskList(1).version + 1))))

        val result = projectService.moveTask(updatedTask.id, updatedTask.version, updatedTask.position, Some(updatedTask.partId))
        Await.result(result, Duration.Inf) should be(\/-(updatedTask.copy(position = 1, version = updatedTask.version + 1)))
      }
      "move QuestionTask and update existing tasks if task new position < min position within same part" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 12)
        ))
        val testTaskList = testPart.tasks
        val originTask = testTaskList(2).asInstanceOf[QuestionTask]
        val updatedTask = originTask.copy(
          position = testTaskList.map(_.position).min - 99
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(originTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        // Move
        // task A
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(0).asInstanceOf[DocumentTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(0).asInstanceOf[DocumentTask].copy(position = 2, version = testTaskList(0).version + 1))))
        // task B
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 3), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 3, version = testTaskList(1).version + 1))))
        // task C
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (originTask.copy(position = 1), *, *, *) returns (Future.successful(\/-(originTask.copy(position = 1, version = originTask.version + 1))))

        val result = projectService.moveTask(updatedTask.id, updatedTask.version, updatedTask.position)
        Await.result(result, Duration.Inf) should be(\/-(updatedTask.copy(position = 1, version = updatedTask.version + 1)))
      }
      "move QuestionTask and update old part tasks positions and new part tasks positions if task new position < min position within another part" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 12)
        ))
        val testAnotherPart = TestValues.testPartB.copy(tasks = Vector(
          TestValues.testOrderingTaskD.copy(position = 10),
          TestValues.testMatchingTaskK.copy(position = 11)
        ))
        val testTaskList = testPart.tasks
        val testAnotherTaskList = testAnotherPart.tasks
        val originTask = testTaskList(2).asInstanceOf[QuestionTask]
        val updatedTask = originTask.copy(
          partId = testAnotherPart.id,
          position = testAnotherTaskList.map(_.position).min - 99
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(originTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testAnotherPart.id, *, *) returns (Future.successful(\/-(testAnotherPart)))

        // Move part A
        // task A
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(0).asInstanceOf[DocumentTask].copy(position = 1), *, *, *) returns (Future.successful(\/-(testTaskList(0).asInstanceOf[DocumentTask].copy(position = 1, version = testTaskList(0).version + 1))))
        // task B
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 2, version = testTaskList(1).version + 1))))

        // Move part B
        // task C
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (updatedTask.copy(position = 1), *, *, *) returns (Future.successful(\/-(updatedTask.copy(position = 1, version = updatedTask.version + 1))))
        // task D
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testAnotherTaskList(0).asInstanceOf[QuestionTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testAnotherTaskList(0).asInstanceOf[QuestionTask].copy(position = 2, version = testAnotherTaskList(0).version + 1))))
        // task K
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testAnotherTaskList(1).asInstanceOf[QuestionTask].copy(position = 3), *, *, *) returns (Future.successful(\/-(testAnotherTaskList(1).asInstanceOf[QuestionTask].copy(position = 3, version = testAnotherTaskList(1).version + 1))))

        val result = projectService.moveTask(updatedTask.id, updatedTask.version, updatedTask.position, Some(updatedTask.partId))
        Await.result(result, Duration.Inf) should be(\/-(updatedTask.copy(position = 1, version = updatedTask.version + 1)))
      }
      "NOT move QuestionTask if newPosition != oldPosition, but position is next task position (taskList positions are unsorted) within same part" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 9),
          TestValues.testShortAnswerTaskB.copy(position = 19),
          TestValues.testMultipleChoiceTaskC.copy(position = 12)
        ))
        val testTaskList = testPart.tasks
        val originTask = testTaskList(0).asInstanceOf[DocumentTask]
        val updatedTask = originTask.copy(
          position = testTaskList(2).position
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(originTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        // Move
        // task A
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(0).asInstanceOf[DocumentTask].copy(position = 1), *, *, *) returns (Future.successful(\/-(testTaskList(0).asInstanceOf[DocumentTask].copy(position = 1))))
        // task B
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 3), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 3))))
        // task C
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(2).asInstanceOf[QuestionTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(2).asInstanceOf[QuestionTask].copy(position = 3))))

        val result = projectService.moveTask(updatedTask.id, updatedTask.version, updatedTask.position)
        Await.result(result, Duration.Inf) should be(\/-(originTask.copy(position = 1)))
      }
      "move QuestionTask one step forward if newPosition != oldPosition and new position is next task position + 1  within same part" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 14)
        ))
        val testTaskList = testPart.tasks
        val originTask = testTaskList(0).asInstanceOf[DocumentTask]
        val updatedTask = originTask.copy(
          position = testTaskList(1).position + 1
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(originTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        // Move
        // task A
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(0).asInstanceOf[DocumentTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(0).asInstanceOf[DocumentTask].copy(position = 2))))
        // task B
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 1), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 1))))
        // task C
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(2).asInstanceOf[QuestionTask].copy(position = 3), *, *, *) returns (Future.successful(\/-(testTaskList(2).asInstanceOf[QuestionTask].copy(position = 3))))

        val result = projectService.moveTask(updatedTask.id, updatedTask.version, updatedTask.position)
        Await.result(result, Duration.Inf) should be(\/-(originTask.copy(position = 2)))
      }
      "move task and give it max position - 1, if newPosition != oldPosition and position = max position within same part" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 14)
        ))
        val testTaskList = testPart.tasks
        val originTask = testTaskList(0).asInstanceOf[DocumentTask]
        val updatedTask = originTask.copy(
          position = testTaskList.last.position
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(originTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        // Move
        // task A
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(0).asInstanceOf[DocumentTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(0).asInstanceOf[DocumentTask].copy(position = 2))))
        // task B
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 1), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 1))))
        // task C
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(2).asInstanceOf[QuestionTask].copy(position = 3), *, *, *) returns (Future.successful(\/-(testTaskList(2).asInstanceOf[QuestionTask].copy(position = 3))))

        val result = projectService.moveTask(updatedTask.id, updatedTask.version, updatedTask.position)
        Await.result(result, Duration.Inf) should be(\/-(originTask.copy(position = 2)))
      }
      "move task to another part and give it max position - 1, if newPosition != oldPosition and position = max position within another part" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 12)
        ))
        val testAnotherPart = TestValues.testPartB.copy(tasks = Vector(
          TestValues.testOrderingTaskD.copy(position = 10),
          TestValues.testMatchingTaskK.copy(position = 11)
        ))
        val testTaskList = testPart.tasks
        val testAnotherTaskList = testAnotherPart.tasks
        val originTask = testTaskList(2).asInstanceOf[QuestionTask]
        val updatedTask = originTask.copy(
          partId = testAnotherPart.id,
          position = testAnotherTaskList.last.position
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(originTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testAnotherPart.id, *, *) returns (Future.successful(\/-(testAnotherPart)))

        // Move part A
        // task A
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(0).asInstanceOf[DocumentTask].copy(position = 1), *, *, *) returns (Future.successful(\/-(testTaskList(0).asInstanceOf[DocumentTask].copy(position = 1))))
        // task B
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 2))))

        // Move part B
        // task C
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (updatedTask.copy(position = 2), *, *, *) returns (Future.successful(\/-(updatedTask.copy(position = 2))))
        // task D
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testAnotherTaskList(0).asInstanceOf[QuestionTask].copy(position = 1), *, *, *) returns (Future.successful(\/-(testAnotherTaskList(0).asInstanceOf[QuestionTask].copy(position = 1))))
        // task K
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testAnotherTaskList(1).asInstanceOf[QuestionTask].copy(position = 3), *, *, *) returns (Future.successful(\/-(testAnotherTaskList(1).asInstanceOf[QuestionTask].copy(position = 3))))

        val result = projectService.moveTask(updatedTask.id, updatedTask.version, updatedTask.position, Some(updatedTask.partId))
        Await.result(result, Duration.Inf) should be(\/-(updatedTask.copy(position = 2)))
      }
      "move task to the last position if newPosition != oldPosition and position > max position within same part" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 14)
        ))
        val testTaskList = testPart.tasks
        val originTask = testTaskList(0).asInstanceOf[DocumentTask]
        val updatedTask = originTask.copy(
          position = testTaskList.last.position + 99
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(originTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        // Move
        // task A
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(0).asInstanceOf[DocumentTask].copy(position = 3), *, *, *) returns (Future.successful(\/-(testTaskList(0).asInstanceOf[DocumentTask].copy(position = 3))))
        // task B
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 1), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 1))))
        // task C
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(2).asInstanceOf[QuestionTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(2).asInstanceOf[QuestionTask].copy(position = 2))))

        val result = projectService.moveTask(updatedTask.id, updatedTask.version, updatedTask.position)
        Await.result(result, Duration.Inf) should be(\/-(originTask.copy(position = 3)))
      }
      "move task to the last position in another part if newPosition != oldPosition and position > max position within another part" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 12)
        ))
        val testAnotherPart = TestValues.testPartB.copy(tasks = Vector(
          TestValues.testOrderingTaskD.copy(position = 10),
          TestValues.testMatchingTaskK.copy(position = 11)
        ))
        val testTaskList = testPart.tasks
        val testAnotherTaskList = testAnotherPart.tasks
        val originTask = testTaskList(2).asInstanceOf[QuestionTask]
        val updatedTask = originTask.copy(
          partId = testAnotherPart.id,
          position = testAnotherTaskList.last.position + 99
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(originTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testAnotherPart.id, *, *) returns (Future.successful(\/-(testAnotherPart)))

        // Move part A
        // task A
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(0).asInstanceOf[DocumentTask].copy(position = 1), *, *, *) returns (Future.successful(\/-(testTaskList(0).asInstanceOf[DocumentTask].copy(position = 1))))
        // task B
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 2))))

        // Move part B
        // task C
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (updatedTask.copy(position = 3), *, *, *) returns (Future.successful(\/-(updatedTask.copy(position = 3))))
        // task D
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testAnotherTaskList(0).asInstanceOf[QuestionTask].copy(position = 1), *, *, *) returns (Future.successful(\/-(testAnotherTaskList(0).asInstanceOf[QuestionTask].copy(position = 1))))
        // task K
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testAnotherTaskList(1).asInstanceOf[QuestionTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testAnotherTaskList(1).asInstanceOf[QuestionTask].copy(position = 2))))

        val result = projectService.moveTask(updatedTask.id, updatedTask.version, updatedTask.position, Some(updatedTask.partId))
        Await.result(result, Duration.Inf) should be(\/-(updatedTask.copy(position = 3)))
      }
      "not move task, but call task.update method if they start not with 1 and newPosition == oldPosition within same part" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 14)
        ))
        val testTaskList = testPart.tasks
        val originTask = testTaskList(0).asInstanceOf[DocumentTask]
        val updatedTask = originTask

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(originTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        // Move
        // task A
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(0).asInstanceOf[DocumentTask].copy(position = 1), *, *, *) returns (Future.successful(\/-(testTaskList(0).asInstanceOf[DocumentTask].copy(position = 1))))
        // task B
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 2))))
        // task C
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(2).asInstanceOf[QuestionTask].copy(position = 3), *, *, *) returns (Future.successful(\/-(testTaskList(2).asInstanceOf[QuestionTask].copy(position = 3))))

        val result = projectService.moveTask(updatedTask.id, updatedTask.version, updatedTask.position)
        Await.result(result, Duration.Inf) should be(\/-(originTask.copy(position = 1)))
      }
      "not move task if newPosition == oldPosition and not call task.update method if positions start with 1 within same part" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 1),
          TestValues.testShortAnswerTaskB.copy(position = 2),
          TestValues.testMultipleChoiceTaskC.copy(position = 3)
        ))
        val testTaskList = testPart.tasks
        val originTask = testTaskList(0).asInstanceOf[DocumentTask]
        val updatedTask = originTask

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(originTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        val result = projectService.moveTask(updatedTask.id, updatedTask.version, updatedTask.position)
        Await.result(result, Duration.Inf) should be(\/-(originTask))
      }
      "move task and put it as first element if newPosition == 1 and task list has positions that start from 0 within same part" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 0),
          TestValues.testShortAnswerTaskB.copy(position = 1),
          TestValues.testMultipleChoiceTaskC.copy(position = 2)
        ))
        val testTaskList = testPart.tasks
        val originTask = testTaskList(2).asInstanceOf[QuestionTask]
        val updatedTask = originTask.copy(
          position = 1
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(originTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        // Move
        // task A
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(0).asInstanceOf[DocumentTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(0).asInstanceOf[DocumentTask].copy(position = 2))))
        // task B
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 3), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 3))))
        // task C
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(2).asInstanceOf[QuestionTask].copy(position = 1), *, *, *) returns (Future.successful(\/-(testTaskList(2).asInstanceOf[QuestionTask].copy(position = 1))))

        val result = projectService.moveTask(updatedTask.id, updatedTask.version, updatedTask.position)
        Await.result(result, Duration.Inf) should be(\/-(originTask.copy(position = 1)))
      }
      "return ServiceError.OfflineLockFail if versions don't match" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 14)
        ))
        val testTaskList = testPart.tasks
        val originTask = testTaskList(0).asInstanceOf[DocumentTask]
        val updatedTask = originTask.copy(
          position = testTaskList.last.position
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(originTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        // Move
        // task A
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(0).asInstanceOf[DocumentTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(0).asInstanceOf[DocumentTask].copy(position = 2))))
        // task B
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 1), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 1))))
        // task C
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(2).asInstanceOf[QuestionTask].copy(position = 3), *, *, *) returns (Future.successful(\/-(testTaskList(2).asInstanceOf[QuestionTask].copy(position = 3))))

        val result = projectService.moveTask(updatedTask.id, 99L, updatedTask.position)
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
      "throw an Exception if current part doesn't have tasks" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector())
        val anotherTestPart = TestValues.testPartB.copy(tasks = Vector())
        val testTaskList = testPart.tasks
        val originTask = TestValues.testLongAnswerTaskA
        val updatedTask = originTask

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(originTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (anotherTestPart.id, *, *) returns (Future.successful(\/-(anotherTestPart)))

        val result = projectService.moveTask(updatedTask.id, updatedTask.version, updatedTask.position, Some(TestValues.testPartB.id))
        an[Exception] should be thrownBy (Await.result(result, Duration.Inf))
      }
      "move tasks in another part on first position if another part doesn't have tasks" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 12)
        ))
        val testAnotherPart = TestValues.testPartB.copy(tasks = Vector())
        val testTaskList = testPart.tasks
        val testAnotherTaskList = testAnotherPart.tasks
        val originTask = testTaskList(2).asInstanceOf[QuestionTask]
        val updatedTask = originTask.copy(
          partId = testAnotherPart.id,
          position = 99
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(originTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testAnotherPart.id, *, *) returns (Future.successful(\/-(testAnotherPart)))

        // Move part A
        // task A
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(0).asInstanceOf[DocumentTask].copy(position = 1), *, *, *) returns (Future.successful(\/-(testTaskList(0).asInstanceOf[DocumentTask].copy(position = 1))))
        // task B
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 2))))

        // Move part B
        // task C
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (updatedTask.copy(position = 1), *, *, *) returns (Future.successful(\/-(updatedTask.copy(position = 1))))

        val result = projectService.moveTask(updatedTask.id, updatedTask.version, updatedTask.position, Some(updatedTask.partId))
        Await.result(result, Duration.Inf) should be(\/-(updatedTask.copy(position = 1)))
      }
      "return SRepositoryError.NoResults if current part doesn't exist" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 14)
        ))
        val testTaskList = testPart.tasks
        val originTask = testTaskList(0).asInstanceOf[DocumentTask]
        val updatedTask = originTask.copy(
          position = testTaskList.last.position
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(originTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))

        // Move
        // task A
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(0).asInstanceOf[DocumentTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(0).asInstanceOf[DocumentTask].copy(position = 2))))
        // task B
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 1), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 1))))
        // task C
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(2).asInstanceOf[QuestionTask].copy(position = 3), *, *, *) returns (Future.successful(\/-(testTaskList(2).asInstanceOf[QuestionTask].copy(position = 3))))

        val result = projectService.moveTask(updatedTask.id, updatedTask.version, updatedTask.position)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("")))
      }
      "return SRepositoryError.NoResults if another part doesn't exist" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 12)
        ))
        val testAnotherPart = TestValues.testPartB.copy(tasks = Vector(
          TestValues.testOrderingTaskD.copy(position = 10),
          TestValues.testMatchingTaskK.copy(position = 11)
        ))
        val testTaskList = testPart.tasks
        val testAnotherTaskList = testAnotherPart.tasks
        val originTask = testTaskList(2).asInstanceOf[QuestionTask]
        val updatedTask = originTask.copy(
          partId = testAnotherPart.id,
          position = testAnotherTaskList.last.position
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(\/-(originTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testAnotherPart.id, *, *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))

        // Move part A
        // task A
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(0).asInstanceOf[DocumentTask].copy(position = 1), *, *, *) returns (Future.successful(\/-(testTaskList(0).asInstanceOf[DocumentTask].copy(position = 1))))
        // task B
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 2))))

        // Move part B
        // task C
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (updatedTask.copy(position = 2), *, *, *) returns (Future.successful(\/-(updatedTask.copy(position = 2))))
        // task D
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testAnotherTaskList(0).asInstanceOf[QuestionTask].copy(position = 1), *, *, *) returns (Future.successful(\/-(testAnotherTaskList(0).asInstanceOf[QuestionTask].copy(position = 1))))
        // task K
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testAnotherTaskList(1).asInstanceOf[QuestionTask].copy(position = 3), *, *, *) returns (Future.successful(\/-(testAnotherTaskList(1).asInstanceOf[QuestionTask].copy(position = 3))))

        val result = projectService.moveTask(updatedTask.id, updatedTask.version, updatedTask.position, Some(updatedTask.partId))
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("")))
      }
      "return RepositoryError.NoResults if task doesn't exist" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 14)
        ))
        val testTaskList = testPart.tasks
        val originTask = testTaskList(0).asInstanceOf[DocumentTask]
        val updatedTask = originTask.copy(
          position = testTaskList.last.position + 99
        )

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (originTask.id, *, *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        // Move
        // task A
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(0).asInstanceOf[DocumentTask].copy(position = 3), *, *, *) returns (Future.successful(\/-(testTaskList(0).asInstanceOf[DocumentTask].copy(position = 3))))
        // task B
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 1), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 1))))
        // task C
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(2).asInstanceOf[QuestionTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(2).asInstanceOf[QuestionTask].copy(position = 2))))

        val result = projectService.moveTask(updatedTask.id, updatedTask.version, updatedTask.position)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("")))
      }
    }
  }

  "ProjectService.deleteTask" should {
    inSequence {
      "delete task between two tasks" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 12)
        ))
        val testTaskList = testPart.tasks
        val deletedTask = testTaskList(1).asInstanceOf[QuestionTask]

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (deletedTask.id, *, *) returns (Future.successful(\/-(deletedTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        // Move
        // task A
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(0).asInstanceOf[DocumentTask].copy(position = 1), *, *, *) returns (Future.successful(\/-(testTaskList(0).asInstanceOf[DocumentTask].copy(position = 1))))
        // task B
        (taskRepository.delete(_: Task)(_: Connection, _: ScalaCachePool)) when (deletedTask, *, *) returns (Future.successful(\/-(deletedTask)))
        // task C
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(2).asInstanceOf[QuestionTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(2).asInstanceOf[QuestionTask].copy(position = 2))))

        val result = projectService.deleteTask(deletedTask.id, deletedTask.version)
        Await.result(result, Duration.Inf) should be(\/-(deletedTask))
      }
      "delete task in the beginning" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 12)
        ))
        val testTaskList = testPart.tasks
        val deletedTask = testTaskList(0).asInstanceOf[DocumentTask]

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (deletedTask.id, *, *) returns (Future.successful(\/-(deletedTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        // Move
        // task A
        (taskRepository.delete(_: Task)(_: Connection, _: ScalaCachePool)) when (deletedTask, *, *) returns (Future.successful(\/-(deletedTask)))
        // task B
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 1), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 1))))
        // task C
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(2).asInstanceOf[QuestionTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(2).asInstanceOf[QuestionTask].copy(position = 2))))

        val result = projectService.deleteTask(deletedTask.id, deletedTask.version)
        Await.result(result, Duration.Inf) should be(\/-(deletedTask))
      }
      "delete task between and change positions only of the tasks that go after, because positions start with 1" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 1),
          TestValues.testShortAnswerTaskB.copy(position = 2),
          TestValues.testMultipleChoiceTaskC.copy(position = 3)
        ))
        val testTaskList = testPart.tasks
        val deletedTask = testTaskList(1).asInstanceOf[QuestionTask]

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (deletedTask.id, *, *) returns (Future.successful(\/-(deletedTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        // task B
        (taskRepository.delete(_: Task)(_: Connection, _: ScalaCachePool)) when (deletedTask, *, *) returns (Future.successful(\/-(deletedTask)))
        // task C
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(2).asInstanceOf[QuestionTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(2).asInstanceOf[QuestionTask].copy(position = 2))))

        val result = projectService.deleteTask(deletedTask.id, deletedTask.version)
        Await.result(result, Duration.Inf) should be(\/-(deletedTask))
      }
      "delete task in the end" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 12)
        ))
        val testTaskList = testPart.tasks
        val deletedTask = testTaskList(2).asInstanceOf[QuestionTask]

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (deletedTask.id, *, *) returns (Future.successful(\/-(deletedTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        // Move
        // task A
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(0).asInstanceOf[DocumentTask].copy(position = 1), *, *, *) returns (Future.successful(\/-(testTaskList(0).asInstanceOf[DocumentTask].copy(position = 1))))
        // task B
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 2))))
        // task C
        (taskRepository.delete(_: Task)(_: Connection, _: ScalaCachePool)) when (deletedTask, *, *) returns (Future.successful(\/-(deletedTask)))

        val result = projectService.deleteTask(deletedTask.id, deletedTask.version)
        Await.result(result, Duration.Inf) should be(\/-(deletedTask))
      }
      "return ServiceError.OfflineLockFail if versions don't match" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 12)
        ))
        val testTaskList = testPart.tasks
        val deletedTask = testTaskList(2).asInstanceOf[QuestionTask]

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (deletedTask.id, *, *) returns (Future.successful(\/-(deletedTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        // Move
        // task A
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(0).asInstanceOf[DocumentTask].copy(position = 1), *, *, *) returns (Future.successful(\/-(testTaskList(0).asInstanceOf[DocumentTask].copy(position = 1))))
        // task B
        (taskRepository.update(_: Task, _: Option[UUID])(_: Connection, _: ScalaCachePool)) when (testTaskList(1).asInstanceOf[QuestionTask].copy(position = 2), *, *, *) returns (Future.successful(\/-(testTaskList(1).asInstanceOf[QuestionTask].copy(position = 2))))
        // task C
        (taskRepository.delete(_: Task)(_: Connection, _: ScalaCachePool)) when (deletedTask, *, *) returns (Future.successful(\/-(deletedTask)))

        val result = projectService.deleteTask(deletedTask.id, 99L)
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
      "return ServiceError.BusinessLogicFail if task list is empty" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector())
        val testTaskList = testPart.tasks
        val deletedTask = TestValues.testLongAnswerTaskA

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (deletedTask.id, *, *) returns (Future.successful(\/-(deletedTask)))
        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testPart.id, *, *) returns (Future.successful(\/-(testPart)))

        val result = projectService.deleteTask(deletedTask.id, deletedTask.version)
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BusinessLogicFail("Weird, task list shouldn't be empty!")))
      }
      "return RepositoryError.NoResults if task doesn't exist" in {
        val testPart = TestValues.testPartA.copy(tasks = Vector(
          TestValues.testLongAnswerTaskA.copy(position = 10),
          TestValues.testShortAnswerTaskB.copy(position = 11),
          TestValues.testMultipleChoiceTaskC.copy(position = 12)
        ))
        val testTaskList = testPart.tasks
        val deletedTask = testTaskList(2).asInstanceOf[QuestionTask]

        (taskRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (deletedTask.id, *, *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))

        val result = projectService.deleteTask(deletedTask.id, deletedTask.version)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("")))
      }
    }
  }

  "ProjectService.copyMasterProject" should {
    inSequence {
      "copy one master project into a course" in {
        val expectedProject = TestValues.testProjectH.copy(id = UUID.randomUUID)
        (projectRepository.cloneProject(_: UUID, _: UUID)(_: Connection, _: ScalaCachePool)) when (TestValues.testProjectH.id, TestValues.testCourseH.id, *, *) returns (Future.successful(\/-(expectedProject)))
        (projectRepository.insert(_: Project)(_: Connection, _: ScalaCachePool)) when (expectedProject, *, *) returns (Future.successful(\/-(expectedProject)))
        (projectRepository.cloneProjectParts(_: UUID, _: UUID, _: UUID)(_: Connection, _: ScalaCachePool)) when (TestValues.testProjectH.id, TestValues.testUserH.id, expectedProject.id, *, *) returns (Future.successful(\/-(IndexedSeq(TestValues.testPartI))))
        (partRepository.insert(_: Part)(_: Connection, _: ScalaCachePool)) when (*, *, *) returns (Future.successful(\/-(TestValues.testPartC)))
        (projectRepository.cloneProjectComponents(_: UUID, _: UUID)(_: Connection, _: ScalaCachePool)) when (TestValues.testProjectH.id, TestValues.testUserH.id, *, *) returns (Future.successful(\/-(IndexedSeq(TestValues.testRubricComponentK, TestValues.testGenericHTMLComponentM))))
        (componentRepository.insert(_: Component)(_: Connection)) when (*, *) returns (Future.successful(\/-(TestValues.testRubricComponentL)))
        (componentRepository.insert(_: Component)(_: Connection)) when (*, *) returns (Future.successful(\/-(TestValues.testGenericHTMLComponentM)))
        (componentRepository.addToPart(_: Component, _: Part)(_: Connection, _: ScalaCachePool)) when (*, *, *, *) returns (Future.successful(\/-(Unit)))
        (componentRepository.addToPart(_: Component, _: Part)(_: Connection, _: ScalaCachePool)) when (*, *, *, *) returns (Future.successful(\/-(Unit)))

        val result = projectService.copyMasterProject(TestValues.testProjectH.id, TestValues.testCourseH.id, TestValues.testUserH.id)
        val \/-(clonedProject) = Await.result(result, Duration.Inf)

        TestValues.testProjectH.id should not be (clonedProject.id)
        TestValues.testProjectH.courseId should be(clonedProject.courseId)
        TestValues.testProjectH.version should be(clonedProject.version)
        TestValues.testProjectH.name should be(clonedProject.name)
        TestValues.testProjectH.slug should be(clonedProject.slug)
        TestValues.testProjectH.description should be(clonedProject.description)
        TestValues.testProjectH.longDescription should be(clonedProject.longDescription)
        TestValues.testProjectH.availability should be(clonedProject.availability)
        TestValues.testProjectH.parts should be(clonedProject.parts)
        TestValues.testPartI.components.head should be(clonedProject.parts.head.components.head)
        TestValues.testProjectH.createdAt.toString should be(clonedProject.createdAt.toString)
        TestValues.testProjectH.updatedAt.toString should be(clonedProject.updatedAt.toString)
      }
    }
  }
}
