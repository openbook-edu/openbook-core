package com.shiftfocus.krispii.core.services

import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import com.shiftfocus.krispii.core.models._
import com.shiftfocus.krispii.core.repositories._
import com.shiftfocus.krispii.core.services.datasource._
import com.shiftfocus.krispii.core.lib.UUID
import play.api.Logger
import org.joda.time.DateTime
import scala.concurrent.Future

trait WorkServiceImplComponent extends WorkServiceComponent {
  self: UserRepositoryComponent with
        ProjectRepositoryComponent with
        TaskRepositoryComponent with
        ComponentRepositoryComponent with

        TaskResponseRepositoryComponent with
        TaskFeedbackRepositoryComponent with
        TaskScratchpadRepositoryComponent with
        ComponentScratchpadRepositoryComponent with

        DB =>

  override val workService: WorkService = new WorkServiceImpl

  private class WorkServiceImpl extends WorkService {

    /*
     * -----------------------------------------------------------
     * TaskResponses methods
     * -----------------------------------------------------------
     */

    /**
     * List all of a user's responses in a project.
     *
     * @param userId the unique ID of the user to list for
     * @param projectId the project within which to search for responses
     * @return a vector of responses
     */
    override def listResponses(userId: UUID, projectId: UUID): Future[IndexedSeq[TaskResponse]] = {
      Logger.debug(s"workService.listResponses(${userId.string}, ${projectId.string})")
      val fUser = userRepository.find(userId).map(_.get)
      val fProject = projectRepository.find(projectId).map(_.get)
      val fResponses = for {
        user <- fUser
        project <- fProject
        responses <- taskResponseRepository.list(user, project)(db.pool)
      }
      yield responses

      fResponses.recover {
        case exception => throw exception
      }
    }

    /**
     * List all of a user's response revisions for a task.
     *
     * @param userId the unique ID of the user to list for
     * @param taskId the task within which to search for responses
     * @return a vector of responses
     */
    override def listResponseRevisions(userId: UUID, taskId: UUID): Future[IndexedSeq[TaskResponse]] = {
      val fUser = userRepository.find(userId).map(_.get)
      val fTask = taskRepository.find(taskId).map(_.get)
      val fResponses = for {
        user <- fUser
        task <- fTask
        responses <- taskResponseRepository.list(user, task)(db.pool)
      }
      yield responses

      fResponses.recover {
        case exception => throw exception
      }
    }

    /**
     * Find the latest revision of a user's response to a task.
     *
     * @param userId the unique ID of the user to list for
     * @param taskId the task within which to search for responses
     * @return an optional response
     */
    override def findResponse(userId: UUID, taskId: UUID): Future[Option[TaskResponse]] = {
      val fUser = userRepository.find(userId).map(_.get)
      val fTask = taskRepository.find(taskId).map(_.get)
      val fResponse = for {
        user <- fUser
        task <- fTask
        response <- taskResponseRepository.find(user, task)(db.pool)
      }
      yield response

      fResponse.recover {
        case exception => throw exception
      }
    }

    /**
     * Find a specific revision of a user's response to a task.
     *
     * @param userId the unique ID of the user to list for
     * @param taskId the task within which to search for responses
     * @param revision the specific revision of this task to find
     * @return an optional response
     */
    override def findResponse(userId: UUID, taskId: UUID, revision: Long): Future[Option[TaskResponse]] = {
      val fUser = userRepository.find(userId).map(_.get)
      val fTask = taskRepository.find(taskId).map(_.get)
      val fResponse = for {
        user <- fUser
        task <- fTask
        response <- taskResponseRepository.find(user, task, revision)(db.pool)
      }
      yield response

      fResponse.recover {
        case exception => throw exception
      }
    }

    /**
     * Create a new task response.
     *
     * @param userId the unique ID of the user whose task response it is
     * @param taskId the unique ID of the task this task response is for
     * @param revision the current revision of the task to be updated
     * @param version the current version of this revision to be updated
     * @param content the text content of this component response
     * @return the updated task response
     */
    override def createResponse(userId: UUID, taskId: UUID, content: String, isComplete: Boolean): Future[TaskResponse] = {
      transactional { implicit connection =>
        val newResponse = TaskResponse(
          userId = userId,
          taskId = taskId,
          content = content,
          isComplete = isComplete
        )
        val fUser = userRepository.find(userId).map(_.get)
        val fTask = taskRepository.find(taskId).map(_.get)

        for {
          user <- fUser
          task <- fTask
          existingRevisions <- taskResponseRepository.list(user, task).map { revisions =>
            if (revisions.nonEmpty) throw TaskResponseAlreadyExistsException("This task already has a response. Call update instead.")
          }
          newResponse <- taskResponseRepository.insert(newResponse)
        }
        yield newResponse
      }.recover {
        case exception => throw exception
      }
    }

    /**
     * Update a task response.
     *
     * @param userId the unique ID of the user whose response it is
     * @param taskId the unique ID of the task this response is for
     * @param revision the current revision of the task to be updated
     * @param version the current version of this revision to be updated
     * @param content the text content of this task response
     * @param isComplete whether the task is marked complete by the user
     * @param newRevision whether a new revision should be forced
     * @return the updated task response
     */
    override def updateResponse(userId: UUID, taskId: UUID, revision: Long, version: Long, content: String, isComplete: Boolean, newRevision: Boolean): Future[TaskResponse] = {
      transactional { implicit connection =>
        val fUser = userRepository.find(userId).map(_.get)
        val fTask = taskRepository.find(taskId).map(_.get)

        for {
          user <- fUser
          task <- fTask
          latestRevision <- taskResponseRepository.find(user, task, revision).map(_.get)
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
          updatedResponse <- {
            // If an out of date response is given, throw an exception. Updates must be made
            // off of the latest copy.
            if (revision != latestRevision.revision ||
                version != latestRevision.version
            ) {
              throw TaskResponseOutOfDateException("The task response has changed since you last saw it!")
            }

            if (revisionToUse > latestRevision.revision) {
              // timeout exceeded, insert new revision
              taskResponseRepository.insert(latestRevision.copy(
                revision = revisionToUse,
                version = version,
                content = content,
                isComplete = isComplete
              ))
            }
            else {
              // timeout not reached, update current revision
              taskResponseRepository.update(latestRevision.copy(
                version = version,
                content = content,
                isComplete = isComplete
              ))
            }
          }
        }
        yield updatedResponse
      }.recover {
        case exception => throw exception
      }
    }
    override def updateResponse(userId: UUID, taskId: UUID, revision: Long, version: Long, content: String, isComplete: Boolean): Future[TaskResponse] =
      updateResponse(userId, taskId, revision, version, content, isComplete, false)


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
            if (!task.notesAllowed) throw new TaskScratchpadDisabledException("This task does not allow notes!")
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
              if (!task.notesAllowed) throw new TaskScratchpadDisabledException("This task does not allow notes!")
              // timeout exceeded, insert new revision
              taskScratchpadRepository.insert(latestRevision.copy(
                revision = revisionToUse,
                version = version,
                content = content
              ))
            }
            else {
              if (!task.notesAllowed) throw new TaskScratchpadDisabledException("This task does not allow notes!")
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
