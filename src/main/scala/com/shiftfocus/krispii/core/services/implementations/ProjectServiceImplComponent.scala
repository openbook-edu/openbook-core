package com.shiftfocus.krispii.core.services

import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import com.shiftfocus.krispii.core.models._
import com.shiftfocus.krispii.core.models.tasks.Task
import com.shiftfocus.krispii.core.repositories._
import com.shiftfocus.krispii.core.services.datasource._
import com.shiftfocus.krispii.core.lib.UUID
import play.api.Logger
import scala.concurrent.Future

trait ProjectServiceImplComponent extends ProjectServiceComponent {
  self: ProjectRepositoryComponent with
        PartRepositoryComponent with
        TaskRepositoryComponent with
        TaskResponseRepositoryComponent with
        TaskScratchpadRepositoryComponent with
        TaskFeedbackRepositoryComponent with
        ComponentRepositoryComponent with
        SectionRepositoryComponent with
        DB =>

  override val projectService: ProjectService = new ProjectServiceImpl

  private class ProjectServiceImpl extends ProjectService {

    /**
     * Lists all projects.
     *
     * @return a vector of projects
     */
    override def list: Future[IndexedSeq[Project]] = {
      projectRepository.list
    }

    /**
     * Find a single project.
     *
     * @return an optional project
     */
    override def find(projectSlug: String): Future[Option[Project]] = {
      for {
        projectOption <- projectRepository.find(projectSlug)
        parts <- { projectOption match {
          case Some(project) => partRepository.list(project)
          case None => Future.successful(IndexedSeq())
        }}
      }
      yield projectOption match {
        case Some(project) => Some(project.copy(parts = parts))
        case None => None
      }
    }

    /**
     * Find a single project.
     *
     * @return an optional project
     */
    override def find(id: UUID): Future[Option[Project]] = {
      for {
        projectOption <- projectRepository.find(id)
        parts <- { projectOption match {
          case Some(project) => partRepository.list(project).flatMap { parts =>
            Future.sequence(parts.map { part =>
              taskRepository.list(part).map { tasks =>
                part.copy(tasks = tasks)
              }
            })
          }
          case None => Future.successful(IndexedSeq())
        }}
      }
      yield projectOption match {
        case Some(project) => Some(project.copy(parts = parts))
        case None => None
      }
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
    override def create(name: String, slug: String, description: String): Future[Project] = {
      // First instantiate a new Project, Part and Task.
      val newProject = Project(
        name = name,
        slug = slug,
        description = description,
        parts = IndexedSeq[Part]()
      )
      val newPart = Part(
        projectId = newProject.id,
        name = ""
      )
      val newTask = Task(
        partId = newPart.id,
        name = ""
      )

      // Then insert the new project, part and task into the database, wrapped
      // in a transaction such that either all three are created, or none.
      transactional { implicit connection =>
        for {
          createdProject <- projectRepository.insert(newProject)
          createdPart    <- partRepository.insert(newPart)
          createdTask    <- taskRepository.insert(newTask)
        }
        yield {
          val tasks = IndexedSeq(createdTask)
          val parts = IndexedSeq(createdPart.copy(tasks = tasks))
          val completeProject = createdProject.copy(parts = parts)
          completeProject
        }
      }
    }

    /**
     * Update an existing project.
     *
     * @param id The unique ID of the project to update.
     * @param version The current version of the project.
     * @param name The new name to give the project.
     * @param slug The new slug to give the project.
     * @param description The new description for the project.
     * @return the updated project.
     */
    override def update(id: UUID, version: Long, name: String, slug: String, description: String): Future[Project] = {
      transactional { implicit connection =>
        for {
          existingProjectOption <- projectRepository.find(id)
          updatedProject <- projectRepository.update(existingProjectOption.get.copy(
            version = version,
            name = name,
            slug = slug,
            description = description
          ))
        }
        yield updatedProject
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
    override def delete(id: UUID, version: Long): Future[Boolean] = {
      transactional { implicit connection =>
        for {
          project <- projectRepository.find(id).map(_.get)
          parts <- partRepository.list(project)
          tasks <- Future.sequence(parts.map { part =>
            taskRepository.list(part)
          }).map { taskLists => taskLists.flatten }
          scratchpadsDeleted <- serialized(tasks)(taskScratchpadRepository.delete)
          responsesDeleted <- serialized(tasks)(taskResponseRepository.delete)
          feedbacksDeleted <- serialized(tasks)(taskFeedbackRepository.delete)
          tasksDeleted <- serialized(tasks)(taskRepository.delete)
          sectionsDisabled <- serialized(parts)(sectionRepository.disablePart)
          componentsRemoved <- serialized(parts)(componentRepository.removeFromPart)
          partsDeleted <- partRepository.delete(project)
          projectDeleted <- projectRepository.delete(project)
        }
        yield projectDeleted
      }
    }

    /**
     * Load the parts, tasks and responses of a project for a user, including
     * part/task status.
     *
     * @param projectId  the unique ID for the project
     * @param user  the user to fetch information for
     * @return a vector of [[TaskGroup]] objects
     */
    override def taskGroups(project: Project, user: User): Future[IndexedSeq[TaskGroup]] = {
      val fTaskGroups = for {
        projectParts <- partRepository.list(project)
        enabledParts <- partRepository.listEnabled(project, user)
        tasks <- taskRepository.list(project)
        responses <- taskResponseRepository.list(user, project)(db.pool)
      }
      yield {
        projectParts.map { part =>
          TaskGroup(
            part = part,
            status = {
              if (enabledParts.filter(_.id.string == part.id.string).nonEmpty) Part.Unlocked
              else Part.Locked
            },
            tasks = tasks.filter(_.partId.string == part.id.string).map { task =>
              val maybeTaskResponse = responses.find(_.taskId.string == task.id.string)

              TaskGroupItem(
                status = { maybeTaskResponse match {
                  case None => Task.NotStarted
                  case Some(response) => {
                    if (response.isComplete) Task.Complete
                    else Task.Incomplete
                  }
                }},
                task = task
              )
            }
          )
        }
      }
      fTaskGroups.recover {
        case exception => throw exception
      }
    }

    /**
     * List the parts in a project.
     *
     * @param projectId the id of the project
     * @return a vector of this project's parts
     */
    override def listParts(projectId: UUID): Future[IndexedSeq[Part]] = {
      val fPartList = for {
        project <- projectRepository.find(projectId).map(_.get)
        partList <- partRepository.list(project)
      } yield partList

      fPartList.recover {
        case exception => throw exception
      }
    }

    /**
     * List the parts that have a component.
     *
     * @param componentId the [[UUID]] of the component to filter by
     */
    override def listPartsInComponent(componentId: UUID): Future[IndexedSeq[Part]] = {
      val fPartList = for {
        component <- componentRepository.find(componentId)(db.pool).map(_.get)
        partList <- partRepository.list(component)
      } yield partList

      fPartList.recover {
        case exception => throw exception
      }
    }

    /**
     * Find a single part.
     *
     * @param projectId the id of the part
     * @return a vector of this project's parts
     */
    override def findPart(partId: UUID): Future[Option[Part]] = {
      partRepository.find(partId)
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
    override def createPart(projectId: UUID, name: String, description: String, position: Int): Future[Part] = {
      transactional { implicit connection =>
        val fPart = for {
          project <- projectRepository.find(projectId).map(_.get)
          partList <- partRepository.list(project)
          partListUpdated <- {
            // If there is already a part with this position, shift it (and all following parts)
            // back by one to make room for the new one.
            val positionExists = partList.filter(_.position == position).nonEmpty
            if (positionExists) {
              val filteredPartList = partList.filter(_.position >= position).map(part => part.copy(position = part.position + 1))
              serialized(filteredPartList)(partRepository.update)
            }
            else {
              // Otherwise do nothing, return the existing part list.
              Future.successful(partList)
            }
          }
          // Now we insert the new part
          newPart <- partRepository.insert(Part(
            projectId = project.id,
            name = name,
            description = description,
            position = position
          ))
        } yield newPart

        // If any of the futures in our for-comprehension fail, recover from the failure,
        // pull the exception into this thread and throw it.
        fPart.recover {
          case exception => throw exception
        }
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
    override def updatePart(partId: UUID, version: Long, name: String, description: String, newPosition: Int): Future[Part] = {
      transactional { implicit connection =>
        val fPart = for {
          existingPart <- partRepository.find(partId).map(_.get)
          oldPosition <- Future.successful(existingPart.position)
          project <- projectRepository.find(existingPart.projectId).map(_.get)
          partList <- partRepository.list(project)
          partListUpdated <- {
            Logger.debug(s"Version from args: $version")
            Logger.debug(s"Version from current: ${existingPart.version}")
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
                // Since we're inside a transaction, we need to fold over the list,
                // rather than map, to ensure the database queries are executed
                // sequentially. We cannot send parallel queries inside of a
                // transaction!
                serialized(filteredOrderedPartList)(partRepository.update)
              }
              else Future.successful(IndexedSeq())
            } else Future.successful(partList)
          }
          // Now we insert the new part
          updatedPart <- partRepository.update(existingPart.copy(
            version = version,
            name = name,
            description = description,
            position = newPosition
          ))
        } yield updatedPart

        fPart.recover {
          case exception => throw exception
        }
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
    override def deletePart(partId: UUID, version: Long): Future[Boolean] = {
      transactional { implicit connection =>
        val fDeleted = for {
          part <- partRepository.find(partId).map(_.get.copy(version = version))
          project <- projectRepository.find(part.projectId).map(_.get)
          partList <- partRepository.list(project)
          partListUpdated <- {
            // If there is already a part with this position, shift it (and all following parts)
            // back by one to make room for the new one.
            val filteredPartList = partList.filter({ item => item.id != part.id && item.position > part.position}).map(part => part.copy(position = part.position - 1))
            if (filteredPartList.nonEmpty)
              serialized(filteredPartList)(partRepository.update)
            else
              Future.successful(IndexedSeq())
          }
          tasks <- Future.sequence(partList.map { part =>
            taskRepository.list(part)
          }).map { taskLists => taskLists.flatten }
          scratchpadsDeleted <- serialized(tasks)(taskScratchpadRepository.delete)
          responsesDeleted <- serialized(tasks)(taskResponseRepository.delete)
          feedbacksDeleted <- serialized(tasks)(taskFeedbackRepository.delete)
          tasksDeleted <- taskRepository.delete(part)
          sectionsDisabled <- sectionRepository.disablePart(part)
          componentsRemoved <- componentRepository.removeFromPart(part)
          deletedPart <- partRepository.delete(part)
        } yield deletedPart

        fDeleted.recover {
          case exception => throw exception
        }
      }
    }

    /**
     * Reorder the parts in a project.
     */
    override def reorderParts(projectId: UUID, partIds: IndexedSeq[UUID]): Future[Project] = {
      for {
        project <- projectRepository.find(projectId).map(_.get)
        parts <- partRepository.list(project)
        reordered <- {
          val orderedParts = parts.map { part =>
            part.copy(position = partIds.indexOf(part.id)+1)
          }
          partRepository.reorder(project, orderedParts)(db.pool).map { reorderedParts =>
            project.copy(parts = reorderedParts)
          }
        }
      } yield reordered
    }.recover {
      case exception => throw exception
    }

    /**
     * List all tasks associated with a project.
     *
     * @param projectId the unique ID of the project to list for
     * @return a vector of tasks
     */
    override def listTasks: Future[IndexedSeq[Task]] = {
      Logger.debug(s"projectService.listTasks")
      for {
        taskList <- taskRepository.list
      } yield taskList
    }.recover {
      case exception => throw exception
    }

    /**
     * List all tasks associated with a project.
     *
     * @param projectId the unique ID of the project to list for
     * @return a vector of tasks
     */
    override def listTasks(projectId: UUID): Future[IndexedSeq[Task]] = {
      Logger.debug(s"projectService.listTasks(${projectId.string})")
      for {
        project <- projectRepository.find(projectId).map(_.get)
        taskList <- taskRepository.list(project)
      } yield taskList
    }.recover {
      case exception => throw exception
    }

    /**
     * List all tasks associated with a project part.
     *
     * @param projectId the unique ID of the project to list for
     * @return a vector of tasks
     */
    override def listTasks(projectId: UUID, partNum: Int): Future[IndexedSeq[Task]] = {
      Logger.debug(s"projectService.listTasks(${projectId.string}, ${partNum})")
      for {
        project <- projectRepository.find(projectId).map(_.get)
        taskList <- taskRepository.list(project, partNum)
      } yield taskList
    }.recover {
      case exception => throw exception
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
    override def createTask(partId: UUID, name: String, description: String, position: Int, dependencyId: Option[UUID]): Future[Task] = {
      transactional { implicit connection =>
        for {
          part <- partRepository.find(partId).map(_.get)
          taskList <- taskRepository.list(part)
          taskListUpdated <- {
            // If there is already a task with this position, shift it (and all following tasks)
            // back by one to make room for the new one.
            val positionExists = taskList.filter(_.position == position).nonEmpty
            if (positionExists) {
              val filteredTaskList = taskList.filter(_.position >= position).map(task => task.copy(position = task.position + 1))
              serialized(filteredTaskList)(taskRepository.update)
            }
            else {
              // Otherwise do nothing, return the existing task list.
              Future.successful(taskList)
            }
          }
          newTask <- taskRepository.insert(Task(
            partId = partId,
            name = name,
            description = description,
            position = position,
            dependencyId = dependencyId
          ))
        }
        yield newTask
      }.recover {
        case exception => throw exception
      }
    }

    /**
     * Find a task by its ID.
     */
    override def findTask(taskId: UUID): Future[Option[Task]] = {
      taskRepository.find(taskId)
    }.recover {
      case exception => throw exception
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
    override def findTask(projectSlug: String, partNum: Int, taskNum: Int): Future[Option[Task]] = {
      for {
        project <- projectRepository.find(projectSlug).map(_.get)
        taskOption <- taskRepository.find(project, partNum, taskNum)
      }
      yield taskOption
    }.recover {
      case exception => throw exception
    }

    /**
     * Update an existing task.
     *
     * The trickiest part of this function is the re-ordering logic. Part of our
     * 'domain logic' is ensuring that the tasks in a part remain in the
     * correct order.
     *
     * @param partId the unique ID of the part this task belongs to
     * @param name the name of this task
     * @param description a brief description of this task
     * @param position the position of this task in the part
     * @param dependencyId optionally make this task dependent on another
     * @param partId optionally move this task to a new part, and if so, we
     *                need to be careful with the re-ordering.
     * @return the newly created task
     */
    override def updateTask(taskId: UUID, version: Long, name: String, description: String, newPosition: Int, notesAllowed: Boolean, dependencyId: Option[UUID], partId: Option[UUID] = None): Future[Task] = {
      transactional { implicit connection =>
        Logger.debug("updating task")
        for {
          existingTask <- taskRepository.find(taskId).map(_.get)
          part <- {partId match {
            case Some(id) => partRepository.find(id).map(_.get)
            case None => partRepository.find(existingTask.partId).map(_.get)
          }}
          taskList <- {
            taskRepository.list(part)
          }
          oldPosition <- Future.successful { partId match {
            case Some(id) => {
              if (id != existingTask.partId) taskList.length
              else existingTask.position
            }
            case None => existingTask.position
          }}
          taskListUpdated <- if (newPosition != oldPosition) {
            val filteredTaskList = taskList.filter(_.id != taskId)
            var temp = IndexedSeq[Task]()
            for (i <- filteredTaskList.indices) {
              if (i >= newPosition) {
                temp = temp :+ filteredTaskList(i).copy(position = i+1)
              }
              else {
                temp = temp :+ filteredTaskList(i).copy(position = i)
              }
            }
            val filteredOrderedTaskList = temp

            if (filteredOrderedTaskList.nonEmpty) {
              // Since we're inside a transaction, we need to fold over the list,
              // rather than map, to ensure the database queries are executed
              // sequentially. We cannot send parallel queries inside of a
              // transaction!
              serialized(filteredOrderedTaskList)(taskRepository.update)
            }
            else Future.successful(IndexedSeq())
          } else Future.successful(taskList)

          // Now we insert the new part
          updatedTask <- {
            Logger.debug("updating task itself")
            taskRepository.update(existingTask.copy(
              version = version,
              name = name,
              description = description,
              position = newPosition,
              notesAllowed = notesAllowed,
              partId = { partId match {
                case Some(partId) => partId
                case None => existingTask.partId
              }}
            ))
          }
        } yield updatedTask
      }.recover {
        case exception => throw exception
      }
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
    override def deleteTask(taskId: UUID, version: Long): Future[Boolean] = {
      transactional { implicit connection =>
        val fDeleted = for {
          task <- taskRepository.find(taskId).map(_.get.copy(version = version))
          part <- partRepository.find(task.partId).map(_.get)
          taskList <- taskRepository.list(part)
          taskListUpdated <- {
            // If there is already a part with this position, shift it (and all following parts)
            // back by one to make room for the new one.
            val filteredTaskList = taskList.filter(_.position > task.position).map(task => task.copy(position = task.position - 1))
            serialized(filteredTaskList)(taskRepository.update)
          }
          scratchpadsDeleted <- taskScratchpadRepository.delete(task)
          responsesDeleted <- taskResponseRepository.delete(task)
          feedbacksDeleted <- taskFeedbackRepository.delete(task)
          deletedTask <- taskRepository.delete(task)
        } yield deletedTask

        fDeleted.recover {
          case exception => throw exception
        }
      }
    }

    /**
     * Moves a task from one part to another.
     *
     * @param taskId the unique ID of the task to be moved
     * @param partId the new part that this task should belong to
     */
    override def moveTask(partId: UUID, taskId: UUID, newPosition: Int): Future[Task] = {
      taskRepository.find(taskId).flatMap { taskOption =>
        val task = taskOption.get

        if (task.partId == partId) {
          // Repositioning the task within the same part, let's refer this to
          // the updateTask method.
          Logger.debug("Moving task within part")
          this.updateTask(task.id, task.version, task.name, task.description, newPosition, task.notesAllowed, task.dependencyId)
        }
        else transactional { implicit connection =>
          // Moving the task to a different part
          Logger.debug("Moving task to new part")
          for {
            oldPart <- partRepository.find(task.partId).map(_.get)
            oldTaskList <- {
              taskRepository.list(oldPart)
            }
            // Insert the task into the new part's task list. This will
            // automatically shift the other tasks in the list to make room in
            // the ordering.
            movedTask <- this.updateTask(task.id, task.version, task.name, task.description, newPosition, task.notesAllowed, task.dependencyId, Some(partId))
            // Now update the old part's task list to correct the order.
            oldTaskListUpdated <- {
              val filteredTaskList = oldTaskList.filter(_.id != taskId)
              var temp = IndexedSeq[Task]()
              for (i <- filteredTaskList.indices) {
                temp = temp :+ filteredTaskList(i).copy(position = i)
              }
              val filteredOrderedTaskList = temp

              if (filteredOrderedTaskList.nonEmpty) {
                // Since we're inside a transaction, we need to fold over the list,
                // rather than map, to ensure the database queries are executed
                // sequentially. We cannot send parallel queries inside of a
                // transaction!
                serialized(filteredOrderedTaskList)(taskRepository.update)
              }
              else Future.successful(IndexedSeq())
            }
          }
          yield movedTask
        }
      }
    }
  }
}
