import ca.shiftfocus.krispii.core.error.{ServiceError, RepositoryError}
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks._
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services._
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.github.mauricio.async.db.Connection
import ca.shiftfocus.uuid.UUID
import org.scalatest._
import Matchers._ // Is used for "should be and etc."
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scalaz.{-\/, \/-}

class ProjectServiceSpec
  extends TestEnvironment(writeToDb = false)
{

  val db = stub[DB]
  val mockConnection    = stub[Connection]
  val authService = stub[AuthService]
  val schoolService = stub[SchoolService]
  val courseRepository = stub[CourseRepository]
  val projectRepository = stub[ProjectRepository]
  val partRepository = stub[PartRepository]
  val taskRepository = stub[TaskRepository]


  val projectService = new ProjectServiceDefault(db, cache, authService, schoolService, courseRepository, projectRepository, partRepository, taskRepository) {
    override implicit def conn: Connection = mockConnection

    override def transactional[A](f: Connection => Future[A]): Future[A] = {
      f(mockConnection)
    }
  }

  "ProjectService.create" should {
    inSequence {
      "return new project" in {
        val testCourse  = TestValues.testCourseA

        val testProject  = TestValues.testProjectA
        val emptyProject = Project(
          id = testProject.id,
          courseId = testCourse.id,
          name = testProject.name,
          slug = testProject.slug,
          description = testProject.description,
          availability = testProject.availability,
          parts = IndexedSeq.empty[Part]
        )

        val emptyPart = Part(
          projectId = testProject.id,
          name = ""
        )

        val emptyTask = LongAnswerTask(
          partId = emptyPart.id,
          position = 1
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

        (projectRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(emptyProject.slug, *, *) returns(Future.successful(-\/(RepositoryError.NoResults)))
        (projectRepository.insert(_: Project)(_: Connection, _: ScalaCachePool)) when(*, *, *) returns(Future.successful(\/-(emptyProject)))
        (partRepository.insert(_: Part)(_: Connection, _: ScalaCachePool)) when(*, *, *) returns(Future.successful(\/-(emptyPart)))
        (taskRepository.insert(_: Task)(_: Connection, _: ScalaCachePool)) when(*, *, *) returns(Future.successful(\/-(emptyTask)))

        val result = projectService.create(testCourse.id, emptyProject.name, emptyProject.slug, emptyProject.description, emptyProject.availability)
        val eitherProject = Await.result(result, Duration.Inf)
        val \/-(project) = eitherProject

        project.id should be(resultProject.id)
        project.courseId should be(resultProject.courseId)
        project.version should be(resultProject.version)
        project.name should be(resultProject.name)
        project.slug should be(resultProject.slug)
        project.description should be(resultProject.description)
        project.availability should be(resultProject.availability)
        project.parts should be(resultProject.parts)
        project.createdAt.toString should be(resultProject.createdAt.toString)
        project.updatedAt.toString should be(resultProject.updatedAt.toString)

      }
      "return ServiceError.BadInput if slug has bad format" in {
        val testCourse  = TestValues.testCourseA

        val testProject  = TestValues.testProjectA
        val emptyProject = Project(
          id = testProject.id,
          courseId = testCourse.id,
          name = testProject.name,
          slug = "bad slug format A",
          description = testProject.description,
          availability = testProject.availability,
          parts = IndexedSeq.empty[Part]
        )

        val emptyPart = Part(
          projectId = testProject.id,
          name = ""
        )

        val emptyTask = LongAnswerTask(
          partId = emptyPart.id,
          position = 1
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

        (projectRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(emptyProject.slug, *, *) returns(Future.successful(-\/(RepositoryError.NoResults)))
        (projectRepository.insert(_: Project)(_: Connection, _: ScalaCachePool)) when(*, *, *) returns(Future.successful(\/-(emptyProject)))
        (partRepository.insert(_: Part)(_: Connection, _: ScalaCachePool)) when(*, *, *) returns(Future.successful(\/-(emptyPart)))
        (taskRepository.insert(_: Task)(_: Connection, _: ScalaCachePool)) when(*, *, *) returns(Future.successful(\/-(emptyTask)))

        val result = projectService.create(testCourse.id, emptyProject.name, emptyProject.slug, emptyProject.description, emptyProject.availability)
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadInput(s"${emptyProject.slug} is not a valid slug format.")))
      }
      "return RepositoryError.UniqueKeyConflict if slug is already in use" in {
        val testCourse  = TestValues.testCourseA

        val testProject  = TestValues.testProjectA
        val emptyProject = Project(
          id = testProject.id,
          courseId = testCourse.id,
          name = testProject.name,
          slug = testProject.slug,
          description = testProject.description,
          availability = testProject.availability,
          parts = IndexedSeq.empty[Part]
        )

        val emptyPart = Part(
          projectId = testProject.id,
          name = ""
        )

        val emptyTask = LongAnswerTask(
          partId = emptyPart.id,
          position = 1
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

        (projectRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(emptyProject.slug, *, *) returns(Future.successful(\/-(testProject)))
        (projectRepository.insert(_: Project)(_: Connection, _: ScalaCachePool)) when(*, *, *) returns(Future.successful(\/-(emptyProject)))
        (partRepository.insert(_: Part)(_: Connection, _: ScalaCachePool)) when(*, *, *) returns(Future.successful(\/-(emptyPart)))
        (taskRepository.insert(_: Task)(_: Connection, _: ScalaCachePool)) when(*, *, *) returns(Future.successful(\/-(emptyTask)))

        val result = projectService.create(testCourse.id, emptyProject.name, emptyProject.slug, emptyProject.description, emptyProject.availability)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.UniqueKeyConflict("slug", s"The slug ${emptyProject.slug} is already in use.")))
      }
    }
  }

  "ProjectService.updateInfo" should {
    inSequence {
      "return ServiceError.OfflineLockFail if versions don't match" in {
        val testProject  = TestValues.testProjectA

        (projectRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testProject.id, *, *) returns(Future.successful(\/-(testProject)))

        val result = projectService.updateInfo(testProject.id, 99L, Some(testProject.courseId), Some(testProject.name), Some(testProject.description), Some(testProject.availability))
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
    }
  }

  "ProjectService.updateSlug" should {
    inSequence {
      "update slug" in {
        val testProject  = TestValues.testProjectA
        val updatedProject = testProject.copy(
         slug = "updated_" + testProject.slug
        )

        (projectRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testProject.id, *, *) returns(Future.successful(\/-(testProject)))
        (projectRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(updatedProject.slug, *, *) returns(Future.successful(-\/(RepositoryError.NoResults)))
        (projectRepository.update(_: Project)(_: Connection, _: ScalaCachePool)) when(updatedProject, *, *) returns(Future.successful(\/-(updatedProject)))

        val result = projectService.updateSlug(testProject.id, testProject.version, updatedProject.slug)
        Await.result(result, Duration.Inf) should be(\/-(updatedProject))
      }
      "return ServiceError.BadInput if slug has bad format" in {
        val testProject  = TestValues.testProjectA
        val updatedProject = testProject.copy(
         slug = "updated ;( " + testProject.slug
        )

        (projectRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testProject.id, *, *) returns(Future.successful(\/-(testProject)))
        (projectRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(updatedProject.slug, *, *) returns(Future.successful(-\/(RepositoryError.NoResults)))
        (projectRepository.update(_: Project)(_: Connection, _: ScalaCachePool)) when(updatedProject, *, *) returns(Future.successful(\/-(updatedProject)))

        val result = projectService.updateSlug(testProject.id, testProject.version, updatedProject.slug)
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadInput(s"${updatedProject.slug} is not a valid slug format.")))
      }
      "return RepositoryError.UniqueKeyConflict if slug is already in use" in {
        val testProject  = TestValues.testProjectA
        val updatedProject = testProject.copy(
         slug = "updated_" + testProject.slug
        )

        (projectRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testProject.id, *, *) returns(Future.successful(\/-(testProject)))
        (projectRepository.find(_: String)(_: Connection, _: ScalaCachePool)) when(updatedProject.slug, *, *) returns(Future.successful(\/-(testProject)))
        (projectRepository.update(_: Project)(_: Connection, _: ScalaCachePool)) when(updatedProject, *, *) returns(Future.successful(\/-(updatedProject)))

        val result = projectService.updateSlug(testProject.id, testProject.version, updatedProject.slug)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.UniqueKeyConflict("slug", s"The slug ${updatedProject.slug} is already in use.")))
      }
      "return ServiceError.OfflineLockFail if versions don't match" in {
        val testProject  = TestValues.testProjectA
        val updatedProject = testProject.copy(
         slug = "updated_" + testProject.slug
        )

        (projectRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testProject.id, *, *) returns(Future.successful(\/-(testProject)))

        val result = projectService.updateSlug(testProject.id, 99L, updatedProject.slug)
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
    }
  }

  "ProjectService.delete" should {
    inSequence {
      "return ServiceError.OfflineLockFail if versions don't match" in {
        val testProject  = TestValues.testProjectA

        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when(testProject.id, false, *, *) returns(Future.successful(\/-(testProject)))

        val result = projectService.delete(testProject.id, 99L)
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
    }
  }

  "ProjectService.createPart" should {
    inSequence {
      // Mocking of the partRepository.update method let us check if part positions were increased.
      // Also we should compare part.position in part.equals method.
      "create new part and update existing parts if new part position = min position" in {
        val testProject  = TestValues.testProjectA.copy(parts = Vector(
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

        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when(testProject.id, false, *, *) returns(Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean)(_: Connection, _: ScalaCachePool)) when(noPartsProject, false, *, *) returns(Future.successful(\/-(testPartList)))

        for (i <- testPartList.indices) {
          (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when(testPartList(i).copy(position = i + 2), *, *) returns(Future.successful(\/-(testPartList(i).copy(position = i + 2))))
        }

        (partRepository.insert(_: Part)(_: Connection, _: ScalaCachePool)) when(newPart.copy(position = 1), *, *) returns(Future.successful(\/-(newPart.copy(position = 1))))


        val result = projectService.createPart(testProject.id, newPart.name, newPart.position, newPart.id)
        val \/-(part) = Await.result(result, Duration.Inf)

        part.projectId should be(newPart.projectId)
        part.name should be(newPart.name)
        part.position should be(1)
      }
      "create new part and put it as first element and move all over parts position if new part position < min position" in {
        val testProject  = TestValues.testProjectA.copy(parts = Vector(
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

        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when(testProject.id, false, *, *) returns(Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean)(_: Connection, _: ScalaCachePool)) when(noPartsProject, false, *, *) returns(Future.successful(\/-(testPartList)))

        for (i <- testPartList.indices) {
          (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when(testPartList(i).copy(position = i + 2), *, *) returns(Future.successful(\/-(testPartList(i).copy(position = i + 2))))
        }

        (partRepository.insert(_: Part)(_: Connection, _: ScalaCachePool)) when(newPart.copy(position = 1), *, *) returns(Future.successful(\/-(newPart.copy(position = 1))))

        val result = projectService.createPart(testProject.id, newPart.name, newPart.position, newPart.id)
        val \/-(part) = Await.result(result, Duration.Inf)

        part.projectId should be(newPart.projectId)
        part.name should be(newPart.name)
        part.position should be(1)
      }
      "create new part and update position of last element if new part position = max position" in {
        val testProject  = TestValues.testProjectA.copy(parts = Vector(
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

        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when(testProject.id, false, *, *) returns(Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean)(_: Connection, _: ScalaCachePool)) when(noPartsProject, false, *, *) returns(Future.successful(\/-(testPartList)))

        for (i <- testPartList.indices) {
          if (i + 1 != testPartList.length)
          (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when(testPartList(i).copy(position = i + 1), *, *) returns(Future.successful(\/-(testPartList(i).copy(position = i + 1))))
        }

        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when(testPartList.last.copy(position = testPartList.length + 1), *, *) returns(Future.successful(\/-(testPartList.last.copy(position = testPartList.length + 1))))

        (partRepository.insert(_: Part)(_: Connection, _: ScalaCachePool)) when(newPart.copy(position = testPartList.length), *, *) returns(Future.successful(\/-(newPart.copy(position = testPartList.length))))

        val result = projectService.createPart(testProject.id, newPart.name, newPart.position, newPart.id)
        val \/-(part) = Await.result(result, Duration.Inf)

        part.projectId should be(newPart.projectId)
        part.name should be(newPart.name)
        part.position should be(testPartList.length)
      }
      "create new part and insert between elements and update positions of all elements even if they are unsorted " in {
        val testProject  = TestValues.testProjectA.copy(parts = Vector(
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

        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when(testProject.id, false, *, *) returns(Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean)(_: Connection, _: ScalaCachePool)) when(noPartsProject, false, *, *) returns(Future.successful(\/-(testPartList)))

        // mock partA
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when(testPartList(0).copy(position = 3), *, *) returns(Future.successful(\/-(testPartList(0).copy(position = 3))))
        // mock PartB
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when(testPartList(1).copy(position = 1), *, *) returns(Future.successful(\/-(testPartList(1).copy(position = 1))))
        // mock partC
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when(testPartList(2).copy(position = 4), *, *) returns(Future.successful(\/-(testPartList(2).copy(position = 4))))
        // mock newPart
        (partRepository.insert(_: Part)(_: Connection, _: ScalaCachePool)) when(newPart.copy(position = 2), *, *) returns(Future.successful(\/-(newPart.copy(position = 2))))

        val result = projectService.createPart(testProject.id, newPart.name, newPart.position, newPart.id)
        val \/-(part) = Await.result(result, Duration.Inf)

        part.projectId should be(newPart.projectId)
        part.name should be(newPart.name)
        part.position should be(2)
      }
      "create new part and give it position of last element + 1 if new part position > max position" in {
        val testProject  = TestValues.testProjectA.copy(parts = Vector(
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

        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when(testProject.id, false, *, *) returns(Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean)(_: Connection, _: ScalaCachePool)) when(noPartsProject, false, *, *) returns(Future.successful(\/-(testPartList)))

        for (i <- testPartList.indices) {
          (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when(testPartList(i).copy(position = i + 1), *, *) returns(Future.successful(\/-(testPartList(i).copy(position = i + 1))))
        }

        (partRepository.insert(_: Part)(_: Connection, _: ScalaCachePool)) when(newPart.copy(position = testPartList.length + 1), *, *) returns(Future.successful(\/-(newPart.copy(position = testPartList.length + 1))))

        val result = projectService.createPart(testProject.id, newPart.name, newPart.position, newPart.id)
        val \/-(part) = Await.result(result, Duration.Inf)

        part.projectId should be(newPart.projectId)
        part.name should be(newPart.name)
        part.position should be(testPartList.length + 1)
      }
      "create new part and give it position 1 if partList is empty" in {
        val testProject  = TestValues.testProjectA.copy(parts = Vector())
        val testPartList = testProject.parts
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val newPart = Part(
          projectId = testProject.id,
          name = "new part name",
          position = 99
        )

        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when(testProject.id, false, *, *) returns(Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean)(_: Connection, _: ScalaCachePool)) when(noPartsProject, false, *, *) returns(Future.successful(\/-(testPartList)))

        (partRepository.insert(_: Part)(_: Connection, _: ScalaCachePool)) when(newPart.copy(position = 1), *, *) returns(Future.successful(\/-(newPart.copy(position = 1))))

        val result = projectService.createPart(testProject.id, newPart.name, newPart.position, newPart.id)
        val \/-(part) = Await.result(result, Duration.Inf)

        part.projectId should be(newPart.projectId)
        part.name should be(newPart.name)
        part.position should be(1)
      }
      "create new part and give it position 1 if partList is not empty and partList positions start with 0 and we assign position 1 to newPart" in {
        val testProject  = TestValues.testProjectA.copy(parts = Vector(
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

        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when(testProject.id, false, *, *) returns(Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean)(_: Connection, _: ScalaCachePool)) when(noPartsProject, false, *, *) returns(Future.successful(\/-(testPartList)))

        for (i <- testPartList.indices) {
          (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when(testPartList(i).copy(position = i + 2), *, *) returns(Future.successful(\/-(testPartList(i).copy(position = i + 2))))
        }

        (partRepository.insert(_: Part)(_: Connection, _: ScalaCachePool)) when(newPart, *, *) returns(Future.successful(\/-(newPart)))

        val result = projectService.createPart(testProject.id, newPart.name, newPart.position, newPart.id)
        val \/-(part) = Await.result(result, Duration.Inf)

        part.projectId should be(newPart.projectId)
        part.name should be(newPart.name)
        part.position should be(newPart.position)
      }
      "create new part add it to the end of list and do not update partList positions if they are correct" in {
        val testProject  = TestValues.testProjectA.copy(parts = Vector(
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

        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when(testProject.id, false, *, *) returns(Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean)(_: Connection, _: ScalaCachePool)) when(noPartsProject, false, *, *) returns(Future.successful(\/-(testPartList)))

        (partRepository.insert(_: Part)(_: Connection, _: ScalaCachePool)) when(newPart, *, *) returns(Future.successful(\/-(newPart)))

        val result = projectService.createPart(testProject.id, newPart.name, newPart.position, newPart.id)
        val \/-(part) = Await.result(result, Duration.Inf)

        part.projectId should be(newPart.projectId)
        part.name should be(newPart.name)
        part.position should be(newPart.position)
      }
    }
  }

  "ProjectService.updatePart" should {
    inSequence {
      "update part and update existing parts if new part position = min position" in {
        val testProject  = TestValues.testProjectA.copy(parts = Vector(
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

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testPartList(1).id, *, *) returns(Future.successful(\/-(testPartList(1))))
        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when(testProject.id, false, *, *) returns(Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean)(_: Connection, _: ScalaCachePool)) when(noPartsProject, false, *, *) returns(Future.successful(\/-(testPartList)))

        for (i <- filteredPartList.indices) {
          (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when(filteredPartList(i).copy(position = i + 2), *, *) returns(Future.successful(\/-(filteredPartList(i).copy(position = i + 2))))
        }

        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when(updatedPart.copy(position = 1), *, *) returns(Future.successful(\/-(updatedPart.copy(position = 1))))

        val result = projectService.updatePart(updatedPart.id, updatedPart.version, Some(updatedPart.name), Some(updatedPart.position), Some(updatedPart.enabled))
        Await.result(result, Duration.Inf) should be(\/-(updatedPart.copy(position = 1)))
      }
      "update part and update existing parts if new part position < min position" in {
        val testProject  = TestValues.testProjectA.copy(parts = Vector(
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

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testPartList(1).id, *, *) returns(Future.successful(\/-(testPartList(1))))
        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when(testProject.id, false, *, *) returns(Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean)(_: Connection, _: ScalaCachePool)) when(noPartsProject, false, *, *) returns(Future.successful(\/-(testPartList)))

        for (i <- filteredPartList.indices) {
          (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when(filteredPartList(i).copy(position = i + 2), *, *) returns(Future.successful(\/-(filteredPartList(i).copy(position = i + 2))))
        }

        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when(updatedPart.copy(position = 1), *, *) returns(Future.successful(\/-(updatedPart.copy(position = 1))))

        val result = projectService.updatePart(updatedPart.id, updatedPart.version, Some(updatedPart.name), Some(updatedPart.position), Some(updatedPart.enabled))
        Await.result(result, Duration.Inf) should be(\/-(updatedPart.copy(position = 1)))
      }
      "update part if newPosition != oldPosition and position is occupied by another part" in {
        val testProject  = TestValues.testProjectA.copy(parts = Vector(
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

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testPartList(0).id, *, *) returns(Future.successful(\/-(testPartList(0))))
        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when(testProject.id, false, *, *) returns(Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean)(_: Connection, _: ScalaCachePool)) when(noPartsProject, false, *, *) returns(Future.successful(\/-(testPartList)))

        // partA
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when(updatedPart.copy(position = 3), *, *) returns(Future.successful(\/-(updatedPart.copy(position = 3))))
        // partB
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when(testPartList(1).copy(position = 1), *, *) returns(Future.successful(\/-(testPartList(1).copy(position = 1))))
        // partC
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when(testPartList(2).copy(position = 2), *, *) returns(Future.successful(\/-(testPartList(2).copy(position = 2))))
        // partE
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when(testPartList(3).copy(position = 4), *, *) returns(Future.successful(\/-(testPartList(3).copy(position = 4))))


        val result = projectService.updatePart(updatedPart.id, updatedPart.version, Some(updatedPart.name), Some(updatedPart.position), Some(updatedPart.enabled))
        Await.result(result, Duration.Inf) should be(\/-(updatedPart.copy(position = 3)))
      }
      "update part if newPosition != oldPosition and position = max position" in {
        val testProject  = TestValues.testProjectA.copy(parts = Vector(
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

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testPartList(0).id, *, *) returns(Future.successful(\/-(testPartList(0))))
        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when(testProject.id, false, *, *) returns(Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean)(_: Connection, _: ScalaCachePool)) when(noPartsProject, false, *, *) returns(Future.successful(\/-(testPartList)))

        for (i <- filteredPartList.indices) {
          (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when(filteredPartList(i).copy(position = i + 1), *, *) returns(Future.successful(\/-(filteredPartList(i).copy(position = i + 1))))
        }

        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when(updatedPart.copy(position = testPartList.length), *, *) returns(Future.successful(\/-(updatedPart.copy(position = testPartList.length))))

        val result = projectService.updatePart(updatedPart.id, updatedPart.version, Some(updatedPart.name), Some(updatedPart.position), Some(updatedPart.enabled))
        Await.result(result, Duration.Inf) should be(\/-(updatedPart.copy(position = testPartList.length)))
      }
      "update part if newPosition != oldPosition and position > max position" in {
        val testProject  = TestValues.testProjectA.copy(parts = Vector(
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

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testPartList(0).id, *, *) returns(Future.successful(\/-(testPartList(0))))
        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when(testProject.id, false, *, *) returns(Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean)(_: Connection, _: ScalaCachePool)) when(noPartsProject, false, *, *) returns(Future.successful(\/-(testPartList)))

        for (i <- filteredPartList.indices) {
          (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when(filteredPartList(i).copy(position = i + 1), *, *) returns(Future.successful(\/-(filteredPartList(i).copy(position = i + 1))))
        }

        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when(updatedPart.copy(position = testPartList.length), *, *) returns(Future.successful(\/-(updatedPart.copy(position = testPartList.length))))

        val result = projectService.updatePart(updatedPart.id, updatedPart.version, Some(updatedPart.name), Some(updatedPart.position), Some(updatedPart.enabled))
        Await.result(result, Duration.Inf) should be(\/-(updatedPart.copy(position = testPartList.length)))
      }
      "update part if newPosition == oldPosition" in {
        val testProject  = TestValues.testProjectA.copy(parts = Vector(
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

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testPartList(0).id, *, *) returns(Future.successful(\/-(testPartList(0))))
        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when(testProject.id, false, *, *) returns(Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean)(_: Connection, _: ScalaCachePool)) when(noPartsProject, false, *, *) returns(Future.successful(\/-(testPartList)))

        // partA
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when(updatedPart.copy(position = 1), *, *) returns(Future.successful(\/-(updatedPart.copy(position = 1))))
        // partB
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when(testPartList(1).copy(position = 2), *, *) returns(Future.successful(\/-(testPartList(1).copy(position = 2))))
        // partC
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when(testPartList(2).copy(position = 3), *, *) returns(Future.successful(\/-(testPartList(2).copy(position = 3))))

        val result = projectService.updatePart(updatedPart.id, updatedPart.version, Some(updatedPart.name), Some(updatedPart.position), Some(updatedPart.enabled))
        Await.result(result, Duration.Inf) should be(\/-(updatedPart.copy(position = 1)))
      }
      "update part if newPosition == 1 and part list has positions that start from 0" in {
        val testProject  = TestValues.testProjectA.copy(parts = Vector(
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

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testPartList(2).id, *, *) returns(Future.successful(\/-(testPartList(2))))
        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when(testProject.id, false, *, *) returns(Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean)(_: Connection, _: ScalaCachePool)) when(noPartsProject, false, *, *) returns(Future.successful(\/-(testPartList)))

        // partA
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when(testPartList(1).copy(position = 3), *, *) returns(Future.successful(\/-(testPartList(1).copy(position = 3))))
        // partB
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when(updatedPart, *, *) returns(Future.successful(\/-(updatedPart)))
        // partC
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when(testPartList(0).copy(position = 2), *, *) returns(Future.successful(\/-(testPartList(0).copy(position = 2))))

        val result = projectService.updatePart(updatedPart.id, updatedPart.version, Some(updatedPart.name), Some(updatedPart.position), Some(updatedPart.enabled))
        Await.result(result, Duration.Inf) should be(\/-(updatedPart))
      }
      "update only one indicated part if newPosition == oldPosition, and position of parts is correct and starts with 1" in {
        val testProject  = TestValues.testProjectA.copy(parts = Vector(
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

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(updatedPart.id, *, *) returns(Future.successful(\/-(testPartList(1))))
        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when(testProject.id, false, *, *) returns(Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean)(_: Connection, _: ScalaCachePool)) when(noPartsProject, false, *, *) returns(Future.successful(\/-(testPartList)))

        // partB
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when(updatedPart, *, *) returns(Future.successful(\/-(updatedPart)))

        val result = projectService.updatePart(updatedPart.id, updatedPart.version, Some(updatedPart.name), Some(updatedPart.position), Some(updatedPart.enabled))
        Await.result(result, Duration.Inf) should be(\/-(updatedPart))
      }
      "return ServiceError.OfflineLockFail if versions don't match" in {
        val testProject  = TestValues.testProjectA.copy(parts = Vector(
          TestValues.testPartA,
          TestValues.testPartB,
          TestValues.testPartC
        ))
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val updatedPart = testPartList(0).copy(
          name = "new " + testPartList(0).name,
          enabled = !testPartList(0).enabled
        )

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(updatedPart.id, *, *) returns(Future.successful(\/-(updatedPart)))

        val result = projectService.updatePart(updatedPart.id, 99L, Some(updatedPart.name), Some(updatedPart.position), Some(updatedPart.enabled))
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
    }
  }

  "ProjectService.deletePart" should {
    inSequence {
      // Mock appart all parts with changed positions, to test if positions were changed within methods
      "delete part between two parts" in {
        val testProject  = TestValues.testProjectA.copy(parts = Vector(
          TestValues.testPartA,
          TestValues.testPartB,
          TestValues.testPartC
        ))
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val deletedPart = testPartList(1)

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(deletedPart.id, *, *) returns(Future.successful(\/-(deletedPart)))
        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when(testProject.id, false, *, *) returns(Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean)(_: Connection, _: ScalaCachePool)) when(noPartsProject, false, *, *) returns(Future.successful(\/-(testPartList)))

        // mock partC with changed position
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when(testPartList(2).copy(position = testPartList(2).position - 1), *, *) returns(Future.successful(\/-(testPartList(2))))

        (taskRepository.delete(_: Part)(_: Connection, _: ScalaCachePool)) when(deletedPart, *, *) returns(Future.successful(\/-(IndexedSeq())))
        (partRepository.delete(_: Part)(_: Connection, _: ScalaCachePool)) when(deletedPart, *, *) returns(Future.successful(\/-(deletedPart)))

        val result = projectService.deletePart(deletedPart.id, deletedPart.version)
        Await.result(result, Duration.Inf) should be(\/-(deletedPart))
      }
      "delete part in the begining" in {
        val testProject  = TestValues.testProjectA.copy(parts = Vector(
          TestValues.testPartA,
          TestValues.testPartB,
          TestValues.testPartC
        ))
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val deletedPart = testPartList(0)

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(deletedPart.id, *, *) returns(Future.successful(\/-(deletedPart)))
        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when(testProject.id, false, *, *) returns(Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean)(_: Connection, _: ScalaCachePool)) when(noPartsProject, false, *, *) returns(Future.successful(\/-(testPartList)))

        // mock partB with changed position
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when(testPartList(1).copy(position = testPartList(1).position - 1), *, *) returns(Future.successful(\/-(testPartList(1))))

        // mock partC with changed position
        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when(testPartList(2).copy(position = testPartList(2).position - 1), *, *) returns(Future.successful(\/-(testPartList(2))))

        (taskRepository.delete(_: Part)(_: Connection, _: ScalaCachePool)) when(deletedPart, *, *) returns(Future.successful(\/-(IndexedSeq())))
        (partRepository.delete(_: Part)(_: Connection, _: ScalaCachePool)) when(deletedPart, *, *) returns(Future.successful(\/-(deletedPart)))

        val result = projectService.deletePart(deletedPart.id, deletedPart.version)
        Await.result(result, Duration.Inf) should be(\/-(deletedPart))
      }
      "delete part in the end" in {
        val testProject  = TestValues.testProjectA.copy(parts = Vector(
          TestValues.testPartA,
          TestValues.testPartB
        ))
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
        val noPartsProject = testProject.copy(parts = IndexedSeq())
        val deletedPart = testPartList(1)

        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(deletedPart.id, *, *) returns(Future.successful(\/-(deletedPart)))
        (projectRepository.find(_: UUID, _: Boolean)(_: Connection, _: ScalaCachePool)) when(testProject.id, false, *, *) returns(Future.successful(\/-(noPartsProject)))
        (partRepository.list(_: Project, _: Boolean)(_: Connection, _: ScalaCachePool)) when(noPartsProject, false, *, *) returns(Future.successful(\/-(testPartList)))
        (taskRepository.delete(_: Part)(_: Connection, _: ScalaCachePool)) when(deletedPart, *, *) returns(Future.successful(\/-(IndexedSeq())))
        (partRepository.delete(_: Part)(_: Connection, _: ScalaCachePool)) when(deletedPart, *, *) returns(Future.successful(\/-(deletedPart)))

        val result = projectService.deletePart(deletedPart.id, deletedPart.version)
        Await.result(result, Duration.Inf) should be(\/-(deletedPart))
      }
      "return ServiceError.OfflineLockFail if versions don't match" in {
        val testProject  = TestValues.testProjectA.copy(parts = Vector(
          TestValues.testPartA,
          TestValues.testPartB,
          TestValues.testPartC
        ))
        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))


        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testPartList(0).id, *, *) returns(Future.successful(\/-(testPartList(0))))

        val result = projectService.deletePart(testPartList(0).id, 99L)
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
    }
  }

//  "ProjectService.togglePart" should {
//    inSequence{
//      "enable a disabled part" in {
//        val testPart = TestValues.testPartA.copy(enabled = false)
//
//        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testPart.id, *, *) returns(Future.successful(\/-(testPart)))
//        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when(testPart.copy(enabled = !testPart.enabled), *, *) returns(Future.successful(\/-(testPart.copy(enabled = !testPart.enabled))))
//
//        val result = projectService.togglePart(testPart.id, testPart.version)
//        Await.result(result, Duration.Inf) should be(\/-(testPart.copy(enabled = !testPart.enabled)))
//      }
//      "disable an enabled part" in {
//        val testPart = TestValues.testPartA.copy(enabled = true)
//
//        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testPart.id, *, *) returns(Future.successful(\/-(testPart)))
//        (partRepository.update(_: Part)(_: Connection, _: ScalaCachePool)) when(testPart.copy(enabled = !testPart.enabled), *, *) returns(Future.successful(\/-(testPart.copy(enabled = !testPart.enabled))))
//
//        val result = projectService.togglePart(testPart.id, testPart.version)
//        Await.result(result, Duration.Inf) should be(\/-(testPart.copy(enabled = !testPart.enabled)))
//      }
//      "return ServiceError.OfflineLockFail if versions don't match" in {
//        val testPart = TestValues.testPartA
//
//        (partRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testPart.id, *, *) returns(Future.successful(\/-(testPart)))
//
//        val result = projectService.togglePart(testPart.id, 99L)
//        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
//      }
//    }
//  }
//
//  "ProjectService.reorderParts" should {
//    inSequence{
//      "reorder the parts in a project" in {
//        val testProject  = TestValues.testProjectA.copy(parts = Vector(
//          TestValues.testPartA,
//          TestValues.testPartB,
//          TestValues.testPartC
//        ))
//        val testPartList = testProject.parts.map(_.copy(tasks = IndexedSeq()))
//
//        (projectRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when(testProject.id, *, *) returns(Future.successful(\/-(testProject)))
//        (partRepository.list(_: Project, _: Boolean)(_: Connection, _: ScalaCachePool)) when(testProject, false, *, *) returns(Future.successful(\/-(testPartList)))
//
//        val result = projectService.reorderParts(testProject.id, testPartList)
//        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
//      }
//    }
//  }

//  "ProjectService.createTask" should {
//    inSequence{
//      "create task" in {
//      }
//    }
//  }
//
//  "ProjectService.updateLongAnswerTask" should {
//    inSequence{
//      "throw an exception if task is not instance of LongAnswerTask" in {
//      }
//      "update LongAnswerTask" in {
//      }
//    }
//  }
//
//  "ProjectService.updateShortAnswerTask" should {
//    inSequence{
//      "throw an exception if task is not instance of ShortAnswerTask" in {
//      }
//      "update ShortAnswerTask" in {
//      }
//    }
//  }
//
//  "ProjectService.updateMultipleChoiceTask" should {
//    inSequence{
//      "throw an exception if task is not instance of MultipleChoiceTask" in {
//      }
//      "update MultipleChoiceTask" in {
//      }
//    }
//  }
//
//  "ProjectService.updateOrderingTask" should {
//    inSequence{
//      "throw an exception if task is not instance of OrderingTask" in {
//      }
//      "update OrderingTask" in {
//      }
//    }
//  }
//
//  "ProjectService.updateMatchingTask" should {
//    inSequence{
//      "throw an exception if task is not instance of MatchingTask" in {
//      }
//      "update MatchingTask" in {
//      }
//    }
//  }
//
//  "ProjectService.deleteTask" should {
//    inSequence{
//      "throw an exception if task is not found" in {
//      }
//      "delete task" in {
//      }
//    }
//  }
//
//  "ProjectService.moveTask" should {
//    inSequence{
//      "throw an exception if task is not found" in {
//      }
//      "move task" in {
//      }
//    }
//  }
}
