package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks._
import ca.shiftfocus.krispii.core.models.tasks.questions._
import ca.shiftfocus.krispii.core.repositories.{TaskRepository, PartRepository, ProjectRepository}
import java.util.UUID
import scala.concurrent.Future
import scalaz.\/

trait ProjectService extends Service[ErrorUnion#Fail] {
  val authService: AuthService
  val schoolService: SchoolService
  val projectRepository: ProjectRepository
  val partRepository: PartRepository
  val taskRepository: TaskRepository

  val maxPartsInProject = 50
  val maxTasksInPart = 100

  // Projects
  def list: Future[\/[ErrorUnion#Fail, IndexedSeq[Project]]]
  def listMasterProjects(enabled: Option[Boolean] = None): Future[\/[ErrorUnion#Fail, IndexedSeq[Project]]]
  def list(courseId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Project]]]
  def listProjectsByUser(userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Project]]]
  def listProjectsByTeacher(userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Project]]]
  def listProjectsByTags(tags: IndexedSeq[(String, String)], distinct: Boolean = true): Future[\/[ErrorUnion#Fail, IndexedSeq[Project]]]

  def find(id: UUID): Future[\/[ErrorUnion#Fail, Project]]
  def find(projectSlug: String): Future[\/[ErrorUnion#Fail, Project]]
  def find(projectId: UUID, userId: UUID): Future[\/[ErrorUnion#Fail, Project]]
  def find(projectSlug: String, userId: UUID): Future[\/[ErrorUnion#Fail, Project]]

  def find(id: UUID, fetchParts: Boolean): Future[\/[ErrorUnion#Fail, Project]]
  def find(projectSlug: String, fetchParts: Boolean): Future[\/[ErrorUnion#Fail, Project]]
  def find(projectId: UUID, userId: UUID, fetchParts: Boolean): Future[\/[ErrorUnion#Fail, Project]]
  def find(projectSlug: String, userId: UUID, fetchParts: Boolean): Future[\/[ErrorUnion#Fail, Project]]

  def userHasProject(userId: UUID, projectSlug: String): Future[\/[ErrorUnion#Fail, Boolean]]

  def create(
    courseId: UUID,
    name: String,
    slug: String,
    description: String,
    longDescription: String,
    availability: String,
    parentId: Option[UUID] = None,
    parentVersion: Option[Long] = None,
    isMaster: Boolean = false,
    enabled: Boolean,
    projectType: String
  ): Future[\/[ErrorUnion#Fail, Project]]

  def copyProject(projectId: UUID, courseId: UUID, userId: UUID): Future[\/[ErrorUnion#Fail, Project]]

  def updateInfo(
    id: UUID,
    version: Long,
    courseId: Option[UUID],
    name: Option[String],
    slug: Option[String],
    description: Option[String],
    longDescription: Option[String],
    availability: Option[String],
    enabled: Option[Boolean],
    projectType: Option[String],
    status: Option[String],
    lastTaskId: Option[Option[UUID]]
  ): Future[\/[ErrorUnion#Fail, Project]]

  def setMaster(id: UUID, version: Long, isMaster: Boolean): Future[\/[ErrorUnion#Fail, Project]]
  def updateSlug(id: UUID, version: Long, slug: String): Future[\/[ErrorUnion#Fail, Project]]
  def delete(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, Project]]

  // Parts
  def listPartsInComponent(componentId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Part]]]
  def findPart(partId: UUID, fetchTasks: Boolean = true): Future[\/[ErrorUnion#Fail, Part]]
  def findPartByPosition(projectId: UUID, position: Int, fetchParts: Boolean = true): Future[\/[ErrorUnion#Fail, Part]]
  def createPart(projectId: UUID, name: String, position: Int, id: UUID = UUID.randomUUID): Future[\/[ErrorUnion#Fail, Part]]
  def updatePart(partId: UUID, version: Long, name: Option[String], position: Option[Int], enabled: Option[Boolean]): Future[\/[ErrorUnion#Fail, Part]]
  def deletePart(partId: UUID, version: Long): Future[\/[ErrorUnion#Fail, Part]]

  def togglePart(partId: UUID, version: Long): Future[\/[ErrorUnion#Fail, Part]]

  // Tasks
  def listTask(partId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Task]]]
  def listTeacherTasks(teacherId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Task]]]
  def findTask(taskId: UUID): Future[\/[ErrorUnion#Fail, Task]]
  def findTask(projectSlug: String, partNum: Int, taskNum: Int): Future[\/[ErrorUnion#Fail, Task]]
  def findNowTask(userId: UUID, projectId: UUID): Future[\/[ErrorUnion#Fail, Task]]

  def createTask(
    partId: UUID,
    taskType: Int,
    name: String,
    description: String,
    position: Int,
    id: UUID = UUID.randomUUID
  ): Future[\/[ErrorUnion#Fail, Task]]

  case class CommonTaskArgs(
    taskId: UUID,
    version: Long,
    name: Option[String],
    help: Option[String],
    description: Option[String],
    instructions: Option[String],
    tagline: Option[String],
    position: Option[Int],
    notesAllowed: Option[Boolean],
    hideResponse: Option[Boolean],
    allowGfile: Option[Boolean],
    partId: Option[UUID] = None,
    responseTitle: Option[Option[String]] = None,
    notesTitle: Option[Option[String]] = None,
    maxGrade: Option[String],
    mediaData: Option[Option[MediaData]],
    layout: Option[Int],
    parentId: Option[Option[UUID]] = None
  )

  def updateDocumentTask(commonArgs: CommonTaskArgs, depId: Option[Option[UUID]] = None): Future[\/[ErrorUnion#Fail, Task]]
  def updateQuestionTask(commonArgs: CommonTaskArgs, questions: Option[IndexedSeq[Question]]): Future[\/[ErrorUnion#Fail, Task]]
  def updateMediaTask(commonArgs: CommonTaskArgs, mediaType: Option[Int] = None): Future[\/[ErrorUnion#Fail, Task]]

  def deleteTask(taskId: UUID, version: Long): Future[\/[ErrorUnion#Fail, Task]]
  def moveTask(taskId: UUID, version: Long, newPosition: Int, partId: Option[UUID] = None): Future[\/[ErrorUnion#Fail, Task]]
  def hasTaskWork(taskId: UUID): Future[\/[ErrorUnion#Fail, Boolean]]
  def hasPartWork(partId: UUID): Future[\/[ErrorUnion#Fail, Boolean]]

  def getToken(token: String): Future[\/[ErrorUnion#Fail, ProjectToken]]
  def listToken(projectId: UUID, email: String): Future[\/[ErrorUnion#Fail, IndexedSeq[ProjectToken]]]
  def listTokenByProject(projectId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[ProjectToken]]]
  def listTokenByEmail(email: String): Future[\/[ErrorUnion#Fail, IndexedSeq[ProjectToken]]]
  def createToken(projectId: UUID, email: String): Future[\/[ErrorUnion#Fail, ProjectToken]]
  def deleteToken(token: String): Future[\/[ErrorUnion#Fail, ProjectToken]]
}
