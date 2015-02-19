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
         |$From, classes, users_classes
         |WHERE projects.id = ?
         |  AND projects.course_id = classes.id
         |  AND (classes.teacher_id = ? OR (
         |    classes.id = users_classes.course_id AND users_classes.user_id = ?
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
      val fProjectList = db.pool.sendQuery(SelectAll).map(res => buildProjectList(res.rows))

      val fResult = for {
        projectList <- lift(fProjectList)
        intermediate <- Future sequence projectList.map{ project =>
          partRepository.list(project).map {
            case \/-(partList) => \/-(project.copy(parts = partList))
            case -\/(error: Fail) => -\/(error)
          }
        }
        result: IndexedSeq[Project] <- lift[IndexedSeq[Project]](Future.successful {
          if (intermediate.filter(_.isLeft).nonEmpty) -\/(intermediate.filter(_.isLeft).head.swap.toOption.get)
          else \/-(intermediate.map(_.toOption.get))
        })
      } yield result

      fResult.run.recover {
        case exception: Throwable => -\/(ExceptionalFail("An unexpected error occurred.", exception))
      }
    }


    /**
     * Find all Projects belonging to a given class.
     *
     * @param course The section to return projects from.
     * @return a vector of the returned Projects
     */
    override def list(course: Course): Future[\/[Fail, IndexedSeq[Project]]] = {
      val projectList = for {
        queryResult <- db.pool.sendPreparedStatement(ListByCourse, Array[Any](course.id.bytes))
        projectList <- Future successful {
          queryResult.rows.get.map { item => Project(item) }
        }
        intermediate <- Future sequence projectList.map{ project =>
          partRepository.list(project).map {
            case \/-(partList) => \/-(project.copy(parts = partList))
            case -\/(error: Fail) => -\/(error)
          }
        }
        result: IndexedSeq[Project] <- lift[IndexedSeq[Project]](Future.successful {
          if (intermediate.filter(_.isLeft).nonEmpty) -\/(intermediate.filter(_.isLeft).head.swap.toOption.get)
          else \/-(intermediate.map(_.toOption.get))
        })
      } yield result

      projectList.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Find a single entry by ID.
     *
     * @param id the 128-bit UUID, as a byte array, to search for.
     * @return an optional Project if one was found
     */
    override def find(id: UUID): Future[\/[Fail, Project]] = {
      val projectQuery = db.pool.sendPreparedStatement(SelectOne, Array[Any](id.bytes)).map {
        result => buildProject(result.rows)
      }

      val result = for {
        project <- lift[Project](projectQuery)
        parts <- lift[IndexedSeq[Part]](partRepository.list(project))
      } yield project.copy(parts = parts)

      result.run.recover {
        case exception: NoSuchElementException => -\/(GenericFail("Invalid data returned from db."))
        case exception: Throwable => -\/(ExceptionalFail("An lunexpected error occurred.", exception))
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
        result => buildProject(result.rows)
      }

      val result = for {
        project <- lift[Project](projectQuery)
        parts <- lift[IndexedSeq[Part]](partRepository.list(project))
      } yield project.copy(parts = parts)

      result.run.recover {
        case exception: NoSuchElementException => -\/(GenericFail("Invalid data returned from db."))
        case exception: Throwable => -\/(ExceptionalFail("An lunexpected error occurred.", exception))
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
        result => buildProject(result.rows)
      }

      val result = for {
        project <- lift[Project](projectQuery)
        parts <- lift[IndexedSeq[Part]](partRepository.list(project))
      } yield project.copy(parts = parts)

      result.run.recover {
        case exception: NoSuchElementException => -\/(GenericFail("Invalid data returned from db."))
        case exception: Throwable => -\/(ExceptionalFail("An lunexpected error occurred.", exception))
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
        result => buildProject(result.rows)
      }.recover {
        case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
          case Some(nField) =>
            if (nField == "projects_pkey") -\/(EntityAlreadyExists(s"A project with key ${project.id.string} already exists"))
            else if (nField == "projects_slug_key") -\/(EntityUniqueFieldError(s"A project with slug ${project.slug} already exists"))
            else -\/(ExceptionalFail(s"Unknown db error", exception))
          case _ => -\/(ExceptionalFail("Unexpected exception", exception))
        }
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
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
        result => buildProject(result.rows)
      }.recover {
        case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
          case Some(nField) =>
            if (nField == "projects_pkey") -\/(EntityAlreadyExists(s"A project with key ${project.id.string} already exists"))
            else if (nField == "projects_slug_key") -\/(EntityUniqueFieldError(s"A project with slug ${project.slug} already exists"))
            else -\/(ExceptionalFail(s"Unknown db error", exception))
          case _ => -\/(ExceptionalFail("Unexpected exception", exception))
        }
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
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
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * Transform a result set into a Project.
     *
     * @param maybeResultSet
     * @return
     */
    private def buildProject(maybeResultSet: Option[ResultSet]): \/[Fail, Project] = {
      try {
        maybeResultSet match {
          case Some(resultSet) => resultSet.headOption match {
            case Some(firstRow) => \/-(Project(firstRow))
            case None => -\/(NoResults("The query was successful but ResultSet was empty."))
          }
          case None => -\/(NoResults("The query was successful but no ResultSet was returned."))
        }
      }
      catch {
        case exception: NoSuchElementException => -\/(ExceptionalFail(s"Invalid data: could not build a project from the row returned.", exception))
      }
    }

    /**
     * Transform a result set into a Project.
     *
     * @param maybeResultSet
     * @return
     */
    private def buildProjectList(maybeResultSet: Option[ResultSet]): \/[Fail, IndexedSeq[Project]] = {
      try {
        maybeResultSet match {
          case Some(resultSet) => \/-(resultSet.map(Project.apply))
          case None => -\/(NoResults("The query was successful but no ResultSet was returned."))
        }
      }
      catch {
        case exception: NoSuchElementException => -\/(ExceptionalFail(s"Invalid data: could not build a project from the row returned.", exception))
      }
    }
  }
}
