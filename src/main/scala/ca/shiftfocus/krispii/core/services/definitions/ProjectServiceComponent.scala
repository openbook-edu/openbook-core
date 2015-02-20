package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.fail._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks._
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future
import scalaz.\/

trait ProjectServiceComponent {

  val projectService: ProjectService

  trait ProjectService {
    // Projects
    def list: Future[\/[Fail, IndexedSeq[Project]]]
    def list(courseId: UUID): Future[\/[Fail, IndexedSeq[Project]]]
    def find(id: UUID): Future[\/[Fail, Project]]
    def find(projectSlug: String): Future[\/[Fail, Project]]
    def find(projectId: UUID, userId: UUID): Future[\/[Fail, Project]]
    def find(projectSlug: String, userId: UUID): Future[\/[Fail, Project]]

    def create(courseId: UUID, name: String, slug: String, description: String, availability: String): Future[\/[Fail, Project]]
    def updateInfo(id: UUID, version: Long, courseId: Option[UUID], name: Option[String], description: Option[String], availability: Option[String]): Future[\/[Fail, Project]]
    def updateSlug(id: UUID, version: Long, slug: String): Future[\/[Fail, Project]]
    def delete(id: UUID, version: Long): Future[\/[Fail, Project]]

    def taskGroups(project: Project, user: User): Future[\/[Fail, IndexedSeq[TaskGroup]]]

    // Parts
    def listPartsInComponent(componentId: UUID): Future[\/[Fail, IndexedSeq[Part]]]
    def findPart(partId: UUID): Future[\/[Fail, Part]]
    def createPart(projectId: UUID, name: String, description: String, position: Int): Future[\/[Fail, Part]]
    def updatePart(partId: UUID, version: Long, name: String, position: Int): Future[\/[Fail, Part]]
    def deletePart(partId: UUID, version: Long): Future[\/[Fail, Part]]
    def reorderParts(projectId: UUID, partIds: IndexedSeq[UUID]): Future[\/[Fail, IndexedSeq[Part]]]

    def togglePart(partId: UUID, version: Long): Future[\/[Fail, Part]]

    // Tasks
    def findTask(taskId: UUID): Future[\/[Fail, Task]]
    def findTask(projectSlug: String, partNum: Int, taskNum: Int): Future[\/[Fail, Task]]
    def findNowTask(userId: UUID, projectId: UUID): Future[\/[Fail, Task]]

    def createTask(partId: UUID, taskType: Int, name: String, description: String, position: Int, dependencyId: Option[UUID] = None): Future[\/[Fail, Task]]

    def updateLongAnswerTask(taskId: UUID,
                             version: Long,
                             name: String,
                             description: String,
                             position: Int,
                             notesAllowed: Boolean,
                             dependencyId: Option[UUID] = None,
                             partId: Option[UUID] = None): Future[\/[Fail, Task]]

    def updateShortAnswerTask(taskId: UUID,
                              version: Long,
                              name: String,
                              description: String,
                              position: Int,
                              notesAllowed: Boolean,
                              maxLength: Int,
                              dependencyId: Option[UUID] = None,
                              partId: Option[UUID] = None): Future[\/[Fail, Task]]

    def updateMultipleChoiceTask(taskId: UUID,
                                 version: Long,
                                 name: String,
                                 description: String,
                                 position: Int,
                                 notesAllowed: Boolean,
                                 choices: IndexedSeq[String] = IndexedSeq(),
                                 answer: IndexedSeq[Int] = IndexedSeq(),
                                 allowMultiple: Boolean = false,
                                 randomizeChoices: Boolean = true,
                                 dependencyId: Option[UUID] = None,
                                 partId: Option[UUID] = None): Future[\/[Fail, Task]]

    def updateOrderingTask(taskId: UUID,
                           version: Long,
                           name: String,
                           description: String,
                           position: Int,
                           notesAllowed: Boolean,
                           elements: IndexedSeq[String] = IndexedSeq(),
                           answer: IndexedSeq[Int] = IndexedSeq(),
                           randomizeChoices: Boolean = true,
                           dependencyId: Option[UUID] = None,
                           partId: Option[UUID] = None): Future[\/[Fail, Task]]

    def updateMatchingTask(taskId: UUID,
                           version: Long,
                           name: String,
                           description: String,
                           position: Int,
                           notesAllowed: Boolean,
                           elementsLeft: IndexedSeq[String] = IndexedSeq(),
                           elementsRight: IndexedSeq[String] = IndexedSeq(),
                           answer: IndexedSeq[MatchingTask.Match] = IndexedSeq(),
                           randomizeChoices: Boolean = true,
                           dependencyId: Option[UUID] = None,
                           partId: Option[UUID] = None): Future[\/[Fail, Task]]

    def deleteTask(taskId: UUID, version: Long): Future[\/[Fail, Task]]

    def moveTask(partId: UUID, taskId: UUID, newPosition: Int): Future[\/[Fail, Task]]
  }
}
