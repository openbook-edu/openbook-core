package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error._
import com.github.mauricio.async.db.{RowData, Connection}
import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future
import org.joda.time.DateTime

import scalacache.ScalaCache
import scalaz._

class ProjectRepositoryPostgres(val partRepository: PartRepository)
  extends ProjectRepository with PostgresRepository[Project] {

  def constructor(row: RowData): Project = {
    Project(
      UUID(row("id").asInstanceOf[Array[Byte]]),
      UUID(row("course_id").asInstanceOf[Array[Byte]]),
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
  override def list(implicit conn: Connection, cache: ScalaCache): Future[\/[RepositoryError.Fail, IndexedSeq[Project]]] = {
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
  override def list(course: Course)(implicit conn: Connection, cache: ScalaCache): Future[\/[RepositoryError.Fail, IndexedSeq[Project]]] = {
    (for {
      projects <- lift(getCached[IndexedSeq[Project]](cacheProjectsKey(course.id)).flatMap {
        case \/-(projectList) => Future successful \/-(projectList)
        case -\/(RepositoryError.NoResults) =>
          for {
            projectList <- lift(queryList(ListByCourse, Seq[Any](course.id.bytes)))
            _ <- lift(putCache[IndexedSeq[Project]](cacheProjectsKey(course.id))(projectList, ttl))
          } yield projectList
        case -\/(error) => Future successful -\/(error)
      })
      result <- liftSeq(projects.map{ project =>
        (for {
          partList <- lift(partRepository.list(project))
          result = project.copy(parts = partList)
        } yield result).run
      })
    } yield result).run
  }

  /**
   * Find a single entry by ID.
   *
   * @param id the 128-bit UUID, as a byte array, to search for.
   * @return an optional Project if one was found
   */
  override def find(id: UUID)(implicit conn: Connection, cache: ScalaCache): Future[\/[RepositoryError.Fail, Project]] = {
    (for {
      project <- lift(getCached[Project](cacheProjectKey(id)).flatMap {
        case \/-(project) => Future successful \/-(project)
        case -\/(RepositoryError.NoResults) =>
          for {
            project <- lift(queryOne(SelectOne, Array[Any](id.bytes)))
            _ <- lift(putCache[Project](cacheProjectKey(project.id))(project, ttl))
            _ <- lift(putCache[UUID](cacheProjectSlugKey(project.slug))(project.id, ttl))
          } yield project
        case -\/(error) => Future successful -\/(error)
      })
      parts <- lift(partRepository.list(project))
    } yield project.copy(parts = parts)).run
  }

  /**
   * Find project by ID and User (teacher || student).
   *
   * @param projectId the 128-bit UUID, as a byte array, to search for.
   * @return an optional Project if one was found
   */
  override def find(projectId: UUID, user: User)(implicit conn: Connection, cache: ScalaCache): Future[\/[RepositoryError.Fail, Project]] = {
    (for {
      project <- lift(queryOne(SelectOneForUser, Array[Any](projectId.bytes, user.id.bytes, user.id.bytes)))
      parts <- lift(partRepository.list(project))
      _ <- lift(putCache[Project](cacheProjectKey(project.id))(project, ttl))
      _ <- lift(putCache[UUID](cacheProjectSlugKey(project.slug))(project.id, ttl))
    } yield project.copy(parts = parts)).run
  }

  /**
   * Find a project by slug.
   *
   * @param slug The project slug to search by.
   * @return an optional RowData object containing the results
   */
  def find(slug: String)(implicit conn: Connection, cache: ScalaCache): Future[\/[RepositoryError.Fail, Project]] = {
    (for {
      project <- lift(getCached[UUID](cacheProjectSlugKey(slug)).flatMap {
        case \/-(projectId) => find(projectId)
        case -\/(RepositoryError.NoResults) =>
          for {
            project <- lift(queryOne(SelectOneBySlug, Seq[Any](slug)))
            _ <- lift(putCache[Project](cacheProjectKey(project.id))(project, ttl))
            _ <- lift(putCache[UUID](cacheProjectSlugKey(project.slug))(project.id, ttl))
          } yield project
        case -\/(error) => Future successful -\/(error)
      })
      parts <- lift(partRepository.list(project))
    } yield project.copy(parts = parts)).run
  }

  /**
   * Save a Project row.
   *
   * @param project The new project to save.
   * @param conn An implicit connection object. Can be used in a transactional chain.
   * @return the new project
   */
  override def insert(project: Project)(implicit conn: Connection, cache: ScalaCache): Future[\/[RepositoryError.Fail, Project]] = {
    val params = Seq(
      project.id.bytes, 1, project.courseId.bytes, project.name, project.slug,
      project.description, project.availability, new DateTime, new DateTime
    )

    for {
      inserted <- lift(queryOne(Insert, params))
      _ <- lift(removeCached(cacheProjectsKey(project.courseId)))
    } yield inserted
  }

  /**
   * Save a Project row.
   *
   * @param project The project to update.
   * @param conn An implicit connection object. Can be used in a transactional chain.
   * @return the updated project.
   */
  override def update(project: Project)(implicit conn: Connection, cache: ScalaCache): Future[\/[RepositoryError.Fail, Project]] = {
    val params = Seq[Any](
      project.courseId.bytes, project.name, project.slug, project.description,
      project.availability, project.version + 1, new DateTime, project.id.bytes, project.version
    )

    (for {
      updatedProject <- lift(queryOne(Update, params))
      oldParts = project.parts
      _ <- lift(removeCached(cacheProjectKey(project.id)))
      _ <- lift(removeCached(cacheProjectSlugKey(project.slug)))
      _ <- lift(removeCached(cacheProjectsKey(project.courseId)))
    } yield updatedProject.copy(parts = oldParts)).run
  }

  /**
   * Delete a single project.
   *
   * @param project The project to be deleted.
   * @param conn An implicit connection object. Can be used in a transactional chain.
   * @return a boolean indicator whether the deletion was successful.
   */
  override def delete(project: Project)(implicit conn: Connection, cache: ScalaCache): Future[\/[RepositoryError.Fail, Project]] = {
    (for {
      deletedProject <- lift(queryOne(Delete, Array(project.id.bytes, project.version)))
      oldParts = project.parts
      _ <- lift(removeCached(cacheProjectKey(project.id)))
      _ <- lift(removeCached(cacheProjectSlugKey(project.slug)))
      _ <- lift(removeCached(cacheProjectsKey(project.courseId)))
    } yield deletedProject.copy(parts = oldParts)).run
  }
}
