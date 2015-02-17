package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.services.error.ServiceError
import ca.shiftfocus.uuid.UUID
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.work._
import ca.shiftfocus.krispii.core.models.tasks.MatchingTask.Match
import scala.concurrent.Future
import scalaz.\/

/**
 * The WorkService provides an interface to manage student work,
 * including task responses, task notes, and component notes.
 */
trait WorkServiceComponent {

  val workService: WorkService

  trait WorkService {

    // Finder methods for work
    def listWork(userId: UUID, courseId: UUID, projectId: UUID): Future[\/[ServiceError, IndexedSeq[Work]]]
    def listWorkRevisions(userId: UUID, courseId: UUID, taskId: UUID): Future[\/[ServiceError, IndexedSeq[Work]]]
    def findWork(workId: UUID): Future[\/[ServiceError, Work]]
    def findWork(userId: UUID, taskId: UUID, courseId: UUID): Future[\/[ServiceError, Work]]
    def findWork(userId: UUID, taskId: UUID, courseId: UUID, revision: Long): Future[\/[ServiceError, Work]]


    // Create methods for the textual work types
    def createLongAnswerWork(userId: UUID, taskId: UUID, courseId: UUID, isComplete: Boolean): Future[\/[ServiceError, LongAnswerWork]]
    def createShortAnswerWork(userId: UUID, taskId: UUID, courseId: UUID, isComplete: Boolean): Future[\/[ServiceError, ShortAnswerWork]]

    // Create methods for the other work types
    def createMultipleChoiceWork(userId: UUID, taskId: UUID, courseId: UUID, answer: IndexedSeq[Int], isComplete: Boolean): Future[\/[ServiceError, MultipleChoiceWork]]
    def createOrderingWork(userId: UUID, taskId: UUID, courseId: UUID, answer: IndexedSeq[Int], isComplete: Boolean): Future[\/[ServiceError, OrderingWork]]
    def createMatchingWork(userId: UUID, taskId: UUID, courseId: UUID, answer: IndexedSeq[Match], isComplete: Boolean): Future[\/[ServiceError, MatchingWork]]


    // Update methods for the textual work types
    def updateLongAnswerWork(userId: UUID, taskId: UUID, courseId: UUID, isComplete: Boolean): Future[\/[ServiceError, LongAnswerWork]]
    def updateShortAnswerWork(userId: UUID, taskId: UUID, courseId: UUID, isComplete: Boolean): Future[\/[ServiceError, ShortAnswerWork]]

    // Update methods for the other work types
    def updateMultipleChoiceWork(userId: UUID, taskId: UUID, courseId: UUID, revision: Long, version: Long, answer: IndexedSeq[Int], isComplete: Boolean): Future[\/[ServiceError, MultipleChoiceWork]]
    def updateOrderingWork(userId: UUID, taskId: UUID, courseId: UUID, revision: Long, version: Long, answer: IndexedSeq[Int], isComplete: Boolean): Future[\/[ServiceError, OrderingWork]]
    def updateMatchingWork(userId: UUID, taskId: UUID, courseId: UUID, revision: Long, version: Long, answer: IndexedSeq[Match], isComplete: Boolean): Future[\/[ServiceError, MatchingWork]]


    // Task feedbacks
    def listFeedbacks(studentId: UUID, projectId: UUID): Future[\/[ServiceError, IndexedSeq[TaskFeedback]]]
    def listFeedbacks(teacherId: UUID, studentId: UUID, projectId: UUID): Future[\/[ServiceError, IndexedSeq[TaskFeedback]]]
    def findFeedback(teacherId: UUID, studentId: UUID, taskId: UUID): Future[\/[ServiceError, TaskFeedback]]
    def findFeedback(teacherId: UUID, studentId: UUID, taskId: UUID, revision: Long): Future[\/[ServiceError, TaskFeedback]]

    def createFeedback(teacherId: UUID, studentId: UUID, taskId: UUID, content: String): Future[\/[ServiceError, TaskFeedback]]
    def updateFeedback(teacherId: UUID, studentId: UUID, taskId: UUID, revision: Long, version: Long, content: String): Future[\/[ServiceError, TaskFeedback]]
    def updateFeedback(teacherId: UUID, studentId: UUID, taskId: UUID, revision: Long, version: Long, content: String, newRevision: Boolean): Future[\/[ServiceError, TaskFeedback]]


    // Task notes
    def listTaskScratchpads(userId: UUID, projectId: UUID): Future[\/[ServiceError, IndexedSeq[TaskScratchpad]]]
    def listTaskScratchpadRevisions(userId: UUID, taskId: UUID): Future[\/[ServiceError, IndexedSeq[TaskScratchpad]]]
    def findTaskScratchpad(userId: UUID, taskId: UUID): Future[\/[ServiceError, TaskScratchpad]]
    def findTaskScratchpad(userId: UUID, taskId: UUID, revision: Long): Future[\/[ServiceError, TaskScratchpad]]

    def createTaskScratchpad(userId: UUID, taskId: UUID, content: String): Future[\/[ServiceError, TaskScratchpad]]
    def updateTaskScratchpad(userId: UUID, taskId: UUID, revision: Long, version: Long, content: String): Future[\/[ServiceError, TaskScratchpad]]
    def updateTaskScratchpad(userId: UUID, taskId: UUID, revision: Long, version: Long, content: String, newRevision: Boolean): Future[\/[ServiceError, TaskScratchpad]]

    // Component notes
    def listComponentScratchpads(userId: UUID, projectId: UUID): Future[\/[ServiceError, IndexedSeq[ComponentScratchpad]]]
    def listComponentScratchpadRevisions(userId: UUID, componentId: UUID): Future[\/[ServiceError, IndexedSeq[ComponentScratchpad]]]
    def findComponentScratchpad(userId: UUID, componentId: UUID): Future[\/[ServiceError, ComponentScratchpad]]
    def findComponentScratchpad(userId: UUID, componentId: UUID, revision: Long): Future[\/[ServiceError, ComponentScratchpad]]

    def createComponentScratchpad(userId: UUID, componentId: UUID, content: String): Future[\/[ServiceError, ComponentScratchpad]]
    def updateComponentScratchpad(userId: UUID, componentId: UUID, revision: Long, version: Long, content: String): Future[\/[ServiceError, ComponentScratchpad]]
    def updateComponentScratchpad(userId: UUID, componentId: UUID, revision: Long, version: Long, content: String, newRevision: Boolean): Future[\/[ServiceError, ComponentScratchpad]]

  }
}
