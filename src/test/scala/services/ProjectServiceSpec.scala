import ca.shiftfocus.krispii.core.lib.UUID
import ca.shiftfocus.krispii.core.models.tasks.{Task, LongAnswerTask}
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services._
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.github.mauricio.async.db.Connection
import grizzled.slf4j.Logger
import org.scalamock.scalatest.MockFactory
import org.scalatest.WordSpec
import webcrank.password.Passwords
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import org.scalatest.Matchers._
import ca.shiftfocus.krispii.core.services.datasource._
import scala.concurrent.ExecutionContext.Implicits.global


trait ProjectTestEnvironmentComponent
  extends ProjectServiceImplComponent
  with ProjectRepositoryComponent
  with PartRepositoryComponent
  with TaskRepositoryComponent
  with TaskResponseRepositoryComponent
  with TaskScratchpadRepositoryComponent
  with TaskFeedbackRepositoryComponent
  with ComponentRepositoryComponent
  with SectionRepositoryComponent
  with DB

class ProjectServiceSpec
  extends WordSpec
  with MockFactory
  with ProjectTestEnvironmentComponent {

  val logger = Logger[this.type]
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
  override val sectionRepository = stub[SectionRepository]
  override val db = stub[DBSettings]

  val webcrank = Passwords.scrypt()
  val password = "userpass"
  val passwordHash = webcrank.crypt(password)

  val testUserA = User(
    email = "testUserA@example.org",
    username = "testUserA",
    passwordHash = Some(passwordHash),
    givenname = "Test",
    surname = "UserA"
  )

  val testProject = Project(
    name = "Project name",
    slug = "Project slug",
    description = "Project description",
    parts = IndexedSeq[Part]()
  )

  val testPart = Part(
    projectId = testProject.id,
    name = "Part name"
  )

  val testTask = LongAnswerTask(
    partId = testPart.id,
    position = 1
  )

  val testTaskGroup = TaskGroup(
    part = testPart,
    status = 1
  )

//  "ProjectService.create" should {
//    inSequence {
//      "return new project" in {
//        (projectRepository.insert(_:Project)(_: Connection)) when(testProject, mockConnection) returns Future.successful(testProject)
//        (partRepository.insert(_:Part)(_: Connection)) when(testPart, mockConnection) returns Future.successful(testPart)
//        (taskRepository.insert(_:Task)(_: Connection)) when(testTask, mockConnection) returns Future.successful(testTask)
//
//        val fNewProject = projectService.create(testProject.name, testProject.slug, testProject.description)
//        Await.result(fNewProject, Duration.Inf) should be (testProject)
//      }
//    }
//  }

//  "ProjectService.update" should {
//    inSequence {
//      "update project" in {
//        (projectRepository.find(_:UUID)) when(testProject.id) returns Future(Option(testProject))
//        (projectRepository.update(_:Project)(_: Connection)) when(testProject, mockConnection) returns Future.successful(testProject)
//
//        val fUpdatedProject = projectService.update(testProject.id, testProject.version, testProject.name, testProject.slug, testProject.description)
//        Await.result(fUpdatedProject, Duration.Inf) should be (testProject)
//      }
//    }
//  }

//  "ProjectService.delete" should {
//    val indexedPart = Vector(testPart)
//    val indexedTask = Vector(testTask)
//
//    inSequence {
//      "delete project" in {
//        (projectRepository.find(_:UUID)) when(testProject.id) returns Future(Option(testProject))
//        (partRepository.list(_:Project)) when(testProject) returns Future.successful(indexedPart)
//        (taskRepository.list(_:Part)) when(testPart) returns Future.successful(indexedTask)
//        (taskScratchpadRepository.delete(_:Task)(_: Connection)) when(testTask, mockConnection) returns Future(true)
//        (taskResponseRepository.delete(_:Task)(_: Connection)) when(testTask, mockConnection) returns Future(true)
//        (taskFeedbackRepository.delete(_:Task)(_: Connection)) when(testTask, mockConnection) returns Future(true)
//        (taskRepository.delete(_:Task)(_: Connection)) when(testTask, mockConnection) returns Future(true)
//        (sectionRepository.disablePart(_:Part)(_: Connection)) when(testPart, mockConnection) returns Future(true)
//        (componentRepository.removeFromPart(_:Part)(_: Connection)) when(testPart, mockConnection) returns Future(true)
//        (partRepository.delete(_:Project)(_: Connection)) when(testProject, mockConnection) returns Future(true)
//        (projectRepository.delete(_:Project)(_: Connection)) when(testProject, mockConnection) returns Future(true)
//
//        val fDeletedProject = projectService.delete(testProject.id, testProject.version)
//        Await.result(fDeletedProject, Duration.Inf) should be (true)
//      }
//    }
//  }

  //  java.lang.NullPointerException
//  "ProjectService.taskGroups" should {
//    val indexedPart = Vector(testPart)
//    val indexedTask = Vector(testTask)
//    val indexedTaskGroup = Vector(testTaskGroup)
//
//    inSequence {
//      "have Task.NotStarted if TaskResponse has another taskId" in {
//        val testTaskResponse = TaskResponse(
//          userId = testUserA.id,
//          taskId = testUserA.id,
//          content = "Content",
//          isComplete = true
//        )
//
//        val indexedTaskResponse = Vector(testTaskResponse)
//
//        (partRepository.list(_:Project)) when(testProject) returns Future.successful(indexedPart)
//        (partRepository.listEnabled(_:Project, _:User)) when(testProject, testUserA) returns Future.successful(indexedPart)
//        (taskRepository.list(_:Project)) when(testProject) returns Future.successful(indexedTask)
//        (taskResponseRepository.list(_:User, _:Project)(_:Connection)) when(testUserA, testProject, mockConnection) returns Future.successful(indexedTaskResponse)
//
//        val fTaskGroups = projectService.taskGroups(testProject, testUserA)
//        Await.result(fTaskGroups, Duration.Inf) should be (Future(indexedTaskGroup))
//      }
//      "have Task.Complete with TRUE TaskResponse" in {
//        val testTaskResponse = TaskResponse(
//          userId = testUserA.id,
//          taskId = testTask.id,
//          content = "Content",
//          isComplete = true
//        )
//
//        val indexedTaskResponse = Vector(testTaskResponse)
//
//        (partRepository.list(_:Project)) when(testProject) returns Future.successful(indexedPart)
//        (partRepository.listEnabled(_:Project, _:User)) when(testProject, testUserA) returns Future.successful(indexedPart)
//        (taskRepository.list(_:Project)) when(testProject) returns Future.successful(indexedTask)
//        (taskResponseRepository.list(_:User, _:Project)(_:Connection)) when(testUserA, testProject, mockConnection) returns Future.successful(indexedTaskResponse)
//
//        val fTaskGroups = projectService.taskGroups(testProject, testUserA)
//        Await.result(fTaskGroups, Duration.Inf) should be (Future(indexedTaskGroup))
//      }
//      "have Task.Incomplete with FALSE TaskResponse" in {
//        val testTaskResponse = TaskResponse(
//          userId = testUserA.id,
//          taskId = testTask.id,
//          content = "Content",
//          isComplete = false
//        )
//
//        val indexedTaskResponse = Vector(testTaskResponse)
//
//        (partRepository.list(_:Project)) when(testProject) returns Future.successful(indexedPart)
//        (partRepository.listEnabled(_:Project, _:User)) when(testProject, testUserA) returns Future.successful(indexedPart)
//        (taskRepository.list(_:Project)) when(testProject) returns Future.successful(indexedTask)
//        (taskResponseRepository.list(_:User, _:Project)(_:Connection)) when(testUserA, testProject, mockConnection) returns Future.successful(indexedTaskResponse)
//
//        val fTaskGroups = projectService.taskGroups(testProject, testUserA)
//        Await.result(fTaskGroups, Duration.Inf) should be (Future(indexedTaskGroup))
//      }
//    }
//  }

  //  java.lang.NullPointerException
  "ProjectService.createPart" should {
    val indexedPart = Vector(testPart)

    inSequence {
      "updatePart if possition >=" in {
        (projectRepository.find(_:UUID)) when(testProject.id) returns Future(Option(testProject))
        (partRepository.list(_:Project)) when(testProject) returns Future.successful(indexedPart)
        (partRepository.update(_:Part)(_: Connection)) when(testPart, mockConnection) returns Future.successful(testPart)
        (partRepository.insert(_:Part)(_: Connection)) when(testPart, mockConnection) returns Future.successful(testPart)

        val fNewPart = projectService.createPart(testProject.id, testPart.name, testPart.description, testPart.position)
        Await.result(fNewPart, Duration.Inf).position should be (testPart.position)
      }
      "createPart if possition !=" in {
        (projectRepository.find(_:UUID)) when(testProject.id) returns Future(Option(testProject))
        (partRepository.list(_:Project)) when(testProject) returns Future.successful(indexedPart)
        (partRepository.update(_:Part)(_: Connection)) when(testPart, mockConnection) returns Future.successful(testPart)
        (partRepository.insert(_:Part)(_: Connection)) when(testPart, mockConnection) returns Future.successful(testPart)

        val fNewPart = projectService.createPart(testProject.id, testPart.name, testPart.description, testPart.position + 1)
        Await.result(fNewPart, Duration.Inf).position should be (testPart.position + 2)
      }
    }
  }
}
