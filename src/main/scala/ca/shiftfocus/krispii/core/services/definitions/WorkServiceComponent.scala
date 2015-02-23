package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.fail._
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
    def listWork(userId: UUID, projectId: UUID): Future[\/[Fail, IndexedSeq[Work]]]
    def listWork(taskId: UUID): Future[\/[Fail, IndexedSeq[Work]]]
    def listWorkRevisions(userId: UUID, taskId: UUID): Future[\/[Fail, IndexedSeq[Work]]]
    def findWork(workId: UUID): Future[\/[Fail, Work]]
    def findWork(userId: UUID, taskId: UUID): Future[\/[Fail, Work]]
    def findWork(userId: UUID, taskId: UUID, version: Long): Future[\/[Fail, Work]]


    // Create methods for the textual work types
    def createLongAnswerWork(userId: UUID, taskId: UUID, isComplete: Boolean): Future[\/[Fail, LongAnswerWork]]
    def createShortAnswerWork(userId: UUID, taskId: UUID, isComplete: Boolean): Future[\/[Fail, ShortAnswerWork]]

    // Create methods for the other work types
    def createMultipleChoiceWork(userId: UUID, taskId: UUID, answer: IndexedSeq[Int], isComplete: Boolean): Future[\/[Fail, MultipleChoiceWork]]
    def createOrderingWork(userId: UUID, taskId: UUID, answer: IndexedSeq[Int], isComplete: Boolean): Future[\/[Fail, OrderingWork]]
    def createMatchingWork(userId: UUID, taskId: UUID, answer: IndexedSeq[Match], isComplete: Boolean): Future[\/[Fail, MatchingWork]]


    // Update methods for the textual work types
    def updateLongAnswerWork(userId: UUID, taskId: UUID, isComplete: Boolean): Future[\/[Fail, LongAnswerWork]]
    def updateShortAnswerWork(userId: UUID, taskId: UUID, isComplete: Boolean): Future[\/[Fail, ShortAnswerWork]]

    // Update methods for the other work types
    def updateMultipleChoiceWork(userId: UUID, taskId: UUID, version: Long, answer: IndexedSeq[Int], isComplete: Boolean): Future[\/[Fail, MultipleChoiceWork]]
    def updateOrderingWork(userId: UUID, taskId: UUID, version: Long, answer: IndexedSeq[Int], isComplete: Boolean): Future[\/[Fail, OrderingWork]]
    def updateMatchingWork(userId: UUID, taskId: UUID, version: Long, answer: IndexedSeq[Match], isComplete: Boolean): Future[\/[Fail, MatchingWork]]

    def forceComplete(taskId: UUID, includingPrevious: Boolean = false): Future[\/[Fail, Unit]]


    // Task feedbacks
    def listFeedbacks(studentId: UUID, projectId: UUID): Future[\/[Fail, IndexedSeq[TaskFeedback]]]
    def listFeedbacks(taskId: UUID): Future[\/[Fail, IndexedSeq[TaskFeedback]]]
    def findFeedback(studentId: UUID, taskId: UUID): Future[\/[Fail, TaskFeedback]]
    def createFeedback(studentId: UUID, taskId: UUID): Future[\/[Fail, TaskFeedback]]
    def updateFeedback(studentId: UUID, taskId: UUID, version: Long, documentId: UUID): Future[\/[Fail, TaskFeedback]]


    // Task notes
    def listTaskScratchpads(userId: UUID, projectId: UUID): Future[\/[Fail, IndexedSeq[TaskScratchpad]]]
    def findTaskScratchpad(userId: UUID, taskId: UUID): Future[\/[Fail, TaskScratchpad]]
    def createTaskScratchpad(userId: UUID, taskId: UUID): Future[\/[Fail, TaskScratchpad]]
    def updateTaskScratchpad(userId: UUID, taskId: UUID, version: Long, documentId: UUID): Future[\/[Fail, TaskScratchpad]]


    // Component notes
    def listComponentScratchpadsByUser(userId: UUID): Future[\/[Fail, IndexedSeq[ComponentScratchpad]]]
    def listComponentScratchpadsByComponent(componentId: UUID): Future[\/[Fail, IndexedSeq[ComponentScratchpad]]]
    def findComponentScratchpad(userId: UUID, componentId: UUID): Future[\/[Fail, ComponentScratchpad]]
    def createComponentScratchpad(userId: UUID, componentId: UUID): Future[\/[Fail, ComponentScratchpad]]
    def updateComponentScratchpad(userId: UUID, componentId: UUID, version: Long, documentId: UUID): Future[\/[Fail, ComponentScratchpad]]

  }
}
