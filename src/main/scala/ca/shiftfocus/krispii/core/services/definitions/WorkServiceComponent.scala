package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.lib.UUID
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.work._
import ca.shiftfocus.krispii.core.models.tasks.MatchingTask.Match
import scala.concurrent.Future

/**
 * The WorkService provides an interface to manage student work,
 * including task responses, task notes, and component notes.
 */
trait WorkServiceComponent {

  val workService: WorkService

  trait WorkService {

    // Finder methods for work
    def listWork(userId: UUID, sectionId: UUID, projectId: UUID): Future[IndexedSeq[Work]]
    def listWorkRevisions(userId: UUID, sectionId: UUID, taskId: UUID): Future[IndexedSeq[Work]]
    def findWork(userId: UUID, taskId: UUID, sectionId: UUID): Future[Option[Work]]
    def findWork(userId: UUID, taskId: UUID, sectionId: UUID, revision: Long): Future[Option[Work]]

    // Create methods for each work type
    def createLongAnswerWork(userId: UUID, taskId: UUID, sectionId: UUID, answer: String, isComplete: Boolean): Future[LongAnswerWork]
    def createShortAnswerWork(userId: UUID, taskId: UUID, sectionId: UUID, answer: String, isComplete: Boolean): Future[ShortAnswerWork]
    def createMultipleChoiceWork(userId: UUID, taskId: UUID, sectionId: UUID, answer: IndexedSeq[Int], isComplete: Boolean): Future[MultipleChoiceWork]
    def createOrderingWork(userId: UUID, taskId: UUID, sectionId: UUID, answer: IndexedSeq[Int], isComplete: Boolean): Future[OrderingWork]
    def createMatchingWork(userId: UUID, taskId: UUID, sectionId: UUID, answer: IndexedSeq[Match], isComplete: Boolean): Future[MatchingWork]

    // Update methods for each work type
    def updateLongAnswerWork(userId: UUID, taskId: UUID, sectionId: UUID, revision: Long, version: Long, answer: String, isComplete: Boolean): Future[LongAnswerWork]
    def updateLongAnswerWork(userId: UUID, taskId: UUID, sectionId: UUID, revision: Long, version: Long, answer: String, isComplete: Boolean, newRevision: Boolean): Future[LongAnswerWork]
    def updateShortAnswerWork(userId: UUID, taskId: UUID, sectionId: UUID, revision: Long, version: Long, answer: String, isComplete: Boolean): Future[ShortAnswerWork]
    def updateMultipleChoiceWork(userId: UUID, taskId: UUID, sectionId: UUID, revision: Long, version: Long, answer: IndexedSeq[Int], isComplete: Boolean): Future[MultipleChoiceWork]
    def updateOrderingWork(userId: UUID, taskId: UUID, sectionId: UUID, revision: Long, version: Long, answer: IndexedSeq[Int], isComplete: Boolean): Future[OrderingWork]
    def updateMatchingWork(userId: UUID, taskId: UUID, sectionId: UUID, revision: Long, version: Long, answer: IndexedSeq[Match], isComplete: Boolean): Future[MatchingWork]


    // Task feedbacks
    def listFeedbacks(studentId: UUID, projectId: UUID): Future[IndexedSeq[TaskFeedback]]
    def listFeedbacks(teacherId: UUID, studentId: UUID, projectId: UUID): Future[IndexedSeq[TaskFeedback]]
    def findFeedback(teacherId: UUID, studentId: UUID, taskId: UUID): Future[Option[TaskFeedback]]
    def findFeedback(teacherId: UUID, studentId: UUID, taskId: UUID, revision: Long): Future[Option[TaskFeedback]]

    def createFeedback(teacherId: UUID, studentId: UUID, taskId: UUID, content: String): Future[TaskFeedback]
    def updateFeedback(teacherId: UUID, studentId: UUID, taskId: UUID, revision: Long, version: Long, content: String): Future[TaskFeedback]
    def updateFeedback(teacherId: UUID, studentId: UUID, taskId: UUID, revision: Long, version: Long, content: String, newRevision: Boolean): Future[TaskFeedback]


    // Task notes
    def listTaskScratchpads(userId: UUID, projectId: UUID): Future[IndexedSeq[TaskScratchpad]]
    def listTaskScratchpadRevisions(userId: UUID, taskId: UUID): Future[IndexedSeq[TaskScratchpad]]
    def findTaskScratchpad(userId: UUID, taskId: UUID): Future[Option[TaskScratchpad]]
    def findTaskScratchpad(userId: UUID, taskId: UUID, revision: Long): Future[Option[TaskScratchpad]]

    def createTaskScratchpad(userId: UUID, taskId: UUID, content: String): Future[TaskScratchpad]
    def updateTaskScratchpad(userId: UUID, taskId: UUID, revision: Long, version: Long, content: String): Future[TaskScratchpad]
    def updateTaskScratchpad(userId: UUID, taskId: UUID, revision: Long, version: Long, content: String, newRevision: Boolean): Future[TaskScratchpad]

    // Component notes
    def listComponentScratchpads(userId: UUID, projectId: UUID): Future[IndexedSeq[ComponentScratchpad]]
    def listComponentScratchpadRevisions(userId: UUID, componentId: UUID): Future[IndexedSeq[ComponentScratchpad]]
    def findComponentScratchpad(userId: UUID, componentId: UUID): Future[Option[ComponentScratchpad]]
    def findComponentScratchpad(userId: UUID, componentId: UUID, revision: Long): Future[Option[ComponentScratchpad]]

    def createComponentScratchpad(userId: UUID, componentId: UUID, content: String): Future[ComponentScratchpad]
    def updateComponentScratchpad(userId: UUID, componentId: UUID, revision: Long, version: Long, content: String): Future[ComponentScratchpad]
    def updateComponentScratchpad(userId: UUID, componentId: UUID, revision: Long, version: Long, content: String, newRevision: Boolean): Future[ComponentScratchpad]

  }
}
