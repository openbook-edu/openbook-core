package com.shiftfocus.krispii.core.services

import com.shiftfocus.krispii.core.lib.UUID
import com.shiftfocus.krispii.core.models._
import scala.concurrent.Future

/**
 * The WorkService provides an interface to manage student work,
 * including task responses, task notes, and component notes.
 */
trait WorkServiceComponent {

  val workService: WorkService

  trait WorkService {

    // Task responses
    def listResponses(userId: UUID, projectId: UUID): Future[IndexedSeq[TaskResponse]]
    def listResponseRevisions(userId: UUID, taskId: UUID): Future[IndexedSeq[TaskResponse]]
    def findResponse(userId: UUID, taskId: UUID): Future[Option[TaskResponse]]
    def findResponse(userId: UUID, taskId: UUID, revision: Long): Future[Option[TaskResponse]]

    def createResponse(userId: UUID, taskId: UUID, content: String, isComplete: Boolean): Future[TaskResponse]
    def updateResponse(userId: UUID, taskId: UUID, revision: Long, version: Long, content: String, isComplete: Boolean): Future[TaskResponse]
    def updateResponse(userId: UUID, taskId: UUID, revision: Long, version: Long, content: String, isComplete: Boolean, newRevision: Boolean): Future[TaskResponse]

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
