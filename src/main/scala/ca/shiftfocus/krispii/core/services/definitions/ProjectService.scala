package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks._
import ca.shiftfocus.krispii.core.repositories.{TaskRepository, PartRepository, ProjectRepository}
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future
import scalaz.\/

trait ProjectService extends Service[ErrorUnion#Fail] {
  val authService: AuthService
  val schoolService: SchoolService
  val projectRepository: ProjectRepository
  val partRepository: PartRepository
  val taskRepository: TaskRepository


  // Projects
  def list: Future[\/[ErrorUnion#Fail, IndexedSeq[Project]]]
  def list(courseId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Project]]]
  def listProjectsByUser(userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Project]]]
  def listProjectsByTeacher(userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Project]]]

  def find(id: UUID): Future[\/[ErrorUnion#Fail, Project]]
  def find(projectSlug: String): Future[\/[ErrorUnion#Fail, Project]]
  def find(projectId: UUID, userId: UUID): Future[\/[ErrorUnion#Fail, Project]]
  def find(projectSlug: String, userId: UUID): Future[\/[ErrorUnion#Fail, Project]]

  def find(id: UUID, fetchParts: Boolean): Future[\/[ErrorUnion#Fail, Project]]
  def find(projectSlug: String, fetchParts: Boolean): Future[\/[ErrorUnion#Fail, Project]]
  def find(projectId: UUID, userId: UUID, fetchParts: Boolean): Future[\/[ErrorUnion#Fail, Project]]
  def find(projectSlug: String, userId: UUID, fetchParts: Boolean): Future[\/[ErrorUnion#Fail, Project]]

  def userHasProject(userId: UUID, projectSlug: String): Future[\/[ErrorUnion#Fail, Boolean]]

  def create(courseId: UUID, name: String, slug: String, description: String, availability: String): Future[\/[ErrorUnion#Fail, Project]]
  def updateInfo(id: UUID, version: Long, courseId: Option[UUID], name: Option[String], description: Option[String], availability: Option[String]): Future[\/[ErrorUnion#Fail, Project]]
  def updateSlug(id: UUID, version: Long, slug: String): Future[\/[ErrorUnion#Fail, Project]]
  def delete(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, Project]]

  // Parts
  def listPartsInComponent(componentId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Part]]]
  def findPart(partId: UUID, fetchTasks: Boolean = true): Future[\/[ErrorUnion#Fail, Part]]
  def createPart(projectId: UUID, name: String, position: Int, id: UUID = UUID.random): Future[\/[ErrorUnion#Fail, Part]]
  def updatePart(partId: UUID, version: Long, name: Option[String], position: Option[Int], enabled: Option[Boolean]): Future[\/[ErrorUnion#Fail, Part]]
  def deletePart(partId: UUID, version: Long): Future[\/[ErrorUnion#Fail, Part]]
  def reorderParts(projectId: UUID, partIds: IndexedSeq[UUID]): Future[\/[ErrorUnion#Fail, Project]]

  def togglePart(partId: UUID, version: Long): Future[\/[ErrorUnion#Fail, Part]]

  // Tasks
  def findTask(taskId: UUID): Future[\/[ErrorUnion#Fail, Task]]
  def findTask(projectSlug: String, partNum: Int, taskNum: Int): Future[\/[ErrorUnion#Fail, Task]]
  def findNowTask(userId: UUID, projectId: UUID): Future[\/[ErrorUnion#Fail, Task]]

  def createTask(partId: UUID, taskType: Int, name: String, description: String, position: Int, dependencyId: Option[UUID] = None): Future[\/[ErrorUnion#Fail, Task]]

  case class CommonTaskArgs(taskId: UUID,
                            version: Long,
                            name: Option[String],
                            description: Option[String],
                            position: Option[Int],
                            notesAllowed: Option[Boolean],
                            dependencyId: Option[UUID] = None,
                            partId: Option[UUID] = None)

  def updateLongAnswerTask(commonArgs: CommonTaskArgs): Future[\/[ErrorUnion#Fail, Task]]

  def updateShortAnswerTask(commonArgs: CommonTaskArgs,
                            maxLength: Option[Int]): Future[\/[ErrorUnion#Fail, Task]]

  def updateMultipleChoiceTask(commonArgs: CommonTaskArgs,
                               choices: Option[IndexedSeq[String]] = Some(IndexedSeq()),
                               answer: Option[IndexedSeq[Int]] = Some(IndexedSeq()),
                               allowMultiple: Option[Boolean] = Some(false),
                               randomizeChoices: Option[Boolean] = Some(true)): Future[\/[ErrorUnion#Fail, Task]]

  def updateOrderingTask(commonArgs: CommonTaskArgs,
                         elements: Option[IndexedSeq[String]] = Some(IndexedSeq()),
                         answer: Option[IndexedSeq[Int]] = Some(IndexedSeq()),
                         randomizeChoices: Option[Boolean] = Some(true)): Future[\/[ErrorUnion#Fail, Task]]

  def updateMatchingTask(commonArgs: CommonTaskArgs,
                         elementsLeft: Option[IndexedSeq[String]] = Some(IndexedSeq()),
                         elementsRight: Option[IndexedSeq[String]] = Some(IndexedSeq()),
                         answer: Option[IndexedSeq[MatchingTask.Match]] = Some(IndexedSeq()),
                         randomizeChoices: Option[Boolean] = Some(true)): Future[\/[ErrorUnion#Fail, Task]]

  def deleteTask(taskId: UUID, version: Long): Future[\/[ErrorUnion#Fail, Task]]

  def moveTask(partId: UUID, taskId: UUID, newPosition: Int): Future[\/[ErrorUnion#Fail, Task]]
}
