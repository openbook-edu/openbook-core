package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.krispii.core.models
import ca.shiftfocus.krispii.core.models.tasks.{ QuestionTask, DocumentTask, Task }
import com.github.mauricio.async.db.{ RowData, Connection }
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.models._
import java.util.UUID
import scala.concurrent.Future
import org.joda.time.DateTime

import scala.util.Try
import scalacache.ScalaCache
import scalaz._

class ProjectRepositoryPostgres(val partRepository: PartRepository)
    extends ProjectRepository with PostgresRepository[Project] {

  override val entityName = "Project"

  def constructor(row: RowData): Project = {
    Logger.error(row("enabled").toString)
    Project(
      row("id").asInstanceOf[UUID],
      row("course_id").asInstanceOf[UUID],
      Option(row("parent_id")).map(_.asInstanceOf[UUID]),
      row("is_master").asInstanceOf[Boolean],
      row("version").asInstanceOf[Long],
      row("name").asInstanceOf[String],
      row("slug").asInstanceOf[String],
      row("description").asInstanceOf[String],
      row("availability").asInstanceOf[String],
      row("enabled").asInstanceOf[Boolean],
      IndexedSeq[Part](),
      row("created_at").asInstanceOf[DateTime],
      row("updated_at").asInstanceOf[DateTime]
    )
  }

  val Table = "projects"
  val Fields = "id, version, course_id, name, slug, parent_id, is_master, description, availability, enabled, created_at, updated_at"
  val FieldsWithTable = Fields.split(", ").map({ field => s"${Table}." + field }).mkString(", ")
  val QMarks = "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?"
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

  val ListByCourse =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE course_id = ?
       |ORDER BY $OrderBy
     """.stripMargin

  // Using here get_slug custom postgres function to generate unique slug if slug already exists
  val Insert =
    s"""
      |INSERT INTO $Table ($Fields)
      |VALUES (?, ?, ?, ?, get_slug(?, '$Table', ?), ?, ?, ?, ?, ?, ?, ?)
      |RETURNING $Fields
    """.stripMargin

  // Using here get_slug custom postgres function to generate unique slug if slug already exists
  val Update =
    s"""
      |UPDATE $Table
      |SET course_id = ?, name = ?, parent_id = ?, is_master = ?, slug = get_slug(?, '$Table', ?), description = ?, availability = ?, enabled = ?, version = ?, updated_at = ?
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
   * @return a vector of the returned Projects
   */
  override def list(showMasters: Option[Boolean] = None)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[Project]]] = {
    val showMastersProjects = showMasters.getOrElse(false)
    val Select = if (showMastersProjects) SelectAllMaster else SelectAll
    (for {
      projectList <- lift(queryList(Select))
      result <- liftSeq {
        projectList.map { project =>
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
   * Clones the master project into the given course.
   * @param projectId
   * @param courseId
   * @param conn
   * @param cache
   * @return
   */
  override def cloneProject(projectId: UUID, courseId: UUID)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Project]] = {
    (for {
      project <- lift(find(projectId))
      newProject = project.copy(id = UUID.randomUUID(), isMaster = false, courseId = courseId, parentId = Some(project.id))
    } yield newProject).run
  }

  /**
   * Cloning the Parts of a Project.
   * @param projectId
   * @param conn
   * @param cache
   * @return
   */
  def cloneProjectParts(projectId: UUID, ownerId: UUID)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[Part]]] = {
    (for {
      project <- lift(find(projectId))
      parts <- lift(partRepository.list(project, true, true))
      clonedParts = parts.map(part => {
        part.copy(
          id = UUID.randomUUID,
          projectId = project.id,
          createdAt = new DateTime,
          updatedAt = new DateTime,
          tasks = cloneTasks(part.tasks),
          components = cloneComponents(part.components, ownerId)
        )
      })
    } yield clonedParts).run
  }

  /**
   * Cloning the Components.
   * This function is used when cloning the master projects
   * @return
   */
  def cloneComponents(components: IndexedSeq[Component], ownerId: UUID): IndexedSeq[Component] = {
    components.map(component => {
      component match {
        case c: VideoComponent =>
          c.copy(id = UUID.randomUUID, createdAt = new DateTime, updatedAt = new DateTime, ownerId = ownerId)
        case c: AudioComponent =>
          c.copy(id = UUID.randomUUID, createdAt = new DateTime, updatedAt = new DateTime, ownerId = ownerId)
        case c: TextComponent =>
          c.copy(id = UUID.randomUUID, createdAt = new DateTime, updatedAt = new DateTime, ownerId = ownerId)
      }
    })
  }

  /**
   * Cloning the tasks of a Part.
   * @param tasks
   * @return
   */
  def cloneTasks(tasks: IndexedSeq[Task]): IndexedSeq[Task] = {
    //map that will contain as a key the old UUID of the task and the value will be the new UUID
    val dependencies = collection.mutable.Map[UUID, UUID]()
    val documentTasks = tasks.filter(task => task.isInstanceOf[DocumentTask])
      .map(task => task.asInstanceOf[DocumentTask])
      .sortBy(_.dependencyId)
    val noDependenciesTasks = documentTasks.filter(task => task.dependencyId.isEmpty)
    val clonedWithoutDependencies = noDependenciesTasks.map(task => {
      val newId = UUID.randomUUID
      dependencies(task.id) = newId
      task.copy(id = newId, createdAt = new DateTime, updatedAt = new DateTime)
    })
    val dependenciesTasks = documentTasks.filter(task => !task.dependencyId.isEmpty)
    val clonedWithDependencies = dependenciesTasks.map(task => {
      val newId = UUID.randomUUID
      val dependencyId = dependencies get task.dependencyId.get
      if (dependencyId.isEmpty) {
        dependencies(task.dependencyId.get) = UUID.randomUUID
      }
      task.copy(id = newId, dependencyId = Some(dependencies(task.dependencyId.get)), createdAt = new DateTime, updatedAt = new DateTime)
    })

    val otherTasks = tasks.filter(task => !task.isInstanceOf[DocumentTask])

    val otherCloned = otherTasks.map(task => cloneTask(task))

    (clonedWithDependencies union clonedWithoutDependencies union otherCloned).sortBy(t => t.position)
  }

  private def cloneTask(task: Task): Task = {
    task match {
      case t: DocumentTask => {
        task.asInstanceOf[DocumentTask].copy(id = UUID.randomUUID, createdAt = new DateTime, updatedAt = new DateTime)
      }
      case t: QuestionTask => task.asInstanceOf[QuestionTask].copy(id = UUID.randomUUID, createdAt = new DateTime, updatedAt = new DateTime)
    }
  }

  /**
   * Find all Projects belonging to a given course.
   *
   * @param course The section to return projects from.
   * @return a vector of the returned Projects
   */
  override def list(course: Course)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[Project]]] =
    list(course, true)

  override def list(course: Course, fetchParts: Boolean) // format: OFF
                   (implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[Project]]] = { // format: ON
    (for {
      projects <- lift(cache.getCached[IndexedSeq[Project]](cacheProjectsKey(course.id)).flatMap {
        case \/-(projectList) => Future successful \/-(projectList)
        case -\/(noResults: RepositoryError.NoResults) =>
          for {
            projectList <- lift(queryList(ListByCourse, Seq[Any](course.id)))
            _ <- lift(cache.putCache[IndexedSeq[Project]](cacheProjectsKey(course.id))(projectList, ttl))
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
  override def find(id: UUID)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Project]] = find(id, true)
  override def find(id: UUID, fetchParts: Boolean)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Project]] = {
    (for {
      project <- lift(cache.getCached[Project](cacheProjectKey(id)).flatMap {
        case \/-(project) => Future successful \/-(project)
        case -\/(noResults: RepositoryError.NoResults) =>
          for {
            project <- lift(queryOne(SelectOne, Array[Any](id)))
            _ <- lift(cache.putCache[Project](cacheProjectKey(project.id))(project, ttl))
            _ <- lift(cache.putCache[UUID](cacheProjectSlugKey(project.slug))(project.id, ttl))
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
  override def find(projectId: UUID, user: User)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Project]] =
    find(projectId, user, true)

  override def find(projectId: UUID, user: User, fetchParts: Boolean)(implicit
    conn: Connection,
    cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Project]] = {
    (for {
      project <- lift(queryOne(SelectOneForUser, Array[Any](projectId, user.id, user.id)))
      parts <- lift(if (fetchParts) partRepository.list(project) else Future successful \/-(IndexedSeq()))
      _ <- lift(cache.putCache[Project](cacheProjectKey(project.id))(project, ttl))
      _ <- lift(cache.putCache[UUID](cacheProjectSlugKey(project.slug))(project.id, ttl))
    } yield project.copy(parts = parts)).run
  }

  /**
   * Find a project by slug.
   *
   * @param slug The project slug to search by.
   * @return an optional RowData object containing the results
   */
  def find(slug: String)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Project]] = find(slug, true)
  def find(slug: String, fetchParts: Boolean)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Project]] = {
    (for {
      project <- lift(cache.getCached[UUID](cacheProjectSlugKey(slug)).flatMap {
        case \/-(projectId) => find(projectId, false)
        case -\/(noResults: RepositoryError.NoResults) =>
          for {
            project <- lift(queryOne(SelectOneBySlug, Seq[Any](slug)))
            _ <- lift(cache.putCache[Project](cacheProjectKey(project.id))(project, ttl))
            _ <- lift(cache.putCache[UUID](cacheProjectSlugKey(project.slug))(project.id, ttl))
          } yield project
        case -\/(error) => Future successful -\/(error)
      })
      parts <- lift(if (fetchParts) partRepository.list(project) else Future successful \/-(IndexedSeq()))
    } yield project.copy(parts = parts)).run
  }

  /**
   * Save a Project row.
   *
   * @param project The new project to save.
   * @param conn An implicit connection object. Can be used in a transactional chain.
   * @return the new project
   */
  override def insert(project: Project)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Project]] = {
    val params = Seq[Any](
      project.id, 1, project.courseId, project.name, project.slug, project.id, project.parentId, project.isMaster,
      project.description, project.availability, project.enabled, new DateTime, new DateTime
    )

    for {
      inserted <- lift(queryOne(Insert, params))
      _ <- lift(cache.removeCached(cacheProjectsKey(project.courseId)))
    } yield inserted
  }

  /**
   * Save a Project row.
   *
   * @param project The project to update.
   * @param conn An implicit connection object. Can be used in a transactional chain.
   * @return the updated project.
   */
  override def update(project: Project)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Project]] = {
    val params = Seq[Any](
      project.courseId, project.name, project.parentId, project.isMaster, project.slug, project.id, project.description,
      project.availability, project.enabled, project.version + 1, new DateTime, project.id, project.version
    )

    (for {
      updatedProject <- lift(queryOne(Update, params))
      oldParts = project.parts
      _ <- lift(cache.removeCached(cacheProjectKey(project.id)))
      _ <- lift(cache.removeCached(cacheProjectSlugKey(project.slug)))
      _ <- lift(cache.removeCached(cacheProjectsKey(project.courseId)))
    } yield updatedProject.copy(parts = oldParts)).run
  }

  /**
   * Delete a single project.
   *
   * @param project The project to be deleted.
   * @param conn An implicit connection object. Can be used in a transactional chain.
   * @return a boolean indicator whether the deletion was successful.
   */
  override def delete(project: Project)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Project]] = {
    (for {
      deletedProject <- lift(queryOne(Delete, Array(project.id, project.version)))
      oldParts = project.parts
      _ <- lift(cache.removeCached(cacheProjectKey(project.id)))
      _ <- lift(cache.removeCached(cacheProjectSlugKey(project.slug)))
      _ <- lift(cache.removeCached(cacheProjectsKey(project.courseId)))
    } yield deletedProject.copy(parts = oldParts)).run
  }
}
