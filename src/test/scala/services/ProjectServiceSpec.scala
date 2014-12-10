import ca.shiftfocus.uuid.UUID
import ca.shiftfocus.krispii.core.models.tasks._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services._
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.exceptions._
import com.redis.E
import grizzled.slf4j.Logger
import org.scalamock.scalatest.MockFactory
import org.scalatest.WordSpec
import webcrank.password.Passwords
import scala.concurrent.duration.Duration
import scala.concurrent.duration.DurationConversions.spanConvert._
import scala.concurrent.{Future, Await}
import org.scalatest.Matchers._
import ca.shiftfocus.krispii.core.services.datasource._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try


trait ProjectTestEnvironmentComponent
  extends ProjectServiceImplComponent
  with ProjectRepositoryComponent
  with PartRepositoryComponent
  with TaskRepositoryComponent
  with TaskResponseRepositoryComponent
  with TaskScratchpadRepositoryComponent
  with TaskFeedbackRepositoryComponent
  with UserRepositoryComponent
  with ComponentRepositoryComponent
  with ClassRepositoryComponent
  with DB {
  val logger = Logger[this.type]

  val webcrank = Passwords.scrypt()
  val password = "userpass"
  val passwordHash = webcrank.crypt(password)

  override def serialized[E, R, L[E] <: IndexedSeq[E]](collection: L[E])(fn: E => Future[R]): Future[IndexedSeq[R]] = {
    collection.foldLeft(Future(IndexedSeq.empty[R])) { (fAccumulated, nextItem) =>
      for {
        accumulated <- fAccumulated
        nextResult <- { (nextItem) match {
          case part: Part => {
            if (part.name == "exception") { throw new DatabaseException("DatabaseException Message") }
            else fn(nextItem)
          }
          case _ => fn(nextItem)
        }}
      }
      yield accumulated :+ nextResult
    }
  }
}

class ProjectServiceSpec
  extends WordSpec
  with MockFactory
  with ProjectTestEnvironmentComponent {

  val mockConnection = stub[Connection]
  override def transactional[A](f : Connection => Future[A]): Future[A] = {
    f(mockConnection)
  }

  override val projectRepository = stub[ProjectRepository]
  override val partRepository = stub[PartRepository]
  override val taskRepository = stub[TaskRepository]
  override val taskResponseRepository = stub[TaskResponseRepository]
  override val taskScratchpadRepository = stub[TaskScratchpadRepository]
  override val taskFeedbackRepository = stub[TaskFeedbackRepository]
  override val componentRepository = stub[ComponentRepository]
  override val classRepository = stub[ClassRepository]
  override val userRepository = stub[UserRepository]
  override val db = stub[DBSettings]

  (db.pool _) when() returns(mockConnection)

  implicit val conn = mockConnection

  val testUserA = User(
    email = "testUserA@example.org",
    username = "testUserA",
    passwordHash = Some(passwordHash),
    givenname = "Test",
    surname = "UserA"
  )

  val testCourse = Course(
    name = "test course"
  )

  val testClass = Class(
    courseId = testCourse.id,
    teacherId = Option(testUserA.id),
    name = "test class"
  )

  val testProject = Project(
    classId = testClass.id,
    name = "Project name",
    slug = "Project slug",
    description = "Project description",
    parts = IndexedSeq[Part]()
  )

  val testPart = Part(
    projectId = testProject.id,
    name = "Part name",
    position = 1
  )

  val testProjectB = Project(
    classId = testClass.id,
    name = "Project B name",
    slug = "Project B slug",
    description = "Project B description",
    parts = IndexedSeq[Part](testPart)
  )

  val testPartB = Part(
    projectId = testProjectB.id,
    name = "Part name B",
    position = 1
  )

  val testPartC = Part(
    projectId = testProjectB.id,
    name = "Part name C",
    position = 2
  )

  val testTask = LongAnswerTask(
    partId = testPart.id,
    position = 1,
    settings = CommonTaskSettings(
      title = "Task title",
      description = "Task description"
    )
  )

  val testUpdatedTask = LongAnswerTask(
    partId = testPartB.id,
    position = 2,
    settings = CommonTaskSettings(
      title = "Updated Task title",
      description = "Updated Task description"
    )
  )

  val testShortAnswerTask = ShortAnswerTask(
    id = UUID.random,
    partId = testPart.id,
    position = 1,
    settings = CommonTaskSettings(
      title = "ShortAnswerTask title",
      description = "ShortAnswerTask description"
    )
  )

  // NOTE: ProjectService.reorderParts test should be first, otherwise it gives java.lang.NullPointerException
  "ProjectService.reorderParts" should {
    inSequence {
      "reorder part" in {
        val indexedPart = Vector(testPart)
        val indexedPartId = Vector(testPart.id)

        (projectRepository.find(_:UUID)) when(testProjectB.id) returns Future(Option(testProjectB))
        (partRepository.list(_:Project)) when(testProjectB) returns Future.successful(indexedPart)
        (partRepository.reorder(_:Project, _:IndexedSeq[Part])(_: Connection)) when(*, *, mockConnection) returns Future.successful(indexedPart)

        val fReorderParts = projectService.reorderParts(testProjectB.id, indexedPartId)
        Await.result(fReorderParts, Duration.Inf) should be (testProjectB)
      }
    }
  }

  "ProjectService.create" should {
    inSequence {
      "return new project" in {
        (projectRepository.insert(_: Project)(_: Connection)) when(*, mockConnection) returns Future.successful(testProjectB)
        (partRepository.insert(_: Part)(_: Connection)) when(*, mockConnection) returns Future.successful(testPart)
        (taskRepository.insert(_: Task)(_: Connection)) when(*, mockConnection) returns Future.successful(testTask)

        val fNewProject = projectService.create(testClass.id, testProjectB.name, testProjectB.slug, testProjectB.description, testProjectB.availability)

        val project = Await.result(fNewProject, Duration.Inf)
        project.id should be(testProjectB.id)
        project.name should be(testProjectB.name)
        project.slug should be(testProjectB.slug)
        project.description should be(testProjectB.description)
        project.parts(0).id should be(testProjectB.parts(0).id)
        project.parts(0).projectId should be(testProjectB.parts(0).projectId)
        project.parts(0).name should be(testProjectB.parts(0).name)
        project.parts(0).position should be(testProjectB.parts(0).position)
      }
    }
  }

  "ProjectService.update" should {
    inSequence {
      "update project" in {
        (projectRepository.find(_:UUID)) when(testProject.id) returns Future(Option(testProject))
        (projectRepository.update(_:Project)(_: Connection)) when(testProject, mockConnection) returns Future.successful(testProject)

        val fUpdatedProject = projectService.update(testProject.id, testProject.version, testClass.id, testProject.name, testProject.slug, testProject.description, testProject.availability)
        Await.result(fUpdatedProject, Duration.Inf) should be (testProject)
      }
    }
  }

  "ProjectService.delete" should {
    val indexedPart = Vector(testPart)
    val indexedTask = Vector(testTask)

    inSequence {
      "delete project" in {
        (projectRepository.find(_:UUID)) when(testProject.id) returns Future(Option(testProject))
        (partRepository.list(_:Project)) when(testProject) returns Future.successful(indexedPart)
        (taskRepository.list(_:Part)) when(testPart) returns Future.successful(indexedTask)
        (taskScratchpadRepository.delete(_:Task)(_: Connection)) when(testTask, mockConnection) returns Future(true)
        (taskResponseRepository.delete(_:Task)(_: Connection)) when(testTask, mockConnection) returns Future(true)
        (taskFeedbackRepository.delete(_:Task)(_: Connection)) when(testTask, mockConnection) returns Future(true)
        (taskRepository.delete(_:Task)(_: Connection)) when(testTask, mockConnection) returns Future(true)
        (classRepository.disablePart(_:Part)(_: Connection)) when(testPart, mockConnection) returns Future(true)
        (componentRepository.removeFromPart(_:Part)(_: Connection)) when(testPart, mockConnection) returns Future(true)
        (partRepository.delete(_:Project)(_: Connection)) when(testProject, mockConnection) returns Future(true)
        (projectRepository.delete(_:Project)(_: Connection)) when(testProject, mockConnection) returns Future(true)

        val fDeletedProject = projectService.delete(testProject.id, testProject.version)
        Await.result(fDeletedProject, Duration.Inf) should be (true)
      }
    }
  }

  "ProjectService.taskGroups" should {
    val indexedPart = Vector(testPart)
    val indexedPartB = Vector(testPartB)
    val indexedTask = Vector(testTask)

    inSequence {
      "have TaskGroupItem status NotStarted if TaskResponse has another taskId and TaskGroup status Unlocked if parts ids are equal" in {
        val testTaskResponse = TaskResponse(
          userId = testUserA.id,
          taskId = testUserA.id, // Use another UUID
          content = "Content",
          isComplete = true
        )

        val testTaskGroupItem = TaskGroupItem(
          status = 0,
          task = testTask
        )

        val testTaskGroup = TaskGroup(
          part = testPart,
          status = 1,
          tasks = IndexedSeq[TaskGroupItem](testTaskGroupItem)
        )

        val indexedTaskGroup = Vector(testTaskGroup)
        val indexedTaskResponse = Vector(testTaskResponse)

        (partRepository.list(_:Project)) when(testProject) returns Future.successful(indexedPart)
        (partRepository.listEnabled(_:Project, _:User)) when(testProject, testUserA) returns Future.successful(indexedPart)
        (taskRepository.list(_:Project)) when(*) returns Future.successful(indexedTask)
        (taskResponseRepository.list(_:User, _:Project)(_:Connection)) when(*, *, *) returns Future.successful(indexedTaskResponse)

        val fTaskGroups = projectService.taskGroups(testProject, testUserA)
        val result = Await.result(fTaskGroups, Duration.Inf)
        result should be (indexedTaskGroup)
      }
      "have TaskGroupItem status NotStarted if TaskResponse has another taskId and TaskGroup status Locked if parts ids are different" in {
        val testTaskResponse = TaskResponse(
          userId = testUserA.id,
          taskId = testUserA.id, // Use another UUID
          content = "Content",
          isComplete = true
        )

        val testTaskGroupItem = TaskGroupItem(
          status = 0,
          task = testTask
        )

        val testTaskGroup = TaskGroup(
          part = testPart,
          status = 0,
          tasks = IndexedSeq[TaskGroupItem](testTaskGroupItem)
        )

        val indexedTaskGroup = Vector(testTaskGroup)
        val indexedTaskResponse = Vector(testTaskResponse)

        (partRepository.list(_:Project)) when(testProject) returns Future.successful(indexedPart)
        (partRepository.listEnabled(_:Project, _:User)) when(testProject, testUserA) returns Future.successful(indexedPartB)
        (taskRepository.list(_:Project)) when(*) returns Future.successful(indexedTask)
        (taskResponseRepository.list(_:User, _:Project)(_:Connection)) when(*, *, *) returns Future.successful(indexedTaskResponse)

        val fTaskGroups = projectService.taskGroups(testProject, testUserA)
        val result = Await.result(fTaskGroups, Duration.Inf)
        result should be (indexedTaskGroup)
      }
      "have TaskGroupItem status Complete with TRUE TaskResponse" in {
        val testTaskResponse = TaskResponse(
          userId = testUserA.id,
          taskId = testTask.id,
          content = "Content",
          isComplete = true
        )

        val testTaskGroupItem = TaskGroupItem(
          status = 2,
          task = testTask
        )

        val testTaskGroup = TaskGroup(
          part = testPart,
          status = 1,
          tasks = IndexedSeq[TaskGroupItem](testTaskGroupItem)
        )

        val indexedTaskGroup = Vector(testTaskGroup)
        val indexedTaskResponse = Vector(testTaskResponse)

        (partRepository.list(_:Project)) when(testProject) returns Future.successful(indexedPart)
        (partRepository.listEnabled(_:Project, _:User)) when(testProject, testUserA) returns Future.successful(indexedPart)
        (taskRepository.list(_:Project)) when(*) returns Future.successful(indexedTask)
        (taskResponseRepository.list(_:User, _:Project)(_:Connection)) when(*, *, *) returns Future.successful(indexedTaskResponse)

        val fTaskGroups = projectService.taskGroups(testProject, testUserA)
        Await.result(fTaskGroups, Duration.Inf) should be (indexedTaskGroup)
      }
      "have TaskGroupItem status Incomplete with FALSE TaskResponse" in {
        val testTaskResponse = TaskResponse(
          userId = testUserA.id,
          taskId = testTask.id,
          content = "Content",
          isComplete = false
        )

        val testTaskGroupItem = TaskGroupItem(
          status = 1,
          task = testTask
        )

        val testTaskGroup = TaskGroup(
          part = testPart,
          status = 1,
          tasks = IndexedSeq[TaskGroupItem](testTaskGroupItem)
        )

        val indexedTaskGroup = Vector(testTaskGroup)
        val indexedTaskResponse = Vector(testTaskResponse)

        (partRepository.list(_:Project)) when(testProject) returns Future.successful(indexedPart)
        (partRepository.listEnabled(_:Project, _:User)) when(testProject, testUserA) returns Future.successful(indexedPart)
        (taskRepository.list(_:Project)) when(*) returns Future.successful(indexedTask)
        (taskResponseRepository.list(_:User, _:Project)(_:Connection)) when(*, *, *) returns Future.successful(indexedTaskResponse)

        val fTaskGroups = projectService.taskGroups(testProject, testUserA)
        Await.result(fTaskGroups, Duration.Inf) should be (indexedTaskGroup)
      }
    }
  }

  // TODO - find the way to check positions of existing parts
  "ProjectService.createPart" should {
    inSequence {
      "create new part and update existing parts if their possition >=" in {
        val indexedPart = Vector(testPartB, testPartC)

        (projectRepository.find(_:UUID)) when(testProject.id) returns Future(Option(testProject))
        (partRepository.list(_:Project)) when(testProject) returns Future.successful(indexedPart)
        (partRepository.update(_:Part)(_: Connection)) when(*, mockConnection) returns Future.successful(testPartB)
        (partRepository.update(_:Part)(_: Connection)) when(*, mockConnection) returns Future.successful(testPartC)
        (partRepository.insert(_:Part)(_: Connection)) when(*, mockConnection) returns Future.successful(testPart)

        val fNewPart = projectService.createPart(testProject.id, testPart.name, testPart.description, testPart.position)
        val result = Await.result(fNewPart, Duration.Inf)

        result.version should be (testPart.version)
        result.name should be (testPart.name)
        result.description should be (testPart.description)
        result.position should be (testPart.position)
      }
      "create new part without updating of existing parts if their possition !=" in {
        val indexedPart = Vector(testPartC)

        (projectRepository.find(_:UUID)) when(testProject.id) returns Future(Option(testProject))
        (partRepository.list(_:Project)) when(testProject) returns Future.successful(indexedPart)
        (partRepository.update(_:Part)(_: Connection)) when(*, mockConnection) returns Future.successful(testPartC)
        (partRepository.insert(_:Part)(_: Connection)) when(*, mockConnection) returns Future.successful(testPart)

        val fNewPart = projectService.createPart(testProject.id, testPart.name, testPart.description, testPart.position)
        val result = Await.result(fNewPart, Duration.Inf)

        result.version should be (testPart.version)
        result.name should be (testPart.name)
        result.description should be (testPart.description)
        result.position should be (testPart.position)
      }
    }
  }

  // TODO - find the way to check shifted positions
  "ProjectService.updatePart" should {
    val testPartForException = Part(
      projectId = testProjectB.id,
      name = "exception",
      position = 2
    )

    inSequence {
      "update part if newPosition != oldPosition" in {
        val indexedPart = Vector(testPartB, testPartC)

        (partRepository.find(_:UUID)) when(testPart.id) returns Future(Option(testPart))
        (projectRepository.find(_:UUID)) when(testPart.projectId) returns Future(Option(testProject))
        (partRepository.list(_:Project)) when(testProject) returns Future.successful(indexedPart)
        (partRepository.update(_:Part)(_: Connection)) when(*, mockConnection) returns Future.successful(testPart.copy(
          version = 16L,
          name = "New part name",
          description = "New part description",
          position = testPart.position + 2))
        (partRepository.update(_:Part)(_: Connection)) when(*, mockConnection) returns Future.successful(testPartB)
        (partRepository.update(_:Part)(_: Connection)) when(*, mockConnection) returns Future.successful(testPartC)

        val fUpdatePart = projectService.updatePart(testPart.id, 16L, "New part name", "New part description", testPart.position - 1)
        val result = Await.result(fUpdatePart, Duration.Inf)

        result.version should be (16L)
        result.name should be ("New part name")
        result.description should be ("New part description")
        result.position should be (testPart.position + 2)
      }
      "throw an DatabaseException to check exception in serialized method" in {
        val indexedPart = Vector(testPartForException)

        (partRepository.find(_:UUID)) when(testPart.id) returns Future(Option(testPart))
        (projectRepository.find(_:UUID)) when(testPart.projectId) returns Future(Option(testProject))
        (partRepository.list(_:Project)) when(testProject) returns Future.successful(indexedPart)
        (partRepository.update(_:Part)(_: Connection)) when(*, mockConnection) returns Future.successful(testPart.copy(
          version = 16L,
          name = "New part name",
          description = "New part description",
          position = testPart.position + 2))

        val fUpdatePart = projectService.updatePart(testPart.id, 16L, "New part name", "New part description", testPart.position + 2)
        an [DatabaseException] should be thrownBy Await.result(fUpdatePart, Duration.Inf)
      }
      "update part if newPosition == oldPosition" in {
        val indexedPart = Vector(testPartB, testPartC)

        (partRepository.find(_:UUID)) when(testPart.id) returns Future(Option(testPart))
        (projectRepository.find(_:UUID)) when(testPart.projectId) returns Future(Option(testProject))
        (partRepository.list(_:Project)) when(testProject) returns Future.successful(indexedPart)
        (partRepository.update(_:Part)(_: Connection)) when(*, mockConnection) returns Future.successful(testPart.copy(
          version = 16L,
          name = "New part name",
          description = "New part description",
          position = testPart.position))
        (partRepository.update(_:Part)(_: Connection)) when(*, mockConnection) returns Future.successful(testPartB)
        (partRepository.update(_:Part)(_: Connection)) when(*, mockConnection) returns Future.successful(testPartC)

        val fUpdatePart = projectService.updatePart(testPart.id, 16L, "New part name", "New part description", testPart.position)
        val result = Await.result(fUpdatePart, Duration.Inf)

        result.version should be (16L)
        result.name should be ("New part name")
        result.description should be ("New part description")
        result.position should be (testPart.position)
      }
    }
  }

  // TODO - find the way to check positions -1
  "ProjectService.deletePart" should {
    inSequence {
      "delete part" in {
        val indexedPart = Vector(testPart)
        val indexedTask = Vector(testTask)

        (partRepository.find(_:UUID)) when(testPart.id) returns Future(Option(testPart))
        (projectRepository.find(_:UUID)) when(testProject.id) returns Future(Option(testProject))
        (partRepository.list(_:Project)) when(testProject) returns Future.successful(indexedPart)
        (partRepository.update(_:Part)(_: Connection)) when(*, mockConnection) returns Future.successful(testPart)
        (taskRepository.list(_:Part)) when(testPart) returns Future.successful(indexedTask)

        (taskScratchpadRepository.delete(_:Task)(_: Connection)) when(*, mockConnection) returns Future.successful(true)
        (taskResponseRepository.delete(_:Task)(_: Connection)) when(*, mockConnection) returns Future.successful(true)
        (taskFeedbackRepository.delete(_:Task)(_: Connection)) when(*, mockConnection) returns Future.successful(true)
        (taskRepository.delete(_:Part)(_: Connection)) when(*, mockConnection) returns Future.successful(true)
        (classRepository.disablePart(_:Part)(_: Connection)) when(*, mockConnection) returns Future.successful(true)
        (componentRepository.removeFromPart(_:Part)(_: Connection)) when(*, mockConnection) returns Future.successful(true)
        (partRepository.delete(_:Part)(_: Connection)) when(*, mockConnection) returns Future.successful(true)

        val fDeletePart = projectService.deletePart(testPart.id, testPart.version)
        Await.result(fDeletePart, Duration.Inf) should be (true)
      }
    }
  }

  "ProjectService.createTask" should {
    inSequence{
      "create task" in {
        val indexedTask = Vector(testTask)

        (partRepository.find(_:UUID)) when(testPart.id) returns Future(Option(testPart))
        (taskRepository.list(_:Part)) when(testPart) returns(Future.successful(indexedTask))
        (taskRepository.update(_:Task)(_: Connection)) when(*, mockConnection) returns Future.successful(testTask)
        (taskRepository.insert(_:Task)(_: Connection)) when(*, mockConnection) returns Future.successful(testTask)

        val fCreateTask = projectService.createTask(testPart.id, testTask.taskType, testTask.settings.title, testTask.settings.description, testTask.position)
        Await.result(fCreateTask, Duration.Inf) should be (testTask)
      }
    }
  }

  "ProjectService.updateLongAnswerTask" should {
    val indexedTask = Vector(testTask)
    val indexedUpdatedTask = Vector(testUpdatedTask)

    inSequence{
      "throw an exception if task is not instance of LongAnswerTask" in {
        (taskRepository.find(_:UUID)) when(testShortAnswerTask.id) returns Future(Option(testShortAnswerTask))

        val fUpdateTask = projectService.updateLongAnswerTask(
          testShortAnswerTask.id,
          testShortAnswerTask.version,
          testShortAnswerTask.settings.title,
          testShortAnswerTask.settings.description,
          testShortAnswerTask.position,
          false
        )
        an [Exception] should be thrownBy Await.result(fUpdateTask, Duration.Inf)
      }
      "update LongAnswerTask" in {
        (taskRepository.find(_:UUID)) when(testTask.id) returns Future(Option(testTask))

        // updateTask method
        (partRepository.find(_:UUID)) when(testPart.id) returns Future(Option(testPart))
        (taskRepository.list(_:Part)) when(testPart) returns Future.successful(indexedTask)
        (partRepository.find(_:UUID)) when(testPartB.id) returns Future(Option(testPartB))
        (taskRepository.list(_:Part)) when(testPartB) returns Future.successful(indexedUpdatedTask)
        (taskRepository.update(_:Task)(_: Connection)) when(*, mockConnection) returns Future.successful(testUpdatedTask)

        val fUpdateTask = projectService.updateLongAnswerTask(
          testTask.id,
          testUpdatedTask.version,
          testUpdatedTask.settings.title,
          testUpdatedTask.settings.description,
          testUpdatedTask.position,
          true
        )
        val result = Await.result(fUpdateTask, Duration.Inf)
        result should be (testUpdatedTask)
      }
    }
  }

  "ProjectService.updateShortAnswerTask" should {
    val indexedTask = Vector(testShortAnswerTask)
    val testUpdatedShortAnswerTask = ShortAnswerTask(
      id = UUID.random,
      partId = testPartB.id,
      position = 2,
      settings = CommonTaskSettings(
        title = "Updated ShortAnswerTask title",
        description = "Updated ShortAnswerTask description"
      )
    )
    val indexedUpdatedTask = Vector(testUpdatedShortAnswerTask)

    inSequence{
      "throw an exception if task is not instance of ShortAnswerTask" in {
        (taskRepository.find(_:UUID)) when(testTask.id) returns Future(Option(testTask))

        val fUpdateTask = projectService.updateShortAnswerTask(
          testTask.id,
          testTask.version,
          testTask.settings.title,
          testTask.settings.description,
          testTask.position,
          false,
          255
        )
        an [Exception] should be thrownBy Await.result(fUpdateTask, Duration.Inf)
      }
      "update ShortAnswerTask" in {
        (taskRepository.find(_:UUID)) when(testShortAnswerTask.id) returns Future(Option(testShortAnswerTask))

        // updateTask method
        (partRepository.find(_:UUID)) when(testPart.id) returns Future(Option(testPart))
        (taskRepository.list(_:Part)) when(testPart) returns Future.successful(indexedTask)
        (partRepository.find(_:UUID)) when(testPartB.id) returns Future(Option(testPartB))
        (taskRepository.list(_:Part)) when(testPartB) returns Future.successful(indexedUpdatedTask)
        (taskRepository.update(_:Task)(_: Connection)) when(*, mockConnection) returns Future.successful(testUpdatedShortAnswerTask)

        val fUpdateTask = projectService.updateShortAnswerTask(
          testShortAnswerTask.id,
          testUpdatedShortAnswerTask.version,
          testUpdatedShortAnswerTask.settings.title,
          testUpdatedShortAnswerTask.settings.description,
          testUpdatedShortAnswerTask.position,
          true,
          255
        )
        val result = Await.result(fUpdateTask, Duration.Inf)
        result should be (testUpdatedShortAnswerTask)
      }
    }
  }

  "ProjectService.updateMultipleChoiceTask" should {
    val testMultipleChoiceTask = MultipleChoiceTask(
      id = UUID.random,
      partId = testPart.id,
      position = 1,
      settings = CommonTaskSettings(
        title = "MultipleChoiceTask title",
        description = "MultipleChoiceTask description"
      )
    )
    val indexedTask = Vector(testTask)
    val testUpdatedMultipleChoiceTask = MultipleChoiceTask(
      id = UUID.random,
      partId = testPartB.id,
      position = 2,
      settings = CommonTaskSettings(
        title = "Updated MultipleChoiceTask title",
        description = "Updated MultipleChoiceTask description"
      )
    )
    val indexedUpdatedTask = Vector(testUpdatedMultipleChoiceTask)

    inSequence{
      "throw an exception if task is not instance of MultipleChoiceTask" in {
        (taskRepository.find(_:UUID)) when(testTask.id) returns Future(Option(testTask))

        val fUpdateTask = projectService.updateMultipleChoiceTask(
          testTask.id,
          testTask.version,
          testTask.settings.title,
          testTask.settings.description,
          testTask.position,
          true
        )
        an [Exception] should be thrownBy Await.result(fUpdateTask, Duration.Inf)
      }
      "update MultipleChoiceTask" in {
        (taskRepository.find(_:UUID)) when(testMultipleChoiceTask.id) returns Future(Option(testMultipleChoiceTask))

        // updateTask method
        (partRepository.find(_:UUID)) when(testPart.id) returns Future(Option(testPart))
        (taskRepository.list(_:Part)) when(testPart) returns Future.successful(indexedTask)
        (partRepository.find(_:UUID)) when(testPartB.id) returns Future(Option(testPartB))
        (taskRepository.list(_:Part)) when(testPartB) returns Future.successful(indexedUpdatedTask)
        (taskRepository.update(_:Task)(_: Connection)) when(*, mockConnection) returns Future.successful(testUpdatedMultipleChoiceTask)

        val fUpdateTask = projectService.updateMultipleChoiceTask(
          testMultipleChoiceTask.id,
          testUpdatedMultipleChoiceTask.version,
          testUpdatedMultipleChoiceTask.settings.title,
          testUpdatedMultipleChoiceTask.settings.description,
          testUpdatedMultipleChoiceTask.position,
          true
        )
        val result = Await.result(fUpdateTask, Duration.Inf)
        result should be (testUpdatedMultipleChoiceTask)
      }
    }
  }

  "ProjectService.updateOrderingTask" should {
    val testOrderingTask = OrderingTask(
      id = UUID.random,
      partId = testPart.id,
      position = 1,
      settings = CommonTaskSettings(
        title = "OrderingTask title",
        description = "OrderingTask description"
      )
    )
    val indexedTask = Vector(testTask)
    val testUpdatedOrderingTask = OrderingTask(
      id = UUID.random,
      partId = testPartB.id,
      position = 2,
      settings = CommonTaskSettings(
        title = "Updated OrderingTask title",
        description = "Updated OrderingTask description"
      )
    )
    val indexedUpdatedTask = Vector(testUpdatedOrderingTask)

    inSequence{
      "throw an exception if task is not instance of OrderingTask" in {
        (taskRepository.find(_:UUID)) when(testTask.id) returns Future(Option(testTask))

        val fUpdateTask = projectService.updateOrderingTask(
          testTask.id,
          testTask.version,
          testTask.settings.title,
          testTask.settings.description,
          testTask.position,
          true
        )
        an [Exception] should be thrownBy Await.result(fUpdateTask, Duration.Inf)
      }
      "update OrderingTask" in {
        (taskRepository.find(_:UUID)) when(testOrderingTask.id) returns Future(Option(testOrderingTask))

        // updateTask method
        (partRepository.find(_:UUID)) when(testPart.id) returns Future(Option(testPart))
        (taskRepository.list(_:Part)) when(testPart) returns Future.successful(indexedTask)
        (partRepository.find(_:UUID)) when(testPartB.id) returns Future(Option(testPartB))
        (taskRepository.list(_:Part)) when(testPartB) returns Future.successful(indexedUpdatedTask)
        (taskRepository.update(_:Task)(_: Connection)) when(*, mockConnection) returns Future.successful(testUpdatedOrderingTask)

        val fUpdateTask = projectService.updateOrderingTask(
          testOrderingTask.id,
          testUpdatedOrderingTask.version,
          testUpdatedOrderingTask.settings.title,
          testUpdatedOrderingTask.settings.description,
          testUpdatedOrderingTask.position,
          true
        )
        val result = Await.result(fUpdateTask, Duration.Inf)
        result should be (testUpdatedOrderingTask)
      }
    }
  }

  "ProjectService.updateMatchingTask" should {
    val testMatchingTask = MatchingTask(
      id = UUID.random,
      partId = testPart.id,
      position = 1,
      settings = CommonTaskSettings(
        title = "MatchingTask title",
        description = "MatchingTask description"
      )
    )
    val indexedTask = Vector(testTask)
    val testUpdatedMatchingTask = MatchingTask(
      id = UUID.random,
      partId = testPartB.id,
      position = 2,
      settings = CommonTaskSettings(
        title = "Updated MatchingTask title",
        description = "Updated MatchingTask description"
      )
    )
    val indexedUpdatedTask = Vector(testUpdatedMatchingTask)

    inSequence{
      "throw an exception if task is not instance of MatchingTask" in {
        (taskRepository.find(_:UUID)) when(testTask.id) returns Future(Option(testTask))

        val fUpdateTask = projectService.updateMatchingTask(
          testTask.id,
          testTask.version,
          testTask.settings.title,
          testTask.settings.description,
          testTask.position,
          true
        )
        an [Exception] should be thrownBy Await.result(fUpdateTask, Duration.Inf)
      }
      "update MatchingTask" in {
        (taskRepository.find(_:UUID)) when(testMatchingTask.id) returns Future(Option(testMatchingTask))

        // updateTask method
        (partRepository.find(_:UUID)) when(testPart.id) returns Future(Option(testPart))
        (taskRepository.list(_:Part)) when(testPart) returns Future.successful(indexedTask)
        (partRepository.find(_:UUID)) when(testPartB.id) returns Future(Option(testPartB))
        (taskRepository.list(_:Part)) when(testPartB) returns Future.successful(indexedUpdatedTask)
        (taskRepository.update(_:Task)(_: Connection)) when(*, mockConnection) returns Future.successful(testUpdatedMatchingTask)

        val fUpdateTask = projectService.updateMatchingTask(
          testMatchingTask.id,
          testUpdatedMatchingTask.version,
          testUpdatedMatchingTask.settings.title,
          testUpdatedMatchingTask.settings.description,
          testUpdatedMatchingTask.position,
          true
        )
        val result = Await.result(fUpdateTask, Duration.Inf)
        result should be (testUpdatedMatchingTask)
      }
    }
  }

  "ProjectService.deleteTask" should {
    val indexedTask = Vector(testTask)

    inSequence{
      "throw an exception if task is not found" in {
        (taskRepository.find(_:UUID)) when(testTask.id) returns Future.successful(None)

        val fUpdateTask = projectService.deleteTask(testTask.id, testTask.version)
        an [Exception] should be thrownBy Await.result(fUpdateTask, Duration.Inf)
      }
      "delete task" in {
        (taskRepository.find(_:UUID)) when(testTask.id) returns Future(Option(testTask))
        (partRepository.find(_:UUID)) when(testPart.id) returns Future(Option(testPart))
        (taskRepository.list(_:Part)) when(testPart) returns Future.successful(indexedTask)
        (taskScratchpadRepository.delete(_:Task)(_: Connection)) when(testTask, mockConnection) returns Future.successful(true)
        (taskResponseRepository.delete(_:Task)(_: Connection)) when(testTask, mockConnection) returns Future.successful(true)
        (taskFeedbackRepository.delete(_:Task)(_: Connection)) when(testTask, mockConnection) returns Future.successful(true)
        (taskRepository.delete(_:Task)(_: Connection)) when(testTask, mockConnection) returns Future.successful(true)

        val fUpdateTask = projectService.deleteTask(testTask.id, testTask.version)
        Await.result(fUpdateTask, Duration.Inf) should be (true)
      }
    }
  }

  "ProjectService.moveTask" should {
    val indexedTask = Vector(testTask)
    val indexedUpdatedTask = Vector(testUpdatedTask)

    inSequence{
      "throw an exception if task is not found" in {
        (taskRepository.find(_:UUID)) when(testTask.id) returns Future.successful(None)

        val fMoveTask = projectService.moveTask(testPart.id, testTask.id, 5)
        an [Exception] should be thrownBy Await.result(fMoveTask, Duration.Inf)
      }
      "move task" in {
        (taskRepository.find(_:UUID)) when(testTask.id) returns Future(Option(testTask))

        // updateTask method
        (partRepository.find(_:UUID)) when(testPart.id) returns Future(Option(testPart))
        (taskRepository.list(_:Part)) when(testPart) returns Future.successful(indexedTask)
        (partRepository.find(_:UUID)) when(testPartB.id) returns Future(Option(testPartB))
        (taskRepository.list(_:Part)) when(testPartB) returns Future.successful(indexedUpdatedTask)
        (taskRepository.update(_:Task)(_: Connection)) when(*, mockConnection) returns Future.successful(testUpdatedTask)

        val fMoveTask = projectService.moveTask(testPart.id, testTask.id, 5)
        Await.result(fMoveTask, Duration.Inf) should be (testUpdatedTask)
      }
    }
  }
}
