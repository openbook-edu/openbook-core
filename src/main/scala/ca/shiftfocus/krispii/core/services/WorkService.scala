package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.repositories.{ TaskScratchpadRepository, TaskFeedbackRepository, WorkRepository }
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.work._
import java.util.UUID
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

  // Finder methods for work
  def listWork(userId: UUID, projectId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Work]]]
  def listWork(taskId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Work]]]
  def listWorkRevisions(userId: UUID, taskId: UUID): Future[\/[ErrorUnion#Fail, Either[DocumentWork, IndexedSeq[QuestionWork]]]]
  def findWork(workId: UUID): Future[\/[ErrorUnion#Fail, Work]]
  def findWork(userId: UUID, taskId: UUID): Future[\/[ErrorUnion#Fail, Work]]
  def findWork(userId: UUID, taskId: UUID, version: Long): Future[\/[ErrorUnion#Fail, Work]]

  def createDocumentWork(userId: UUID, taskId: UUID, isComplete: Boolean): Future[\/[ErrorUnion#Fail, DocumentWork]]
  def createQuestionWork(userId: UUID, taskId: UUID, isComplete: Boolean): Future[\/[ErrorUnion#Fail, QuestionWork]]
  def createMediaWork(userId: UUID, taskId: UUID, isComplete: Boolean): Future[\/[ErrorUnion#Fail, MediaWork]]

  def updateDocumentWork(userId: UUID, taskId: UUID, isComplete: Boolean, grade: Option[String] = None): Future[\/[ErrorUnion#Fail, DocumentWork]]
  def updateQuestionWork(
    userId: UUID,
    taskId: UUID,
    version: Long,
    answers: Option[Answers] = None,
    isComplete: Option[Boolean] = None,
    grade: Option[String] = None
  ): Future[\/[ErrorUnion#Fail, QuestionWork]]
  def updateMediaWork(
    userId: UUID,
    taskId: UUID,
    version: Long,
    fileData: Option[MediaAnswer] = None,
    isComplete: Option[Boolean] = None,
    grade: Option[String] = None
  ): Future[\/[ErrorUnion#Fail, MediaWork]]
  def updateAnswer(workId: UUID, version: Long, questionId: UUID, answer: Answer): Future[\/[ErrorUnion#Fail, QuestionWork]]

  def deleteWork(taskId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Work]]]

  // Task feedbacks
  def listFeedbacks(studentId: UUID, projectId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[TaskFeedback]]]
  def listFeedbacks(taskId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[TaskFeedback]]]
  def findFeedback(studentId: UUID, taskId: UUID): Future[\/[ErrorUnion#Fail, TaskFeedback]]
  def createFeedback(studentId: UUID, taskId: UUID): Future[\/[ErrorUnion#Fail, TaskFeedback]]
  def deleteFeedback(taskId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[TaskFeedback]]]

  // Task notes
  def listTaskScratchpads(userId: UUID, projectId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[TaskScratchpad]]]
  def findTaskScratchpad(userId: UUID, taskId: UUID): Future[\/[ErrorUnion#Fail, TaskScratchpad]]
  def createTaskScratchpad(userId: UUID, taskId: UUID): Future[\/[ErrorUnion#Fail, TaskScratchpad]]

  // Project notes
  def listProjectScratchpads(userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[ProjectScratchpad]]]
  def findProjectScratchpad(userId: UUID, projectId: UUID): Future[\/[ErrorUnion#Fail, ProjectScratchpad]]
  def createProjectScratchpad(userId: UUID, projectId: UUID): Future[\/[ErrorUnion#Fail, ProjectScratchpad]]

  // ########## GOOGLE FILES ###########################################################################################
  def getGfile(gFileId: UUID): Future[\/[ErrorUnion#Fail, Gfile]]
  def createGfile(
    workId: UUID,
    fileId: String,
    mimeType: String,
    fileType: String,
    fileName: String,
    embedUrl: String,
    url: String,
    sharedEmail: Option[String]
  ): Future[\/[ErrorUnion#Fail, Work]]
  def updateGfile(gFileId: UUID, sharedEmail: Option[Option[String]], permissionId: Option[Option[String]], revisionId: Option[Option[String]]): Future[\/[ErrorUnion#Fail, Work]]
  def deleteGfile(gFileId: UUID): Future[\/[ErrorUnion#Fail, Work]]
}
