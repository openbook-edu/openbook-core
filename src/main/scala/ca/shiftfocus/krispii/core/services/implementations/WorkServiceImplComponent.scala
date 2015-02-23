package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.fail._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks.MatchingTask.Match
import ca.shiftfocus.krispii.core.models.work._
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services.datasource._
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import scala.concurrent.Future
import scalaz.{\/, -\/, \/-}
import ws.kahn.ot.Delta

trait WorkServiceImplComponent extends WorkServiceComponent {
  self: AuthServiceComponent with
        ProjectServiceComponent with
        ComponentServiceComponent with
        SchoolServiceComponent with

        WorkRepositoryComponent with
        TaskFeedbackRepositoryComponent with
        TaskScratchpadRepositoryComponent with
        ComponentScratchpadRepositoryComponent with

        DocumentServiceComponent with
        DocumentRepositoryComponent with

        DB =>

  override val workService: WorkService = new WorkServiceImpl

  private class WorkServiceImpl extends WorkService {

    /**
     * List the latest revision of all of a user's work in a project for a specific
     * course.
     *
     * @param userId the unique id of the user to filter by
     * @param courseId the unique id of the course to filter by
     * @param projectId the unique id of the project to filter by
     * @return a future disjunction containing either a list of work, or a failure
     */
    override def listWork(userId: UUID, courseId: UUID, projectId: UUID): Future[\/[Fail, IndexedSeq[Work]]] = {
      val fUser = authService.find(userId)
      val fCourse = schoolService.findCourse(courseId)
      val fProject = projectService.find(projectId)

      (for {
        userInfo <- lift(fUser)
        course <- lift(fCourse)
        project <- lift(fProject)
        workList <- lift(workRepository.list(userInfo.user, course, project))
      }
      yield workList).run
    }

    /**
     * List all of a user's work revisions for a specific task in a specific course.
     *
     * @param userId the unique id of the user to filter by
     * @param courseId the unique id of the course to filter by
     * @param taskId the unique id of the task to filter by
     * @return a future disjunction containing either a list of work, or a failure
     */
    override def listWorkRevisions(userId: UUID, courseId: UUID, taskId: UUID): Future[\/[Fail, IndexedSeq[Work]]] = {
      val fUser = authService.find(userId)
      val fCourse = schoolService.findCourse(courseId)
      val fTask = projectService.findTask(taskId)

      (for {
        userInfo <- lift(fUser)
        course <- lift(fCourse)
        task <- lift(fTask)
        workList <- lift(workRepository.list(userInfo.user, task, course))
      }
      yield workList).run
    }

    /**
     * Find the latest revision of a user's work, for a task, in a course.
     *
     * @param workId the unique id of the work to find
     * @return a future disjunction containing either a work, or a failure
     */
    override def findWork(workId: UUID): Future[\/[Fail, Work]] = {
      workRepository.find(workId)
    }

    /**
     * Find the latest revision of a user's work, for a task, in a course.
     *
     * @param userId the unique id of the user to filter by
     * @param courseId the unique id of the course to filter by
     * @param taskId the unique id of the task to filter by
     * @return a future disjunction containing either a work, or a failure
     */
    override def findWork(userId: UUID, taskId: UUID, courseId: UUID): Future[\/[Fail, Work]] = {
      val fUser = authService.find(userId)
      val fCourse = schoolService.findCourse(courseId)
      val fTask = projectService.findTask(taskId)

      (for {
        userInfo <- lift(fUser)
        course <- lift(fCourse)
        task <- lift(fTask)
        work <- lift(workRepository.find(userInfo.user, task, course))
      }
      yield work).run
    }

    /**
     * Find a specific revision of a user's work, for a task, in a course.
     *
     * @param userId the unique id of the user to filter by
     * @param taskId the unique id of the task to filter by
     * @param courseId the unique id of the course to filter by
     * @param version the version of the work to find
     * @return a future disjunction containing either a work, or a failure
     */
    override def findWork(userId: UUID, taskId: UUID, courseId: UUID, version: Long): Future[\/[Fail, Work]] = {
      val fUser = authService.find(userId)
      val fCourse = schoolService.findCourse(courseId)
      val fTask = projectService.findTask(taskId)

      (for {
        userInfo <- lift(fUser)
        course <- lift(fCourse)
        task <- lift(fTask)
        work <- lift(workRepository.find(userInfo.user, task, course, version))
      }
      yield work).run
    }

    /**
     * Generic internal work-creating method. This should only be used privately... expose the type-specific
     * methods to the outside world so that we'll have control over exactly how certain types of work
     * are creted.
     *
     * @param newWork the new work to create
     * @return a future disjunction containing either a work, or a failure
     */
    private def createWork(newWork: Work)(implicit conn: Connection): Future[\/[Fail, Work]] = {
      workRepository.insert(newWork)
    }

    // Create methods for each work type

    /**
     * Create a long-answer work item.
     *
     * Use this method when entering student work on a task for the first time (in a given course).
     *
     * @param userId the unique id of the user the work belongs to
     * @param taskId the unique id of the task the work is for
     * @param courseId the unique id of the course
     * @param isComplete whether the student is finished with the task
     * @return a future disjunction containing either a work, or a failure
     */
    override def createLongAnswerWork(userId: UUID, taskId: UUID, isComplete: Boolean): Future[\/[Fail, LongAnswerWork]] = {
      transactional { implicit connection =>
        val fUser = authService.find(userId)
        val fCourse = schoolService.findCourse(courseId)
        val fTask = projectService.findTask(taskId)
        
        (for {
          userInfo <- lift(fUser)
          course <- lift(fCourse)
          task <- lift(fTask)
          document <- lift(documentService.create(UUID.random, userInfo.user, "", Delta(IndexedSeq())))
          newWork = LongAnswerWork(
            studentId = userInfo.user.id,
            taskId = task.id,
            documentId = document.id,
            isComplete = isComplete
          )
          work <- lift(workRepository.insert(newWork))
        } yield work.asInstanceOf[LongAnswerWork]).run
      }
    }

    /**
     * Create a short-answer work item.
     *
     * Use this method when entering student work on a task for the first time (in a given course).
     *
     * @param userId the id of the student whose work is being entered
     * @param taskId the task for which the work was done
     * @param courseId the course to which the task's project belongs
     * @param answer the student's answer to the task (this is the actual "work")
     * @param isComplete whether the student is finished with the task
     * @return the newly created work
     */
    override def createShortAnswerWork(userId: UUID, taskId: UUID, isComplete: Boolean): Future[\/[Fail, ShortAnswerWork]] = {
      transactional { implicit connection =>
        val fUser = authService.find(userId)
        val fCourse = schoolService.findCourse(courseId)
        val fTask = projectService.findTask(taskId)

        (for {
          userInfo <- lift(fUser)
          course <- lift(fCourse)
          task <- lift(fTask)
          document <- lift(documentService.create(UUID.random, userInfo.user, "", Delta(IndexedSeq())))
          newWork = ShortAnswerWork(
            studentId = userInfo.user.id,
            taskId = task.id,
            documentId = document.id,
            isComplete = isComplete
          )
          work <- lift(workRepository.insert(newWork))
        } yield work.asInstanceOf[ShortAnswerWork]).run
      }
    }

    /**
     * Create a multiple-choice work item.
     *
     * Use this method when entering student work on a task for the first time (in a given course).
     *
     * @param userId the id of the student whose work is being entered
     * @param taskId the task for which the work was done
     * @param courseId the course to which the task's project belongs
     * @param answer the student's answer to the task (this is the actual "work")
     * @param isComplete whether the student is finished with the task
     * @return the newly created work
     */
    override def createMultipleChoiceWork(userId: UUID, taskId: UUID, answer: IndexedSeq[Int], isComplete: Boolean): Future[\/[Fail, MultipleChoiceWork]] = {
      transactional { implicit connection =>
        val fUser = authService.find(userId)
        val fCourse = schoolService.findCourse(courseId)
        val fTask = projectService.findTask(taskId)

        (for {
          userInfo <- lift(fUser)
          course <- lift(fCourse)
          task <- lift(fTask)
          document <- lift(documentService.create(UUID.random, userInfo.user, "", Delta(IndexedSeq())))
          newWork = MultipleChoiceWork(
            studentId = userId,
            taskId = taskId,
            version = 1,
            answer = answer,
            isComplete = isComplete
          )
          work <- lift(workRepository.insert(newWork))
        } yield work.asInstanceOf[MultipleChoiceWork]).run
      }
    }

    /**
     * Create an ordering work item.
     *
     * Use this method when entering student work on a task for the first time (in a given course).
     *
     * @param userId the id of the student whose work is being entered
     * @param taskId the task for which the work was done
     * @param courseId the course to which the task's project belongs
     * @param answer the student's answer to the task (this is the actual "work")
     * @param isComplete whether the student is finished with the task
     * @return the newly created work
     */
    override def createOrderingWork(userId: UUID, taskId: UUID, answer: IndexedSeq[Int], isComplete: Boolean): Future[\/[Fail, OrderingWork]] = {
      transactional { implicit connection =>
        val fUser = authService.find(userId)
        val fCourse = schoolService.findCourse(courseId)
        val fTask = projectService.findTask(taskId)

        (for {
          userInfo <- lift(fUser)
          course <- lift(fCourse)
          task <- lift(fTask)
          document <- lift(documentService.create(UUID.random, userInfo.user, "", Delta(IndexedSeq())))
          newWork = OrderingWork(
            studentId = userId,
            taskId = taskId,
            version = 1,
            answer = answer,
            isComplete = isComplete
          )
          work <- lift(workRepository.insert(newWork))
        } yield work.asInstanceOf[OrderingWork]).run
      }
    }

    /**
     * Create a matching work item.
     *
     * Use this method when entering student work on a task for the first time (in a given course).
     *
     * @param userId the id of the student whose work is being entered
     * @param taskId the task for which the work was done
     * @param courseId the course to which the task's project belongs
     * @param answer the student's answer to the task (this is the actual "work")
     * @param isComplete whether the student is finished with the task
     * @return the newly created work
     */
    override def createMatchingWork(userId: UUID, taskId: UUID, answer: IndexedSeq[Match], isComplete: Boolean): Future[\/[Fail, MatchingWork]] = {
      transactional { implicit connection =>
        val fUser = authService.find(userId)
        val fCourse = schoolService.findCourse(courseId)
        val fTask = projectService.findTask(taskId)

        (for {
          userInfo <- lift(fUser)
          course <- lift(fCourse)
          task <- lift(fTask)
          document <- lift(documentService.create(UUID.random, userInfo.user, "", Delta(IndexedSeq())))
          newWork = MatchingWork(
            studentId = userId,
            taskId = taskId,
            version = 1,
            answer = answer,
            isComplete = isComplete
          )
          work <- lift(workRepository.insert(newWork))
        } yield work.asInstanceOf[MatchingWork]).run
      }
    }

    /**
     * Internal method for updating work.
     *
     * External classes should call the type-specific update methods which can perform any logic that need to
     * for specific work types. For example, the long-answer tasks only require new revisions on timed intervals,
     * whereas most other forms of work will store a new revision each time a change is made.
     *
     * Please wrap this method in a transaction and provide it with a connection.
     *
     * @param newerWork
     * @param newRevision
     * @param conn
     * @return
     */
    private def updateWork(newerWork: Work, newRevision: Boolean = true)(implicit conn: Connection): Future[\/[Fail, Work]] = {
      val fUser = authService.find(newerWork.studentId)
      val fTask = projectService.findTask(newerWork.taskId)

      (for {
        userInfo <- lift(fUser)
        task <- lift(fTask)
        latestRevision <- lift(workRepository.find(userInfo.user, task))
        updatedWork <- lift(workRepository.update(newerWork, newRevision))
      } yield updatedWork).run
    }

    /**
     * Update a long answer work.
     *
     * Because the contents of the work are handled by the Document service, this method only
     * serves to update the work's completed status.
     */
    def updateLongAnswerWork(userId: UUID, taskId: UUID, isComplete: Boolean): Future[\/[Fail, LongAnswerWork]] = {
      transactional { implicit connection =>
        val fUser = authService.find(userId)
        val fTask = projectService.findTask(taskId)

        (for {
          userInfo <- lift(fUser)
          task <- lift(fTask)
          existingWork <- lift(workRepository.find(user, task))
          existingLAWork = existingWork.asInstanceOf[LongAnswerWork]
          workToUpdate = existingLAWork.copy(isComplete = isComplete)
          updatedWork <- lift(workRepository.update(workToUpdate))
        }
        yield updatedWork.asInstanceOf[LongAnswerWork]).run
      }
    }

    /**
     * Update a short answer work.
     *
     * Because the contents of the work are handled by the Document service, this method only
     * serves to update the work's completed status.
     */
    def updateShortAnswerWork(userId: UUID, taskId: UUID, isComplete: Boolean): Future[\/[Fail, ShortAnswerWork]] = {
      transactional { implicit connection =>
        val fUser = authService.find(userId)
        val fTask = projectService.findTask(taskId)

        (for {
          userInfo <- lift(fUser)
          task <- lift(fTask)
          existingWork <- lift(workRepository.find(userInfo.user, task))
          existingSAWork = existingWork.asInstanceOf[ShortAnswerWork]
          workToUpdate = existingSAWork.copy(isComplete = isComplete)
          updatedWork <- lift(workRepository.update(workToUpdate))
        }
        yield updatedWork.asInstanceOf[ShortAnswerWork]).run
      }
    }

    def updateMultipleChoiceWork(userId: UUID, taskId: UUID, revision: Long, version: Long, answer: IndexedSeq[Int], isComplete: Boolean): Future[\/[Fail, MultipleChoiceWork]] = {
      transactional { implicit connection =>
        val fUser = authService.find(userId)
        val fTask = projectService.findTask(taskId)

        (for {
          userInfo <- lift(fUser)
          task <- lift(fTask)
          existingWork <- lift(workRepository.find(userInfo.user, task))
          existingMCWork = existingWork.asInstanceOf[MultipleChoiceWork]
          workToUpdate = existingMCWork.copy(answer = answer, isComplete = isComplete)
          updatedWork <- lift(workRepository.update(workToUpdate))
        }
        yield updatedWork.asInstanceOf[MultipleChoiceWork]).run
      }
    }

    def updateOrderingWork(userId: UUID, taskId: UUID, revision: Long, version: Long, answer: IndexedSeq[Int], isComplete: Boolean): Future[\/[Fail, OrderingWork]] = {
      transactional { implicit connection =>
        val fUser = authService.find(userId)
        val fTask = projectService.findTask(taskId)

        (for {
          userInfo <- lift(fUser)
          task <- lift(fTask)
          existingWork <- lift(workRepository.find(userInfo.user, task))
          existingOrdWork = existingWork.asInstanceOf[OrderingWork]
          workToUpdate = existingOrdWork.copy(answer = answer, isComplete = isComplete)
          updatedWork <- lift(workRepository.update(workToUpdate))
        }
        yield updatedWork.asInstanceOf[OrderingWork]).run
      }
    }

    def updateMatchingWork(userId: UUID, taskId: UUID, revision: Long, version: Long, answer: IndexedSeq[Match], isComplete: Boolean): Future[\/[Fail, MatchingWork]] = {
      transactional { implicit connection =>
        val fUser = authService.find(userId)
        val fTask = projectService.findTask(taskId)

        (for {
          userInfo <- lift(fUser)
          task <- lift(fTask)
          existingWork <- lift(workRepository.find(userInfo.user, task))
          existingMatchingWork = existingWork.asInstanceOf[MatchingWork]
          workToUpdate = existingMatchingWork.copy(answer = answer, isComplete = isComplete)
          updatedWork <- lift(workRepository.update(workToUpdate))
        }
        yield updatedWork.asInstanceOf[MatchingWork]).run
      }
    }

    override def forceComplete(taskId: UUID, justThis: Boolean = true): Future[\/[Fail, Unit]] = {
      transactional { implicit connection =>
        if (justThis) {
          (for {
            task <- lift(projectService.findTask(taskId))
            works <- lift(workService.listWork(task.id))
            worksToUpdate = works.map {
              case work: LongAnswerWork => work.copy(isComplete = true)
              case work: ShortAnswerWork => work.copy(isComplete = true)
              case work: MultipleChoiceWork => work.copy(isComplete = true)
              case work: OrderingWork => work.copy(isComplete = true)
              case work: MatchingWork => work.copy(isComplete = true)
            }
            updatedWorks <- serializedT(worksToUpdate.map(workRepository.update))
          } yield ()).run
        }
        else {
          (for {
            task <- lift(projectService.findTask(taskId))
            part <- lift(projectService.findPart(task.partId))
            project <- lift(projectService.find(part.projectId))
            tasks = project.parts.filter(_.position <= part.position)
                                 .map(_.tasks).flatten
                                 .filter { task => task.partId != part.id || task.position <= task.position }
            worksLists <- liftSeq(tasks.map { task => workRepository.list(task) })
            worksToUpdate = worksLists.flatten.map {
              case work: LongAnswerWork => work.copy(isComplete = true)
              case work: ShortAnswerWork => work.copy(isComplete = true)
              case work: MultipleChoiceWork => work.copy(isComplete = true)
              case work: OrderingWork => work.copy(isComplete = true)
              case work: MatchingWork => work.copy(isComplete = true)
            }
            updatedWorks <- serializedT(worksToUpdate.map(workRepository.update))
          } yield ()).run
        }
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
    def listFeedbacks(studentId: UUID, projectId: UUID): Future[\/[Fail, IndexedSeq[TaskFeedback]]] = {
      val fStudent = authService.find(studentId)
      val fProject = projectService.find(projectId)

      (for {
        student <- lift(fStudent)
        project <- lift(fProject)
        feedbacks <- lift(taskFeedbackRepository.list(student.user, project))
      }
      yield feedbacks).run
    }

    /**
     *
     * @param teacherId
     * @param studentId
     * @param projectId
     * @return
     */
    def listFeedbacks(taskId: UUID): Future[\/[Fail, IndexedSeq[TaskFeedback]]] = {
      val fTask = projectService.findTask(taskId)

      (for {
        task <- lift(fTask)
        feedbackList <- lift(taskFeedbackRepository.list(task))
      }
      yield feedbackList).run
    }

    /**
     *
     * @param teacherId
     * @param studentId
     * @param taskId
     * @return
     */
    def findFeedback(studentId: UUID, taskId: UUID): Future[\/[Fail, TaskFeedback]] = {
      val fStudent = authService.find(studentId)
      val fTask = projectService.findTask(taskId)

      (for {
        student <- lift(fStudent)
        task <- lift(fTask)
        feedback <- lift(taskFeedbackRepository.find(student.user, task))
      }
      yield feedback).run
    }

    /**
     *
     * @param studentId
     * @param taskId
     * @param content
     * @return
     */
    def createFeedback(studentId: UUID, taskId: UUID): Future[\/[Fail, TaskFeedback]] = {
      transactional { implicit connection =>
        // First load up the teacher, student and task
        val fStudent = authService.find(studentId)
        val fTask = projectService.findTask(taskId)

        (for {
          student <- lift(fStudent)
          task <- lift(fTask)
          document <- lift(documentService.create(UUID.random, student.user, "", Delta(IndexedSeq())))
          newFeedback = TaskFeedback(
            studentId = student.user.id,
            taskId = task.id,
            documentId = document.id
          )
          // Insert the new feedback
          createdFeedback <- lift(taskFeedbackRepository.insert(newFeedback))
        }
        yield createdFeedback).run
      }
    }

    /**
     * Update a feedback item.
     *
     * Determines whether to create a new revision of this feedback, or to
     * update an existing revision, using the configuration option.
     *
     * @param studentId
     * @param taskId
     * @param version
     * @param documentId
     * @return
     */
    override def updateFeedback(studentId: UUID, taskId: UUID, version: Long, documentId: UUID): Future[\/[Fail, TaskFeedback]] = {
      transactional { implicit connection =>
        val fStudent = authService.find(studentId)
        val fTask = projectService.findTask(taskId)

        (for {
          student <- lift(fStudent)
          task <- lift(fTask)
          existing <- lift(taskFeedbackRepository.find(student.user, task))
          toUpdate = existing.copy(version = version, documentId = documentId)
          // Insert the new feedback
          updatedFeedback <- lift(taskFeedbackRepository.update(toUpdate))
        }
        yield updatedFeedback).run
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
    override def listTaskScratchpads(userId: UUID, projectId: UUID): Future[\/[Fail, IndexedSeq[TaskScratchpad]]] = {
      val fUser = authService.find(userId)
      val fProject = projectService.find(projectId)

      (for {
        userInfo <- lift(fUser)
        project <- lift(fProject)
        responses <- lift(taskScratchpadRepository.list(userInfo.user, project)(db.pool))
      }
      yield responses).run
    }

    /**
     * Find the latest revision of a user's task scratchpad to a task.
     *
     * @param userId the unique ID of the user to list for
     * @param taskId the task within which to search for task scratchpads
     * @return an optional response
     */
    override def findTaskScratchpad(userId: UUID, taskId: UUID): Future[\/[Fail, TaskScratchpad]] = {
      val fUser = authService.find(userId)
      val fTask = projectService.findTask(taskId)

      (for {
        userInfo <- lift(fUser)
        task <- lift(fTask)
        responses <- lift(taskScratchpadRepository.find(userInfo.user, task)(db.pool))
      }
      yield responses).run
    }

    /**
     * Create a new task task scratchpad.
     *
     * @param userId the unique ID of the user whose component scratchpad it is
     * @param taskId the unique ID of the task this task scratchpad is for
     * @return the updated task scratchpad
     */
    override def createTaskScratchpad(userId: UUID, taskId: UUID): Future[\/[Fail, TaskScratchpad]] = {
      transactional { implicit connection =>
        val fStudent = authService.find(userId)
        val fTask = projectService.findTask(taskId)

        (for {
          student <- lift(fStudent)
          task <- lift(fTask)
          document <- lift(documentService.create(UUID.random, student.user, "", Delta(IndexedSeq())))
          newScratchpad = TaskScratchpad(
            userId = student.user.id,
            taskId = task.id,
            documentId = document.id
          )
          // Insert the new feedback
          createdFeedback <- lift(taskScratchpadRepository.insert(newScratchpad))
        }
        yield createdFeedback).run
      }
    }

    /**
     * Update a task response.
     *
     * @param userId the unique ID of the user whose task scratchpad it is
     * @param taskId the unique ID of the task this task scratchpad is for
     * @param revision the current revision of the task to be updated
     * @param version the current version of this revision to be updated
     * @param content the text content of this task scratchpad
     * @param newRevision whether a new revision should be forced
     * @return the updated task scratchpad
     */
    override def updateTaskScratchpad(userId: UUID, taskId: UUID, version: Long, documentId: UUID): Future[\/[Fail, TaskScratchpad]] = {
      transactional { implicit connection =>
        val fStudent = authService.find(userId)
        val fTask = projectService.findTask(taskId)

        (for {
          student <- lift(fStudent)
          task <- lift(fTask)
          existing <- lift(taskScratchpadRepository.find(student.user, task))
          toUpdate = existing.copy(version = version, documentId = documentId)
          // Insert the new feedback
          updatedFeedback <- lift(taskScratchpadRepository.update(toUpdate))
        }
        yield updatedFeedback).run
      }
    }



    /*
     * -----------------------------------------------------------
     * ComponentScratchpad methods
     * -----------------------------------------------------------
     */

    /**
     * List all of a user's component scratchpads in a project.
     *
     * @param userId the unique ID of the user to list for
     * @param projectId the project within which to search for component scratchpads
     * @return a vector of responses
     */
    override def listComponentScratchpadsByUser(userId: UUID): Future[\/[Fail, IndexedSeq[ComponentScratchpad]]] = {
      val fUser = authService.find(userId)

      (for {
        userInfo <- lift(fUser)
        responses <- lift(componentScratchpadRepository.list(userInfo.user)(db.pool))
      }
      yield responses).run
    }

    /**
     * List all of a user's component scratchpads in a project.
     *
     * @param userId the unique ID of the user to list for
     * @param projectId the project within which to search for component scratchpads
     * @return a vector of responses
     */
    override def listComponentScratchpadsByComponent(componentId: UUID): Future[\/[Fail, IndexedSeq[ComponentScratchpad]]] = {
      val fComponent = componentService.find(componentId)

      (for {
        component <- lift(fComponent)
        responses <- lift(componentScratchpadRepository.list(component)(db.pool))
      }
      yield responses).run
    }

    /**
     * Find the latest revision of a user's component scratchpad
     *
     * @param userId the unique ID of the user to list for
     * @param componentId the task within which to search for component scratchpads
     * @return an optional response
     */
    override def findComponentScratchpad(userId: UUID, componentId: UUID): Future[\/[Fail, ComponentScratchpad]] = {
      val fUser = authService.find(userId)
      val fComponent = componentService.find(componentId)

      (for {
        userInfo <- lift(fUser)
        component <- lift(fComponent)
        responses <- lift(componentScratchpadRepository.find(userInfo.user, component)(db.pool))
      }
      yield responses).run
    }

    /**
     * Create a new task component scratchpad.
     *
     * @param userId the unique ID of the user whose component scratchpad it is
     * @param componentId the unique ID of the task this component scratchpad is for
     * @param revision the current revision of the task to be updated
     * @param version the current version of this revision to be updated
     * @param content the text content of this component scratchpad
     * @return the updated component scratchpad
     */
    override def createComponentScratchpad(userId: UUID, componentId: UUID): Future[\/[Fail, ComponentScratchpad]] = {
      transactional { implicit connection =>
        val fStudent = authService.find(userId)
        val fComponent = componentService.find(componentId)

        (for {
          student <- lift(fStudent)
          component <- lift(fComponent)
          document <- lift(documentService.create(UUID.random, student.user, "", Delta(IndexedSeq())))
          newScratchpad = ComponentScratchpad(
            userId = userId,
            componentId = componentId,
            documentId = document.id
          )
          createdScratchpad <- lift(componentScratchpadRepository.insert(newScratchpad))
        }
        yield createdScratchpad).run
      }
    }

    /**
     * Update a component scratchpad.
     *
     * @param userId the unique ID of the user whose component scratchpad it is
     * @param componentId the current revision of the task to be updated
     * @param version the current version of this revision to be updated
     * @param documentId
     * @return the updated component scratchpad
     */
    override def updateComponentScratchpad(userId: UUID, componentId: UUID, version: Long, documentId: UUID): Future[\/[Fail, ComponentScratchpad]] = {
      transactional { implicit connection =>
        val fStudent = authService.find(userId)
        val fComponent = componentService.find(componentId)

        (for {
          student <- lift(fStudent)
          component <- lift(fComponent)
          existing <- lift(componentScratchpadRepository.find(student.user, component))
          toUpdate = existing.copy(version = version, documentId = documentId)
          // Insert the new feedback
          updatedFeedback <- lift(componentScratchpadRepository.update(toUpdate))
        }
        yield updatedFeedback).run
      }
    }
  }
}
