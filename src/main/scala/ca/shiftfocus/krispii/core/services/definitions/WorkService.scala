package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models.tasks.MatchingTask
import ca.shiftfocus.krispii.core.repositories.{ComponentScratchpadRepository, TaskScratchpadRepository, TaskFeedbackRepository, WorkRepository}
import ca.shiftfocus.uuid.UUID
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.work._
import ca.shiftfocus.krispii.core.models.tasks.MatchingTask.Match
import scala.concurrent.Future
import scalaz.\/

trait WorkService extends Service[ErrorUnion#Fail] {
  val authService: AuthService
  val schoolService: SchoolService
  val projectService: ProjectService
  val documentService: DocumentService
  val componentService: ComponentService
  val workRepository: WorkRepository
  val taskFeedbackRepository: TaskFeedbackRepository
  val taskScratchpadRepository: TaskScratchpadRepository
  val componentScratchpadRepository: ComponentScratchpadRepository

  // Finder methods for work
  def listWork(userId: UUID, projectId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Work]]]
  def listWork(taskId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Work]]]
  def listWorkRevisions(userId: UUID, taskId: UUID): Future[\/[ErrorUnion#Fail, Either[DocumentWork, IndexedSeq[ListWork[_ >: Int with MatchingTask.Match]]]]]
  def findWork(workId: UUID): Future[\/[ErrorUnion#Fail, Work]]
  def findWork(userId: UUID, taskId: UUID): Future[\/[ErrorUnion#Fail, Work]]
  def findWork(userId: UUID, taskId: UUID, version: Long): Future[\/[ErrorUnion#Fail, Work]]


  // Create methods for the textual work types
  def createLongAnswerWork(userId: UUID, taskId: UUID, isComplete: Boolean): Future[\/[ErrorUnion#Fail, LongAnswerWork]]
  def createShortAnswerWork(userId: UUID, taskId: UUID, isComplete: Boolean): Future[\/[ErrorUnion#Fail, ShortAnswerWork]]

  // Create methods for the other work types
  def createMultipleChoiceWork(userId: UUID, taskId: UUID, isComplete: Boolean): Future[\/[ErrorUnion#Fail, MultipleChoiceWork]]
  def createOrderingWork(userId: UUID, taskId: UUID, isComplete: Boolean): Future[\/[ErrorUnion#Fail, OrderingWork]]
  def createMatchingWork(userId: UUID, taskId: UUID, isComplete: Boolean): Future[\/[ErrorUnion#Fail, MatchingWork]]


  // Update methods for the textual work types
  def updateLongAnswerWork(userId: UUID, taskId: UUID, isComplete: Boolean): Future[\/[ErrorUnion#Fail, LongAnswerWork]]
  def updateShortAnswerWork(userId: UUID, taskId: UUID, isComplete: Boolean): Future[\/[ErrorUnion#Fail, ShortAnswerWork]]

  // Update methods for the other work types
  def updateMultipleChoiceWork(userId: UUID, taskId: UUID, version: Long, answer: IndexedSeq[Int], isComplete: Boolean): Future[\/[ErrorUnion#Fail, MultipleChoiceWork]]
  def updateOrderingWork(userId: UUID, taskId: UUID, version: Long, answer: IndexedSeq[Int], isComplete: Boolean): Future[\/[ErrorUnion#Fail, OrderingWork]]
  def updateMatchingWork(userId: UUID, taskId: UUID, version: Long, answer: IndexedSeq[Match], isComplete: Boolean): Future[\/[ErrorUnion#Fail, MatchingWork]]

//  def forceComplete(taskId: UUID, includingPrevious: Boolean = false): Future[\/[ErrorUnion#Fail, Unit]]


  // Task feedbacks
  def listFeedbacks(studentId: UUID, projectId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[TaskFeedback]]]
  def listFeedbacks(taskId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[TaskFeedback]]]
  def findFeedback(studentId: UUID, taskId: UUID): Future[\/[ErrorUnion#Fail, TaskFeedback]]
  def createFeedback(studentId: UUID, taskId: UUID): Future[\/[ErrorUnion#Fail, TaskFeedback]]


  // Task notes
  def listTaskScratchpads(userId: UUID, projectId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[TaskScratchpad]]]
  def findTaskScratchpad(userId: UUID, taskId: UUID): Future[\/[ErrorUnion#Fail, TaskScratchpad]]
  def createTaskScratchpad(userId: UUID, taskId: UUID): Future[\/[ErrorUnion#Fail, TaskScratchpad]]

}