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

trait PartRepositoryPostgresComponent extends PartRepositoryComponent {
  self: TaskRepositoryComponent with
    PostgresDB =>

  /**
   * Override with this trait's version of the ProjectRepository.
   */
  override val partRepository: PartRepository = new PartRepositoryPSQL

  /**
   * A concrete implementation of the ProjectRepository class.
   */
  private class PartRepositoryPSQL extends PartRepository {
    def fields = Seq("project_id", "name", "position", "enabled")
    def table = "parts"

    val fieldsText = fields.mkString(", ")
    val questions = fields.map(_ => "?").mkString(", ")

    // CRUD operations
    val SelectAll = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM $table
    """

    val SelectOne = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE id = ?
    """

    val Insert = {
      val extraFields = fields.mkString(",")
      val questions = fields.map(_ => "?").mkString(",")
      s"""
        INSERT INTO $table (id, version, created_at, updated_at, $extraFields)
        VALUES (?, 1, ?, ?, $questions)
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
        RETURNING id, version, created_at, updated_at, $fieldsText
      """
    }

    val Delete = s"""
      DELETE FROM $table WHERE id = ? AND version = ?
    """

    val DeleteByProject = s"""
      DELETE FROM $table WHERE project_id = ?
    """

    val Restore = s"""
      UPDATE $table SET status = 1 WHERE id = ? AND version = ? AND status = 0
    """

    val Purge = s"""
      DELETE FROM $table WHERE id = ? AND version = ?
    """

    val SelectByProjectId = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE project_id = ?
      ORDER BY position ASC
    """

    val FindByProjectPosition = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE project_id = ?
        AND position = ?
      LIMIT 1
    """

    //TODO - remove
//    val SelectByProjectSlug = s"""
//      SELECT parts.id as id, parts.version as version, parts.created_at as created_at, parts.updated_at as updated_at,
//             project_id, parts.name as name, parts.position as position, parts.enabled as enabled
//      FROM $table, projects
//      WHERE parts.project_id = projects.id
//        AND projects.slug = ?
//    """

    val SelectByComponentId = s"""
      SELECT parts.id as id, parts.version as version, parts.created_at as created_at, parts.updated_at as updated_at,
             project_id, parts.name as name, parts.position as position, parts.enabled as enabled
      FROM $table
      INNER JOIN parts_components ON parts.id = parts_components.part_id
      WHERE parts_components.component_id = ?
    """

    // TODO remove
//    val SelectEnabledForUserAndProjectId = s"""
//      SELECT parts.id as id, parts.version as version, parts.created_at as created_at, parts.updated_at as updated_at,
//             parts.project_id, parts.name as name, parts.position as position, parts.enabled as enabled,
//             classes.name as section_name, users.username as username
//      FROM parts
//      INNER JOIN projects ON projects.id = parts.project_id
//      INNER JOIN classes ON classes.id = projects.class_id
//      INNER JOIN users_classes ON classes.id = users_classes.class_id
//      INNER JOIN users ON users.id = users_classes.user_id
//      WHERE parts.enabled = TRUE
//        AND projects.id = ?
//        AND users.id = ?
//    """
//    val SelectEnabledForUserAndProjectId = s"""
//      SELECT parts.id as id, parts.version as version, parts.created_at as created_at, parts.updated_at as updated_at,
//             parts.project_id, parts.name as name, parts.position as position, parts.enabled as enabled,
//             classes.name as section_name, users.username as username
//      FROM parts, projects, classes, classes_projects, users_classes, users, scheduled_classes_parts
//      WHERE projects.id = ?
//        AND users.id = ?
//        AND parts.project_id = projects.id
//        AND classes_projects.project_id = projects.id
//        AND classes_projects.class_id = classes.id
//        AND users_classes.class_id = classes_projects.class_id
//        AND users_classes.user_id = users.id
//        AND scheduled_classes_parts.class_id = classes.id
//        AND scheduled_classes_parts.part_id = parts.id
//        AND scheduled_classes_parts.active = TRUE
//    """

    // TODO - remove
//    val SelectEnabledForSectionAndProjectId = s"""
//      SELECT parts.id as id, parts.version as version, parts.created_at as created_at, parts.updated_at as updated_at,
//             parts.project_id, parts.name as name, parts.position as position, parts.enabled as enabled,
//             classes.name as section_name
//      FROM parts
//      INNER JOIN projects ON parts.project_id = projects.id
//      INNER JOIN classes ON projects.class_id = classes.id
//      WHERE parts.enabled = TRUE
//        AND projects.id = ?
//        AND classes.id = ?
//    """
//    val SelectEnabledForSectionAndProjectId = s"""
//      SELECT parts.id as id, parts.version as version, parts.created_at as created_at, parts.updated_at as updated_at,
//             parts.project_id, parts.name as name, parts.position as position, parts.enabled as enabled,
//             classes.name as section_name
//      FROM parts, projects, classes, classes_projects, scheduled_classes_parts
//      WHERE projects.id = ?
//        AND classes.id = ?
//        AND parts.project_id = projects.id
//        AND classes_projects.project_id = projects.id
//        AND classes_projects.class_id = classes.id
//        AND scheduled_classes_parts.class_id = classes.id
//        AND scheduled_classes_parts.part_id = parts.id
//        AND scheduled_classes_parts.active = TRUE
//    """

    // TODO remove
//    val IsPartEnabledForUser = s"""
//      SELECT scheduled_classes_parts.active
//      FROM parts, users_classes, scheduled_classes_parts
//      WHERE parts.id = ?
//        AND users_classes.user_id = ?
//        AND users_classes.class_id = scheduled_classes_parts.class_id
//        AND scheduled_classes_parts.part_id = parts.id
//    """

    //TODO remove
//    val IsPartEnabledForSection = s"""
//      SELECT scheduled_classes_parts.active
//      FROM scheduled_classes_parts
//      WHERE scheduled_classes_parts.part_id = ?
//        AND scheduled_classes_parts.class_id = ?
//    """

    val ReorderParts1 = s"""
      UPDATE parts AS p SET
        position = c.position
      FROM (VALUES
    """

    val ReorderParts2 = s"""
      ) AS c(id, position)
      WHERE c.id = p.id
    """

    /**
     * Find all parts.
     *
     * @return a vector of the returned Projects
     */
    override def list: Future[IndexedSeq[Part]] = {
      val partList = for {
        queryResult <- db.pool.sendQuery(SelectAll)
        parts <- Future successful {
          queryResult.rows.get.map { item => Part(item) }
        }
        result <- Future sequence { parts.map { part =>
          taskRepository.list(part).map { taskList =>
            part.copy(tasks = taskList)
          }
        }}
      } yield result

      partList.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Find all Parts belonging to a given Project.
     *
     * @param project The project to return parts from.
     * @return a vector of the returned Projects
     */
    override def list(project: Project): Future[IndexedSeq[Part]] = {
      val partList = for {
        queryResult <- db.pool.sendPreparedStatement(SelectByProjectId, Array[Any](project.id.bytes))
        parts <- Future successful {
          queryResult.rows.get.map { item => Part(item) }
        }
        result <- Future sequence { parts.map { part =>
          taskRepository.list(part).map { taskList =>
            part.copy(tasks = taskList)
          }
        }}
      } yield result

      partList.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Find all Parts belonging to a given Component.
     */
    override def list(component: Component): Future[IndexedSeq[Part]] = {
      val partList = for {
        queryResult <- db.pool.sendPreparedStatement(SelectByComponentId, Array[Any](component.id.bytes))
        parts <- Future successful {
          queryResult.rows.get.map { item => Part(item) }
        }
        result <- Future sequence { parts.map { part =>
          taskRepository.list(part).map { taskList =>
            part.copy(tasks = taskList)
          }
        }}
      } yield result

      partList.recover {
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
    override def find(id: UUID): Future[Option[Part]] = {
      val part = for {
        queryResult <- db.pool.sendPreparedStatement(SelectOne, Array[Any](id.bytes))
        partOption <- Future successful {
          queryResult.rows.get.headOption match {
            case Some(rowData) => Some(Part(rowData))
            case None => None
          }
        }
        tasks <- { partOption match {
          case Some(part) => taskRepository.list(part)
          case None => Future.successful(IndexedSeq())
        }}
      } yield partOption match {
        case Some(part) => Some(part.copy(tasks = tasks))
        case None => None
      }

      part.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Find a single entry by its position within a project.
     *
     * @param project The project to search within.
     * @param position The part's position within the project.
     * @param conn An implicit connection object. Can be used in a transactional chain.
     * @return an optional RowData object containing the results
     */
    override def find(project: Project, position: Int): Future[Option[Part]] = {
      val part = for {
        queryResult <- db.pool.sendPreparedStatement(FindByProjectPosition, Array[Any](project.id.bytes, position))
        partOption <- Future successful {
          queryResult.rows.get.headOption match {
            case Some(rowData) => Some(Part(rowData))
            case None => None
          }
        }
        tasks <- { partOption match {
          case Some(part) => taskRepository.list(part)
          case None => Future.successful(IndexedSeq())
        }}
      } yield partOption match {
          case Some(part) => Some(part.copy(tasks = tasks))
          case None => None
        }

      part.recover {
        case exception => {
          throw exception
        }
      }
    }

//    /**
//     * TODO remove
//     * List enabled parts of a project for a specific class.
//     *
//     * @param project the [[Project]] to list parts from
//     * @param section the [[Class]] to select enabled parts for
//     * @return an vector of the enabled parts
//     */
//    def listEnabled(project: Project, section: Class): Future[IndexedSeq[Part]] = {
//      val partList = for {
//        queryResult <- db.pool.sendPreparedStatement(SelectEnabledForSectionAndProjectId, Seq[Any](project.id.bytes, section.id.bytes))
//        parts <- Future successful {
//          queryResult.rows.get.map { item => Part(item) }
//        }
//        result <- Future sequence { parts.map { part =>
//          taskRepository.list(part).map { taskList =>
//            part.copy(tasks = taskList)
//          }
//        }}
//      } yield result
//
//      partList.recover {
//        case exception => {
//          throw exception
//        }
//      }
//    }

//    /**
//     * TODO  remove
//     * List enabled parts of a project for a user. Will check for parts
//     * enabled in *any* of that user's classes.
//     *
//     * @param project the [[Project]] to list parts from
//     * @param user the [[User]] to select enabled parts for
//     * @return an vector of the enabled parts
//     */
//    def listEnabled(project: Project, user: User): Future[IndexedSeq[Part]] = {
//      val partList = for {
//        queryResult <- db.pool.sendPreparedStatement(SelectEnabledForUserAndProjectId, Seq[Any](project.id.bytes, user.id.bytes))
//        parts <- Future successful {
//          queryResult.rows.get.map { item => Part(item) }
//        }
//        result <- Future sequence { parts.map { part =>
//          taskRepository.list(part).map { taskList =>
//            part.copy(tasks = taskList)
//          }
//        }}
//      } yield result
//
//      partList.recover {
//        case exception => {
//          throw exception
//        }
//      }
//    }

//    /**
//     * TODO remove
//     * Returns a boolean indicating whether a part is active for a given user.
//     *
//     * @param part
//     */
//    def isEnabled(part: Part, user: User): Future[Boolean] = {
//      val isEnabled = for {
//        result <- db.pool.sendPreparedStatement(IsPartEnabledForUser, Array(part.id.bytes, user.id.bytes))
//      }
//      yield result.rows.headOption match {
//          case Some(resultSet) => resultSet.headOption match {
//            case Some(row) => {
//              val result = row("active").asInstanceOf[Boolean]
//              result
//            }
//            case None => false
//          }
//          case None => false
//        }
//      isEnabled.recover {
//        case exception => {
//          throw exception
//        }
//      }
//    }

//    /**
//     * TODO remove
//     * Returns a boolean indicating whether a part is active for a given section.
//     */
//    def isEnabled(part: Part, section: Class): Future[Boolean] = {
//      val isEnabled = for {
//        result <- db.pool.sendPreparedStatement(IsPartEnabledForSection, Array(part.id.bytes, section.id.bytes))
//      }
//      yield result.rows.headOption match {
//          case Some(resultSet) => resultSet.headOption match {
//            case Some(row) => {
//              val result = row("active").asInstanceOf[Boolean]
//              result
//            }
//            case None => false
//          }
//          case None => false
//        }
//      isEnabled.recover {
//        case exception => {
//          throw exception
//        }
//      }
//    }

    /**
     * Save a Part row.
     *
     * @param part The part to be inserted
     * @return the new part
     */
    def insert(part: Part)(implicit conn: Connection): Future[Part] = {
      conn.sendPreparedStatement(Insert, Array(
        part.id.bytes,
        new DateTime,
        new DateTime,
        part.projectId.bytes,
        part.name,
        //        part.description,
        part.position,
        part.enabled
      )).map {
        result => {
          Part(result.rows.get.head)
        }
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Update a part.
     *
     * @param part The part
     * @return id of the saved/new Part.
     */
    def update(part: Part)(implicit conn: Connection): Future[Part] = {
      conn.sendPreparedStatement(Update, Array(
        part.projectId.bytes,
        part.name,
        //        part.description,
        part.position,
        part.enabled,
        (part.version + 1),
        new DateTime,
        part.id.bytes,
        part.version
      )).map {
        result => {
          Part(result.rows.get.head)
        }
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Delete a part.
     *
     * @param part The part to delete.
     * @return A boolean indicating whether the operation was successful.
     */
    def delete(part: Part)(implicit conn: Connection): Future[Boolean] = {
      conn.sendPreparedStatement(Purge, Array(part.id.bytes, part.version)).map {
        result => {
          (result.rowsAffected > 0)
        }
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Delete parts in a project.
     *
     * @param part The part to delete.
     * @return A boolean indicating whether the operation was successful.
     */
    def delete(project: Project)(implicit conn: Connection): Future[Boolean] = {
      list(project).flatMap { partList =>
        conn.sendPreparedStatement(DeleteByProject, Array(project.id.bytes)).map {
          result => {
            (result.rowsAffected > 0)
          }
        }.recover {
          case exception => {
            throw exception
          }
        }
      }
    }

    /**
     * Re-order parts.
     */
    override def reorder(project: Project, parts: IndexedSeq[Part])(implicit conn: Connection): Future[IndexedSeq[Part]] = {
      val query = parts.map { part =>
        s"(decode('${part.id.cleanString}', 'hex'), ${part.position})"
      }.mkString(ReorderParts1, ",", ReorderParts2)

      conn.sendQuery(query).flatMap {
        queryResult => {
          list(project)
        }
      }.recover {
        case exception => {
          throw exception
        }
      }
    }
  }
}
