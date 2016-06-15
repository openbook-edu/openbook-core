package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.krispii.core.models.tasks.questions.Question
import com.github.mauricio.async.db.Connection
import com.sun.org.apache.xalan.internal.xsltc.runtime.BasisLibrary
import org.joda.time.DateTime
import play.api.Logger
import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks._
import ca.shiftfocus.krispii.core.models.work._
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services.datasource._
import java.util.UUID
import scala.concurrent.Future
import scalacache.ScalaCache
import scalaz.{ EitherT, \/, -\/, \/- }

class ProjectServiceDefault(
    val db: DB,
    val scalaCache: ScalaCachePool,
    val authService: AuthService,
    val schoolService: SchoolService,
    val courseRepository: CourseRepository,
    val projectRepository: ProjectRepository,
    val partRepository: PartRepository,
    val taskRepository: TaskRepository,
    val componentRepository: ComponentRepository,
    val tagRepository: TagRepository
) extends ProjectService {

  implicit def conn: Connection = db.pool
  implicit def cache: ScalaCachePool = scalaCache

  /**
   * Lists master projects.
   *
   * @return a future disjunction containing either a vector of projects, or a failure
   */
  override def listMasterProjects(enabled: Option[Boolean] = None): Future[\/[ErrorUnion#Fail, IndexedSeq[Project]]] = {
    projectRepository.list(Some(true), enabled)
  }

  /**
   * Lists all projects.
   *
   * @return a future disjunction containing either a vector of projects, or a failure
   */
  override def list: Future[\/[ErrorUnion#Fail, IndexedSeq[Project]]] = {
    projectRepository.list()
  }

  /**
   * Find all projects belonging to a given course
   *
   * @param courseId the unique id of the course to filter by
   * @return a future disjunction containing either a vector of projects, or a failure
   */
  override def list(courseId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Project]]] = {
    for {
      course <- lift(schoolService.findCourse(courseId))
      projects <- lift(projectRepository.list(course))
    } yield projects
  }

  /**
   * Find all projects a user has access to.
   *
   * @param userId the unique id of the user to filter by
   * @return a future disjunction containing either a vector of projects, or a failure
   */
  override def listProjectsByUser(userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Project]]] = {
    for {
      courses <- lift(schoolService.listCoursesByUser(userId))
      projects <- lift(serializedT(courses)((course: Course) => list(course.id)))
    } yield projects.flatten
  }

  /**
   * Find all projects a user has access to.
   *
   * @param userId the unique id of the user to filter by
   * @return a future disjunction containing either a vector of projects, or a failure
   */
  override def listProjectsByTeacher(userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Project]]] = {
    for {
      courses <- lift(schoolService.listCoursesByTeacher(userId))
      projects <- lift(serializedT(courses)((course: Course) => list(course.id)))
    } yield projects.flatten
  }

  /**
   * Find a single project by slug.
   *
   * @return an optional project
   */
  override def find(projectSlug: String): Future[\/[ErrorUnion#Fail, Project]] = find(projectSlug, true)
  override def find(projectSlug: String, fetchParts: Boolean): Future[\/[ErrorUnion#Fail, Project]] = {
    projectRepository.find(projectSlug, fetchParts)
  }

  /**
   * Find a single project by ID.
   *
   * @return an optional project
   */
  override def find(id: UUID): Future[\/[ErrorUnion#Fail, Project]] = find(id, true)
  override def find(id: UUID, fetchParts: Boolean): Future[\/[ErrorUnion#Fail, Project]] = {
    projectRepository.find(id, fetchParts)
  }

  /**
   * Find a project *if and only if* a user has access to that project.
   *
   * @param projectId the unique id of the project to find
   * @param userId the unique id of the user to filter by
   * @return a future disjunction containing either a project, or a failure
   */
  override def find(projectId: UUID, userId: UUID): Future[\/[ErrorUnion#Fail, Project]] = find(projectId, userId, true)
  override def find(projectId: UUID, userId: UUID, fetchParts: Boolean): Future[\/[ErrorUnion#Fail, Project]] = {
    for {
      user <- lift(authService.find(userId))
      project <- lift(projectRepository.find(projectId, user, fetchParts))
    } yield project
  }

  /**
   * insert array of componets
   */

  def insertComponents(components: IndexedSeq[Component]): Future[\/[ErrorUnion#Fail, IndexedSeq[Component]]] = {
    for {
      components <- lift(serializedT(components)(component => {
        Logger.error("executing for component " + component.id)
        for {
          newComponent <- lift(componentRepository.insert(component).map {
            case \/-(insertedComponent) => \/-(insertedComponent)
            case -\/(error) => {
              Logger.error(s" error within insertComponents ${error.toString}")
              \/-(component)
            }
          })
        } yield newComponent
      }))
    } yield components
  }

  def insertTasks(tasks: IndexedSeq[Task], part: Part): Future[\/[ErrorUnion#Fail, IndexedSeq[Task]]] = {
    transactional { implicit conn: Connection =>
      for {
        tasks <- lift(serializedT(tasks)(task => {
          Logger.error(s" inserting task ${part.toString}")
          taskRepository.insert(task)
        }))
      } yield tasks
    }
  }

  def insertParts(parts: IndexedSeq[Part]): Future[\/[ErrorUnion#Fail, IndexedSeq[Part]]] = {
    transactional { implicit conn: Connection =>
      for {
        parts <- lift(serializedT(parts)(part => {
          Logger.error(s" inserting part ${part.toString}")
          partRepository.insert(part)
        }))
      } yield parts
    }
  }

  def insertProject(project: Project): Future[\/[ErrorUnion#Fail, Project]] = {
    transactional { implicit conn: Connection =>
      for {
        //          t = Logger.error(s" inserting project ${project.toString}")
        newProject <- lift(projectRepository.insert(project))
      } yield newProject
    }
  }

  /**
   * insert array of tasks in for
   *
   * @param projectId
   * @param courseId
   * @param userId
   * @return
   */
  override def copyMasterProject(projectId: UUID, courseId: UUID, userId: UUID): Future[\/[ErrorUnion#Fail, Project]] = {
    transactional { implicit conn: Connection =>

      val futureProject = for {
        clonedProject <- lift(projectRepository.cloneProject(projectId, courseId))
        newProject <- lift(insertProject(clonedProject))
        clonedParts <- lift(projectRepository.cloneProjectParts(projectId, userId, clonedProject.id))
        clonedComponents <- lift(projectRepository.cloneProjectComponents(projectId, userId))
        parts <- lift(insertParts(clonedParts))
        components <- lift(insertComponents(clonedComponents))
        partsWithAdditions <- lift(serializedT(clonedParts)(part => {
          for {
            tasks <- lift(insertTasks(part.tasks, part))
            partComponents <- lift(insertPartsComponents(components, part))
          } yield tasks
        }))
        //we need to return the newProject not the clonedProject because the latter one doesn't have the updated slug
      } yield newProject.copy(parts = clonedParts)
      futureProject
    }
  }

  def insertPartsComponents(components: IndexedSeq[Component], part: Part): Future[\/[ErrorUnion#Fail, IndexedSeq[Component]]] = {
    for {
      components <- lift(serializedT(components)(component => {
        Logger.error("executing for component " + component.id)
        for {
          added <- lift(componentRepository.addToPart(component, part))
        } yield component
      }))
    } yield components

  }

  /**
   * List projects for autocomplete search
   *
   * @param key the partially typed word from the user
   */
  override def listByKey(key: String): Future[\/[RepositoryError.Fail, IndexedSeq[Project]]] = {
    transactional { implicit conn =>
      projectRepository.trigramSearch(key)
    }
  }

  /**
   * Find a single project by slug and UserID.
   *
   * @return an optional project
   */
  override def find(projectSlug: String, userId: UUID): Future[\/[ErrorUnion#Fail, Project]] = find(projectSlug, userId, true)
  override def find(projectSlug: String, userId: UUID, fetchParts: Boolean): Future[\/[ErrorUnion#Fail, Project]] = {
    for {
      user <- lift(authService.find(userId))
      project <- lift(projectRepository.find(projectSlug, false))
      course <- lift(schoolService.findCourse(project.courseId))
      projectFiltered <- lift {
        if (userId == course.teacherId) {
          if (fetchParts) projectRepository.find(project.id, true)
          else Future.successful(\/-(project))
        }
        else {
          projectRepository.find(project.id, user, fetchParts)
        }
      }
    } yield projectFiltered
  }

  /**
   * Create a new project.
   *
   * @param name The new name to give the project.
   * @param slug The new slug to give the project.
   * @param parentId The id of the parent project, if the parent project is empty then the project is a master project.
   * @param description The new description for the project.
   * @return the updated project.
   */
  override def create(
    courseId: UUID,
    name: String,
    slug: String,
    description: String,
    longDescription: String,
    availability: String,
    parentId: Option[UUID] = None,
    isMaster: Boolean = false,
    enabled: Boolean = false,
    projectType: String
  ): Future[\/[ErrorUnion#Fail, Project]] = {
    // First instantiate a new Project, Part and Task.
    val newProject = Project(
      courseId = courseId,
      parentId = parentId,
      name = name,
      slug = slug,
      enabled = enabled,
      isMaster = isMaster,
      description = description,
      longDescription = longDescription,
      availability = availability,
      projectType = projectType,
      parts = IndexedSeq.empty[Part]
    )

    transactional { implicit conn: Connection =>
      for {
        _ <- lift(isValidSlug(slug))
        createdProject <- lift(projectRepository.insert(newProject))
      } yield createdProject
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
  override def updateInfo(
    id: UUID,
    version: Long,
    courseId: Option[UUID],
    name: Option[String],
    slug: Option[String],
    description: Option[String],
    longDescription: Option[String],
    availability: Option[String],
    enabled: Option[Boolean],
    projectType: Option[String]
  ): Future[\/[ErrorUnion#Fail, Project]] = {
    transactional { implicit conn: Connection =>
      for {
        existingProject <- lift(projectRepository.find(id))
        _ <- predicate(existingProject.version == version)(ServiceError.OfflineLockFail)
        toUpdate = existingProject.copy(
          courseId = courseId.getOrElse(existingProject.courseId),
          name = name.getOrElse(existingProject.name),
          slug = slug.getOrElse(existingProject.slug),
          description = description.getOrElse(existingProject.description),
          longDescription = longDescription.getOrElse(existingProject.longDescription),
          availability = availability.getOrElse(existingProject.availability),
          enabled = enabled.getOrElse(existingProject.enabled),
          projectType = projectType.getOrElse(existingProject.projectType)
        )
        updatedProject <- lift(projectRepository.update(toUpdate))
      } yield updatedProject
    }
  }

  /**
   * Update a project's slug. This is a URL-friendly unique identifier for the project.
   *
   * @param id
   * @param version
   * @return
   */
  override def updateSlug(id: UUID, version: Long, newSlug: String): Future[\/[ErrorUnion#Fail, Project]] = {
    transactional { implicit conn: Connection =>
      for {
        existingProject <- lift(projectRepository.find(id))
        _ <- predicate(existingProject.version == version)(ServiceError.OfflineLockFail)
        _ <- lift(isValidSlug(newSlug))
        toUpdate = existingProject.copy(slug = newSlug)
        updatedProject <- lift(projectRepository.update(toUpdate))
      } yield updatedProject
    }
  }

  /**
   * Delete a project.
   *
   * NB: This deletes the entire project! All parts, all tasks, including all
   *     responses and scratch pads. Wrapped in a transaction.
   *
   * @param id the UUID of the project to delete
   * @param version the version of the project to delete
   * @return a boolean indicating success/failure
   */
  override def delete(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, Project]] = {
    transactional { implicit conn: Connection =>
      for {
        project <- lift(projectRepository.find(id, false))
        _ <- predicate(project.version == version)(ServiceError.OfflineLockFail)
        partsDeleted <- lift(partRepository.delete(project))
        projectDeleted <- lift(projectRepository.delete(project))
      } yield projectDeleted
    }
  }

  // TODO - method does nothing
  /**
   * List the parts that have a component.
   *
   * @param componentId the UUID of the component to filter by
   */
  override def listPartsInComponent(componentId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Part]]] = ???
  //    {
  //      val fPartList = for {
  //        component <- componentRepository.find(componentId).map(_.get)
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
  override def findPart(partId: UUID, fetchParts: Boolean = true): Future[\/[ErrorUnion#Fail, Part]] = {
    partRepository.find(partId, fetchParts)
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
   * @param position this part's position in the project. If this position has already
   *                 been taken, the existing parts will be shifted down.
   */
  override def createPart(
    projectId: UUID,
    name: String,
    position: Int,
    id: UUID = UUID.randomUUID
  ): Future[\/[ErrorUnion#Fail, Part]] = {
    transactional { implicit conn: Connection =>
      for {
        project <- lift(projectRepository.find(projectId, false))
        partList <- lift(partRepository.list(project, false))
        _ <- predicate(partList.size < maxPartsInProject)(ServiceError.BusinessLogicFail("Maximum number of parts reached"))
        truePosition <- lift {
          val positionMax = partList.nonEmpty match {
            case true => partList.map(_.position).max
            case false => 1
          }
          val positionMin = partList.nonEmpty match {
            case true => partList.map(_.position).min
            case false => 1
          }

          if (position == 1 && positionMin == 0) Future.successful(\/-(positionMin)) // linter:ignore
          else if (position < positionMin) Future.successful(\/-(positionMin))
          else if (position > positionMax && partList.nonEmpty) Future.successful(\/-(positionMax + 1))
          else if (position > positionMax && partList.isEmpty) Future.successful(\/-(positionMax))
          else Future.successful(\/-(position))
        }
        newPart <- lift {
          // If there is already a part with this position, shift it (and all following parts)
          // back by one to make room for the new one.
          val positionExists = partList.exists(_.position == truePosition)
          val filteredPartList = positionExists match {
            case true => partList.filter(_.position < truePosition) ++ partList.filter(_.position >= truePosition).map(
              part => part.copy(position = part.position + 1)
            )
            case false => partList
          }

          val newPart = Part(
            id = id,
            projectId = project.id,
            name = name,
            position = truePosition
          )
          // Add newPart and order parts by position
          val orderedParts = (filteredPartList :+ newPart).sortWith(_.position < _.position)

          val newPartIndex = orderedParts.indexOf(newPart)
          // Save and update parts
          serializedT(orderedParts.indices.asInstanceOf[IndexedSeq[Int]])(createOrderedParts(orderedParts, newPart.id, _)).map {
            case -\/(error) => -\/(error)
            case \/-(parts) => \/-(parts(newPartIndex))
          }
        }
      } yield newPart
    }
  }

  private def createOrderedParts(orderedParts: IndexedSeq[Part], newPartId: UUID, i: Int): Future[\/[RepositoryError.Fail, Part]] = {
    if (orderedParts(i).id == newPartId) {
      partRepository.insert(orderedParts(i).copy(position = i + 1))
    }
    else if (orderedParts(i).position != i + 1) {
      partRepository.update(orderedParts(i).copy(position = i + 1))
    }
    else {
      Future.successful(\/-(orderedParts(i)))
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
  override def updatePart(partId: UUID, version: Long, name: Option[String], maybePosition: Option[Int], enabled: Option[Boolean]): Future[\/[ErrorUnion#Fail, Part]] = {
    transactional { implicit conn: Connection =>
      for {
        existingPart <- lift(partRepository.find(partId))
        _ <- predicate(existingPart.version == version)(ServiceError.OfflineLockFail)
        movedPart <- lift(movePart(partId, version, maybePosition.getOrElse(existingPart.position)))
        updatedPart <- lift(if (name.isDefined || enabled.isDefined) {
          partRepository.update(movedPart.copy(
            name = name.getOrElse(existingPart.name),
            enabled = enabled.getOrElse(existingPart.enabled)
          ))
        }
        else {
          Future.successful(\/-(movedPart))
        })
      } yield updatedPart
    }
  }

  /**
   * Move part to another position
   *
   * @param partId
   * @param version
   * @param newPosition
   * @return
   */
  def movePart(partId: UUID, version: Long, newPosition: Int): Future[\/[ErrorUnion#Fail, Part]] = {
    transactional { implicit conn: Connection =>
      for {
        existingPart <- lift(partRepository.find(partId))
        _ <- predicate(existingPart.version == version)(ServiceError.OfflineLockFail)
        oldPosition = existingPart.position
        project <- lift(projectRepository.find(existingPart.projectId, false))
        partList <- lift(partRepository.list(project, false))
        _ <- predicate(partList.nonEmpty)(ServiceError.BusinessLogicFail("Weird, part list shouldn't be empty!"))
        truePosition <- lift {
          val position = newPosition
          val positionMax = partList.map(_.position).max
          val positionMin = partList.map(_.position).min

          if (position == 1 && positionMin == 0) Future.successful(\/-(positionMin)) // linter:ignore
          else if (position < positionMin) Future.successful(\/-(positionMin))
          else if (position > positionMax) Future.successful(\/-(positionMax + 1))
          else Future.successful(\/-(position))
        }
        movedPart <- lift {
          val positionExists = partList.filter(_.id != partId).filter(_.position == truePosition).nonEmpty // linter:ignore
          val updatedPartList = positionExists match {
            case true => partList.map { part =>
              {
                if (part.id == partId) part.copy(position = truePosition)
                else if (part.position < truePosition) part.copy(position = part.position)
                else part.copy(position = part.position + 1)
              }
            }
            case false => partList.map { part =>
              {
                if (part.id == partId) part.copy(position = truePosition)
                else part
              }
            }
          }

          // Order all parts by position
          val orderedParts = updatedPartList.sortWith(_.position < _.position)
          serializedT(orderedParts.indices.asInstanceOf[IndexedSeq[Int]])(updateOrderedParts(orderedParts, partList, _)).map {
            case -\/(error) => -\/(error)
            case \/-(parts) => \/-(parts.filter(_.id == partId).head)
          }
        }
      } yield movedPart
    }
  }

  private def updateOrderedParts(orderedParts: IndexedSeq[Part], originalParts: IndexedSeq[Part], i: Int): Future[\/[ErrorUnion#Fail, Part]] = {
    (orderedParts.lift(i), originalParts.find(_.id == orderedParts(i).id)) match {
      case (Some(reorderedPart), Some(originalPart)) => {
        if (originalPart.position != i + 1) {
          partRepository.update(reorderedPart.copy(position = i + 1))
        }
        else {
          Future.successful(\/-(reorderedPart))
        }
      }
      case _ => Future successful -\/(ServiceError.BusinessLogicFail("Tried to re-order a part that doesn't exist???"))
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
  override def deletePart(partId: UUID, version: Long): Future[\/[ErrorUnion#Fail, Part]] = {
    transactional { implicit conn: Connection =>
      for {
        part <- lift(partRepository.find(partId))
        _ <- predicate(part.version == version)(ServiceError.OfflineLockFail)
        //_ <- predicate(part.tasks.isEmpty) (ServiceError.BusinessLogicFail("Cannot delete a part that still has tasks"))
        project <- lift(projectRepository.find(part.projectId, false))
        partList <- lift(partRepository.list(project, false))
        _ <- predicate(partList.nonEmpty)(ServiceError.BusinessLogicFail("Weird, part list shouldn't be empty!"))
        partListUpdated <- lift {
          val filteredOderedPartList = partList.filter(_.id != partId).sortWith(_.position < _.position)
          serializedT(filteredOderedPartList.indices.asInstanceOf[IndexedSeq[Int]])(updateOrderedParts(filteredOderedPartList, partList, _))
        }
        tasksDeleted <- lift(taskRepository.delete(part))
        deletedPart <- lift(partRepository.delete(part))
      } yield deletedPart
    }
  }

  /**
   * Enable a disabled part, and disable an enabled part.
   *
   * @param partId the unique ID of the part to toggle
   * @param version the current version of the part to toggle
   * @return a future disjunction containing either the toggled part, or a failure
   */
  override def togglePart(partId: UUID, version: Long): Future[\/[ErrorUnion#Fail, Part]] = {
    transactional { implicit conn: Connection =>
      for {
        part <- lift(partRepository.find(partId))
        _ <- predicate(part.version == version)(ServiceError.OfflineLockFail)
        toUpdate = part.copy(version = version, enabled = !part.enabled)
        toggled <- lift(partRepository.update(toUpdate))
      } yield toggled
    }
  }

  /**
   * Create a new task.
   *
   * @param partId the unique ID of the part this task belongs to
   * @param taskType the type of the task
   * @param name the name of this task
   * @param description a brief description of this task
   * @param position the position of this task in the part
   * @return the newly created task
   */
  override def createTask(
    partId: UUID,
    taskType: Int,
    name: String,
    description: String,
    position: Int,
    id: UUID = UUID.randomUUID
  ): Future[\/[ErrorUnion#Fail, Task]] = {
    transactional { implicit conn: Connection =>
      for {
        part <- lift(partRepository.find(partId))
        _ <- predicate(part.tasks.size < maxTasksInPart)(ServiceError.BusinessLogicFail("Maximum number of tasks in a part reached"))
        taskList = part.tasks
        truePosition <- lift {
          val positionMax = taskList.nonEmpty match {
            case true => taskList.map(_.position).max
            case false => 1
          }
          val positionMin = taskList.nonEmpty match {
            case true => taskList.map(_.position).min
            case false => 1
          }

          if (position == 1 && positionMin == 0) Future.successful(\/-(positionMin)) // linter:ignore
          else if (position < positionMin) Future.successful(\/-(positionMin))
          else if (position > positionMax && taskList.nonEmpty) Future.successful(\/-(positionMax + 1))
          else if (position > positionMax && taskList.isEmpty) Future.successful(\/-(positionMax))
          else Future.successful(\/-(position))
        }
        newTask <- lift {
          // If there is already a task with this position, shift it (and all following tasks)
          // back by one to make room for the new one.
          val positionExists = taskList.exists(_.position == truePosition)
          val filteredTaskList = positionExists match {
            case true => taskList.filter(_.position < truePosition) ++ taskList.filter(_.position >= truePosition).map {
              case task: DocumentTask => task.copy(position = task.position + 1)
              case task: QuestionTask => task.copy(position = task.position + 1)
              case task: MediaTask => task.copy(position = task.position + 1)
            }
            case false => taskList
          }

          val newTask = Task(
            id = id,
            partId = partId,
            taskType = taskType,
            position = truePosition,
            settings = CommonTaskSettings(
              title = name,
              description = description
            ),
            maxGrade = "0"
          )
          // Add newTask and order tasks by position
          val orderedTasks = (filteredTaskList :+ newTask).sortWith(_.position < _.position)

          val newTaskIndex = orderedTasks.indexOf(newTask)
          // Save and update tasks
          serializedT(orderedTasks.indices.asInstanceOf[IndexedSeq[Int]])(createOrderedTasks(orderedTasks, newTask.id, _)).map {
            case -\/(error) => -\/(error)
            case \/-(tasks) => \/-(tasks(newTaskIndex))
          }
        }
      } yield newTask
    }
  }

  private def createOrderedTasks(orderedTasks: IndexedSeq[Task], newTaskId: UUID, i: Int): Future[\/[RepositoryError.Fail, Task]] = {
    if (orderedTasks(i).id == newTaskId) {
      orderedTasks(i) match {
        case task: DocumentTask => taskRepository.insert(task.copy(position = i + 1))
        case task: QuestionTask => taskRepository.insert(task.copy(position = i + 1))
        case task: MediaTask => taskRepository.insert(task.copy(position = i + 1))

      }
    }
    else if (orderedTasks(i).position != i + 1) {
      orderedTasks(i) match {
        case task: DocumentTask => taskRepository.update(task.copy(position = i + 1))
        case task: QuestionTask => taskRepository.update(task.copy(position = i + 1))
        case task: MediaTask => taskRepository.update(task.copy(position = i + 1))
      }
    }
    else {
      Future.successful(\/-(orderedTasks(i)))
    }
  }

  /**
   * Find a task by its ID.
   */
  override def findTask(taskId: UUID): Future[\/[ErrorUnion#Fail, Task]] = {
    taskRepository.find(taskId)
  }

  /**
   * Find a task by its position in a project.
   *
   * @param projectSlug the text slug of the project this task is in
   * @param partNum the number (corresponds to 'position' field) of the part
   *                that this task is in.
   * @param taskNum the number (position) of the task inside the part.
   * @return an Option[Task] if one was found.
   */
  override def findTask(projectSlug: String, partNum: Int, taskNum: Int): Future[\/[ErrorUnion#Fail, Task]] = {
    for {
      project <- lift(projectRepository.find(projectSlug))
      task <- lift {
        Future successful {
          project.parts.find(_.position == partNum) match {
            case Some(part) => part.tasks.find(_.position == taskNum) match {
              case Some(task) => \/.right(task)
              case None => \/.left(RepositoryError.NoResults(s"Could not find a task in project $projectSlug, part $partNum at position $taskNum"))
            }
            case None => \/.left(RepositoryError.NoResults(s"Could not find a part in project $projectSlug at position $partNum"))
          }
        }
      }
    } yield task
  }

  /**
   * Find the "now" task for a student in a project
   *
   * @param userId the unique of the id of the student
   * @param projectId the unique id of the project
   * @return a future disjunction containing either the now task, or a failure
   */
  override def findNowTask(userId: UUID, projectId: UUID): Future[\/[ErrorUnion#Fail, Task]] = {
    for {
      student <- lift(authService.find(userId))
      project <- lift(projectRepository.find(projectId))
      taskOption <- lift(taskRepository.findNow(student, project))
    } yield taskOption
  }

  /**
   * Update an existing task.
   *
   * The trickiest part of this function is the re-ordering logic. Part of our
   * 'domain logic' is ensuring that the tasks in a part remain in the
   * correct order.
   */
  private def updateTask(taskToUpdate: Task): Future[\/[ErrorUnion#Fail, Task]] = {
    transactional { implicit conn: Connection =>
      for {
        movedTask <- lift(moveTask(taskToUpdate.id, taskToUpdate.version, taskToUpdate.position, Some(taskToUpdate.partId)))
        updatedTask <- {
          taskToUpdate match {
            case task: DocumentTask => lift(taskRepository.update(task.copy(position = movedTask.position, version = movedTask.version)))
            case task: QuestionTask => lift(taskRepository.update(task.copy(position = movedTask.position, version = movedTask.version)))
            case task: MediaTask => lift(taskRepository.update(task.copy(position = movedTask.position, version = movedTask.version)))

          }
        }
      } yield updatedTask
    }
  }

  /**
   * Moves a task from one part to another or just changes the position if part is the same.
   *
   * @param taskId the unique ID of the task to be moved
   * @param partId the new part that this task should belong to
   * @param newPosition the new position for this task
   */
  override def moveTask(taskId: UUID, version: Long, newPosition: Int, partId: Option[UUID] = None): Future[\/[ErrorUnion#Fail, Task]] = {
    transactional { implicit conn: Connection =>
      for {
        oldTask <- lift(taskRepository.find(taskId))
        _ <- predicate(oldTask.version == version)(ServiceError.OfflineLockFail)

        oldPart <- lift(partRepository.find(oldTask.partId))
        newPart <- lift(partRepository.find(partId.getOrElse(oldTask.partId)))

        _ <- predicate(newPart.tasks.size < maxTasksInPart)(ServiceError.BusinessLogicFail("Maximum number of tasks in a part reached"))

        otList = oldPart.tasks
        ntList = newPart.tasks

        // Something realy bad has happen
        _ = if (otList.isEmpty) throw new Exception("This task list shouldn't be empty")

        updatedTask <- lift {
          // If moved to another part, reorder tasks in the origin part
          if (oldPart.id != newPart.id) {
            val orderedTasks = otList.filter(_.id != taskId).sortWith(_.position < _.position)
            serializedT(orderedTasks.indices.asInstanceOf[IndexedSeq[Int]])(updateOrderedTasks(orderedTasks, otList, _))
          }

          // Update task and taskList
          for {
            truePosition <- lift {
              val position = newPosition
              val positionMax = ntList.nonEmpty match {
                case true => ntList.map(_.position).max
                case false => 1
              }
              val positionMin = ntList.nonEmpty match {
                case true => ntList.map(_.position).min
                case false => 1
              }

              if (position == 1 && positionMin == 0) Future.successful(\/-(positionMin)) // linter:ignore
              else if (position < positionMin) Future.successful(\/-(positionMin))
              else if (position > positionMax && ntList.nonEmpty) Future.successful(\/-(positionMax + 1))
              else if (position > positionMax && ntList.isEmpty) Future.successful(\/-(positionMax))
              else Future.successful(\/-(position))
            }
            updatedTask <- lift {
              val taskList =
                // if moved, than add the task to the new part
                if (oldPart.id != newPart.id) ntList :+ oldTask
                // otherwise, the task stays in the part
                else ntList

              val positionExists = taskList.iterator.filter(_.id != taskId).exists(_.position == truePosition) // linter:ignore
              val taskListPositions = positionExists match {
                case true => taskList.map { task =>
                  if (task.id == taskId) task.id -> truePosition
                  else if (task.position < truePosition) task.id -> task.position
                  else task.id -> (task.position + 1)
                }.toMap
                case false => taskList.map { task =>
                  if (task.id == taskId) task.id -> truePosition
                  else task.id -> task.position
                }.toMap
              }

              val updatedTasks = taskList.map {
                case task: DocumentTask => task.copy(position = taskListPositions(task.id), partId = newPart.id)
                case task: QuestionTask => task.copy(position = taskListPositions(task.id), partId = newPart.id)
                case task: MediaTask => task.copy(position = taskListPositions(task.id), partId = newPart.id)

              }

              val orderedTasks = updatedTasks.sortWith(_.position < _.position)
              println("Going to re-order them thar tasks")
              println(ntList.map({ task => s"Task(${task.settings.title}, ${task.position})" }).mkString(", "))
              println(orderedTasks.map({ task => s"Task(${task.settings.title}, ${task.position})" }).mkString(", "))
              serializedT(orderedTasks.indices.asInstanceOf[IndexedSeq[Int]])(updateOrderedTasks(orderedTasks, taskList, _)).map {
                case -\/(error) => -\/(error)
                case \/-(tasks) => \/-(tasks.filter(_.id == taskId).head)
              }
            }
          } yield updatedTask
        }
      } yield updatedTask
    }
  }

  private def updateOrderedTasks(orderedTasks: IndexedSeq[Task], originalTasks: IndexedSeq[Task], i: Int): Future[\/[RepositoryError.Fail, Task]] = {
    val taskTuple = for {
      orderedTask <- orderedTasks.lift(i)
      originalTask <- originalTasks.find(_.id == orderedTask.id)
    } yield (orderedTask, originalTask)

    taskTuple match {
      case Some((orderedTask, originalTask)) => {
        if (originalTask.position != i + 1 || originalTask.partId != orderedTask.partId) {
          orderedTask match {
            case task: DocumentTask => taskRepository.update(task.copy(position = i + 1), Some(originalTask.partId))
            case task: QuestionTask => taskRepository.update(task.copy(position = i + 1), Some(originalTask.partId))
            case task: MediaTask => taskRepository.update(task.copy(position = i + 1), Some(originalTask.partId))

          }
        }
        else {
          Future.successful(\/-(orderedTask))
        }
      }
      case None => Future successful -\/(RepositoryError.DatabaseError("Something screwed up"))
    }
  }

  /**
   * Update a DocumentTask.
   *
   * @param commonArgs
   * @return
   */
  override def updateDocumentTask(commonArgs: CommonTaskArgs, dependencyId: Option[Option[UUID]]): Future[\/[ErrorUnion#Fail, Task]] =
    {
      for {
        task <- lift(taskRepository.find(commonArgs.taskId))
        _ <- predicate(task.isInstanceOf[DocumentTask])(ServiceError.BadInput("services.ProjectService.updateDocumentTask.wrongTaskType"))
        _ <- predicate(task.version == commonArgs.version)(ServiceError.OfflineLockFail)
        documentTask = task.asInstanceOf[DocumentTask]
        toUpdate = documentTask.copy(
          partId = commonArgs.partId.getOrElse(task.partId),
          position = commonArgs.position.getOrElse(task.position),
          maxGrade = commonArgs.maxGrade.getOrElse(task.maxGrade),
          settings = task.settings.copy(
            title = commonArgs.name.getOrElse(task.settings.title),
            description = commonArgs.description.getOrElse(task.settings.description),
            help = commonArgs.help.getOrElse(task.settings.help),
            notesAllowed = commonArgs.notesAllowed.getOrElse(task.settings.notesAllowed),
            notesTitle = commonArgs.notesTitle match {
              case Some(Some(newNotesTitle)) => Some(newNotesTitle)
              case Some(None) => None
              case None => task.settings.notesTitle
            },
            responseTitle = commonArgs.responseTitle match {
              case Some(Some(newResponseTitle)) => Some(newResponseTitle)
              case Some(None) => None
              case None => task.settings.responseTitle
            }
          ),
          dependencyId = dependencyId match {
            case Some(Some(newDepId)) => Some(newDepId)
            case Some(None) => None
            case None => documentTask.dependencyId
          }
        )
        updatedTask <- lift(updateTask(toUpdate))
      } yield updatedTask
    }

  /**
   * Update a MatchingTask
   *
   * @param commonArgs
   * @param questions
   * @return
   */
  def updateQuestionTask(
    commonArgs: CommonTaskArgs,
    questions: Option[IndexedSeq[Question]] = None
  ): Future[\/[ErrorUnion#Fail, Task]] =
    {
      for {
        task <- lift(taskRepository.find(commonArgs.taskId))
        _ <- predicate(task.isInstanceOf[QuestionTask])(ServiceError.BadInput("services.ProjectService.updateQuestionTask.wrongTaskType"))
        _ <- predicate(task.version == commonArgs.version)(ServiceError.OfflineLockFail)
        questionTask = task.asInstanceOf[QuestionTask]
        toUpdate = questionTask.copy(
          partId = commonArgs.partId.getOrElse(task.partId),
          position = commonArgs.position.getOrElse(task.position),
          maxGrade = commonArgs.maxGrade.getOrElse(task.maxGrade),
          settings = task.settings.copy(
            title = commonArgs.name.getOrElse(task.settings.title),
            help = commonArgs.help.getOrElse(task.settings.help),
            description = commonArgs.description.getOrElse(task.settings.description),
            notesAllowed = commonArgs.notesAllowed.getOrElse(task.settings.notesAllowed),
            notesTitle = commonArgs.notesTitle match {
              case Some(Some(newNotesTitle)) => Some(newNotesTitle)
              case Some(None) => None
              case None => task.settings.notesTitle
            },
            responseTitle = commonArgs.responseTitle match {
              case Some(Some(newResponseTitle)) => Some(newResponseTitle)
              case Some(None) => None
              case None => task.settings.responseTitle
            }
          ),
          questions = questions.getOrElse(questionTask.questions)
        )
        updatedTask <- lift(updateTask(toUpdate))
      } yield updatedTask
    }

  /**
   * Update a MediaTask.
   *
   * @param commonArgs
   * @return
   */
  override def updateMediaTask(commonArgs: CommonTaskArgs, mediaType: Option[Int]): Future[\/[ErrorUnion#Fail, Task]] =
    {
      for {
        task <- lift(taskRepository.find(commonArgs.taskId))
        _ <- predicate(task.isInstanceOf[MediaTask])(ServiceError.BadInput("services.ProjectService.updateDocumentTask.wrongTaskType"))
        _ <- predicate(task.version == commonArgs.version)(ServiceError.OfflineLockFail)
        mediaTask = task.asInstanceOf[MediaTask]
        toUpdate = mediaTask.copy(
          partId = commonArgs.partId.getOrElse(task.partId),
          position = commonArgs.position.getOrElse(task.position),
          maxGrade = commonArgs.maxGrade.getOrElse(task.maxGrade),
          settings = task.settings.copy(
            title = commonArgs.name.getOrElse(task.settings.title),
            description = commonArgs.description.getOrElse(task.settings.description),
            help = commonArgs.help.getOrElse(task.settings.help),
            notesAllowed = commonArgs.notesAllowed.getOrElse(task.settings.notesAllowed),
            notesTitle = commonArgs.notesTitle match {
              case Some(Some(newNotesTitle)) => Some(newNotesTitle)
              case Some(None) => None
              case None => task.settings.notesTitle
            },
            responseTitle = commonArgs.responseTitle match {
              case Some(Some(newResponseTitle)) => Some(newResponseTitle)
              case Some(None) => None
              case None => task.settings.responseTitle
            }
          ),
          mediaType = mediaType.getOrElse(mediaTask.mediaType)

        )
        updatedTask <- lift(updateTask(toUpdate))
      } yield updatedTask
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
  override def deleteTask(taskId: UUID, version: Long): Future[\/[ErrorUnion#Fail, Task]] = {
    transactional { implicit conn: Connection =>
      for {
        task <- lift(taskRepository.find(taskId))
        _ <- predicate(task.version == version)(ServiceError.OfflineLockFail)
        part <- lift(partRepository.find(task.partId))
        taskList = part.tasks
        _ <- predicate(taskList.nonEmpty)(ServiceError.BusinessLogicFail("Weird, task list shouldn't be empty!"))
        // TODO - place deleteTask before, because transactional doesn't work, so in case of an error, positions of another tasks won't be changed
        deletedTask <- lift(taskRepository.delete(task))
        taskListUpdated <- lift {
          val filteredOderedTaskList = taskList.filter(_.id != taskId).sortWith(_.position < _.position)
          serializedT(filteredOderedTaskList.indices.asInstanceOf[IndexedSeq[Int]])(updateOrderedTasks(filteredOderedTaskList, taskList, _))
        }
      } yield deletedTask
    }
  }

  override def createTag(name: String): Future[\/[ErrorUnion#Fail, Tag]] = {
    transactional { implicit conn: Connection =>
      tagRepository.create(Tag(UUID.randomUUID(), name))
    }
  }
  override def tag(projectId: UUID, tagId: UUID): Future[\/[ErrorUnion#Fail, Unit]] = {
    transactional { implicit conn: Connection =>
      tagRepository.tag(projectId, tagId)
    }
  }
  override def findTag(name: String): Future[\/[ErrorUnion#Fail, Tag]] = {
    transactional { implicit conn: Connection =>
      tagRepository.find(name)
    }
  }

  override def untag(projectId: UUID, tagId: UUID): Future[\/[ErrorUnion#Fail, Unit]] = {
    transactional { implicit conn: Connection =>
      tagRepository.untag(projectId, tagId)
    }
  }
  /**
   * Checks if a user has access to a project.
   *
   * @param userId the UUID of the user to check
   * @param projectSlug the slug of the project to look for
   * @return a boolean indicating success or failure
   */
  override def userHasProject(userId: UUID, projectSlug: String): Future[\/[ErrorUnion#Fail, Boolean]] = {
    for {
      user <- lift(authService.find(userId))
      project <- lift(find(projectSlug))
      hasProject <- lift(courseRepository.hasProject(user, project))
    } yield hasProject
  }

  /**
   * Check if a slug is of the valid format.
   *
   * @param slug the slug to be checked
   * @return a future disjunction containing either the slug, or a failure
   */
  private def isValidSlug(slug: String): Future[\/[ErrorUnion#Fail, String]] = Future successful {
    if ("""[A-Za-z0-9\-]+""".r.unapplySeq(slug).isDefined) \/-(slug)
    else -\/(ServiceError.BadInput(s"$slug is not a valid slug format."))
  }

  //  partsWithAdditions <- lift(serializedT(clonedParts)(part => {
  //    for {
  //      tasks <- lift(insertTasks(part.tasks, part))
  //      partComponents <- lift(insertPartsComponents(components, part))
  //    } yield tasks
  //  }))
  /**
   * clone tags from one project to another
   * @param newProjectId
   * @param oldProjectId
   * @return
   */
  override def cloneTags(newProjectId: UUID, oldProjectId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Tag]]] = {
    for {
      toClone <- lift(tagRepository.listByProjectId(oldProjectId))
      cloned <- lift(serializedT(toClone)(tag => {
        for {
          inserted <- lift(tagRepository.create(tag))
        } yield inserted
      }))
    } yield cloned
  }
}
