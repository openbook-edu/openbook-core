package ca.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.{RowData, Connection}
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib.ExceptionWriter
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import play.api.Play.current

import play.api.Logger
import scala.concurrent.Future
import org.joda.time.DateTime
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB

trait ProjectRepositoryPostgresComponent extends ProjectRepositoryComponent {
  self: PostgresDB =>

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
         |SELECT id, version, class_id, name, slug, description, availability, created_at, updated_at
       """.stripMargin

    val From =
      """
        |FROM projects
      """.stripMargin

    val Returning =
      """
        |RETURNING id, version, class_id, name, slug, description, availability, created_at, updated_at
      """.stripMargin


    // User CRUD operations
    val SelectAll =
      s"""
         |$Select
         |$From
      """

    val SelectOne =
      s"""
         |$Select
         |$From
         |WHERE id = ?
      """

    val SelectOneBySlug =
      s"""
         |$Select
         |$From
         |WHERE slug = ?
       """.stripMargin

    val SelectIdBySlug =
      s"""
         |SELECT id
         |$From
         |WHERE slug = ?
       """.stripMargin

    val ListByClass =
      s"""
         |$Select
         |$From
         |WHERE class_id = ?
       """.stripMargin


    val Insert =
      s"""
        |INSERT INTO projects (id, version, class_id, name, slug, description, availability, created_at, updated_at)
        |VALUES (?, 1, ?, ?, ?, ?, ?, ?, ?)
        |$Returning
      """.stripMargin

    val Update =
      s"""
        |UPDATE projects
        |SET class_id = ?, name = ?, slug = ?, description = ?, availability = ?, version = ?, updated_at = ?
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
    override def list: Future[IndexedSeq[Project]] = {
      db.pool.sendQuery(SelectAll).map { queryResult =>
        val projectList = queryResult.rows.get.map {
          item: RowData => Project(item)
        }
        projectList
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Find all Projects belonging to a given section.
     *
     * @param section The section to return projects from.
     * @return a vector of the returned Projects
     */
    override def list(`class`: Class): Future[IndexedSeq[Project]] = {
      db.pool.sendPreparedStatement(ListByClass, Array[Any](`class`.id.bytes)).map { queryResult =>
        queryResult.rows.get.map {
          item: RowData => Project(item)
        }
      }.recover {
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
    override def find(id: UUID): Future[Option[Project]] = {
      db.pool.sendPreparedStatement(SelectOne, Array[Any](id.bytes)).map { result =>
        result.rows.get.headOption match {
          case Some(rowData) => Some(Project(rowData))
          case None => None
        }
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Find a single entry by ID.
     *
     * @param slug The project slug to search by.
     * @return an optional RowData object containing the results
     */
    def find(slug: String): Future[Option[Project]] = {
      db.pool.sendPreparedStatement(SelectOneBySlug, Array[Any](slug)).map { result =>
        result.rows.get.headOption match {
          case Some(rowData) => Some(Project(rowData))
          case None => None
        }
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Save a Project row.
     *
     * @param project The new project to save.
     * @param conn An implicit connection object. Can be used in a transactional chain.
     * @return the new project
     */
    override def insert(project: Project)(implicit conn: Connection): Future[Project] = {
      conn.sendPreparedStatement(Insert, Array(
        project.id.bytes,
        new DateTime,
        new DateTime,
        project.name,
        project.slug,
        project.description
      )).map {
        result => {
          Project(result.rows.get.head)
        }
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Save a Project row.
     *
     * @param project The project to update.
     * @param conn An implicit connection object. Can be used in a transactional chain.
     * @return the updated project.
     */
    override def update(project: Project)(implicit conn: Connection): Future[Project] = {
      conn.sendPreparedStatement(Update, Array(
        project.name,
        project.slug,
        project.description,
        (project.version + 1),
        new DateTime,
        project.id.bytes,
        project.version
      )).map {
        result => {
          Project(result.rows.get.head)
        }
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Delete a single project.
     *
     * @param project The project to be deleted.
     * @param conn An implicit connection object. Can be used in a transactional chain.
     * @return a boolean indicator whether the deletion was successful.
     */
    override def delete(project: Project)(implicit conn: Connection): Future[Boolean] = {
      val future = for {
        queryResult <- conn.sendPreparedStatement(Delete, Array(project.id.bytes, project.version))
      }
      yield {
        queryResult.rowsAffected > 0
      }

      future.recover {
        case exception => {
          throw exception
        }
      }
    }
  }
}
