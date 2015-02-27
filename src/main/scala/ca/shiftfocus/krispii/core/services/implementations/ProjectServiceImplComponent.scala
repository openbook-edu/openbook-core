package ca.shiftfocus.krispii.core.services

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.fail._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks._
import ca.shiftfocus.krispii.core.models.work._
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services.datasource._
import ca.shiftfocus.uuid.UUID
import play.api.Logger
import play.api.i18n.Messages
import scala.concurrent.Future
import scalaz.{EitherT, \/, -\/, \/-}

trait ProjectServiceImplComponent extends ProjectServiceComponent {
  self: ProjectRepositoryComponent with
        PartRepositoryComponent with
        TaskRepositoryComponent with
        TaskScratchpadRepositoryComponent with
        TaskFeedbackRepositoryComponent with
        UserRepositoryComponent with
        ComponentRepositoryComponent with
        CourseRepositoryComponent with
        WorkServiceComponent with
        WorkRepositoryComponent with
        DB =>

  override val projectService: ProjectService = new ProjectServiceImpl

  private class ProjectServiceImpl extends ProjectService {

    implicit def conn: Connection = db.pool
    
    /**
     * Lists all projects.
     *
     * @return a future disjunction containing either a vector of projects, or a failure
     */
    override def list: Future[\/[Fail, IndexedSeq[Project]]] = {
      projectRepository.list(db.pool)
    }

    /**
     * Find all projects belonging to a given course
     *
     * @param courseId the unique id of the course to filter by
     * @return a future disjunction containing either a vector of projects, or a failure
     */
    override def list(courseId: UUID): Future[\/[Fail, IndexedSeq[Project]]] = {
      (for {
        course <- lift(courseRepository.find(courseId)(db.pool))
        projects <- lift(projectRepository.list(course)(db.pool))
      } yield projects).run
    }

    /**
     * Find a single project by slug.
     *
     * @return an optional project
     */
    override def find(projectSlug: String): Future[\/[Fail, Project]] = {
      projectRepository.find(projectSlug)(db.pool)
    }

    /**
     * Find a single project by ID.
     *
     * @return an optional project
     */
    override def find(id: UUID): Future[\/[Fail, Project]] = {
      projectRepository.find(id)(db.pool)
    }

    /**
     * Find a project *if and only if* a user has access to that project.
     *
     * @param projectId the unique id of the project to find
     * @param userId the unique id of the user to filter by
     * @return a future disjunction containing either a project, or a failure
     */
    override def find(projectId: UUID, userId: UUID): Future[\/[Fail, Project]] = {
      (for {
        user <- lift(userRepository.find(userId)(db.pool))
        project <- lift(projectRepository.find(projectId, user)(db.pool))
      }
      yield project).run
    }

    /**
     * Find a single project by slug and UserID.
     *
     * @return an optional project
     */
    override def find(projectSlug: String, userId: UUID): Future[\/[Fail, Project]] = {
      (for {
        user <- lift(userRepository.find(userId)(db.pool))
        project <- lift(projectRepository.find(projectSlug)(db.pool))
        projectFiltered <- lift(projectRepository.find(project.id, user)(db.pool))
      }
      yield projectFiltered).run
    }

    /**
     * Create a new project, with a single part and an empty task.
     *
     * New projects will *always* need at least one part and task to be useful,
     * so we will simply the creation process and do it all at once.
     *
     * @param name The new name to give the project.
     * @param slug The new slug to give the project.
     * @param description The new description for the project.
     * @return the updated project.
     */
    override def create(courseId: UUID, name: String, slug: String, description: String, availability: String): Future[\/[Fail, Project]] = {
      // First instantiate a new Project, Part and Task.
      val newProject = Project(
        courseId = courseId,
        name = name,
        slug = slug,
        description = description,
        availability = availability,
        parts = IndexedSeq.empty[Part]
      )
      val newPart = Part(projectId = newProject.id, name = "")
      val newTask = LongAnswerTask(partId = newPart.id, position = 1)

      // Then insert the new project, part and task into the database, wrapped
      // in a transaction such that either all three are created, or none.
      transactional { implicit conn: Connection =>
        (for {
          _              <- lift(validateSlug(slug))
          createdProject <- lift(projectRepository.insert(newProject))
          createdPart    <- lift(partRepository.insert(newPart))
          createdTask    <- lift(taskRepository.insert(newTask))
        }
        yield {
          val tasks = IndexedSeq(createdTask)
          val parts = IndexedSeq(createdPart.copy(tasks = tasks))
          val completeProject = createdProject.copy(parts = parts)
          completeProject
        }).run
      }
    }

    /**
     * Update an existing project.
     *
     * @param id The unique ID of the project to update.
     * @param version The current version of the project.
     * @param name The new name to give the project.
     * @param description The new description for the project.
     * @return the updated project.
     */
    override def updateInfo(id: UUID, version: Long,
                            courseId: Option[UUID],
                            name: Option[String],
                            description: Option[String],
                            availability: Option[String]): Future[\/[Fail, Project]] = {
      transactional { implicit conn: Connection =>
        (for {
          existingProject <- lift(projectRepository.find(id))
          _ <- predicate (existingProject.version == version) (LockFail(Messages("services.ProjectService.updateInfo.lockFail", version, existingProject.version)))
          toUpdate = existingProject.copy(
            courseId     = courseId.getOrElse(existingProject.courseId),
            name         = name.getOrElse(existingProject.name),
            description  = description.getOrElse(existingProject.description),
            availability = availability.getOrElse(existingProject.availability)
          )
          updatedProject <- lift(projectRepository.update(toUpdate))
        }
        yield updatedProject).run
      }
    }

    /**
     * Update a project's slug. This is a URL-friendly unique identifier for the project.
     *
     * @param id
     * @param version
     * @param slug
     * @return
     */
    override def updateSlug(id: UUID, version: Long, slug: String): Future[\/[Fail, Project]] = {
      transactional { implicit conn: Connection =>
        (for {
          existingProject <- lift(projectRepository.find(id))
          _ <- predicate (existingProject.version == version) (LockFail(Messages("services.ProjectService.updateSlug.lockFail", version, existingProject.version)))
          validSlug <- lift(validateSlug(slug))
          toUpdate = existingProject.copy(slug = validSlug)
          updatedProject <- lift(projectRepository.update(toUpdate))
        } yield updatedProject).run
      }
    }

    /**
     * Delete a project.
     *
     * NB: This deletes the entire project! All parts, all tasks, including all
     *     responses and scratch pads. Wrapped in a transaction.
     *
     * @param id the [[UUID]] of the project to delete
     * @param version the [[version]] of the project to delete
     * @return a boolean indicating success/failure
     */
    override def delete(id: UUID, version: Long): Future[\/[Fail, Project]] = {
      transactional { implicit conn: Connection =>
        (for {
          project <- lift(find(id))
          _ <- predicate (project.version == version) (LockFail(Messages("services.ProjectService.delete.lockFail", version, project.version)))
          componentsRemoved <- lift(serializedT(project.parts)(componentRepository.removeFromPart))
          partsDeleted <- lift(partRepository.delete(project))
          projectDeleted <- lift(projectRepository.delete(project))
        }
        yield projectDeleted).run
      }
    }

    /**
     * List the parts that have a component.
     *
     * @param componentId the [[UUID]] of the component to filter by
     */
    override def listPartsInComponent(componentId: UUID): Future[\/[Fail, IndexedSeq[Part]]] = ???
//    {
//      val fPartList = for {
//        component <- componentRepository.find(componentId)(db.pool).map(_.get)
//        partList <- partRepository.list(component)
//      } yield partList
//
//      fPartList.recover {
//        case exception => throw exception
//      }
//    }

    /**
     * Find a single part.
     *
     * @param partId the id of the part
     * @return a vector of this project's parts
     */
    override def findPart(partId: UUID): Future[\/[Fail, Part]] = {
      partRepository.find(partId)(db.pool)
    }

    /**
     * Create a new part.
     *
     * NB: If the new part's position interferes with the position of an existing part,
     *     the part list for the project will be updated to reflect the reordered positions.
     *     This will necessarily increment their version numbers. The client should be aware
     *     to update its part list after calling create.
     *
     * @param projectId the unique ID of the project to attach this part to
     * @param name the part's name
     * @param description a brief description of the part
     * @param position this part's position in the project. If this position has already
     *                 been taken, the existing parts will be shifted down.
     */
    override def createPart(projectId: UUID, name: String, description: String, position: Int): Future[\/[Fail, Part]] = {
      transactional { implicit conn: Connection =>
        (for {
          project <- lift(projectRepository.find(projectId))
          partList <- lift(partRepository.list(project))
          partListUpdated <- lift {
            // If there is already a part with this position, shift it (and all following parts)
            // back by one to make room for the new one.
            val positionExists = partList.filter(_.position == position).nonEmpty
            if (positionExists) {
              val filteredPartList = partList.filter(_.position >= position).map(part => part.copy(position = part.position + 1))
              serializedT(filteredPartList)(partRepository.update)
            }
            else {
              // Otherwise do nothing, return the existing part list.
              Future.successful(\/-(partList))
            }
          }
          newPart = Part(projectId = project.id, name = name, position = position)
          createdPart <- lift(partRepository.insert(newPart))
        } yield createdPart).run
      }
    }

    /**
     * Update an existing part.
     *
     * NB: If the part's updated position interferes with the position of another part,
     *     the part list for the project will be updated to reflect the reordered positions.
     *     This will necessarily increment their version numbers. The client should be aware
     *     to update its part list after calling update.
     */
    override def updatePart(partId: UUID, version: Long, name: String, newPosition: Int): Future[\/[Fail, Part]] = {
      transactional { implicit conn: Connection =>
        (for {
          existingPart <- lift(partRepository.find(partId))
          _ <- predicate (existingPart.version == version) (LockFail(Messages("services.ProjectService.updatePart.lockFail", version, existingPart.version)))
          oldPosition = existingPart.position
          project <- lift(projectRepository.find(existingPart.projectId))
          partList <- lift(partRepository.list(project))
          partListUpdated <- lift {
            if (newPosition != oldPosition) {
              val filteredPartList = partList.filter(_.id != partId)
              var temp = IndexedSeq[Part]()
              for (i <- filteredPartList.indices) {
                if (i >= newPosition) {
                  temp = temp :+ filteredPartList(i).copy(position = i+1)
                }
                else {
                  temp = temp :+ filteredPartList(i).copy(position = i)
                }
              }
              val filteredOrderedPartList = temp

              if (filteredOrderedPartList.nonEmpty) {
                serializedT(filteredOrderedPartList)(partRepository.update)
              }
              else {
                Future.successful(\/-(IndexedSeq()))
              }
            } else Future.successful(\/-(partList))
          }
          toUpdate = existingPart.copy(
            name = name,
            position = newPosition
          )
          // Now we insert the new part
          updatedPart <- lift(partRepository.update(toUpdate))
        } yield updatedPart).run
      }
    }

    /**
     * Delete a part.
     *
     * NB: When a part is removed from a project, the project's remaining parts must be re-ordered.
     *     This necessarily updates the versions of the updated parts w.r.t. optimistic
     *     offline lock.
     *
     * @param partId the unique ID of the part to delete
     * @param version the current version of the part to delete
     * @return a boolean indicator whether the operation was successful
     */
    override def deletePart(partId: UUID, version: Long): Future[\/[Fail, Part]] = {
      transactional { implicit conn: Connection =>
        (for {
          part <- lift(partRepository.find(partId))
          project <- lift(projectRepository.find(part.projectId))
          partList <- lift(partRepository.list(project))
          partListUpdated <- lift {
            val filteredPartList = partList.filter({ item => item.id != part.id && item.position > part.position}).map(part => part.copy(position = part.position - 1))
            if (filteredPartList.nonEmpty)
              serializedT(filteredPartList)(partRepository.update)
            else
              Future.successful(\/-(IndexedSeq()))
          }
          tasksDeleted <- lift(taskRepository.delete(part))
          componentsRemoved <- lift(componentRepository.removeFromPart(part))
          deletedPart <- lift(partRepository.delete(part))
        } yield deletedPart).run
      }
    }

    /**
     * Enable a disabled part, and disable an enabled part.
     *
     * @param partId the unique ID of the part to toggle
     * @param version the current version of the part to toggle
     * @return a future disjunction containing either the toggled part, or a failure
     */
    override def togglePart(partId: UUID, version: Long): Future[\/[Fail, Part]] = {
      transactional { implicit conn: Connection =>
        (for {
          part <- lift(partRepository.find(partId))
          toUpdate = part.copy(version = version, enabled = if (part.enabled) false else true)
          toggled <- lift(partRepository.update(toUpdate))
        } yield toggled).run
      }
    }

    /**
     * Reorder the parts in a project.
     *
     * @param projectId the unique id of the project to reorder parts for
     * @param partIds the unique ids of the project's current parts, in the new
     *                order the parts should have
     * @return a future disjunction containing either the updated project, or a failure
     */
    override def reorderParts(projectId: UUID, partIds: IndexedSeq[UUID]): Future[\/[Fail, Project]] = {
      transactional { implicit conn: Connection =>
        (for {
          project <- lift(projectRepository.find(projectId))
          parts <- lift(partRepository.list(project))
          reordered <- lift {
            val orderedParts = parts.map { part =>
              part.copy(position = partIds.indexOf(part.id)+1)
            }
            serializedT(orderedParts)(partRepository.update)
          }
          project <- lift(projectRepository.find(projectId))
        } yield project).run
      }
    }

    /**
     * Create a new task.
     *
     * @param partId the unique ID of the part this task belongs to
     * @param name the name of this task
     * @param description a brief description of this task
     * @param position the position of this task in the part
     * @param dependencyId optionally make this task dependent on another
     * @return the newly created task
     */
    override def createTask(partId: UUID, taskType: Int, name: String, description: String, position: Int, dependencyId: Option[UUID]): Future[\/[Fail, Task]] = {
      transactional { implicit conn: Connection =>
        (for {
          part <- lift(partRepository.find(partId))
          taskList <- lift(taskRepository.list(part))
          // If the dependency id is given, ensure the depended-upon task exists
          dependency <- lift(dependencyId match {
            case Some(depId) => taskRepository.find(depId).map {
              case \/-(depTask) => \/-(Some(depTask))
              case -\/(error) => -\/(error)
            }
            case None => Future.successful(\/-(None))
          })
          taskListUpdated <- lift {
            // If there is already a task with this position, shift it (and all following tasks)
            // back by one to make room for the new one.
            val positionExists = taskList.filter(_.position == position).nonEmpty
            if (positionExists) {
              val filteredTaskList = taskList.filter(_.position >= position).map {
                case task: LongAnswerTask     => task.copy(position = task.position + 1)
                case task: ShortAnswerTask    => task.copy(position = task.position + 1)
                case task: MultipleChoiceTask => task.copy(position = task.position + 1)
                case task: OrderingTask       => task.copy(position = task.position + 1)
                case task: MatchingTask       => task.copy(position = task.position + 1)
              }
              serializedT(filteredTaskList)(taskRepository.update)
            }
            else {
              // Otherwise do nothing, return the existing task list.
              Future.successful(\/-(taskList))
            }
          }
          newTask = Task(
            partId = partId,
            taskType = taskType,
            position = position,
            settings = CommonTaskSettings(
              title = name,
              description = description,
              dependencyId = dependencyId
            )
          )
          createdTask <- lift(taskRepository.insert(newTask))
        }
        yield newTask).run
      }
    }

    /**
     * Find a task by its ID.
     */
    override def findTask(taskId: UUID): Future[\/[Fail, Task]] = {
      taskRepository.find(taskId)(db.pool)
    }

    /**
     * Find a task by its position in a project.
     *
     * @param projectSlug the text slug of the project this task is in
     * @param partNum the number (corresponds to 'position' field) of the part
     *                that this task is in.
     * @param taskNum the number (position) of the task inside the part.
     * @return an [[Option[Task]]] if one was found.
     */
    override def findTask(projectSlug: String, partNum: Int, taskNum: Int): Future[\/[Fail, Task]] = {
      (for {
        project <- lift(projectRepository.find(projectSlug)(db.pool))
        taskOption <- lift(taskRepository.find(project, partNum, taskNum)(db.pool))
      }
      yield taskOption).run
    }

    /**
     * Find the "now" task for a student in a project
     *
     * @param userId the unique of the id of the student
     * @param projectId the unique id of the project
     * @return a future disjunction containing either the now task, or a failure
     */
    override def findNowTask(userId: UUID, projectId: UUID): Future[\/[Fail, Task]] = {
      (for {
        student <- lift(userRepository.find(userId)(db.pool))
        project <- lift(projectRepository.find(projectId)(db.pool))
        taskOption <- lift(taskRepository.findNow(student, project)(db.pool))
      }
      yield taskOption).run
    }

    /**
     * Update an existing task.
     *
     * The trickiest part of this function is the re-ordering logic. Part of our
     * 'domain logic' is ensuring that the tasks in a part remain in the
     * correct order.
     */
    private def updateTask(existingTask: Task, updatedTask: Task): Future[\/[Fail, Task]] = {
      transactional { implicit conn: Connection =>
        (for {
          oldPart <- lift(partRepository.find(existingTask.partId))
          newPart <- lift(partRepository.find(updatedTask.partId))

          opTasks <- lift(taskRepository.list(oldPart))
          npTasks <- lift(taskRepository.list(newPart))

          oldPosition = existingTask.position
          newPosition = updatedTask.position

          oldListUpdated <- lift {
            if (existingTask.partId == updatedTask.partId) {
              Future successful \/-(IndexedSeq.empty[Task])
            }
            else if (opTasks.nonEmpty) {
              // Update old task list to remove this task from the ordering.
              var filteredOrderedTaskList = IndexedSeq.empty[Task]
              for (i <- opTasks.indices) {
                filteredOrderedTaskList = filteredOrderedTaskList :+ {opTasks(i) match {
                  case task: LongAnswerTask => task.copy(position = i)
                  case task: ShortAnswerTask => task.copy(position = i)
                  case task: MultipleChoiceTask => task.copy(position = i)
                  case task: OrderingTask => task.copy(position = i)
                  case task: MatchingTask => task.copy(position = i)
                  case _ => throw new Exception("Gold star for epic coding failure.")
                }}
              }
              serializedT(filteredOrderedTaskList)(taskRepository.update)
            }
            else { Future successful \/-(IndexedSeq.empty[Task]) }
          }

          newListUpdated <- lift {
            // The "new" list is the list that the task ends up in, whether it has
            // moved or not. If the task has changed parts, or if it hasn't changed
            // parts but its position number has changed, then the "new" list's ordering
            // must be updated.
            if (existingTask.partId   != updatedTask.partId ||
                existingTask.position != updatedTask.position) {
              val filteredTaskList = npTasks.filter(_.id != updatedTask.id)
              var filteredOrderedTaskList = IndexedSeq.empty[Task]
              for (i <- filteredTaskList.indices) {
                if (i >= newPosition) {
                  filteredOrderedTaskList = filteredOrderedTaskList :+ {filteredTaskList(i) match {
                    case task: LongAnswerTask => task.copy(position = i+1)
                    case task: ShortAnswerTask => task.copy(position = i+1)
                    case task: MultipleChoiceTask => task.copy(position = i+1)
                    case task: OrderingTask => task.copy(position = i+1)
                    case task: MatchingTask => task.copy(position = i+1)
                    case _ => throw new Exception("Gold star for epic coding failure.")
                  }}
                }
                else {
                  filteredOrderedTaskList = filteredOrderedTaskList :+ {filteredTaskList(i) match {
                    case task: LongAnswerTask => task.copy(position = i)
                    case task: ShortAnswerTask => task.copy(position = i)
                    case task: MultipleChoiceTask => task.copy(position = i)
                    case task: OrderingTask => task.copy(position = i)
                    case task: MatchingTask => task.copy(position = i)
                    case _ => throw new Exception("Gold star for epic coding failure.")
                  }}
                }
              }

              if (filteredOrderedTaskList.nonEmpty) {
                serializedT(filteredOrderedTaskList)(taskRepository.update)
              }
              else { Future.successful(\/-(IndexedSeq())) }
            }
            else { Future successful \/-(npTasks) }
          }

          // Now we insert the new part
          updatedTask <- lift(taskRepository.update(updatedTask))
        } yield updatedTask).run
      }
    }

    /**
     * Update a LongAnswerTask.
     *
     * @param taskId
     * @param version
     * @param name
     * @param description
     * @param position
     * @param notesAllowed
     * @param dependencyId
     * @param partId
     * @return
     */
    override def updateLongAnswerTask(taskId: UUID,
                                      version: Long,
                                      name: Option[String],
                                      description: Option[String],
                                      position: Option[Int],
                                      notesAllowed: Option[Boolean],
                                      dependencyId: Option[UUID] = None,
                                      partId: Option[UUID] = None): Future[\/[Fail, Task]] =
    {
      (for {
        task <- lift(taskRepository.find(taskId))
        _ <- predicate (task.version == version) (LockFail(Messages("services.ProjectService.updateTask.lockFail", version, task.version)))
        _ <- predicate (task.isInstanceOf[LongAnswerTask]) (BadInput(Messages("services.ProjectService.updateLongAnswerTask.wrongTaskType")))
        toUpdate = task.asInstanceOf[LongAnswerTask].copy(
          partId = partId.getOrElse(task.partId),
          position = position.getOrElse(task.position),
          settings = task.settings.copy(
            title = name.getOrElse(task.settings.title),
            description = description.getOrElse(task.settings.description),
            notesAllowed = notesAllowed.getOrElse(task.settings.notesAllowed),
            dependencyId = dependencyId match {
              case Some(newDepId) => Some(newDepId)
              case None => task.settings.dependencyId
            }
          )
        )
        updatedTask <- lift(updateTask(task, toUpdate))
      } yield updatedTask).run
    }

    /**
     * Update a ShortAnswerTask
     *
     * @param taskId
     * @param version
     * @param name
     * @param description
     * @param position
     * @param notesAllowed
     * @param maxLength
     * @param dependencyId
     * @param partId
     * @return
     */
    def updateShortAnswerTask(taskId: UUID,
                              version: Long,
                              name: Option[String],
                              description: Option[String],
                              position: Option[Int],
                              notesAllowed: Option[Boolean],
                              maxLength: Option[Int],
                              dependencyId: Option[UUID] = None,
                              partId: Option[UUID] = None): Future[\/[Fail, Task]] =
    {
      (for {
        task <- lift(taskRepository.find(taskId))
        _ <- predicate (task.version == version) (LockFail(Messages("services.ProjectService.updatePart.lockFail", version, task.version)))
        _ <- predicate (task.isInstanceOf[ShortAnswerTask]) (BadInput(Messages("services.ProjectService.updateShortAnswerTask.wrongTaskType")))
        shortAnswerTask = task.asInstanceOf[ShortAnswerTask]
        toUpdate = shortAnswerTask.copy(
          partId = partId.getOrElse(task.partId),
          position = position.getOrElse(task.position),
          settings = task.settings.copy(
            title = name.getOrElse(task.settings.title),
            description = description.getOrElse(task.settings.description),
            notesAllowed = notesAllowed.getOrElse(task.settings.notesAllowed),
            dependencyId = dependencyId match {
              case Some(newDepId) => Some(newDepId)
              case None => task.settings.dependencyId
            }
          ),
          maxLength = maxLength.getOrElse(shortAnswerTask.maxLength)
        )
        updatedTask <- lift(updateTask(task, toUpdate))
      } yield updatedTask).run
    }

    /**
     * Update a MultipleChoiceTask
     *
     * @param taskId
     * @param version
     * @param name
     * @param description
     * @param position
     * @param notesAllowed
     * @param choices
     * @param answer
     * @param allowMultiple
     * @param randomizeChoices
     * @param dependencyId
     * @param partId
     * @return
     */
    def updateMultipleChoiceTask(taskId: UUID,
                                 version: Long,
                                 name: Option[String],
                                 description: Option[String],
                                 position: Option[Int],
                                 notesAllowed: Option[Boolean],
                                 choices: Option[IndexedSeq[String]] = Some(IndexedSeq()),
                                 answer: Option[IndexedSeq[Int]] = Some(IndexedSeq()),
                                 allowMultiple: Option[Boolean] = Some(false),
                                 randomizeChoices: Option[Boolean] = Some(true),
                                 dependencyId: Option[UUID] = None,
                                 partId: Option[UUID] = None): Future[\/[Fail, Task]] =
    {
      (for {
        task <- lift(taskRepository.find(taskId))
        _ <- predicate (task.version == version) (LockFail(Messages("services.ProjectService.updateTask.lockFail", version, task.version)))
        _ <- predicate (task.isInstanceOf[MultipleChoiceTask]) (BadInput(Messages("services.ProjectService.updateMultipleChoiceTask.wrongTaskType")))
        mcTask = task.asInstanceOf[MultipleChoiceTask]
        toUpdate = mcTask.copy(
          partId = partId.getOrElse(task.partId),
          position = position.getOrElse(task.position),
          settings = task.settings.copy(
            title = name.getOrElse(task.settings.title),
            description = description.getOrElse(task.settings.description),
            notesAllowed = notesAllowed.getOrElse(task.settings.notesAllowed),
            dependencyId = dependencyId match {
              case Some(newDepId) => Some(newDepId)
              case None => mcTask.settings.dependencyId
            }
          ),
          choices = choices.getOrElse(mcTask.choices),
          answer = answer.getOrElse(mcTask.answer),
          allowMultiple = allowMultiple.getOrElse(mcTask.allowMultiple),
          randomizeChoices = randomizeChoices.getOrElse(mcTask.randomizeChoices)
        )
        updatedTask <- lift(updateTask(task, toUpdate))
      } yield updatedTask).run
    }

    /**
     * Update an OrderingTask
     *
     * @param taskId
     * @param version
     * @param name
     * @param description
     * @param position
     * @param notesAllowed
     * @param elements
     * @param answer
     * @param randomizeChoices
     * @param dependencyId
     * @param partId
     * @return
     */
    def updateOrderingTask(taskId: UUID,
                           version: Long,
                           name: Option[String],
                           description: Option[String],
                           position: Option[Int],
                           notesAllowed: Option[Boolean],
                           elements: Option[IndexedSeq[String]] = Some(IndexedSeq()),
                           answer: Option[IndexedSeq[Int]] = Some(IndexedSeq()),
                           randomizeChoices: Option[Boolean] = Some(true),
                           dependencyId: Option[UUID] = None,
                           partId: Option[UUID] = None): Future[\/[Fail, Task]] =
    {
      (for {
        task <- lift(taskRepository.find(taskId))
        _ <- predicate (task.version == version) (LockFail(Messages("services.ProjectService.updateTask.lockFail", version, task.version)))
        _ <- predicate (task.isInstanceOf[OrderingTask]) (BadInput(Messages("services.ProjectService.updateOrderingTask.wrongTaskType")))
        orderingTask = task.asInstanceOf[OrderingTask]
        toUpdate = orderingTask.copy(
          partId = partId.getOrElse(task.partId),
          position = position.getOrElse(task.position),
          settings = task.settings.copy(
            title = name.getOrElse(task.settings.title),
            description = description.getOrElse(task.settings.description),
            notesAllowed = notesAllowed.getOrElse(task.settings.notesAllowed),
            dependencyId = dependencyId match {
              case Some(newDepId) => Some(newDepId)
              case None => task.settings.dependencyId
            }
          ),
          elements = elements.getOrElse(orderingTask.elements),
          answer = answer.getOrElse(orderingTask.answer),
          randomizeChoices = randomizeChoices.getOrElse(orderingTask.randomizeChoices)
        )
        updatedTask <- lift(updateTask(task, toUpdate))
      } yield updatedTask).run
    }

    /**
     * Update a MatchingTask
     *
     * @param taskId
     * @param version
     * @param name
     * @param description
     * @param position
     * @param notesAllowed
     * @param elementsLeft
     * @param elementsRight
     * @param answer
     * @param randomizeChoices
     * @param dependencyId
     * @param partId
     * @return
     */
    def updateMatchingTask(taskId: UUID,
                           version: Long,
                           name: Option[String],
                           description: Option[String],
                           position: Option[Int],
                           notesAllowed: Option[Boolean],
                           elementsLeft: Option[IndexedSeq[String]] = Some(IndexedSeq()),
                           elementsRight: Option[IndexedSeq[String]] = Some(IndexedSeq()),
                           answer: Option[IndexedSeq[MatchingTask.Match]] = Some(IndexedSeq()),
                           randomizeChoices: Option[Boolean] = Some(true),
                           dependencyId: Option[UUID] = None,
                           partId: Option[UUID] = None): Future[\/[Fail, Task]] =
    {
      (for {
        task <- lift(taskRepository.find(taskId))
        _ <- predicate (task.version == version) (LockFail(Messages("services.ProjectService.updateTask.lockFail", version, task.version)))
        _ <- predicate (task.isInstanceOf[MatchingTask]) (BadInput(Messages("services.ProjectService.updateMatchingTask.wrongTaskType")))
        matchingTask = task.asInstanceOf[MatchingTask]
        toUpdate = matchingTask.copy(
          partId = partId.getOrElse(task.partId),
          position = position.getOrElse(task.position),
          settings = task.settings.copy(
            title = name.getOrElse(task.settings.title),
            description = description.getOrElse(task.settings.description),
            notesAllowed = notesAllowed.getOrElse(task.settings.notesAllowed),
            dependencyId = dependencyId match {
              case Some(newDepId) => Some(newDepId)
              case None => task.settings.dependencyId
            }
          ),
          elementsLeft = elementsLeft.getOrElse(matchingTask.elementsLeft),
          elementsRight = elementsRight.getOrElse(matchingTask.elementsRight),
          answer = answer.getOrElse(matchingTask.answer),
          randomizeChoices = randomizeChoices.getOrElse(matchingTask.randomizeChoices)
        )
        updatedTask <- lift(updateTask(task, toUpdate))
      } yield updatedTask).run
    }


    /**
     * Delete a task.
     *
     * NB: When a task is removed from a part, the part's remaining tasks must be re-ordered.
     *     This necessarily updates the versions of the updated tasks w.r.t. optimistic
     *     offline lock.
     *
     * @param taskId the unique ID of the task to delete
     * @param version the current version of the task to delete
     * @return a boolean indicator whether the operation was successful
     */
    override def deleteTask(taskId: UUID, version: Long): Future[\/[Fail, Task]] = {
      transactional { implicit conn: Connection =>
        (for {
          genericTask <- lift(taskRepository.find(taskId))
          _ <- predicate (genericTask.version == version) (LockFail(Messages("services.ProjectService.deleteTask.lockFail", version, genericTask.version)))
          task = genericTask match {
            case task: LongAnswerTask => task.copy(version = version)
            case task: ShortAnswerTask => task.copy(version = version)
            case task: MultipleChoiceTask => task.copy(version = version)
            case task: OrderingTask => task.copy(version = version)
            case task: MatchingTask => task.copy(version = version)
            case _ => throw new Exception("Gold star for epic coding failure.")
          }
          part <- lift(partRepository.find(task.partId))
          taskList <- lift(taskRepository.list(part))
          taskListUpdated <- lift {
            // If there is already a part with this position, shift it (and all following parts)
            // back by one to make room for the new one.
            val filteredTaskList = taskList.filter(_.position > task.position).map {
              case task: LongAnswerTask => task.copy(position = task.position - 1)
              case task: ShortAnswerTask => task.copy(position = task.position - 1)
              case task: MultipleChoiceTask => task.copy(position = task.position - 1)
              case task: OrderingTask => task.copy(position = task.position - 1)
              case task: MatchingTask => task.copy(position = task.position - 1)
              case _ => throw new Exception("Gold star for epic coding failure.")
            }
            serializedT(filteredTaskList)(taskRepository.update)
          }
          deletedTask <- lift(taskRepository.delete(task))
        } yield deletedTask).run
      }
    }

    /**
     * Moves a task from one part to another.
     *
     * @param taskId the unique ID of the task to be moved
     * @param partId the new part that this task should belong to
     * @param newPosition the new position for this task
     */
    override def moveTask(partId: UUID, taskId: UUID, newPosition: Int): Future[\/[Fail, Task]] = {
      transactional { implicit conn: Connection =>
        (for {
          task <- lift(taskRepository.find(taskId))
          toUpdate = task match {
            case task: LongAnswerTask =>     task.copy(position = newPosition, partId = partId)
            case task: ShortAnswerTask =>    task.copy(position = newPosition, partId = partId)
            case task: MultipleChoiceTask => task.copy(position = newPosition, partId = partId)
            case task: OrderingTask =>       task.copy(position = newPosition, partId = partId)
            case task: MatchingTask =>       task.copy(position = newPosition, partId = partId)
            case _ => throw new Exception("Gold star for epic coding failure.")
          }
          movedTask <- lift(this.updateTask(task, toUpdate))
        } yield movedTask).run
      }
    }

    /**
     * Check if a slug is of the valid format.
     *
     * @param slug the slug to be checked
     * @return a future disjunction containing either the slug, or a failure
     */
    private def isValidSlug(slug: String): Future[\/[Fail, String]] = Future successful {
      if ("""[A-Za-z0-9\_\-]+""".r.unapplySeq(slug).isDefined) \/-(slug)
      else -\/(BadInput(s"$slug is not a valid e-mail format."))
    }

    /**
     * Validate a slug for use in a project.
     *
     * @param slug the slug to be checked
     * @param existingId an optional unique id for an existing project to exclude
     * @return a future disjunction containing either the slug, or a failure
     */
    private def validateSlug(slug: String, existingId: Option[UUID] = None)(implicit conn: Connection): Future[\/[Fail, String]] = {
      val existing = for {
        validSlug <- lift(isValidSlug(slug))
        project <- lift(projectRepository.find(validSlug))
      } yield project

      existing.run.map {
        case \/-(project) =>
          if (existingId.isEmpty || (existingId.get != project.id)) -\/(UniqueFieldConflict(s"The slug $slug is already in use."))
          else \/-(slug)
        case -\/(error: NoResults) => \/-(slug)
        case -\/(otherErrors: Fail) => -\/(otherErrors)
      }
    }
  }
}
