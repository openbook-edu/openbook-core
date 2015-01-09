package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.models.tasks.MatchingTask.Match
import ca.shiftfocus.krispii.core.models.work._
import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services.datasource._
import ca.shiftfocus.uuid.UUID
import play.api.Logger
import org.joda.time.DateTime
import scala.concurrent.Future

trait WorkServiceImplComponent extends WorkServiceComponent {
  self: UserRepositoryComponent with
        ProjectRepositoryComponent with
        TaskRepositoryComponent with
        ComponentRepositoryComponent with
        ClassRepositoryComponent with
        WorkRepositoryComponent with
        TaskResponseRepositoryComponent with
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
     * section.
     *
     * @param userId the user to list work for
     * @param classId a section that the user belongs to to list work in
     * @param projectId a project that belongs to the section to list work for
     * @return an array of work
     */
    override def listWork(userId: UUID, classId: UUID, projectId: UUID): Future[IndexedSeq[Work]] = {
      val fUser = userRepository.find(userId).map(_.get)
      val fSection = classRepository.find(classId).map(_.get)
      val fProject = projectRepository.find(projectId).map(_.get)
      val fResponses = for {
        user <- fUser
        section <- fSection
        project <- fProject
        responses <- workRepository.list(user, section, project)
      }
      yield responses

      fResponses.recover {
        case exception => throw exception
      }
    }

    /**
     * List all of a user's work revisions for a specific task in a specific section.
     *
     * @param userId
     * @param classId
     * @param taskId
     * @return
     */
    override def listWorkRevisions(userId: UUID, classId: UUID, taskId: UUID): Future[IndexedSeq[Work]] = {
      val fUser = userRepository.find(userId).map(_.get)
      val fTask = taskRepository.find(taskId).map(_.get)
      val fSection = classRepository.find(classId).map(_.get)
      val fResponses = for {
        user <- fUser
        task <- fTask
        section <- fSection
        responses <- workRepository.list(user, task, section)
      }
      yield responses

      fResponses.recover {
        case exception => throw exception
      }
    }

    /**
     * Find the latest revision of a user's work, for a task, in a section.
     *
     * @param userId
     * @param taskId
     * @param classId
     * @return
     */
    override def findWork(workId: UUID): Future[Option[Work]] = {
      val fWork = for {
        work <- workRepository.find(workId)
      }
      yield work

      fWork.recover {
        case exception => throw exception
      }
    }

    /**
     * Find the latest revision of a user's work, for a task, in a section.
     *
     * @param userId
     * @param taskId
     * @param classId
     * @return
     */
    override def findWork(userId: UUID, taskId: UUID, classId: UUID): Future[Option[Work]] = {
      val fUser = userRepository.find(userId).map(_.get)
      val fTask = taskRepository.find(taskId).map(_.get)
      val fSection = classRepository.find(classId).map(_.get)
      val fResponses = for {
        user <- fUser
        task <- fTask
        section <- fSection
        responses <- workRepository.find(user, task, section)
      }
      yield responses

      fResponses.recover {
        case exception => throw exception
      }
    }

    /**
     * Find a specific revision of a user's work, for a task, in a section.
     *
     * @param userId
     * @param taskId
     * @param classId
     * @param revision
     * @return
     */
    override def findWork(userId: UUID, taskId: UUID, classId: UUID, revision: Long): Future[Option[Work]] = {
      val fUser = userRepository.find(userId).map(_.get)
      val fTask = taskRepository.find(taskId).map(_.get)
      val fSection = classRepository.find(classId).map(_.get)
      val fResponses = for {
        user <- fUser
        task <- fTask
        section <- fSection
        responses <- workRepository.find(user, task, section, revision)
      }
      yield responses

      fResponses.recover {
        case exception => throw exception
      }
    }

    /**
     * Generic internal work-creating method. This should only be used privately... expose the type-specific
     * methods to the outside world so that we'll have control over exactly how certain types of work
     * are creted.
     *
     * @param newWork
     * @return
     */
    private def createWork(newWork: Work)(implicit conn: Connection): Future[Work] = {
      val fUser = userRepository.find(newWork.studentId).map(_.get)
      val fTask = taskRepository.find(newWork.taskId).map(_.get)
      val fSection = classRepository.find(newWork.classId).map(_.get)
      for {
        user <- fUser
        task <- fTask
        section <- fSection
        createdWork <- workRepository.insert(newWork)
      } yield createdWork
    }

    // Create methods for each work type

    /**
     * Create a long-answer work item.
     *
     * Use this method when entering student work on a task for the first time (in a given section).
     *
     * @param userId the id of the student whose work is being entered
     * @param taskId the task for which the work was done
     * @param classId the section to which the task's project belongs
     * @param answer the student's answer to the task (this is the actual "work")
     * @param isComplete whether the student is finished with the task
     * @return the newly created work
     */
    override def createLongAnswerWork(userId: UUID, taskId: UUID, classId: UUID, isComplete: Boolean): Future[LongAnswerWork] = {
      transactional { implicit connection =>
        Logger.debug("Starting to create long answer work")
        val fUser = userRepository.find(userId).map(_.get)
        val fTask = taskRepository.find(taskId).map(_.get)
        val fSection = classRepository.find(classId).map(_.get)
        for {
          user <- fUser
          task <- fTask
          section <- fSection
          document <- documentService.create(UUID.random, user, "", "")
          work <- createWork(LongAnswerWork(
            studentId = user.id,
            taskId = task.id,
            classId = section.id,
            documentId = document.id,
            isComplete = isComplete
          ))
        } yield work.asInstanceOf[LongAnswerWork]
      }
    }

    /**
     * Create a short-answer work item.
     *
     * Use this method when entering student work on a task for the first time (in a given section).
     *
     * @param userId the id of the student whose work is being entered
     * @param taskId the task for which the work was done
     * @param classId the section to which the task's project belongs
     * @param answer the student's answer to the task (this is the actual "work")
     * @param isComplete whether the student is finished with the task
     * @return the newly created work
     */
    override def createShortAnswerWork(userId: UUID, taskId: UUID, classId: UUID, isComplete: Boolean): Future[ShortAnswerWork] = {
      transactional { implicit connection =>
        val fUser = userRepository.find(userId).map(_.get)
        val fTask = taskRepository.find(taskId).map(_.get)
        val fSection = classRepository.find(classId).map(_.get)
        for {
          user <- fUser
          task <- fTask
          section <- fSection
          document <- documentService.create(UUID.random, user, "", "")
          work <- createWork(ShortAnswerWork(
            studentId = user.id,
            taskId = task.id,
            classId = section.id,
            documentId = document.id,
            isComplete = isComplete
          ))
        } yield work.asInstanceOf[ShortAnswerWork]
      }
    }

    /**
     * Create a multiple-choice work item.
     *
     * Use this method when entering student work on a task for the first time (in a given section).
     *
     * @param userId the id of the student whose work is being entered
     * @param taskId the task for which the work was done
     * @param classId the section to which the task's project belongs
     * @param answer the student's answer to the task (this is the actual "work")
     * @param isComplete whether the student is finished with the task
     * @return the newly created work
     */
    override def createMultipleChoiceWork(userId: UUID, taskId: UUID, classId: UUID, answer: IndexedSeq[Int], isComplete: Boolean): Future[MultipleChoiceWork] = {
      transactional { implicit connection =>
        val newWork = MultipleChoiceWork(
          studentId = userId,
          taskId = taskId,
          classId = classId,
          version = 1,
          answer = answer,
          isComplete = isComplete
        )
        createWork(newWork).map(_.asInstanceOf[MultipleChoiceWork])
      }
    }

    /**
     * Create an ordering work item.
     *
     * Use this method when entering student work on a task for the first time (in a given section).
     *
     * @param userId the id of the student whose work is being entered
     * @param taskId the task for which the work was done
     * @param classId the section to which the task's project belongs
     * @param answer the student's answer to the task (this is the actual "work")
     * @param isComplete whether the student is finished with the task
     * @return the newly created work
     */
    override def createOrderingWork(userId: UUID, taskId: UUID, classId: UUID, answer: IndexedSeq[Int], isComplete: Boolean): Future[OrderingWork] = {
      transactional { implicit connection =>
        val newWork = OrderingWork(
          studentId = userId,
          taskId = taskId,
          classId = classId,
          version = 1,
          answer = answer,
          isComplete = isComplete
        )
        createWork(newWork).map(_.asInstanceOf[OrderingWork])
      }
    }

    /**
     * Create a matching work item.
     *
     * Use this method when entering student work on a task for the first time (in a given section).
     *
     * @param userId the id of the student whose work is being entered
     * @param taskId the task for which the work was done
     * @param classId the section to which the task's project belongs
     * @param answer the student's answer to the task (this is the actual "work")
     * @param isComplete whether the student is finished with the task
     * @return the newly created work
     */
    override def createMatchingWork(userId: UUID, taskId: UUID, classId: UUID, answer: IndexedSeq[Match], isComplete: Boolean): Future[MatchingWork] = {
      transactional { implicit connection =>
        val newWork = MatchingWork(
          studentId = userId,
          taskId = taskId,
          classId = classId,
          version = 1,
          answer = answer,
          isComplete = isComplete
        )
        createWork(newWork).map(_.asInstanceOf[MatchingWork])
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
    private def updateWork(newerWork: Work, newRevision: Boolean = true)(implicit conn: Connection): Future[Work] = {
      val fUser = userRepository.find(newerWork.studentId).map(_.get)
      val fTask = taskRepository.find(newerWork.taskId).map(_.get)
      val fSection = classRepository.find(newerWork.classId).map(_.get)
      for {
        user <- fUser
        task <- fTask
        section <- fSection
        latestRevision <- workRepository.find(user, task, section).map(_.get)
        updatedWork <- {
          if (newerWork.version != latestRevision.version) {
            throw TaskResponseOutOfDateException("The task response has changed since you last saw it!")
          }
          workRepository.update(newerWork, newRevision)
        }
      } yield updatedWork
    }

    /**
     * Update a long answer work.
     *
     * Because the contents of the work are handled by the Document service, this method only
     * serves to update the work's completed status.
     */
    def updateLongAnswerWork(userId: UUID, taskId: UUID, classId: UUID, isComplete: Boolean): Future[LongAnswerWork] = {
      transactional { implicit connection =>
        val fUser = userRepository.find(userId).map(_.get)
        val fTask = taskRepository.find(taskId).map(_.get)
        val fSection = classRepository.find(classId).map(_.get)
        for {
          user <- fUser
          task <- fTask
          section <- fSection
          existingWork <- workRepository.find(user, task, section).map(_.get.asInstanceOf[LongAnswerWork])
          workToUpdate <- Future successful existingWork.copy(
            isComplete = isComplete
          )
          updatedWork <- workRepository.update(workToUpdate)
        }
        yield updatedWork.asInstanceOf[LongAnswerWork]
      }
    }

    /**
     * Update a short answer work.
     *
     * Because the contents of the work are handled by the Document service, this method only
     * serves to update the work's completed status.
     */
    def updateShortAnswerWork(userId: UUID, taskId: UUID, classId: UUID, isComplete: Boolean): Future[ShortAnswerWork] = {
      transactional { implicit connection =>
        val fUser = userRepository.find(userId).map(_.get)
        val fTask = taskRepository.find(taskId).map(_.get)
        val fSection = classRepository.find(classId).map(_.get)
        for {
          user <- fUser
          task <- fTask
          section <- fSection
          existingWork <- workRepository.find(user, task, section).map(_.get.asInstanceOf[ShortAnswerWork])
          workToUpdate <- Future successful existingWork.copy(
            isComplete = isComplete
          )
          updatedWork <- workRepository.update(workToUpdate)
        }
        yield updatedWork.asInstanceOf[ShortAnswerWork]
      }
    }



    def updateMultipleChoiceWork(userId: UUID, taskId: UUID, classId: UUID, revision: Long, version: Long, answer: IndexedSeq[Int], isComplete: Boolean): Future[MultipleChoiceWork] = {
      transactional { implicit connection =>
        val fUser = userRepository.find(userId).map(_.get)
        val fTask = taskRepository.find(taskId).map(_.get)
        val fSection = classRepository.find(classId).map(_.get)
        for {
          user <- fUser
          task <- fTask
          section <- fSection
          existingWork <- workRepository.find(user, task, section, revision).map(_.get.asInstanceOf[MultipleChoiceWork])
          newerWork <- Future successful existingWork.copy(
            answer = answer,
            isComplete = isComplete
          )
          updatedWork <- workRepository.update(existingWork, true)
        }
        yield updatedWork.asInstanceOf[MultipleChoiceWork]
      }
    }

    def updateOrderingWork(userId: UUID, taskId: UUID, classId: UUID, revision: Long, version: Long, answer: IndexedSeq[Int], isComplete: Boolean): Future[OrderingWork] = {
      transactional { implicit connection =>
        val fUser = userRepository.find(userId).map(_.get)
        val fTask = taskRepository.find(taskId).map(_.get)
        val fSection = classRepository.find(classId).map(_.get)
        for {
          user <- fUser
          task <- fTask
          section <- fSection
          existingWork <- workRepository.find(user, task, section, revision).map(_.get.asInstanceOf[OrderingWork])
          newerWork <- Future successful existingWork.copy(
            answer = answer,
            isComplete = isComplete
          )
          updatedWork <- workRepository.update(existingWork, false)
        }
        yield updatedWork.asInstanceOf[OrderingWork]
      }
    }

    def updateMatchingWork(userId: UUID, taskId: UUID, classId: UUID, revision: Long, version: Long, answer: IndexedSeq[Match], isComplete: Boolean): Future[MatchingWork] = {
      transactional { implicit connection =>
        val fUser = userRepository.find(userId).map(_.get)
        val fTask = taskRepository.find(taskId).map(_.get)
        val fSection = classRepository.find(classId).map(_.get)
        for {
          user <- fUser
          task <- fTask
          section <- fSection
          existingWork <- workRepository.find(user, task, section, revision).map(_.get.asInstanceOf[MatchingWork])
          newerWork <- Future successful existingWork.copy(
            answer = answer,
            isComplete = isComplete
          )
          updatedWork <- workRepository.update(existingWork, false)
        }
        yield updatedWork.asInstanceOf[MatchingWork]
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
    def listFeedbacks(studentId: UUID, projectId: UUID): Future[IndexedSeq[TaskFeedback]] = {
      val fStudent = userRepository.find(studentId).map(_.get)
      val fProject = projectRepository.find(projectId).map(_.get)

      val feedbacks = for {
        student <- fStudent
        project <- fProject
        feedbacks <- taskFeedbackRepository.list(student, project)
      }
      yield feedbacks

      feedbacks.recover {
        case exception => throw exception
      }
    }

    /**
     *
     * @param teacherId
     * @param studentId
     * @param projectId
     * @return
     */
    def listFeedbacks(teacherId: UUID, studentId: UUID, projectId: UUID): Future[IndexedSeq[TaskFeedback]] = {
      val fTeacher = userRepository.find(teacherId).map(_.get)
      val fStudent = userRepository.find(studentId).map(_.get)
      val fProject = projectRepository.find(projectId).map(_.get)

      val feedbacks = for {
        teacher <- fTeacher
        student <- fStudent
        project <- fProject
        feedbacks <- taskFeedbackRepository.list(teacher, student, project)
      }
      yield feedbacks

      feedbacks.recover {
        case exception => throw exception
      }
    }

    /**
     *
     * @param teacherId
     * @param studentId
     * @param taskId
     * @return
     */
    def findFeedback(teacherId: UUID, studentId: UUID, taskId: UUID): Future[Option[TaskFeedback]] = {
      val fTeacher = userRepository.find(teacherId).map(_.get)
      val fStudent = userRepository.find(studentId).map(_.get)
      val fTask = taskRepository.find(taskId).map(_.get)

      val feedback = for {
        teacher <- fTeacher
        student <- fStudent
        task <- fTask
        feedback <- taskFeedbackRepository.find(teacher, student, task)
      }
      yield feedback

      feedback.recover {
        case exception => throw exception
      }
    }

    /**
     *
     * @param teacherId
     * @param studentId
     * @param taskId
     * @param revision
     * @return
     */
    def findFeedback(teacherId: UUID, studentId: UUID, taskId: UUID, revision: Long): Future[Option[TaskFeedback]] = {
      val fTeacher = userRepository.find(teacherId).map(_.get)
      val fStudent = userRepository.find(studentId).map(_.get)
      val fTask = taskRepository.find(taskId).map(_.get)

      val feedback = for {
        teacher <- fTeacher
        student <- fStudent
        task <- fTask
        feedback <- taskFeedbackRepository.find(teacher, student, task, revision)
      }
      yield feedback

      feedback.recover {
        case exception => throw exception
      }
    }

    /**
     *
     * @param teacherId
     * @param studentId
     * @param taskId
     * @param content
     * @return
     */
    def createFeedback(teacherId: UUID, studentId: UUID, taskId: UUID, content: String): Future[TaskFeedback] = {
      transactional { implicit connection =>
        // First load up the teacher, student and task
        val fTeacher = userRepository.find(teacherId).map(_.get)
        val fStudent = userRepository.find(studentId).map(_.get)
        val fTask = taskRepository.find(taskId).map(_.get)

        for {
          teacher <- fTeacher
          student <- fStudent
          task <- fTask
          // Check if this feedback already exists
          existingFeedback <- taskFeedbackRepository.find(teacher, student, task).map {
            case Some(existingFeedback) => throw TaskResponseAlreadyExistsException("This task already has a response. Call update instead.")
            case None => None
          }
          // Insert the new feedback
          newResponse <- taskFeedbackRepository.insert(TaskFeedback(
            teacherId = teacher.id,
            studentId = student.id,
            taskId = task.id,
            content = content
          ))
        }
        yield newResponse
      }
    }

    /**
     * Update a feedback item.
     *
     * Determines whether to create a new revision of this feedback, or to
     * update an existing revision, using the configuration option.
     *
     * @param teacherId
     * @param studentId
     * @param taskId
     * @param revision
     * @param version
     * @param content
     * @param newRevision
     * @return
     */
    override def updateFeedback(teacherId: UUID, studentId: UUID, taskId: UUID, revision: Long, version: Long, content: String, newRevision: Boolean): Future[TaskFeedback] = {
      transactional { implicit connection =>
        // Load the teacher, student and task in parallel
        val fTeacher = userRepository.find(teacherId).map(_.get)
        val fStudent = userRepository.find(studentId).map(_.get)
        val fTask = taskRepository.find(taskId).map(_.get)

        for {
          teacher <- fTeacher
          student <- fStudent
          task    <- fTask
          latestRevision <- taskFeedbackRepository.find(teacher, student, task, revision).map(_.get)
          revisionToUse <- Future.successful {
            val timeout = play.Configuration.root().getString("response.newRevision.timeout").toInt
            if (newRevision ||
              (latestRevision.createdAt.isDefined &&
                latestRevision.createdAt.get.plusSeconds(timeout).isBefore(new DateTime))
            ) {
              revision + 1
            }
            else revision
          }
          updatedFeedback <- {
            // If an out of date response is given, throw an exception. Updates must be made
            // off of the latest copy.
            if (revision != latestRevision.revision ||
              version != latestRevision.version
            ) {
              throw TaskResponseOutOfDateException("The task response has changed since you last saw it!")
            }

            if (revisionToUse > latestRevision.revision) {
              // timeout exceeded, insert new revision
              taskFeedbackRepository.insert(latestRevision.copy(
                revision = revisionToUse,
                version = version,
                content = content
              ))
            }
            else {
              // timeout not reached, update current revision
              taskFeedbackRepository.update(latestRevision.copy(
                version = version,
                content = content
              ))
            }
          }
        }
        yield updatedFeedback
      }
    }
    override def updateFeedback(teacherId: UUID, studentId: UUID, taskId: UUID, revision: Long, version: Long, content: String): Future[TaskFeedback] =
      updateFeedback(teacherId, studentId, taskId, revision, version, content, false)

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
    override def listTaskScratchpads(userId: UUID, projectId: UUID): Future[IndexedSeq[TaskScratchpad]] = {
      val fUser = userRepository.find(userId).map(_.get)
      val fProject = projectRepository.find(projectId).map(_.get)
      val fTaskScratchpadList = for {
        user <- fUser
        project <- fProject
        responses <- taskScratchpadRepository.list(user, project)(db.pool)
      }
      yield responses

      fTaskScratchpadList.recover {
        case exception => throw exception
      }
    }

    /**
     * List all of a user's task scratchpad revisions for a task.
     *
     * @param userId the unique ID of the user to list for
     * @param taskId the task within which to search for task scratchpads
     * @return a vector of responses
     */
    override def listTaskScratchpadRevisions(userId: UUID, taskId: UUID): Future[IndexedSeq[TaskScratchpad]] = {
      val fUser = userRepository.find(userId).map(_.get)
      val fTask = taskRepository.find(taskId).map(_.get)
      val fTaskScratchpadList = for {
        user <- fUser
        task <- fTask
        responses <- taskScratchpadRepository.list(user, task)(db.pool)
      }
      yield responses

      fTaskScratchpadList.recover {
        case exception => throw exception
      }
    }

    /**
     * Find the latest revision of a user's task scratchpad to a task.
     *
     * @param userId the unique ID of the user to list for
     * @param taskId the task within which to search for task scratchpads
     * @return an optional response
     */
    override def findTaskScratchpad(userId: UUID, taskId: UUID): Future[Option[TaskScratchpad]] = {
      val fUser = userRepository.find(userId).map(_.get)
      val fTask = taskRepository.find(taskId).map(_.get)
      val fTaskScratchpad = for {
        user <- fUser
        task <- fTask
        response <- taskScratchpadRepository.find(user, task)(db.pool)
      }
      yield response

      fTaskScratchpad.recover {
        case exception => throw exception
      }
    }

    /**
     * Find a specific revision of a user's task scratchpad to a task.
     *
     * @param userId the unique ID of the user to list for
     * @param taskId the task within which to search for task scratchpads
     * @param revision the specific revision of this task to find
     * @return an optional response
     */
    override def findTaskScratchpad(userId: UUID, taskId: UUID, revision: Long): Future[Option[TaskScratchpad]] = {
      val fUser = userRepository.find(userId).map(_.get)
      val fTask = taskRepository.find(taskId).map(_.get)
      val fTaskScratchpad = for {
        user <- fUser
        task <- fTask
        response <- taskScratchpadRepository.find(user, task, revision)(db.pool)
      }
      yield response

      fTaskScratchpad.recover {
        case exception => throw exception
      }
    }

    /**
     * Create a new task task scratchpad.
     *
     * @param userId the unique ID of the user whose component scratchpad it is
     * @param taskId the unique ID of the task this task scratchpad is for
     * @param revision the current revision of the task to be updated
     * @param version the current version of this revision to be updated
     * @param content the text content of this component scratchpad
     * @return the updated task scratchpad
     */
    override def createTaskScratchpad(userId: UUID, taskId: UUID, content: String): Future[TaskScratchpad] = {
      transactional { implicit connection =>
        val newTaskScratchpad = TaskScratchpad(
          userId = userId,
          taskId = taskId,
          content = content
        )
        val fUser = userRepository.find(userId).map(_.get)
        val fTask = taskRepository.find(taskId).map(_.get)

        for {
          user <- fUser
          task <- fTask
          existingRevisions <- taskScratchpadRepository.list(user, task).map { revisions =>
            if (revisions.nonEmpty) throw TaskScratchpadAlreadyExistsException("This task already has a response. Call update instead.")
          }
          newTaskScratchpad <- {
            if (!task.settings.notesAllowed) throw new TaskScratchpadDisabledException("This task does not allow notes!")
            taskScratchpadRepository.insert(newTaskScratchpad)
          }
        }
        yield newTaskScratchpad
      }.recover {
        case exception => throw exception
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
    override def updateTaskScratchpad(userId: UUID, taskId: UUID, revision: Long, version: Long, content: String, newRevision: Boolean): Future[TaskScratchpad] = {
      transactional { implicit connection =>
        val fUser = userRepository.find(userId).map(_.get)
        val fTask = taskRepository.find(taskId).map(_.get)

        for {
          user <- fUser
          task <- fTask
          latestRevision <- taskScratchpadRepository.find(user, task, revision).map(_.get)
          revisionToUse <- Future.successful {
            val timeout = play.Configuration.root().getString("response.newRevision.timeout").toInt
            if (newRevision ||
                (latestRevision.createdAt.isDefined &&
                 latestRevision.createdAt.get.plusSeconds(timeout).isBefore(new DateTime))
            ) {
              revision + 1
            }
            else revision
          }
          updatedTaskScratchpad <- {
            // If an out of date response is given, throw an exception. Updates must be made
            // off of the latest copy.
            if (revision != latestRevision.revision ||
                version != latestRevision.version
            ) {
              throw TaskScratchpadOutOfDateException("The task response has changed since you last saw it!")
            }

            if (revisionToUse > latestRevision.revision) {
              if (!task.settings.notesAllowed) throw new TaskScratchpadDisabledException("This task does not allow notes!")
              // timeout exceeded, insert new revision
              taskScratchpadRepository.insert(latestRevision.copy(
                revision = revisionToUse,
                version = version,
                content = content
              ))
            }
            else {
              if (!task.settings.notesAllowed) throw new TaskScratchpadDisabledException("This task does not allow notes!")
              // timeout not reached, update current revision
              taskScratchpadRepository.update(latestRevision.copy(
                version = version,
                content = content
              ))
            }
          }
        }
        yield updatedTaskScratchpad
      }.recover {
        case exception => throw exception
      }
    }
    override def updateTaskScratchpad(userId: UUID, taskId: UUID, revision: Long, version: Long, content: String): Future[TaskScratchpad] =
      updateTaskScratchpad(userId, taskId, revision, version, content, false)



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
    override def listComponentScratchpads(userId: UUID, projectId: UUID): Future[IndexedSeq[ComponentScratchpad]] = {
      val fUser = userRepository.find(userId).map(_.get)
      val fProject = projectRepository.find(projectId).map(_.get)
      val fComponentScratchpadList = for {
        user <- fUser
        project <- fProject
        responses <- componentScratchpadRepository.list(user, project)(db.pool)
      }
      yield responses

      fComponentScratchpadList.recover {
        case exception => throw exception
      }
    }

    /**
     * List all of a user's component scratchpad revisions
     *
     * @param userId the unique ID of the user to list for
     * @param taskId the task within which to search for component scratchpads
     * @return a vector of responses
     */
    override def listComponentScratchpadRevisions(userId: UUID, componentId: UUID): Future[IndexedSeq[ComponentScratchpad]] = {
      val fUser = userRepository.find(userId).map(_.get)
      val fComponent = componentRepository.find(componentId)(db.pool).map(_.get)
      val fComponentScratchpadList = for {
        user <- fUser
        component <- fComponent
        responses <- componentScratchpadRepository.list(user, component)(db.pool)
      }
      yield responses

      fComponentScratchpadList.recover {
        case exception => throw exception
      }
    }

    /**
     * Find the latest revision of a user's component scratchpad
     *
     * @param userId the unique ID of the user to list for
     * @param taskId the task within which to search for component scratchpads
     * @return an optional response
     */
    override def findComponentScratchpad(userId: UUID, componentId: UUID): Future[Option[ComponentScratchpad]] = {
      val fUser = userRepository.find(userId).map(_.get)
      val fComponent = componentRepository.find(componentId)(db.pool).map(_.get)
      val fComponentScratchpad = for {
        user <- fUser
        component <- fComponent
        response <- componentScratchpadRepository.find(user, component)(db.pool)
      }
      yield response

      fComponentScratchpad.recover {
        case exception => throw exception
      }
    }

    /**
     * Find a specific revision of a user's component scratchpad.
     *
     * @param userId the unique ID of the user to list for
     * @param taskId the task within which to search for component scratchpads
     * @param revision the specific revision of this task to find
     * @return an optional response
     */
    override def findComponentScratchpad(userId: UUID, componentId: UUID, revision: Long): Future[Option[ComponentScratchpad]] = {
      val fUser = userRepository.find(userId).map(_.get)
      val fComponent = componentRepository.find(componentId)(db.pool).map(_.get)
      val fComponentScratchpad = for {
        user <- fUser
        component <- fComponent
        response <- componentScratchpadRepository.find(user, component, revision)(db.pool)
      }
      yield response

      fComponentScratchpad.recover {
        case exception => throw exception
      }
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
    override def createComponentScratchpad(userId: UUID, componentId: UUID, content: String): Future[ComponentScratchpad] = {
      transactional { implicit connection =>
        val newComponentScratchpad = ComponentScratchpad(
          userId = userId,
          componentId = componentId,
          content = content
        )
        val fUser = userRepository.find(userId).map(_.get)
        val fComponent = componentRepository.find(componentId)(db.pool).map(_.get)

        for {
          user <- fUser
          component <- fComponent
          existingRevisions <- componentScratchpadRepository.list(user, component).map { revisions =>
            if (revisions.nonEmpty) throw ComponentScratchpadAlreadyExistsException("This task already has a response. Call update instead.")
          }
          newComponentScratchpad <- componentScratchpadRepository.insert(newComponentScratchpad)
        }
        yield newComponentScratchpad
      }.recover {
        case exception => throw exception
      }
    }

    /**
     * Update a component scratchpad.
     *
     * @param userId the unique ID of the user whose component scratchpad it is
     * @param taskId the unique ID of the task this component scratchpad is for
     * @param revision the current revision of the task to be updated
     * @param version the current version of this revision to be updated
     * @param content the text content of this component scratchpad
     * @param newRevision whether a new revision should be forced
     * @return the updated component scratchpad
     */
    override def updateComponentScratchpad(userId: UUID, componentId: UUID, revision: Long, version: Long, content: String, newRevision: Boolean): Future[ComponentScratchpad] = {
      transactional { implicit connection =>
        val fUser = userRepository.find(userId).map(_.get)
        val fComponent = componentRepository.find(componentId)(db.pool).map(_.get)

        for {
          user <- fUser
          component <- fComponent
          latestRevision <- componentScratchpadRepository.find(user, component, revision).map(_.get)
          revisionToUse <- Future.successful {
            val timeout = play.Configuration.root().getString("response.newRevision.timeout").toInt
            if (newRevision ||
                (latestRevision.createdAt.isDefined &&
                 latestRevision.createdAt.get.plusSeconds(timeout).isBefore(new DateTime))
            ) {
              revision + 1
            }
            else revision
          }
          updatedComponentScratchpad <- {
            // If an out of date response is given, throw an exception. Updates must be made
            // off of the latest copy.
            if (revision != latestRevision.revision ||
                version != latestRevision.version
            ) {
              throw ComponentScratchpadOutOfDateException("The task response has changed since you last saw it!")
            }

            if (revisionToUse > latestRevision.revision) {
              // timeout exceeded, insert new revision
              componentScratchpadRepository.insert(latestRevision.copy(
                revision = revisionToUse,
                version = version,
                content = content
              ))
            }
            else {
              // timeout not reached, update current revision
              componentScratchpadRepository.update(latestRevision.copy(
                version = version,
                content = content
              ))
            }
          }
        }
        yield updatedComponentScratchpad
      }.recover {
        case exception => throw exception
      }
    }
    override def updateComponentScratchpad(userId: UUID, taskId: UUID, revision: Long, version: Long, content: String): Future[ComponentScratchpad] =
      updateComponentScratchpad(userId, taskId, revision, version, content, false)
  }
}
