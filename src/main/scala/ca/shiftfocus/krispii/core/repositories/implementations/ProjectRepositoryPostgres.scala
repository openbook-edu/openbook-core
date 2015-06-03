package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import com.github.mauricio.async.db.{RowData, Connection}

import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.models._
import java.util.UUID
import scala.concurrent.Future
import org.joda.time.DateTime

import scalacache.ScalaCache
import scalaz._

class ProjectRepositoryPostgres(val partRepository: PartRepository)
  extends ProjectRepository with PostgresRepository[Project] {

  def constructor(row: RowData): Project = {
    Project(
      row("id").asInstanceOf[UUID],
      row("course_id").asInstanceOf[UUID],
      row("version").asInstanceOf[Long],
      row("name").asInstanceOf[String],
      row("slug").asInstanceOf[String],
      row("description").asInstanceOf[String],
      row("availability").asInstanceOf[String],
      IndexedSeq[Part](),
      row("created_at").asInstanceOf[DateTime],
      row("updated_at").asInstanceOf[DateTime]
    )
  }

  val Table           = "projects"
  val Fields          = "id, version, course_id, name, slug, description, availability, created_at, updated_at"
  val FieldsWithTable = Fields.split(", ").map({ field => s"${Table}." + field}).mkString(", ")
  val QMarks          = "?, ?, ?, ?, ?, ?, ?, ?, ?"

  // User CRUD operations
  val SelectAll =
    s"""
       |SELECT $Fields
       |FROM $Table
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
     """.stripMargin


  val Insert =
    s"""
      |INSERT INTO $Table ($Fields)
      |VALUES ($QMarks)
      |RETURNING $Fields
    """.stripMargin

  val Update =
    s"""
      |UPDATE $Table
      |SET course_id = ?, name = ?, slug = ?, description = ?, availability = ?, version = ?, updated_at = ?
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
  override def list(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[Project]]] = {
    (for {
      projectList <- lift(queryList(SelectAll))
      result <- liftSeq { projectList.map{ project =>
        (for {
          partList <- lift(partRepository.list(project))
          result = project.copy(parts = partList)
        } yield result).run
      }}
    } yield result).run
  }


  /**
   * Find all Projects belonging to a given course.
   *
   * @param course The section to return projects from.
   * @return a vector of the returned Projects
   */
  override def list(course: Course)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[Project]]] = list(course, true)
  override def list(course: Course, fetchParts: Boolean)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[Project]]] = {
    (for {
      projects <- lift(cache.getCached[IndexedSeq[Project]](cacheProjectsKey(course.id)).flatMap {
        case \/-(projectList) => Future successful \/-(projectList)
        case -\/(RepositoryError.NoResults) =>
          for {
            projectList <- lift(queryList(ListByCourse, Seq[Any](course.id)))
            _ <- lift(cache.putCache[IndexedSeq[Project]](cacheProjectsKey(course.id))(projectList, ttl))
          } yield projectList
        case -\/(error) => Future successful -\/(error)
      })
      result <- if (fetchParts) liftSeq(projects.map{ project =>
        (for {
          partList <- lift(partRepository.list(project))
          result = project.copy(parts = partList)
        } yield result).run
      }) else lift(Future successful \/-(projects))
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
        case -\/(RepositoryError.NoResults) =>
          for {
            project <- lift(queryOne(SelectOne, Array[Any](id)))
            _ <- lift(cache.putCache[Project](cacheProjectKey(project.id))(project, ttl))
            _ <- lift(cache.putCache[UUID](cacheProjectSlugKey(project.slug))(project.id, ttl))
          } yield project
        case -\/(error) => Future successful -\/(error)
      })
      parts <- lift(if (fetchParts) partRepository.list(project) else Future successful \/-(IndexedSeq()))
    } yield project.copy(parts = parts)).run
  }

  /**
   * Find project by ID and User (teacher || student).
   *
   * @param projectId the 128-bit UUID, as a byte array, to search for.
   * @return an optional Project if one was found
   */
  override def find(projectId: UUID, user: User)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Project]] = find(projectId, user, true)
  override def find(projectId: UUID, user: User, fetchParts: Boolean)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Project]] = {
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
        case -\/(RepositoryError.NoResults) =>
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
      project.id, 1, project.courseId, project.name, project.slug,
      project.description, project.availability, new DateTime, new DateTime
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
      project.courseId, project.name, project.slug, project.description,
      project.availability, project.version + 1, new DateTime, project.id, project.version
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
