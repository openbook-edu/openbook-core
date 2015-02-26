package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.fail._
import com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException
import com.github.mauricio.async.db.{ResultSet, RowData, Connection}
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib.ExceptionWriter
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import play.api.Play.current

import play.api.Logger
import scala.concurrent.Future
import org.joda.time.DateTime
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB

import scalaz.syntax.traverse._
import scalaz._
import Scalaz._

trait ProjectRepositoryPostgresComponent extends ProjectRepositoryComponent {
  self: PartRepositoryComponent with
        PostgresDB =>

  /**
   * Override with this trait's version of the ProjectRepository.
   */
  override val projectRepository: ProjectRepository = new ProjectRepositoryPSQL

  /**
   * A concrete implementation of the ProjectRepository class.
   */
  private class ProjectRepositoryPSQL extends ProjectRepository with PostgresRepository[Project] {

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

    val Select =
      """
         |SELECT projects.id as id, projects.version as version, projects.course_id, projects.name as name, projects.slug as slug,
         |       projects.description as description, projects.availability as availability, projects.created_at as created_at, projects.updated_at as updated_at
       """.stripMargin

    val From =
      """
        |FROM projects
      """.stripMargin

    val Returning =
      """
        |RETURNING id, version, course_id, name, slug, description, availability, created_at, updated_at
      """.stripMargin


    // User CRUD operations
    val SelectAll =
      s"""
         |$Select
         |$From
      """.stripMargin

    val SelectOne =
      s"""
         |$Select
         |$From
         |WHERE projects.id = ?
      """.stripMargin

    val SelectOneForUser =
      s"""
         |$Select
         |$From, courses, users_courses
         |WHERE projects.id = ?
         |  AND projects.course_id = courses.id
         |  AND (courses.teacher_id = ? OR (
         |    courses.id = users_courses.course_id AND users_courses.user_id = ?
         |  ))
       """.stripMargin

    val SelectOneBySlug =
      s"""
         |$Select
         |$From
         |WHERE slug = ?
       """.stripMargin

    val ListByCourse =
      s"""
         |$Select
         |$From
         |WHERE course_id = ?
       """.stripMargin


    val Insert =
      s"""
        |INSERT INTO projects (id, version, course_id, name, slug, description, availability, created_at, updated_at)
        |VALUES (?, 1, ?, ?, ?, ?, ?, ?, ?)
        |$Returning
      """.stripMargin

    val Update =
      s"""
        |UPDATE projects
        |SET course_id = ?, name = ?, slug = ?, description = ?, availability = ?, version = ?, updated_at = ?
        |WHERE id = ?
        |  AND version = ?
        |$Returning
      """.stripMargin

    val Delete = s"""
      DELETE FROM projects WHERE id = ? AND version = ?
    """

    /**
     * Find all Projects.
     *
     * @return a vector of the returned Projects
     */
    override def list(implicit conn: Connection): Future[\/[Fail, IndexedSeq[Project]]] = {
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
    override def list(course: Course)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[Project]]] = {
      (for {
        projects <- lift(queryList(ListByCourse, Seq[Any](course.id.bytes)))
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
    override def find(id: UUID)(implicit conn: Connection): Future[\/[Fail, Project]] = {
      (for {
        project <- lift(queryOne(SelectOne, Array[Any](id.bytes)))
        parts <- lift(partRepository.list(project))
      } yield project.copy(parts = parts)).run
    }

    /**
     * Find project by ID and User (teacher || student).
     *
     * @param projectId the 128-bit UUID, as a byte array, to search for.
     * @return an optional Project if one was found
     */
    override def find(projectId: UUID, user: User)(implicit conn: Connection): Future[\/[Fail, Project]] = {
      (for {
        project <- lift(queryOne(SelectOneForUser, Array[Any](projectId.bytes, user.id.bytes, user.id.bytes)))
        parts <- lift(partRepository.list(project))
      } yield project.copy(parts = parts)).run
    }

    /**
     * Find a project by slug.
     *
     * @param slug The project slug to search by.
     * @return an optional RowData object containing the results
     */
    def find(slug: String)(implicit conn: Connection): Future[\/[Fail, Project]] = {
      (for {
        project <- lift(queryOne(SelectOneBySlug, Seq[Any](slug)))
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
    override def insert(project: Project)(implicit conn: Connection): Future[\/[Fail, Project]] = {
      val params = Seq(
        project.id.bytes, project.courseId.bytes, project.name, project.slug,
        project.description, project.availability, new DateTime, new DateTime
      )

      queryOne(Insert, params).recover {
        case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
          case Some(nField) =>
            if (nField == "projects_pkey") -\/(UniqueFieldConflict(s"A project with key ${project.id.string} already exists"))
            else if (nField == "projects_slug_key") -\/(UniqueFieldConflict(s"A project with slug ${project.slug} already exists"))
            else throw exception
          case _ => throw exception
        }
        case exception: Throwable => throw exception
      }
    }

    /**
     * Save a Project row.
     *
     * @param project The project to update.
     * @param conn An implicit connection object. Can be used in a transactional chain.
     * @return the updated project.
     */
    override def update(project: Project)(implicit conn: Connection): Future[\/[Fail, Project]] = {
      val params = Seq(
        project.courseId.bytes, project.name, project.slug, project.description,
        project.availability, project.version + 1, new DateTime, project.id.bytes, project.version
      )

      queryOne(Update, params).recover {
        case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
          case Some(nField) =>
            if (nField == "projects_pkey") -\/(UniqueFieldConflict(s"A project with key ${project.id.string} already exists"))
            else if (nField == "projects_slug_key") -\/(UniqueFieldConflict(s"A project with slug ${project.slug} already exists"))
            else throw exception
          case _ => throw exception
        }
        case exception: Throwable => throw exception
      }
    }

    /**
     * Delete a single project.
     *
     * @param project The project to be deleted.
     * @param conn An implicit connection object. Can be used in a transactional chain.
     * @return a boolean indicator whether the deletion was successful.
     */
    override def delete(project: Project)(implicit conn: Connection): Future[\/[Fail, Project]] = {
      queryOne(Delete, Array(project.id.bytes, project.version)).recover {
        case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
          case Some(nField) =>
            if (nField.endsWith("fkey")) -\/(ReferenceConflict(s"We cannot delete this project while it's still being referenced by other entities."))
            else throw exception
          case _ => throw exception
        }
        case exception: Throwable => throw exception
      }
    }
  }
}
