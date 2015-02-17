package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.services.error.ServiceError
import ca.shiftfocus.uuid.UUID
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks.{MatchingTask, Task}
import scala.concurrent.Future
import scalaz.\/

trait ProjectServiceComponent {

  val projectService: ProjectService

  trait ProjectService {
    // Projects
    def list: Future[\/[ServiceError, IndexedSeq[Project]]]
    def list(courseId: UUID): Future[\/[ServiceError, IndexedSeq[Project]]]
    def find(id: UUID): Future[\/[ServiceError, Project]]
    def find(projectSlug: String): Future[\/[ServiceError, Project]]
    def find(projectId: UUID, userId: UUID): Future[\/[ServiceError, Project]]
    def find(projectSlug: String, userId: UUID): Future[\/[ServiceError, Project]]
    def create(courseId: UUID, name: String, slug: String, description: String, availability: String): Future[\/[ServiceError, Project]]
    def update(id: UUID, version: Long, courseId: UUID, name: String, slug: String, description: String, availability: String): Future[\/[ServiceError, Project]]
    def delete(id: UUID, version: Long): Future[\/[ServiceError, Project]]

    def taskGroups(project: Project, user: User): Future[\/[ServiceError, IndexedSeq[TaskGroup]]]

    // Parts
    def listPartsInComponent(componentId: UUID): Future[\/[ServiceError, IndexedSeq[Part]]]
    def findPart(partId: UUID): Future[\/[ServiceError, Part]]
    def createPart(projectId: UUID, name: String, description: String, position: Int): Future[\/[ServiceError, Part]]
    def updatePart(partId: UUID, version: Long, name: String, description: String, position: Int): Future[\/[ServiceError, Part]]
    def deletePart(partId: UUID, version: Long): Future[\/[ServiceError, Part]]
    def reorderParts(projectId: UUID, partIds: IndexedSeq[UUID]): Future[\/[ServiceError, IndexedSeq[Part]]]

    def togglePart(partId: UUID): Future[\/[ServiceError, Part]]

    // Tasks
    def findTask(taskId: UUID): Future[\/[ServiceError, Task]]
    def findTask(projectSlug: String, partNum: Int, taskNum: Int): Future[\/[ServiceError, Task]]
    def findNowTask(userId: UUID, projectId: UUID): Future[\/[ServiceError, Task]]

    def createTask(partId: UUID, taskType: Int, name: String, description: String, position: Int, dependencyId: Option[UUID] = None): Future[\/[ServiceError, Task]]

    def updateLongAnswerTask(taskId: UUID,
                             version: Long,
                             name: String,
                             description: String,
                             position: Int,
                             notesAllowed: Boolean,
                             dependencyId: Option[UUID] = None,
                             partId: Option[UUID] = None): Future[\/[ServiceError, Task]]

    def updateShortAnswerTask(taskId: UUID,
                              version: Long,
                              name: String,
                              description: String,
                              position: Int,
                              notesAllowed: Boolean,
                              maxLength: Int,
                              dependencyId: Option[UUID] = None,
                              partId: Option[UUID] = None): Future[\/[ServiceError, Task]]

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
                                 partId: Option[UUID] = None): Future[\/[ServiceError, Task]]

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
                           partId: Option[UUID] = None): Future[\/[ServiceError, Task]]

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
                           partId: Option[UUID] = None): Future[\/[ServiceError, Task]]

    def deleteTask(taskId: UUID, version: Long): Future[\/[ServiceError, Task]]

    def moveTask(partId: UUID, taskId: UUID, newPosition: Int): Future[\/[ServiceError, Task]]
  }
}
