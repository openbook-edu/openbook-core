package ca.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.{RowData, Connection}
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib.{UUID, ExceptionWriter}
import ca.shiftfocus.krispii.core.models._
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
    def fields = Seq("name", "slug", "description")
    def table = "projects"
    def orderBy = "name ASC"
    val fieldsText = fields.mkString(", ")
    val questions = fields.map(_ => "?").mkString(", ")

    // User CRUD operations
    val SelectAll = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE status = 1
      ORDER BY $orderBy
    """

    val SelectOne = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE id = ?
        AND status = 1
    """

    val Insert = {
      val extraFields = fields.mkString(",")
      val questions = fields.map(_ => "?").mkString(",")
      s"""
        INSERT INTO $table (id, version, status, created_at, updated_at, $extraFields)
        VALUES (?, 1, 1, ?, ?, $questions)
        RETURNING id, version, created_at, updated_at, $fieldsText
      """
    }

    val Update = {
      val extraFields = fields.map(" " + _ + " = ? ").mkString(",")
      s"""
        UPDATE $table
        SET $extraFields , version = ?, updated_at = ?
        WHERE id = ?
          AND version = ?
          AND status = 1
        RETURNING id, version, created_at, updated_at, $fieldsText
      """
    }

    val Delete = s"""
      UPDATE $table SET status = 0 WHERE id = ? AND version = ?
    """

    val Restore = s"""
      UPDATE $table SET status = 1 WHERE id = ? AND version = ? AND status = 0
    """

    val Purge = s"""
      DELETE FROM $table WHERE id = ? AND version = ?
    """

    val SelectOneBySlug = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE slug = ?
        AND status = 1
    """

    val SelectIdBySlug = s"""
      SELECT id
      FROM $table
      WHERE slug = ?
        AND status = 1
    """

    val AddProject = """
      INSERT INTO sections_projects (section_id, project_id, created_at)
      VALUES (?, ?, ?)
    """

    val RemoveProject = """
      DELETE FROM sections_projects
      WHERE section_id = ?
        AND project_id = ?
    """

    val RemoveProjectFromAll = """
      DELETE FROM sections_projects
      WHERE project_id = ?
    """

    val ListBySection = """
      SELECT id, version, projects.name as name, projects.slug as slug, description, projects.created_at as created_at, projects.updated_at as updated_at
      FROM projects, sections_projects
      WHERE projects.id = sections_projects.project_id
        AND sections_projects.section_id = ?
        AND projects.status = 1
    """

    /**
     * Find all Projects.
     *
     * @param conn An implicit connection object. Can be used in a transactional chain.
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
     * @param conn An implicit connection object. Can be used in a transactional chain.
     * @return a vector of the returned Projects
     */
    override def list(section: Section): Future[IndexedSeq[Project]] = {
      db.pool.sendPreparedStatement(ListBySection, Array[Any](section.id.bytes)).map { queryResult =>
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
     * @param conn An implicit connection object. Can be used in a transactional chain.
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
     * @param conn An implicit connection object. Can be used in a transactional chain.
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
        queryResult <- conn.sendPreparedStatement(Purge, Array(project.id.bytes, project.version))
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

    /**
     * Associate a role to a user.
     *
     * @param section The section to add to
     * @param project The project to add to the section
     * @param conn An implicit connection object. Can be used in a transactional chain.
     * @return a boolean indicator whether the operation was successful.
     */
    override def addToSection(section: Section, project: Project)(implicit conn: Connection): Future[Boolean] = {
      conn.sendPreparedStatement(AddProject, Array(section.id.bytes, project.id.bytes, new DateTime)).map {
        result => (result.rowsAffected > 0)
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Remove a project from a section.
     *
     * @param section The section to remove from
     * @param project The project to remove
     * @param conn An implicit connection object. Can be used in a transactional chain.
     * @return a boolean indicator whether the operation was successful.
     */
    override def removeFromSection(section: Section, project: Project)(implicit conn: Connection): Future[Boolean] = {
      conn.sendPreparedStatement(RemoveProject, Array(section.id.bytes, project.id.bytes)).map {
        result => (result.rowsAffected > 0)
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Remove a project from a section.
     *
     * @param project The project to remove.
     * @param conn An implicit connection object. Can be used in a transactional chain.
     * @return a boolean indicator whether the operation was successful.
     */
    override def removeFromAllSections(project: Project)(implicit conn: Connection): Future[Boolean] = {
      conn.sendPreparedStatement(RemoveProjectFromAll, Array(project.id.bytes)).map {
        result => (result.rowsAffected > 0)
      }.recover {
        case exception => {
          throw exception
        }
      }
    }
  }
}
