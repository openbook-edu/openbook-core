package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.group.Course
import ca.shiftfocus.krispii.core.models.tasks.{DocumentTask, MediaTask, QuestionTask, Task}
import com.github.mauricio.async.db.{Connection, RowData}
import org.joda.time.DateTime
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz._

class ProjectRepositoryPostgres(
  val userRepository: UserRepository,
  val courseRepository: CourseRepository,
  val partRepository: PartRepository,
  val taskRepository: TaskRepository,
  val componentRepository: ComponentRepository,
  val tagRepository: TagRepository,
  val cacheRepository: CacheRepository
)
    extends ProjectRepository with PostgresRepository[Project] {

  override val entityName = "Project"

  def constructor(row: RowData): Project = {
    Logger.debug("Creating project " + row("name").toString() + ", enabled: " + row("enabled").toString)
    Project(
      row("id").asInstanceOf[UUID],
      row("course_id").asInstanceOf[UUID],
      Option(row("parent_id")).map(_.asInstanceOf[UUID]),
      Option(row("parent_version")).map(_.asInstanceOf[Long]),
      row("is_master").asInstanceOf[Boolean],
      row("version").asInstanceOf[Long],
      row("name").asInstanceOf[String],
      row("slug").asInstanceOf[String],
      row("description").asInstanceOf[String],
      row("long_description").asInstanceOf[String],
      row("availability").asInstanceOf[String],
      row("enabled").asInstanceOf[Boolean],
      row("project_type").asInstanceOf[String],
      IndexedSeq[Part](),
      IndexedSeq[Tag](),
      Option(row("status").asInstanceOf[String]) match {
        case Some(status) => Some(status)
        case _ => None
      },
      Option(row("last_task_id").asInstanceOf[UUID]) match {
        case Some(lastTaskId) => Some(lastTaskId)
        case _ => None
      },
      row("created_at").asInstanceOf[DateTime],
      row("updated_at").asInstanceOf[DateTime]
    )
  }

  val Table = "projects"
  val ProjectsTagsTable = "project_tags"
  val TagsTable = "tags"
  val Fields = "id, version, course_id, name, slug, parent_id, parent_version, is_master, description, long_description, availability, enabled, project_type, status, last_task_id, created_at, updated_at"
  val FieldsWithTable = Fields.split(", ").map({ field => s"${Table}." + field }).mkString(", ")
  val OrderBy = s"${Table}.created_at DESC"

  // User CRUD operations
  val SelectAll =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE is_master is false
     """.stripMargin

  val SelectAllMaster =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE is_master is true
     """.stripMargin

  val SelectOne =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE id = ?
    """.stripMargin

  val SelectOneForUser =
    s"""
       |SELECT $FieldsWithTable
       |FROM $Table, courses, users_courses
       |WHERE $Table.id = ?
       |  AND $Table.course_id = courses.id
       |  AND (courses.teacher_id = ? OR (
       |    courses.id = users_courses.course_id AND users_courses.user_id = ?
       |  ))
     """.stripMargin

  val SelectOneBySlug =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE slug = ?
     """.stripMargin

  def SelectByTags(tags: IndexedSeq[(String, String)], distinct: Boolean): String = {
    var whereClause = ""
    var distinctClause = ""
    val length = tags.length

    tags.zipWithIndex.map {
      case ((tagName, tagLang), index) =>
        whereClause += s"""(tags.name='${tagName}' AND tags.lang='${tagLang}')"""
        if (index != (length - 1)) whereClause += " OR "
    }

    whereClause = {
      if (whereClause != "") "WHERE " + whereClause
      // If tagList is empty, then there should be unexisting condition
      else "WHERE false != false"
    }

    if (distinct) {
      distinctClause = s"HAVING COUNT(DISTINCT tags.name) = $length"
    }

    def query(whereClause: String) =
      s"""
         SELECT *
         |FROM projects
         |WHERE id IN (
         |    SELECT project_id
         |    FROM project_tags
         |    LEFT JOIN tags
         |      ON project_tags.tag_id = tags.id
         |    ${whereClause}
         |    GROUP BY project_id
         |    ${distinctClause}
         |) AND is_master = true
         |GROUP BY $Table.id
    """.stripMargin

    query(whereClause)
  }

  val ListByCourse =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE course_id = ?
       |ORDER BY $OrderBy
     """.stripMargin

  val SelectAllByTag =
    s"""
       |SELECT * FROM (
       |SELECT $FieldsWithTable, (1 - similarity($TagsTable.name, ?)) as dist
       |FROM $Table
       |INNER JOIN $ProjectsTagsTable ON
       |$Table.id = $ProjectsTagsTable.project_id INNER JOIN $TagsTable on $TagsTable.id = $ProjectsTagsTable.tag_id
       |) inn
       |WHERE dist < 0.9 ORDER BY dist LIMIT 10
    """.stripMargin

  // Using here get_slug custom postgres function to generate unique slug if slug already exists
  val Insert =
    s"""
      |INSERT INTO $Table ($Fields)
      |VALUES (?, ?, ?, ?, get_slug(?, '$Table', ?), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      |RETURNING $Fields
    """.stripMargin

  // Using here get_slug custom postgres function to generate unique slug if slug already exists
  val Update =
    s"""
      |UPDATE $Table
      |SET course_id = ?, name = ?, parent_id = ?, parent_version = ?, is_master = ?, slug = get_slug(?, '$Table', ?), description = ?, long_description = ?, availability = ?, enabled = ?, project_type = ?, status = ?, last_task_id = ?, version = ?, updated_at = ?
      |WHERE id = ?
      |  AND version = ?
      |RETURNING $Fields
    """.stripMargin

  val Delete = s"""
    |DELETE
    |FROM $Table
    |WHERE id = ?
    | AND version = ?
    |RETURNING $Fields
  """.stripMargin

  /**
   * Find all Projects.
   *
   * @param showMasters optional parameter, by default is false, if true it will return the master projects.
   * @return a vector of the returned Projects
   */
  override def list(showMasters: Option[Boolean] = None, enabled: Option[Boolean] = None)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Project]]] = {
    val showMastersProjects = showMasters.getOrElse(false)
    val Select = if (showMastersProjects) SelectAllMaster else SelectAll
    (for {
      projectList <- lift(queryList(Select))
      result <- liftSeq {
        projectList.filter(project => if (enabled.isDefined) enabled.get.equals(project.enabled) else true).map { project =>
          (for {
            partList <- {
              lift(partRepository.list(project))
            }
            result = project.copy(parts = partList)
          } yield result).run
        }
      }
    } yield result).run
  }

  /**
   * List projects by tags
   *
   * @param tags (tagName:String, tagLang:String)
   * @param distinct Boolean If true each project should have all listed tags,
   *                 if false project should have at least one listed tag
   * @return
   */
  override def listByTags(tags: IndexedSeq[(String, String)], distinct: Boolean = true)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Project]]] = {
    val select = SelectByTags(tags, distinct)
    (for {
      projectList <- lift(queryList(select))
      result <- liftSeq {
        projectList.map { project =>
          (for {
            partList <- lift(partRepository.list(project))
            tagList <- lift(tagRepository.listByEntity(project.id, TaggableEntities.project))
            result = project.copy(parts = partList, tags = tagList)
          } yield result).run
        }
      }
    } yield result).run
  }

  /**
   * Clones the master project into the given group.
   *
   * @param projectId
   * @param courseId
   * @param conn
   * @return
   */
  override def cloneProject(projectId: UUID, courseId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Project]] = {
    (for {
      project <- lift(find(projectId))
      newProject = {
        if (project.isMaster) project.copy(id = UUID.randomUUID(), isMaster = false, courseId = courseId, parentId = Some(project.id), parentVersion = Some(project.version), enabled = true, createdAt = new DateTime, updatedAt = new DateTime)
        else project.copy(id = UUID.randomUUID(), courseId = courseId, parentId = Some(project.id), parentVersion = Some(project.version), createdAt = new DateTime, updatedAt = new DateTime)
      }
    } yield newProject).run
  }

  /**
   * Cloning the Parts of a Project.
   *
   * @param projectId
   * @return
   */
  def cloneProjectParts(projectId: UUID, ownerId: UUID, newProjectId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Part]]] = {
    (for {
      project <- lift(find(projectId))
      course <- lift(courseRepository.find(project.courseId))
      parts <- lift(partRepository.list(project))
      // We clone task but with old part ids!!!
      clonedTasks <- lift(cloneTasks(parts, project.isMaster))
      clonedParts <- lift(serializedT(parts)(part => {
        for {
          components <- lift(componentRepository.list(part))
          partId = UUID.randomUUID
          clonedPart = part.copy(
            id = partId,
            projectId = newProjectId,
            createdAt = new DateTime,
            updatedAt = new DateTime,
            // All parts should be enabled by default
            enabled = true,
            // We cloned task with old part ids, we should update them
            tasks = clonedTasks.filter(_.partId == part.id).map {
            case task: DocumentTask => task.copy(partId = partId)
            case task: MediaTask => task.copy(partId = partId)
            case _ => -\/(RepositoryError.BadParam("core.ProjectRepository.cloneProjectParts.wrong.task.type"))
          }.asInstanceOf[IndexedSeq[Task]],
            components = {
            // If we have new components owner
            if (course.ownerId != ownerId) cloneComponents(components, ownerId, project.isMaster)
            else components
          }
          )
        } yield clonedPart
      }))
    } yield clonedParts).run
  }

  /**
   * Cloning the Parts of a Project.
   *
   * @param projectId
   * @return
   */
  def cloneProjectComponents(projectId: UUID, ownerId: UUID, isMaster: Boolean)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Component]]] = {
    (for {
      project <- lift(find(projectId))
      projectComponents <- lift(componentRepository.list(project))
      clonedProjectComponents = cloneComponents(projectComponents, ownerId, isMaster)
    } yield clonedProjectComponents).run
  }

  /**
   * Cloning the Components.
   * This function is used when cloning the master projects
   *
   * @return
   */
  def cloneComponents(components: IndexedSeq[Component], ownerId: UUID, isMaster: Boolean): IndexedSeq[Component] = {
    components.map {
      case c: GoogleComponent =>
        c.copy(id = UUID.randomUUID, createdAt = new DateTime, updatedAt = new DateTime, ownerId = ownerId, parentId = if (isMaster) Some(c.id) else c.parentId, parentVersion = if (isMaster) Some(c.version) else c.parentVersion)
      case c: MicrosoftComponent =>
        c.copy(id = UUID.randomUUID, createdAt = new DateTime, updatedAt = new DateTime, ownerId = ownerId, parentId = if (isMaster) Some(c.id) else c.parentId, parentVersion = if (isMaster) Some(c.version) else c.parentVersion)
      case c: VideoComponent =>
        c.copy(id = UUID.randomUUID, createdAt = new DateTime, updatedAt = new DateTime, ownerId = ownerId, parentId = if (isMaster) Some(c.id) else c.parentId, parentVersion = if (isMaster) Some(c.version) else c.parentVersion)
      case c: AudioComponent =>
        c.copy(id = UUID.randomUUID, createdAt = new DateTime, updatedAt = new DateTime, ownerId = ownerId, parentId = if (isMaster) Some(c.id) else c.parentId, parentVersion = if (isMaster) Some(c.version) else c.parentVersion)
      case c: ImageComponent =>
        c.copy(id = UUID.randomUUID, createdAt = new DateTime, updatedAt = new DateTime, ownerId = ownerId, parentId = if (isMaster) Some(c.id) else c.parentId, parentVersion = if (isMaster) Some(c.version) else c.parentVersion)
      case c: BookComponent =>
        c.copy(id = UUID.randomUUID, createdAt = new DateTime, updatedAt = new DateTime, ownerId = ownerId, parentId = if (isMaster) Some(c.id) else c.parentId, parentVersion = if (isMaster) Some(c.version) else c.parentVersion)
      case c: TextComponent =>
        c.copy(id = UUID.randomUUID, createdAt = new DateTime, updatedAt = new DateTime, ownerId = ownerId, parentId = if (isMaster) Some(c.id) else c.parentId, parentVersion = if (isMaster) Some(c.version) else c.parentVersion)
      case c: GenericHTMLComponent =>
        c.copy(id = UUID.randomUUID, createdAt = new DateTime, updatedAt = new DateTime, ownerId = ownerId, parentId = if (isMaster) Some(c.id) else c.parentId, parentVersion = if (isMaster) Some(c.version) else c.parentVersion)
      case c: RubricComponent =>
        c.copy(id = UUID.randomUUID, createdAt = new DateTime, updatedAt = new DateTime, ownerId = ownerId, parentId = if (isMaster) Some(c.id) else c.parentId, parentVersion = if (isMaster) Some(c.version) else c.parentVersion)
    }
  }

  /**
   * Cloning all project task.
   * N.B. Part ids in the tasks are not updated, because at this point we don't know new part ids.
   *
   * @param parts All parts of a project
   * @return
   */
  def cloneTasks(parts: IndexedSeq[Part], isMaster: Boolean)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Task]]] = {
    val empty: Future[\/[RepositoryError.Fail, IndexedSeq[Task]]] = Future.successful(\/-(IndexedSeq.empty[Task]))
    // Go threw every part and get all tasks
    for {
      allTasks <- lift(parts.foldLeft(empty) { (fAccumulated, part) =>
        (for {
          accumulated <- lift(fAccumulated)
          tasks <- lift(taskRepository.list(part))
        } yield accumulated ++ tasks).run
      })
      //map that will contain as a key the old UUID of the task and the value will be the new UUID
      dependencies = collection.mutable.Map[UUID, UUID]()
      documentTasks = allTasks.filter(task => task.isInstanceOf[DocumentTask])
        .map(task => task.asInstanceOf[DocumentTask])
        .sortBy(_.dependencyId)

      noDependenciesTasks = documentTasks.filter(task => task.dependencyId.isEmpty)
      clonedWithoutDependencies = noDependenciesTasks.map(task => {
        val newId = UUID.randomUUID
        dependencies(task.id) = newId
        // If project is master then we save info about parent task
        task.copy(
          id = newId,
          partId = task.partId,
          settings = task.settings.copy(parentId = if (isMaster) Some(task.id) else task.settings.parentId),
          createdAt = new DateTime,
          updatedAt = new DateTime
        )
      })

      dependenciesTasks = documentTasks.filter(task => !task.dependencyId.isEmpty)
      clonedWithDependencies = dependenciesTasks.map(task => {
        val newId = UUID.randomUUID
        val dependencyId = dependencies get task.dependencyId.get
        if (dependencyId.isEmpty) {
          dependencies(task.dependencyId.get) = UUID.randomUUID
        }
        task.copy(
          id = newId,
          partId = task.partId,
          dependencyId = Some(dependencies(task.dependencyId.get)),
          settings = task.settings.copy(parentId = if (isMaster) Some(task.id) else task.settings.parentId),
          createdAt = new DateTime,
          updatedAt = new DateTime
        )
      })

      otherTasks = allTasks.filter(task => !task.isInstanceOf[DocumentTask])
      otherCloned = otherTasks.map(task => cloneTask(task, task.partId, isMaster))
    } yield (clonedWithDependencies union clonedWithoutDependencies union otherCloned).sortBy(t => t.position)
  }

  private def cloneTask(task: Task, partId: UUID, isMaster: Boolean): Task = {
    // If project is master then we save info about parent task
    task match {
      case t: DocumentTask => {
        task.asInstanceOf[DocumentTask].copy(
          id = UUID.randomUUID,
          partId = partId,
          settings = task.settings.copy(parentId = if (isMaster) Some(task.id) else task.settings.parentId),
          createdAt = new DateTime,
          updatedAt = new DateTime
        )
      }
      case t: MediaTask => task.asInstanceOf[MediaTask].copy(
        id = UUID.randomUUID,
        partId = partId,
        settings = task.settings.copy(parentId = if (isMaster) Some(task.id) else task.settings.parentId),
        createdAt = new DateTime,
        updatedAt = new DateTime
      )
      case t: QuestionTask => task.asInstanceOf[QuestionTask].copy(
        id = UUID.randomUUID,
        partId = partId,
        settings = task.settings.copy(parentId = if (isMaster) Some(task.id) else task.settings.parentId),
        createdAt = new DateTime,
        updatedAt = new DateTime
      )
    }
  }

  /**
   * Find all Projects belonging to a given group.
   *
   * @param course The section to return projects from.
   * @return a vector of the returned Projects
   */
  override def list(course: Course)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Project]]] =
    list(course, true)

  override def list(course: Course, fetchParts: Boolean) // format: OFF
                   (implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Project]]] = { // format: ON
    (for {
      projects <- lift(cacheRepository.cacheSeqProject.getCached(cacheProjectsKey(course.id)).flatMap {
        case \/-(projectList) => Future successful \/-(projectList)
        case -\/(noResults: RepositoryError.NoResults) =>
          for {
            projectList <- lift(queryList(ListByCourse, Seq[Any](course.id)))
            _ <- lift(cacheRepository.cacheSeqProject.putCache(cacheProjectsKey(course.id))(projectList, ttl))
          } yield projectList
        case -\/(error) => Future successful -\/(error)
      })
      result <- if (fetchParts) {
        liftSeq(projects.map { project =>
          (for {
            partList <- lift(partRepository.list(project))
            result = project.copy(parts = partList)
          } yield result).run
        })
      }
      else {
        lift(Future successful \/-(projects))
      }
    } yield result).run
  }

  /**
   * Find a single entry by ID.
   *
   * @param id the 128-bit UUID, as a byte array, to search for.
   * @return an optional Project if one was found
   */
  override def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Project]] = find(id, true)
  override def find(id: UUID, fetchParts: Boolean)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Project]] = {
    (for {
      project <- lift(cacheRepository.cacheProject.getCached(cacheProjectKey(id)).flatMap {
        case \/-(project) => Future successful \/-(project)
        case -\/(noResults: RepositoryError.NoResults) =>
          for {
            project <- lift(queryOne(SelectOne, Array[Any](id)))
            _ <- lift(cacheRepository.cacheProject.putCache(cacheProjectKey(project.id))(project, ttl))
            _ <- lift(cacheRepository.cacheUUID.putCache(cacheProjectSlugKey(project.slug))(project.id, ttl))
          } yield project
        case -\/(error) => Future successful -\/(error)
      })
      parts <- lift(if (fetchParts) partRepository.list(project, true) else Future successful \/-(IndexedSeq()))
    } yield project.copy(parts = parts)).run
  }

  /**
   * Find project by ID and User (teacher || student).
   *
   * @param projectId the 128-bit UUID, as a byte array, to search for.
   * @return an optional Project if one was found
   */
  override def find(projectId: UUID, user: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Project]] =
    find(projectId, user, true)

  override def find(projectId: UUID, user: User, fetchParts: Boolean)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Project]] = {
    (for {
      project <- lift(queryOne(SelectOneForUser, Array[Any](projectId, user.id, user.id)))
      parts <- lift(if (fetchParts) partRepository.list(project) else Future successful \/-(IndexedSeq()))
      _ <- lift(cacheRepository.cacheProject.putCache(cacheProjectKey(project.id))(project, ttl))
      _ <- lift(cacheRepository.cacheUUID.putCache(cacheProjectSlugKey(project.slug))(project.id, ttl))
    } yield project.copy(parts = parts)).run
  }

  /**
   * Find a project by slug.
   *
   * @param slug The project slug to search by.
   * @return an optional RowData object containing the results
   */
  def find(slug: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Project]] = find(slug, true)
  def find(slug: String, fetchParts: Boolean)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Project]] = {
    (for {
      project <- lift(cacheRepository.cacheUUID.getCached(cacheProjectSlugKey(slug)).flatMap {
        case \/-(projectId) => find(projectId, false)
        case -\/(noResults: RepositoryError.NoResults) =>
          for {
            project <- lift(queryOne(SelectOneBySlug, Seq[Any](slug)))
            _ <- lift(cacheRepository.cacheProject.putCache(cacheProjectKey(project.id))(project, ttl))
            _ <- lift(cacheRepository.cacheUUID.putCache(cacheProjectSlugKey(project.slug))(project.id, ttl))
          } yield project
        case -\/(error) => Future successful -\/(error)
      })
      parts <- lift(if (fetchParts) partRepository.list(project) else Future successful \/-(IndexedSeq()))
    } yield project.copy(parts = parts)).run
  }

  /**
   * Search by trigrams for autocomplete
   *
   * @param key
   * @param conn
   */
  def trigramSearch(key: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Project]]] = {
    queryList(SelectAllByTag, Seq[Any](key))
  }

  /**
   * Save a Project row.
   *
   * @param project The new project to save.
   * @param conn An implicit connection object. Can be used in a transactional chain.
   * @return the new project
   */
  override def insert(project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Project]] = {
    val params = Seq[Any](
      project.id, 1, project.courseId, project.name, project.slug, project.id, project.parentId, project.parentVersion, project.isMaster,
      project.description, project.longDescription, project.availability, project.enabled, project.projectType, project.status, project.lastTaskId, new DateTime, new DateTime
    )

    for {
      inserted <- lift(queryOne(Insert, params))
      _ <- lift(cacheRepository.cacheSeqProject.removeCached(cacheProjectsKey(project.courseId)))
    } yield inserted
  }

  /**
   * Update a Project row.
   *
   * @param project The project to update.
   * @param conn An implicit connection object. Can be used in a transactional chain.
   * @return the updated project.
   */
  override def update(project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Project]] = {
    val params = Seq[Any](
      project.courseId, project.name, project.parentId, project.parentVersion, project.isMaster, project.slug, project.id, project.description, project.longDescription,
      project.availability, project.enabled, project.projectType, project.status, project.lastTaskId, project.version + 1, new DateTime, project.id, project.version
    )

    (for {
      updatedProject <- lift(queryOne(Update, params))
      oldParts = project.parts
      _ <- lift(cacheRepository.cacheProject.removeCached(cacheProjectKey(project.id)))
      _ <- lift(cacheRepository.cacheUUID.removeCached(cacheProjectSlugKey(project.slug)))
      _ <- lift(cacheRepository.cacheSeqProject.removeCached(cacheProjectsKey(project.courseId)))
    } yield updatedProject.copy(parts = oldParts)).run
  }

  /**
   * Delete a single project.
   *
   * @param project The project to be deleted.
   * @param conn An implicit connection object. Can be used in a transactional chain.
   * @return the deleted project (if the deletion was successful) or an error
   */
  override def delete(project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Project]] = {
    (for {
      deletedProject <- lift(queryOne(Delete, Array(project.id, project.version)))
      oldParts = project.parts
      _ <- lift(cacheRepository.cacheProject.removeCached(cacheProjectKey(project.id)))
      _ <- lift(cacheRepository.cacheUUID.removeCached(cacheProjectSlugKey(project.slug)))
      _ <- lift(cacheRepository.cacheSeqProject.removeCached(cacheProjectsKey(project.courseId)))
    } yield deletedProject.copy(parts = oldParts)).run
  }
}
