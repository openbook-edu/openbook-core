package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks.MatchingTask
import ca.shiftfocus.krispii.core.models.tasks.MatchingTask.Match
import ca.shiftfocus.krispii.core.models.work._
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services.datasource._
import java.util.UUID
import com.github.mauricio.async.db.Connection
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.{ \/, -\/, \/- }
import ws.kahn.ot.Delta

class WorkServiceDefault(
  val db: DB,
  val authService: AuthService,
  val schoolService: SchoolService,
  val projectService: ProjectService,
  val documentService: DocumentService,
  val componentService: ComponentService,
  val workRepository: WorkRepository,
  val taskFeedbackRepository: TaskFeedbackRepository,
  val taskScratchpadRepository: TaskScratchpadRepository
)
    extends WorkService {

  implicit def conn: Connection = db.pool

  /**
   * List the latest revision of all of a user's work in a project for a specific
   * course.
   *
   * @param userId the unique id of the user to filter by
   * @param projectId the unique id of the project to filter by
   * @return a future disjunction containing either a list of work, or a failure
   */
  override def listWork(userId: UUID, projectId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Work]]] = {
    val fUser = authService.find(userId)
    val fProject = projectService.find(projectId, false)

    for {
      user <- lift(fUser)
      project <- lift(fProject)
      workList <- lift(workRepository.list(user, project))
    } yield workList
  }

  /**
   * List the latest revision of all of a user's work in a project for a specific
   * course.
   *
   * @param taskId
   * @return a future disjunction containing either a list of work, or a failure
   */
  override def listWork(taskId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Work]]] = {
    for {
      task <- lift(projectService.findTask(taskId))
      workList <- lift(workRepository.list(task))
    } yield workList
  }

  /**
   * List all of a user's work revisions for a specific task in a specific course.
   *
   * @param userId the unique id of the user to filter by
   * @param taskId the unique id of the task to filter by
   * @return a future disjunction containing either a list of work, or a failure
   */
  override def listWorkRevisions(userId: UUID, taskId: UUID) // format: OFF
  : Future[\/[ErrorUnion#Fail, Either[DocumentWork, IndexedSeq[ListWork[_ >: Int with MatchingTask.Match]]]]] = { // format: ON
    val fUser = authService.find(userId)
    val fTask = projectService.findTask(taskId)

    for {
      user <- lift(fUser)
      task <- lift(fTask)
      result <- lift(workRepository.list(user, task))
    } yield result
  }

  /**
   * Find the latest revision of a user's work, for a task, in a course.
   *
   * @param workId the unique id of the work to find
   * @return a future disjunction containing either a work, or a failure
   */
  override def findWork(workId: UUID): Future[\/[ErrorUnion#Fail, Work]] = {
    workRepository.find(workId)
  }

  /**
   * Find the latest revision of a user's work, for a task, in a course.
   *
   * @param userId the unique id of the user to filter by
   * @param taskId the unique id of the task to filter by
   * @return a future disjunction containing either a work, or a failure
   */
  override def findWork(userId: UUID, taskId: UUID): Future[\/[ErrorUnion#Fail, Work]] = {
    val fUser = authService.find(userId)
    val fTask = projectService.findTask(taskId)

    for {
      user <- lift(fUser)
      task <- lift(fTask)
      work <- lift(workRepository.find(user, task))
    } yield work
  }

  /**
   * Find a specific revision of a user's work, for a task, in a course.
   *
   * @param userId the unique id of the user to filter by
   * @param taskId the unique id of the task to filter by
   * @param version the version of the work to find
   * @return a future disjunction containing either a work, or a failure
   */
  override def findWork(userId: UUID, taskId: UUID, version: Long): Future[\/[ErrorUnion#Fail, Work]] = {
    val fUser = authService.find(userId)
    val fTask = projectService.findTask(taskId)

    for {
      user <- lift(fUser)
      task <- lift(fTask)
      work <- lift(workRepository.find(user, task, version))
    } yield work
  }

  // --------- Create ---------------------------------------------------------------------------------------------

  /**
   * Generic internal work-creating method. This should only be used privately... expose the type-specific
   * methods to the outside world so that we'll have control over exactly how certain types of work
   * are creted.
   *
   * @param newWork the new work to create
   * @return a future disjunction containing either a work, or a failure
   */
  private def createWork(newWork: Work)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, Work]] = {
    workRepository.insert(newWork)
  }

  // --------- Create methods for each work type --------------------------------------------------------------------

  // TODO - verify if user already has work for this task
  /**
   * Create a long-answer work item.
   *
   * Use this method when entering student work on a task for the first time (in a given course).
   *
   * @param userId the unique id of the user the work belongs to
   * @param taskId the unique id of the task the work is for
   * @param isComplete whether the student is finished with the task
   * @return a future disjunction containing either a work, or a failure
   */
  override def createLongAnswerWork(userId: UUID, taskId: UUID, isComplete: Boolean): Future[\/[ErrorUnion#Fail, LongAnswerWork]] = {
    transactional { implicit conn =>
      val fUser = authService.find(userId)
      val fTask = projectService.findTask(taskId)

      for {
        user <- lift(fUser)
        task <- lift(fTask)
        document <- lift(documentService.create(UUID.randomUUID, user, "", Delta(IndexedSeq())))
        newWork = LongAnswerWork(
          studentId = user.id,
          taskId = task.id,
          documentId = document.id,
          isComplete = isComplete,
          response = Some(document)
        )
        work <- lift(workRepository.insert(newWork))
      } yield work.asInstanceOf[LongAnswerWork]
    }
  }

  /**
   * Create a short-answer work item.
   *
   * Use this method when entering student work on a task for the first time (in a given course).
   *
   * @param userId the id of the student whose work is being entered
   * @param taskId the task for which the work was done
   * @param isComplete whether the student is finished with the task
   * @return the newly created work
   */
  override def createShortAnswerWork(userId: UUID, taskId: UUID, isComplete: Boolean): Future[\/[ErrorUnion#Fail, ShortAnswerWork]] = {
    transactional { implicit conn =>
      val fUser = authService.find(userId)
      val fTask = projectService.findTask(taskId)

      for {
        user <- lift(fUser)
        task <- lift(fTask)
        document <- lift(documentService.create(UUID.randomUUID, user, "", Delta(IndexedSeq())))
        newWork = ShortAnswerWork(
          studentId = user.id,
          taskId = task.id,
          documentId = document.id,
          isComplete = isComplete,
          response = Some(document)
        )
        work <- lift(workRepository.insert(newWork))
      } yield work.asInstanceOf[ShortAnswerWork]
    }
  }

  /**
   * Create a multiple-choice work item.
   *
   * Use this method when entering student work on a task for the first time (in a given course).
   *
   * @param userId the id of the student whose work is being entered
   * @param taskId the task for which the work was done
   * @param response the student's response to the task (this is the actual "work")
   * @param isComplete whether the student is finished with the task
   * @return the newly created work
   */
  override def createMultipleChoiceWork(userId: UUID, taskId: UUID, isComplete: Boolean): Future[\/[ErrorUnion#Fail, MultipleChoiceWork]] = {
    transactional { implicit conn =>
      val fUser = authService.find(userId)
      val fTask = projectService.findTask(taskId)

      for {
        user <- lift(fUser)
        task <- lift(fTask)
        document <- lift(documentService.create(UUID.randomUUID, user, "", Delta(IndexedSeq())))
        newWork = MultipleChoiceWork(
          studentId = userId,
          taskId = taskId,
          version = 1,
          response = IndexedSeq.empty[Int],
          isComplete = isComplete
        )
        work <- lift(workRepository.insert(newWork))
      } yield work.asInstanceOf[MultipleChoiceWork]
    }
  }

  /**
   * Create an ordering work item.
   *
   * Use this method when entering student work on a task for the first time (in a given course).
   *
   * @param userId the id of the student whose work is being entered
   * @param taskId the task for which the work was done
   * @param response the student's response to the task (this is the actual "work")
   * @param isComplete whether the student is finished with the task
   * @return the newly created work
   */
  override def createOrderingWork(userId: UUID, taskId: UUID, isComplete: Boolean): Future[\/[ErrorUnion#Fail, OrderingWork]] = {
    transactional { implicit conn =>
      val fUser = authService.find(userId)
      val fTask = projectService.findTask(taskId)

      for {
        user <- lift(fUser)
        task <- lift(fTask)
        document <- lift(documentService.create(UUID.randomUUID, user, "", Delta(IndexedSeq())))
        newWork = OrderingWork(
          studentId = userId,
          taskId = taskId,
          version = 1,
          response = IndexedSeq.empty[Int],
          isComplete = isComplete
        )
        work <- lift(workRepository.insert(newWork))
      } yield work.asInstanceOf[OrderingWork]
    }
  }

  /**
   * Create a matching work item.
   *
   * Use this method when entering student work on a task for the first time (in a given course).
   *
   * @param userId the id of the student whose work is being entered
   * @param taskId the task for which the work was done
   * @param response the student's response to the task (this is the actual "work")
   * @param isComplete whether the student is finished with the task
   * @return the newly created work
   */
  override def createMatchingWork(userId: UUID, taskId: UUID, isComplete: Boolean): Future[\/[ErrorUnion#Fail, MatchingWork]] = {
    transactional { implicit conn =>
      val fUser = authService.find(userId)
      val fTask = projectService.findTask(taskId)

      for {
        user <- lift(fUser)
        task <- lift(fTask)
        document <- lift(documentService.create(UUID.randomUUID, user, "", Delta(IndexedSeq())))
        newWork = MatchingWork(
          studentId = userId,
          taskId = taskId,
          version = 1,
          response = IndexedSeq.empty[Match],
          isComplete = isComplete
        )
        work <- lift(workRepository.insert(newWork))
      } yield work.asInstanceOf[MatchingWork]
    }
  }

  // --------- Update ------------------------------------------------------------------------------------------

  /**
   * Internal method for updating work.
   *
   * External classes should call the type-specific update methods which can perform any logic that need to
   * for specific work types. For example, the long-response tasks only require new revisions on timed intervals,
   * whereas most other forms of work will store a new revision each time a change is made.
   *
   * Please wrap this method in a transaction and provide it with a connection.
   *
   * @param newerWork
   * @param newRevision
   * @param conn
   * @return
   */
  private def updateWork(newerWork: Work, newRevision: Boolean = true)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, Work]] = {
    val fUser = authService.find(newerWork.studentId)
    val fTask = projectService.findTask(newerWork.taskId)

    for {
      user <- lift(fUser)
      task <- lift(fTask)
      latestRevision <- lift(workRepository.find(user, task))
      updatedWork <- lift(workRepository.update(newerWork, newRevision))
    } yield updatedWork
  }

  // --------- Update methods for each work type --------------------------------------------------------------------

  /**
   * Update a long answer work.
   *
   * Because the contents of the work are handled by the Document service, this method only
   * serves to update the work's completed status.
   */
  def updateLongAnswerWork(userId: UUID, taskId: UUID, isComplete: Boolean): Future[\/[ErrorUnion#Fail, LongAnswerWork]] = {
    transactional { implicit conn =>
      val fUser = authService.find(userId)
      val fTask = projectService.findTask(taskId)

      for {
        user <- lift(fUser)
        task <- lift(fTask)
        existingWork <- lift(workRepository.find(user, task))
        existingLAWork = existingWork.asInstanceOf[LongAnswerWork]
        workToUpdate = existingLAWork.copy(isComplete = isComplete)
        updatedWork <- lift(workRepository.update(workToUpdate))
      } yield updatedWork.asInstanceOf[LongAnswerWork]
    }
  }

  /**
   * Update a short answer work.
   *
   * Because the contents of the work are handled by the Document service, this method only
   * serves to update the work's completed status.
   */
  def updateShortAnswerWork(userId: UUID, taskId: UUID, isComplete: Boolean): Future[\/[ErrorUnion#Fail, ShortAnswerWork]] = {
    transactional { implicit conn =>
      val fUser = authService.find(userId)
      val fTask = projectService.findTask(taskId)

      for {
        user <- lift(fUser)
        task <- lift(fTask)
        existingWork <- lift(workRepository.find(user, task))
        existingSAWork = existingWork.asInstanceOf[ShortAnswerWork]
        workToUpdate = existingSAWork.copy(isComplete = isComplete)
        updatedWork <- lift(workRepository.update(workToUpdate))
      } yield updatedWork.asInstanceOf[ShortAnswerWork]
    }
  }

  override def updateMultipleChoiceWork(
    userId: UUID,
    taskId: UUID,
    version: Long,
    response: IndexedSeq[Int],
    isComplete: Boolean
  ): Future[\/[ErrorUnion#Fail, MultipleChoiceWork]] = {
    transactional { implicit conn =>
      val fUser = authService.find(userId)
      val fTask = projectService.findTask(taskId)

      for {
        user <- lift(fUser)
        task <- lift(fTask)
        existingWork <- lift(workRepository.find(user, task))
        existingMCWork = existingWork.asInstanceOf[MultipleChoiceWork]
        workToUpdate = existingMCWork.copy(response = response, isComplete = isComplete)
        updatedWork <- lift(workRepository.update(workToUpdate, true))
      } yield updatedWork.asInstanceOf[MultipleChoiceWork]
    }
  }

  override def updateOrderingWork(
    userId: UUID,
    taskId: UUID,
    version: Long,
    response: IndexedSeq[Int],
    isComplete: Boolean
  ): Future[\/[ErrorUnion#Fail, OrderingWork]] = {
    transactional { implicit conn =>
      val fUser = authService.find(userId)
      val fTask = projectService.findTask(taskId)

      for {
        user <- lift(fUser)
        task <- lift(fTask)
        existingWork <- lift(workRepository.find(user, task))
        existingOrdWork = existingWork.asInstanceOf[OrderingWork]
        workToUpdate = existingOrdWork.copy(response = response, isComplete = isComplete)
        updatedWork <- lift(workRepository.update(workToUpdate, true))
      } yield updatedWork.asInstanceOf[OrderingWork]
    }
  }

  override def updateMatchingWork(
    userId: UUID,
    taskId: UUID,
    version: Long,
    response: IndexedSeq[Match],
    isComplete: Boolean
  ): Future[\/[ErrorUnion#Fail, MatchingWork]] = {
    transactional { implicit conn =>
      val fUser = authService.find(userId)
      val fTask = projectService.findTask(taskId)

      for {
        user <- lift(fUser)
        task <- lift(fTask)
        existingWork <- lift(workRepository.find(user, task))
        existingMatchingWork = existingWork.asInstanceOf[MatchingWork]
        workToUpdate = existingMatchingWork.copy(response = response, isComplete = isComplete)
        updatedWork <- lift(workRepository.update(workToUpdate, true))
      } yield updatedWork.asInstanceOf[MatchingWork]
    }
  }

  //  override def forceComplete(taskId: UUID, justThis: Boolean = true): Future[\/[ErrorUnion#Fail, Unit]] = {
  //    transactional { implicit conn =>
  //      if (justThis) {
  //        for {
  //          task <- lift(projectService.findTask(taskId))
  //          works <- lift(listWork(task.id))
  //          worksToUpdate = works.map {
  //            case work: LongAnswerWork => work.copy(isComplete = true)
  //            case work: ShortAnswerWork => work.copy(isComplete = true)
  //            case work: MultipleChoiceWork => work.copy(isComplete = true)
  //            case work: OrderingWork => work.copy(isComplete = true)
  //            case work: MatchingWork => work.copy(isComplete = true)
  //          }
  //          updatedWorks <- lift(serializedT(worksToUpdate)(workRepository.update))
  //        } yield ()
  //      }
  //      else {
  //        for {
  //          task <- lift(projectService.findTask(taskId))
  //          part <- lift(projectService.findPart(task.partId, false))
  //          project <- lift(projectService.find(part.projectId, false))
  //          tasks = project.parts.filter(_.position <= part.position)
  //                               .map(_.tasks).flatten
  //                               .filter { task => task.partId != part.id || task.position <= task.position }
  //          worksLists <- liftSeq(tasks.map { task => workRepository.list(task) })
  //          worksToUpdate = worksLists.flatten.map {
  //            case work: LongAnswerWork => work.copy(isComplete = true)
  //            case work: ShortAnswerWork => work.copy(isComplete = true)
  //            case work: MultipleChoiceWork => work.copy(isComplete = true)
  //            case work: OrderingWork => work.copy(isComplete = true)
  //            case work: MatchingWork => work.copy(isComplete = true)
  //          }
  //          updatedWorks <- lift(serializedT(worksToUpdate)(workRepository.update))
  //        } yield ()
  //      }
  //    }
  //  }

  /*
   * -----------------------------------------------------------
   * TaskFeedback methods
   * -----------------------------------------------------------
   */

  /**
   *
   * @param studentId
   * @param projectId
   * @return
   */
  def listFeedbacks(studentId: UUID, projectId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[TaskFeedback]]] = {
    val fStudent = authService.find(studentId)
    val fProject = projectService.find(projectId)

    for {
      student <- lift(fStudent)
      project <- lift(fProject)
      feedbacks <- lift(taskFeedbackRepository.list(student, project))
    } yield feedbacks
  }

  /**
   * Find a teacher's feedback for a student's work on a task.
   *
   * @param taskId
   * @return
   */
  def listFeedbacks(taskId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[TaskFeedback]]] = {
    val fTask = projectService.findTask(taskId)

    for {
      task <- lift(fTask)
      feedbackList <- lift(taskFeedbackRepository.list(task))
    } yield feedbackList
  }

  /**
   * Find a teacher's feedback for a student's work on a task.
   *
   * @param studentId
   * @param taskId
   * @return
   */
  def findFeedback(studentId: UUID, taskId: UUID): Future[\/[ErrorUnion#Fail, TaskFeedback]] = {
    val fStudent = authService.find(studentId)
    val fTask = projectService.findTask(taskId)

    for {
      student <- lift(fStudent)
      task <- lift(fTask)
      feedback <- lift(taskFeedbackRepository.find(student, task))
    } yield feedback
  }

  /**
   * Create a teacher's feedback for a student's work on a task.
   *
   * @param studentId
   * @param taskId
   * @param content
   * @return
   */
  def createFeedback(studentId: UUID, taskId: UUID): Future[\/[ErrorUnion#Fail, TaskFeedback]] = {
    transactional { implicit conn =>
      // First load up the teacher, student and task
      val fStudent = authService.find(studentId)
      val fTask = projectService.findTask(taskId)

      for {
        student <- lift(fStudent)
        task <- lift(fTask)
        part <- lift(projectService.findPart(task.partId, false))
        project <- lift(projectService.find(part.projectId, false))
        course <- lift(schoolService.findCourse(project.courseId))
        teacher <- lift(authService.find(course.teacherId))
        document <- lift(documentService.create(UUID.randomUUID, teacher, "", Delta(IndexedSeq())))
        newFeedback = TaskFeedback(
          studentId = student.id,
          taskId = task.id,
          documentId = document.id
        )
        // Insert the new feedback
        createdFeedback <- lift(taskFeedbackRepository.insert(newFeedback))
      } yield createdFeedback
    }
  }

  /*
   * -----------------------------------------------------------
   * TaskScratchpad methods
   * -----------------------------------------------------------
   */

  /**
   * List all of a user's task scratchpads in a project.
   *
   * @param userId the unique ID of the user to list for
   * @param projectId the project within which to search for task scratchpads
   * @return a vector of responses
   */
  override def listTaskScratchpads(userId: UUID, projectId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[TaskScratchpad]]] = {
    val fUser = authService.find(userId)
    val fProject = projectService.find(projectId)

    for {
      user <- lift(fUser)
      project <- lift(fProject)
      responses <- lift(taskScratchpadRepository.list(user, project))
    } yield responses
  }

  /**
   * Find the latest revision of a user's task scratchpad to a task.
   *
   * @param userId the unique ID of the user to list for
   * @param taskId the task within which to search for task scratchpads
   * @return an optional response
   */
  override def findTaskScratchpad(userId: UUID, taskId: UUID): Future[\/[ErrorUnion#Fail, TaskScratchpad]] = {
    val fUser = authService.find(userId)
    val fTask = projectService.findTask(taskId)

    for {
      user <- lift(fUser)
      task <- lift(fTask)
      responses <- lift(taskScratchpadRepository.find(user, task))
    } yield responses
  }

  /**
   * Create a new task task scratchpad.
   *
   * @param userId the unique ID of the user whose component scratchpad it is
   * @param taskId the unique ID of the task this task scratchpad is for
   * @return the updated task scratchpad
   */
  override def createTaskScratchpad(userId: UUID, taskId: UUID): Future[\/[ErrorUnion#Fail, TaskScratchpad]] = {
    transactional { implicit conn =>
      val fStudent = authService.find(userId)
      val fTask = projectService.findTask(taskId)

      for {
        student <- lift(fStudent)
        task <- lift(fTask)
        document <- lift(documentService.create(UUID.randomUUID, student, "", Delta(IndexedSeq())))
        newScratchpad = TaskScratchpad(
          userId = student.id,
          taskId = task.id,
          documentId = document.id
        )
        // Insert the new feedback
        createdFeedback <- lift(taskScratchpadRepository.insert(newScratchpad))
      } yield createdFeedback
    }
  }
}
