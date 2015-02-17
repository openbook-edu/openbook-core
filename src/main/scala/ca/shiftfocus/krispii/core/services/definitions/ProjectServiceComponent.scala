package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.uuid.UUID
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks.{MatchingTask, Task}
import scala.concurrent.Future

trait ProjectServiceComponent {

  val projectService: ProjectService

  trait ProjectService {
    // Projects
    def list: Future[IndexedSeq[Project]]
    def list(courseId: UUID): Future[IndexedSeq[Project]]
    def find(id: UUID): Future[Option[Project]]
    def find(projectSlug: String): Future[Option[Project]]
    def find(projectId: UUID, userId: UUID): Future[Option[Project]]
    def find(projectSlug: String, userId: UUID): Future[Option[Project]]
    def create(courseId: UUID, name: String, slug: String, description: String, availability: String): Future[Project]
    def update(id: UUID, version: Long, courseId: UUID, name: String, slug: String, description: String, availability: String): Future[Project]
    def delete(id: UUID, version: Long): Future[Boolean]

    def taskGroups(project: Project, user: User): Future[IndexedSeq[TaskGroup]]

    // Parts
    def listPartsInComponent(componentId: UUID): Future[IndexedSeq[Part]]
    def findPart(partId: UUID): Future[Option[Part]]
    def createPart(projectId: UUID, name: String, description: String, position: Int): Future[Part]
    def updatePart(partId: UUID, version: Long, name: String, description: String, position: Int): Future[Part]
    def deletePart(partId: UUID, version: Long): Future[Boolean]
    def reorderParts(projectId: UUID, partIds: IndexedSeq[UUID]): Future[Project]

    // Tasks
    def findTask(taskId: UUID): Future[Option[Task]]
    def findTask(projectSlug: String, partNum: Int, taskNum: Int): Future[Option[Task]]
    def findNowTask(userId: UUID, projectId: UUID): Future[Option[Task]]

    def createTask(partId: UUID, taskType: Int, name: String, description: String, position: Int, dependencyId: Option[UUID] = None): Future[Task]

    def updateLongAnswerTask(taskId: UUID,
                             version: Long,
                             name: String,
                             description: String,
                             position: Int,
                             notesAllowed: Boolean,
                             dependencyId: Option[UUID] = None,
                             partId: Option[UUID] = None): Future[Task]

    def updateShortAnswerTask(taskId: UUID,
                              version: Long,
                              name: String,
                              description: String,
                              position: Int,
                              notesAllowed: Boolean,
                              maxLength: Int,
                              dependencyId: Option[UUID] = None,
                              partId: Option[UUID] = None): Future[Task]

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
                                 partId: Option[UUID] = None): Future[Task]

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
                           partId: Option[UUID] = None): Future[Task]

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
                           partId: Option[UUID] = None): Future[Task]

    def deleteTask(taskId: UUID, version: Long): Future[Boolean]

    def moveTask(partId: UUID, taskId: UUID, newPosition: Int): Future[Task]
  }
}
