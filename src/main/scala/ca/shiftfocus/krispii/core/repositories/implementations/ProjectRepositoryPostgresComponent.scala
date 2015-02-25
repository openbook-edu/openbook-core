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

trait ProjectRepositoryPostgresComponent extends ProjectRepositoryComponent with PostgresRepository {
  self: PartRepositoryComponent with
        PostgresDB =>

  /**
   * Override with this trait's version of the ProjectRepository.
   */
  override val projectRepository: ProjectRepository = new ProjectRepositoryPSQL

  /**
   * A concrete implementation of the ProjectRepository class.
   */
  private class ProjectRepositoryPSQL extends ProjectRepository {

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
    override def list: Future[\/[Fail, IndexedSeq[Project]]] = {
      val fProjectList = db.pool.sendQuery(SelectAll).map(res => buildEntityList(res.rows, Project.apply))

      val fResult = for {
        projectList <- lift(fProjectList)
        result <- liftSeq { projectList.map{ project =>
          (for {
            partList <- lift(partRepository.list(project))
            result = project.copy(parts = partList)
          } yield result).run
        }}
      } yield result

      fResult.run.recover {
        case exception: Throwable => throw exception
      }
    }


    /**
     * Find all Projects belonging to a given course.
     *
     * @param course The section to return projects from.
     * @return a vector of the returned Projects
     */
    override def list(course: Course): Future[\/[Fail, IndexedSeq[Project]]] = {
      val fProjectList = db.pool.sendPreparedStatement(ListByCourse, Array[Any](course.id.bytes)).map {
        result => buildEntityList(result.rows, Project.apply)
      }.recover {
        case exception: Throwable => throw exception
      }

      (for {
        projects <- lift(fProjectList)
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
    override def find(id: UUID): Future[\/[Fail, Project]] = {
      val projectQuery = db.pool.sendPreparedStatement(SelectOne, Array[Any](id.bytes)).map {
        result => buildEntity(result.rows, Project.apply)
      }

      val result = for {
        project <- lift(projectQuery)
        parts <- lift(partRepository.list(project))
      } yield project.copy(parts = parts)

      result.run.recover {
        case exception: Throwable => throw exception
      }
    }

    /**
     * Find project by ID and User (teacher || student).
     *
     * @param projectId the 128-bit UUID, as a byte array, to search for.
     * @return an optional Project if one was found
     */
    override def find(projectId: UUID, user: User): Future[\/[Fail, Project]] = {
      val projectQuery = db.pool.sendPreparedStatement(SelectOneForUser, Array[Any](projectId.bytes, user.id.bytes, user.id.bytes)).map {
        result => buildEntity(result.rows, Project.apply)
      }

      val result = for {
        project <- lift(projectQuery)
        parts <- lift(partRepository.list(project))
      } yield project.copy(parts = parts)

      result.run.recover {
        case exception: Throwable => throw exception
      }
    }

    /**
     * Find a project by slug.
     *
     * @param slug The project slug to search by.
     * @return an optional RowData object containing the results
     */
    def find(slug: String): Future[\/[Fail, Project]] = {
      val projectQuery = db.pool.sendPreparedStatement(SelectOneBySlug, Array[Any](slug)).map {
        result => buildEntity(result.rows, Project.apply)
      }

      val result = for {
        project <- lift(projectQuery)
        parts <- lift(partRepository.list(project))
      } yield project.copy(parts = parts)

      result.run.recover {
        case exception: Throwable => throw exception
      }
    }

    /**
     * Save a Project row.
     *
     * @param project The new project to save.
     * @param conn An implicit connection object. Can be used in a transactional chain.
     * @return the new project
     */
    override def insert(project: Project)(implicit conn: Connection): Future[\/[Fail, Project]] = {
      conn.sendPreparedStatement(Insert, Array(
        project.id.bytes,
        project.courseId.bytes,
        project.name,
        project.slug,
        project.description,
        project.availability,
        new DateTime,
        new DateTime
      )).map {
        result => buildEntity(result.rows, Project.apply)
      }.recover {
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
      conn.sendPreparedStatement(Update, Array(
        project.courseId.bytes,
        project.name,
        project.slug,
        project.description,
        project.availability,
        project.version + 1,
        new DateTime,
        project.id.bytes,
        project.version
      )).map {
        result => buildEntity(result.rows, Project.apply)
      }.recover {
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
      conn.sendPreparedStatement(Delete, Array(project.id.bytes, project.version)).map {
        result =>
          if (result.rowsAffected == 1) \/-(project)
          else -\/(GenericFail("Query succeeded but a project was not deleted."))
      }.recover {
        case exception: Throwable => throw exception
      }
    }
  }
}
