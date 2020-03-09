package ca.shiftfocus.krispii.core.services

import java.util.UUID

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models.tasks.QuestionTask
import ca.shiftfocus.krispii.core.models.work._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services.datasource._
import ca.shiftfocus.otlib.Delta
import com.github.mauricio.async.db.Connection
import scalaz.{\/, \/-}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WorkServiceDefault(
  val db: DB,
  val authService: AuthService,
  val schoolService: SchoolService,
  val projectService: ProjectService,
  val documentService: DocumentService,
  val componentService: ComponentService,
  val workRepository: WorkRepository,
  val documentRepository: DocumentRepository,
  val taskFeedbackRepository: TaskFeedbackRepository,
  val taskScratchpadRepository: TaskScratchpadRepository,
  val projectScratchpadRepository: ProjectScratchpadRepository,
  val gfileRepository: GfileRepository,
  val scoreRepository: ScoreRepository
)
    extends WorkService {

  implicit def conn: Connection = db.pool

  /**
   * List the latest revision of all of a user's work in a project for a specific
   * course.
   *
   * @param userId    the unique id of the user to filter by
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
      result <- liftSeq {
        workList.map { work =>
          (for {
            gFiles <- lift(gfileRepository.listByWork(work))
            toReturn <- lift(Future successful (work match {
              case work: DocumentWork => \/-(work.copy(gFiles = gFiles))
              case work: MediaWork => \/-(work.copy(gFiles = gFiles))
              case work: QuestionWork => \/-(work.copy(gFiles = gFiles))
            }))
          } yield toReturn.asInstanceOf[Work]).run
        }
      }
    } yield result
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
      result <- liftSeq {
        workList.map { work =>
          (for {
            gFiles <- lift(gfileRepository.listByWork(work))
            toReturn <- lift(Future successful (work match {
              case work: DocumentWork => \/-(work.copy(gFiles = gFiles))
              case work: MediaWork => \/-(work.copy(gFiles = gFiles))
              case work: QuestionWork => \/-(work.copy(gFiles = gFiles))
            }))
          } yield toReturn.asInstanceOf[Work]).run
        }
      }
    } yield result
  }

  /**
   * TODO - not used, will list revisions by each work type
   * List all of a user's work revisions for a specific task in a specific course.
   *
   * @param userId the unique id of the user to filter by
   * @param taskId the unique id of the task to filter by
   * @return a future disjunction containing either a list of work, or a failure
   */
  override def listWorkRevisions(userId: UUID, taskId: UUID) // format: OFF
  : Future[\/[ErrorUnion#Fail, Either[DocumentWork, IndexedSeq[QuestionWork]]]] = { // format: ON
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
    for {
      work <- lift(workRepository.find(workId))
      gFiles <- lift(gfileRepository.listByWork(work))
      result <- lift(Future successful (work match {
        case work: DocumentWork => \/-(work.copy(gFiles = gFiles))
        case work: MediaWork => \/-(work.copy(gFiles = gFiles))
        case work: QuestionWork => \/-(work.copy(gFiles = gFiles))
      }))
    } yield result.asInstanceOf[Work]
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
      gFiles <- lift(gfileRepository.listByWork(work))
      result <- lift(Future successful (work match {
        case work: DocumentWork => \/-(work.copy(gFiles = gFiles))
        case work: MediaWork => \/-(work.copy(gFiles = gFiles))
        case work: QuestionWork => \/-(work.copy(gFiles = gFiles))
      }))
    } yield result.asInstanceOf[Work]
  }

  /**
   * Find a specific revision of a user's work, for a task, in a course.
   *
   * @param userId  the unique id of the user to filter by
   * @param taskId  the unique id of the task to filter by
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
      gFiles <- lift(gfileRepository.listByWork(work))
      result <- lift(Future successful (work match {
        case work: DocumentWork => \/-(work.copy(gFiles = gFiles))
        case work: MediaWork => \/-(work.copy(gFiles = gFiles))
        case work: QuestionWork => \/-(work.copy(gFiles = gFiles))
      }))
    } yield result.asInstanceOf[Work]
  }

  // --------- Create methods for each work type --------------------------------------------------------------------

  // TODO - verify if user already has work for this task
  /**
   * Create a document work item.
   *
   * Use this method when entering student work on a task for the first time (in a given course).
   *
   * @param userId     the unique id of the user the work belongs to
   * @param taskId     the unique id of the task the work is for
   * @param isComplete whether the student is finished with the task
   * @return a future disjunction containing either a work, or a failure
   */
  override def createDocumentWork(userId: UUID, taskId: UUID, isComplete: Boolean): Future[\/[ErrorUnion#Fail, DocumentWork]] = {
    transactional { implicit conn =>
      val fUser = authService.find(userId)
      val fTask = projectService.findTask(taskId)

      for {
        user <- lift(fUser)
        task <- lift(fTask)
        document <- lift(documentService.create(UUID.randomUUID, user, "", Delta(IndexedSeq())))
        newWork = DocumentWork(
          studentId = user.id,
          taskId = task.id,
          documentId = document.id,
          isComplete = isComplete,
          grade = "0",
          response = Some(document)
        )
        work <- lift(workRepository.insert(newWork))
      } yield work.asInstanceOf[DocumentWork].copy(response = Some(document))
    }
  }

  /**
   * Create a question work item.
   *
   * Use this method when entering student work on a task for the first time (in a given course).
   *
   * @param userId     the id of the student whose work is being entered
   * @param taskId     the task for which the work was done
   * @param isComplete whether the student is finished with the task
   * @return the newly created work
   */
  override def createQuestionWork(userId: UUID, taskId: UUID, isComplete: Boolean): Future[\/[ErrorUnion#Fail, QuestionWork]] = {
    transactional { implicit conn =>
      val fUser = authService.find(userId)
      val fTask = projectService.findTask(taskId)

      for {
        user <- lift(fUser)
        task <- lift(fTask)
        newWork = QuestionWork(
          studentId = userId,
          taskId = taskId,
          version = 1,
          response = Answers(Map()),
          isComplete = isComplete,
          grade = "0"
        )
        work <- lift(workRepository.insert(newWork))
      } yield work.asInstanceOf[QuestionWork]
    }
  }

  /**
   * Create a media work item.
   *
   * Use this method when entering student work on a task for the first time (in a given course).
   *
   * @param userId     the id of the student whose work is being entered
   * @param taskId     the task for which the work was done
   * @param isComplete whether the student is finished with the task
   * @return the newly created work
   */
  override def createMediaWork(userId: UUID, taskId: UUID, isComplete: Boolean): Future[\/[ErrorUnion#Fail, MediaWork]] = {
    transactional { implicit conn =>
      val fUser = authService.find(userId)
      val fTask = projectService.findTask(taskId)

      for {
        user <- lift(fUser)
        task <- lift(fTask)
        newWork = MediaWork(
          studentId = userId,
          taskId = taskId,
          version = 1,
          fileData = MediaAnswer(),
          isComplete = isComplete,
          grade = "0"
        )
        work <- lift(workRepository.insert(newWork))
      } yield work.asInstanceOf[MediaWork]
    }
  }

  // --------- Update methods for each work type --------------------------------------------------------------------

  /**
   * Update a long answer work.
   *
   * Because the contents of the work are handled by the Document service, this method only
   * serves to update the work's completed status UPD: and grade.
   *
   * @param userId
   * @param taskId
   * @param isComplete Boolean
   * @param grade      Option[String]: if missing, not updated, TODO: if "", delete grade!
   */
  def updateDocumentWork(userId: UUID, taskId: UUID, isComplete: Boolean, grade: Option[String]): Future[\/[ErrorUnion#Fail, DocumentWork]] = {
    transactional { implicit conn =>
      val fUser = authService.find(userId)
      val fTask = projectService.findTask(taskId)

      for {
        user <- lift(fUser)
        task <- lift(fTask)
        existingWork <- lift(workRepository.find(user, task))
        _ <- predicate(existingWork.isInstanceOf[DocumentWork])(ServiceError.BadInput("Attempted to update the answer for a question work"))
        existingDocWork = existingWork.asInstanceOf[DocumentWork]
        workToUpdate = existingDocWork.copy(isComplete = isComplete, grade = grade.getOrElse(existingWork.grade))
        updatedWork <- lift(workRepository.update(workToUpdate))
        gFiles <- lift(gfileRepository.listByWork(updatedWork))
        result = updatedWork match {
          case work: DocumentWork => work.copy(gFiles = gFiles)
          case work: MediaWork => work.copy(gFiles = gFiles)
          case work: QuestionWork => work.copy(gFiles = gFiles)
        }
      } yield result.asInstanceOf[DocumentWork]
    }
  }

  override def updateQuestionWork(
    userId: UUID,
    taskId: UUID,
    version: Long,
    response: Option[Answers],
    isComplete: Option[Boolean],
    grade: Option[String]
  ): Future[\/[ErrorUnion#Fail, QuestionWork]] = {
    transactional { implicit conn =>
      val fUser = authService.find(userId)
      val fTask = projectService.findTask(taskId)

      for {
        user <- lift(fUser)
        task <- lift(fTask)
        existingWork <- lift(workRepository.find(user, task))
        _ <- predicate(existingWork.isInstanceOf[QuestionWork])(ServiceError.BadInput("Attempted to update the answer for a document work"))
        _ <- predicate(existingWork.version == version)(ServiceError.OfflineLockFail)
        existingQuestionWork = existingWork.asInstanceOf[QuestionWork]
        workToUpdate = existingQuestionWork.copy(
          response = response.getOrElse(existingQuestionWork.response),
          isComplete = isComplete.getOrElse(existingQuestionWork.isComplete),
          grade = grade.getOrElse(existingWork.grade)
        )
        updatedWork <- lift(workRepository.update(workToUpdate))
        gFiles <- lift(gfileRepository.listByWork(updatedWork))
        result = updatedWork match {
          case work: DocumentWork => work.copy(gFiles = gFiles)
          case work: MediaWork => work.copy(gFiles = gFiles)
          case work: QuestionWork => work.copy(gFiles = gFiles)
        }
      } yield result.asInstanceOf[QuestionWork]
    }
  }

  /**
   * Update a media work. User ID, task ID and version are required,
   * fileData, isComplete and grade need only be supplied if they have changed.
   * The newest versions of the gFiles are automatically read in before updating,
   * while the possible OMS scorer with their grades cannot be updated here (use
   * the specific functions below).
   *
   * @param userId
   * @param taskId
   * @param version
   * @param fileData
   * @param isComplete
   * @param grade
   * @return
   */
  override def updateMediaWork(
    userId: UUID,
    taskId: UUID,
    version: Long,
    fileData: Option[MediaAnswer],
    isComplete: Option[Boolean],
    grade: Option[String]
  ): Future[\/[ErrorUnion#Fail, MediaWork]] = {
    transactional { implicit conn =>
      val fUser = authService.find(userId)
      val fTask = projectService.findTask(taskId)

      for {
        user <- lift(fUser)
        task <- lift(fTask)
        existingWork <- lift(workRepository.find(user, task))
        _ <- predicate(existingWork.isInstanceOf[MediaWork])(ServiceError.BadInput("Attempted to update the answer for a document work"))
        _ <- predicate(existingWork.version == version)(ServiceError.OfflineLockFail)
        existingMediaWork = existingWork.asInstanceOf[MediaWork]
        workToUpdate = existingMediaWork.copy(
          fileData = fileData.getOrElse(existingMediaWork.fileData),
          isComplete = isComplete.getOrElse(existingMediaWork.isComplete),
          grade = grade.getOrElse(existingWork.grade)
        )
        updatedWork <- lift(workRepository.update(workToUpdate))
        gFiles <- lift(gfileRepository.listByWork(updatedWork))
        result = updatedWork match {
          case work: DocumentWork => work.copy(gFiles = gFiles)
          case work: MediaWork => work.copy(gFiles = gFiles)
          case work: QuestionWork => work.copy(gFiles = gFiles)
        }
      } yield result.asInstanceOf[MediaWork]
    }
  }

  override def updateAnswer(workId: UUID, version: Long, questionId: UUID, answer: Answer): Future[\/[ErrorUnion#Fail, QuestionWork]] = {
    transactional { implicit conn =>
      for {
        work <- lift(findWork(workId))
        _ <- predicate(work.isInstanceOf[QuestionWork])(ServiceError.BadInput("Attempted to update the answer for a document work"))
        _ <- predicate(work.version == version)(ServiceError.OfflineLockFail)
        questionWork = work.asInstanceOf[QuestionWork]

        task <- lift(projectService.findTask(questionWork.taskId))
        _ <- predicate(task.isInstanceOf[QuestionTask])(ServiceError.BadInput("Retrieved a QuestionWork that points to a DocumentTask. Kindly curl up into a ball and cry."))
        questionTask = task.asInstanceOf[QuestionTask]
        _ <- predicate(questionTask.questions.exists(_.id == questionId))(ServiceError.BadInput(s"There is no Question for answer."))
        question = questionTask.questions.filter(_.id == questionId).head

        // Verify that the updated answer actually corresponds to the right question
        _ <- predicate(answer.answers(question))(ServiceError.BadInput(s"The provided answer does not match the question type"))

        toUpdate = questionWork.copy(response = questionWork.response.updated(questionId, answer))
        updated <- lift(workRepository.update(toUpdate))
        gFiles <- lift(gfileRepository.listByWork(updated))
        result = updated match {
          case work: DocumentWork => work.copy(gFiles = gFiles)
          case work: MediaWork => work.copy(gFiles = gFiles)
          case work: QuestionWork => work.copy(gFiles = gFiles)
        }
      } yield result.asInstanceOf[QuestionWork]
    }
  }

  //  override def forceComplete(taskId: UUID, justThis: Boolean = true): Future[\/[ErrorUnion#Fail, Unit]] = {
  //    transactional { implicit conn =>
  //      if (justThis) {
  //        for {
  //          task <- lift(projectService.findTask(taskId))
  //          works <- lift(listWork(task.id))
  //          worksToUpdate = works.map {
  //            case work: DocumentWork => work.copy(isComplete = true)
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
  //            case work: DocumentWork => work.copy(isComplete = true)
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

  // --------- Delete methods --------------------------------------------------------------------

  /**
   * Delete all work for a given task with all the documents and revisions
   *
   * @param taskId
   * @return
   */
  override def deleteWork(taskId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Work]]] = {
    transactional { implicit conn =>
      for {
        task <- lift(projectService.findTask(taskId))
        deletedWork <- lift(workRepository.delete(task))
        documentIds = deletedWork.map {
          case work: DocumentWork => Some(work.documentId)
          case _ => None
        }.flatten
        _ <- lift(serializedT(documentIds)(documentRepository.delete))
      } yield deletedWork
    }
  }

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

  /**
   * Delete all teacher feedbacks for a given task.
   *
   * @param taskId
   * @return
   */
  def deleteFeedback(taskId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[TaskFeedback]]] = {
    transactional { implicit conn =>
      val fTask = projectService.findTask(taskId)

      for {
        task <- lift(fTask)
        feedbacks <- lift(taskFeedbackRepository.delete(task))
        documentIds = feedbacks.map(_.documentId)
        _ <- lift(serializedT(documentIds)(documentRepository.delete))
      } yield feedbacks
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
   * @param userId    the unique ID of the user to list for
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

  /*
  * -----------------------------------------------------------
  * ProjectScratchpad methods
  * -----------------------------------------------------------
  */

  /**
   * List all of a user's task scratchpads in a project.
   *
   * @param userId the unique ID of the user to list for
   * @return a vector of responses
   */
  override def listProjectScratchpads(userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[ProjectScratchpad]]] = {
    val fUser = authService.find(userId)
    for {
      user <- lift(fUser)
      responses <- lift(projectScratchpadRepository.list(user))
    } yield responses
  }

  /**
   * Find the latest revision of a user's task scratchpad to a task.
   *
   * @param userId    the unique ID of the user to list for
   * @param projectId the project within which to search for task scratchpads
   * @return an optional response
   */
  override def findProjectScratchpad(userId: UUID, projectId: UUID): Future[\/[ErrorUnion#Fail, ProjectScratchpad]] = {
    val fUser = authService.find(userId)
    val fProject = projectService.find(projectId)

    for {
      user <- lift(fUser)
      project <- lift(fProject)
      responses <- lift(projectScratchpadRepository.find(user, project))
    } yield responses
  }

  /**
   * Create a new task scratchpad.
   *
   * @param userId    the unique ID of the user whose component scratchpad it is
   * @param projectId the unique ID of the project this project scratchpad is for
   * @return the updated task scratchpad
   */
  override def createProjectScratchpad(userId: UUID, projectId: UUID): Future[\/[ErrorUnion#Fail, ProjectScratchpad]] = {
    transactional { implicit conn =>
      val fStudent = authService.find(userId)
      val fProject = projectService.find(projectId)

      for {
        student <- lift(fStudent)
        project <- lift(fProject)
        document <- lift(documentService.create(UUID.randomUUID, student, "", Delta(IndexedSeq())))
        newScratchpad = ProjectScratchpad(
          userId = student.id,
          projectId = project.id,
          documentId = document.id
        )
        // Insert the new feedback
        createdFeedback <- lift(projectScratchpadRepository.insert(newScratchpad))
      } yield createdFeedback
    }
  }

  // ########## GOOGLE FILES ###########################################################################################

  override def getGfile(gFileId: UUID): Future[\/[ErrorUnion#Fail, Gfile]] = {
    for {
      gFile <- lift(gfileRepository.get(gFileId))
    } yield gFile
  }

  override def createGfile(
    workId: UUID,
    fileId: String,
    mimeType: String,
    fileType: String,
    fileName: String,
    embedUrl: String,
    url: String,
    sharedEmail: Option[String]
  ): Future[\/[ErrorUnion#Fail, Work]] = {
    for {
      work <- lift(workRepository.find(workId))
      gFiles <- lift(gfileRepository.listByWork(work))
      newGfile <- lift(gfileRepository.insert(
        Gfile(
          workId = workId,
          fileId = fileId,
          mimeType = mimeType,
          fileType = fileType,
          fileName = fileName,
          embedUrl = embedUrl,
          url = url,
          sharedEmail = sharedEmail
        )
      ))
      result = work match {
        case work: DocumentWork => work.copy(gFiles = gFiles :+ newGfile)
        case work: MediaWork => work.copy(gFiles = gFiles :+ newGfile)
        case work: QuestionWork => work.copy(gFiles = gFiles :+ newGfile)
      }
    } yield result.asInstanceOf[Work]
  }

  override def updateGfile(
    gFileId: UUID,
    sharedEmail: Option[Option[String]],
    permissionId: Option[Option[String]],
    revisionId: Option[Option[String]]
  ): Future[\/[ErrorUnion#Fail, Work]] = for {
    gFile <- lift(gfileRepository.get(gFileId))
    work <- lift(workRepository.find(gFile.workId))
    gFiles <- lift(gfileRepository.listByWork(work))
    updatedGfile <- lift(gfileRepository.update(
      gFile.copy(
        sharedEmail = sharedEmail match {
        case Some(Some(sharedEmail)) => Some(sharedEmail)
        case Some(None) => None
        case None => gFile.sharedEmail
      },
        permissionId = permissionId match {
        case Some(Some(permissionId)) => Some(permissionId)
        case Some(None) => None
        case None => gFile.permissionId
      },
        revisionId = revisionId match {
        case Some(Some(revisionId)) => Some(revisionId)
        case Some(None) => None
        case None => gFile.revisionId
      }
      )
    ))
    // Update list of google files in the work
    updatedGfiles = gFiles.map(gF => {
      if (gF.id == updatedGfile.id) updatedGfile
      else gF
    })
    result = work match {
      case work: DocumentWork => work.copy(gFiles = updatedGfiles)
      case work: MediaWork => work.copy(gFiles = updatedGfiles)
      case work: QuestionWork => work.copy(gFiles = updatedGfiles)
    }
  } yield result.asInstanceOf[Work]

  override def deleteGfile(gFileId: UUID): Future[\/[ErrorUnion#Fail, Work]] = for {
    gFile <- lift(gfileRepository.get(gFileId))
    work <- lift(workRepository.find(gFile.workId))
    gFiles <- lift(gfileRepository.listByWork(work))
    deletedGfile <- lift(gfileRepository.delete(gFile))
    // Remove delete google file from the list
    updatedGfiles = gFiles.filter(_.id != gFile.id)
    result = work match {
      case work: DocumentWork => work.copy(gFiles = updatedGfiles)
      case work: MediaWork => work.copy(gFiles = updatedGfiles)
      case work: QuestionWork => work.copy(gFiles = updatedGfiles)
    }
  } yield result.asInstanceOf[Work]

  /*
  ---------------------------------- OMS: individual scorers and their grades -------------------------------------------
  */

  /**
   * Add an OMS scorer to a work (initial grade is the empty string).
   * No limit on the number of scorers is imposed, nor is there a check on
   * the scorers that are registered for the course (classroom) that the work belongs to.
   *
   * @param workId
   * @param scorerId
   * @return
   */
  override def addScorer(workId: UUID, scorerId: UUID): Future[\/[ErrorUnion#Fail, Work]] = for {
    existingWork <- lift(workRepository.find(workId))
    _ <- predicate(existingWork.isInstanceOf[MediaWork])(ServiceError.BadInput("Attempted to add a scorer for a work without a media file"))
    existingMediaWork = existingWork.asInstanceOf[MediaWork]
    scorer <- lift(authService.find(scorerId))
    _ <- lift(scoreRepository.addScorers(existingMediaWork, IndexedSeq(scorer)))
    scores <- lift(scoreRepository.list(existingMediaWork))
    updatedWork = existingMediaWork.copy(scores = scores)
  } yield updatedWork

  override def deleteScorer(workId: UUID, scorerId: UUID): Future[\/[ErrorUnion#Fail, Work]] = for {
    existingWork <- lift(workRepository.find(workId))
    _ <- predicate(existingWork.isInstanceOf[MediaWork])(ServiceError.BadInput("Attempted to remove a scorer from a work without a media file"))
    existingMediaWork = existingWork.asInstanceOf[MediaWork]
    scorer <- lift(authService.find(scorerId))
    _ <- lift(scoreRepository.removeScorers(existingMediaWork, IndexedSeq(scorer)))
    scores <- lift(scoreRepository.list(existingMediaWork))
    updatedWork = existingMediaWork.copy(scores = scores)
  } yield updatedWork

  override def getScore(workId: UUID, scorerId: UUID): Future[\/[ErrorUnion#Fail, Score]] = for {
    work <- lift(findWork(workId = workId))
    scorer <- lift(authService.find(id = scorerId))
    score <- lift(scoreRepository.find(work, scorer))
  } yield score

  override def updateScore(workId: UUID, scorerId: UUID, grade: String): Future[\/[ErrorUnion#Fail, Score]] = for {
    work <- lift(findWork(workId = workId))
    scorer <- lift(authService.find(id = scorerId))
    existingScore <- lift(scoreRepository.find(work, scorer))
    updatedScore <- lift(scoreRepository.update(existingScore.copy(grade = grade)))
  } yield updatedScore
}
