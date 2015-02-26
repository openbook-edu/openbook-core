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

import scalaz.{-\/, \/-, \/}

trait PartRepositoryPostgresComponent extends PartRepositoryComponent {
  self: ProjectRepositoryComponent with
        TaskRepositoryComponent with
        PostgresDB =>

  /**
   * Override with this trait's version of the ProjectRepository.
   */
  override val partRepository: PartRepository = new PartRepositoryPSQL

  /**
   * A concrete implementation of the ProjectRepository class.
   */
  private class PartRepositoryPSQL extends PartRepository with PostgresRepository[Part] {

    override def constructor(row: RowData): Part = {
      Part(
        id          = UUID(row("id").asInstanceOf[Array[Byte]]),
        version     = row("version").asInstanceOf[Long],
        projectId   = UUID(row("project_id").asInstanceOf[Array[Byte]]),
        name        = row("name").asInstanceOf[String],
        position    = row("position").asInstanceOf[Int],
        enabled     = row("enabled").asInstanceOf[Boolean],
        createdAt   = row("created_at").asInstanceOf[DateTime],
        updatedAt   = row("updated_at").asInstanceOf[DateTime]
      )
    }

    val Fields = "id, version, created_at, updated_at, project_id, name, position, enabled"
    val QMarks = "?, ?, ?, ?, ?, ?, ?, ?"
    val Table = "parts"

    // CRUD operations
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

    val Insert =
      s"""
         |INSERT INTO $Table ($Fields)
         |VALUES ($QMarks)
         |RETURNING $Fields
       """.stripMargin

    val Update =
      s"""
         |UPDATE $Table
         |SET project_id = ?, name = ?, position = ?, enabled = ?, version = ?, updated_at = ?
         |WHERE id = ?
         |  AND version = ?
         |RETURNING $Fields
       """.stripMargin

    val DeleteByProject =
      s"""
         |DELETE FROM $Table
         |WHERE project_id = ?
         |RETURNING $Fields
       """.stripMargin

    val Delete =
      s"""
         |DELETE FROM $Table
         |WHERE id = ?
         |  AND version = ?
         |RETURNING $Fields
       """.stripMargin

    val SelectByProjectId = s"""
      SELECT $Fields
      FROM $Table
      WHERE project_id = ?
      ORDER BY position ASC
    """

    val FindByProjectPosition = s"""
      SELECT $Fields
      FROM $Table
      WHERE project_id = ?
        AND position = ?
      LIMIT 1
    """

    val SelectByComponentId = s"""
      SELECT parts.id as id, parts.version as version, parts.created_at as created_at, parts.updated_at as updated_at,
             project_id, parts.name as name, parts.position as position, parts.enabled as enabled
      FROM $Table
      INNER JOIN parts_components ON parts.id = parts_components.part_id
      WHERE parts_components.component_id = ?
    """

    val ReorderParts1 = s"""
      UPDATE parts AS p
      SET position = c.position
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
    override def list(implicit conn: Connection): Future[\/[Fail, IndexedSeq[Part]]] = {
      (for {
        partList <- lift(queryList(SelectAll))
        partsWithTasks <- liftSeq(partList.map{ part =>
          (for {
            tasks <- lift(taskRepository.list(part))
            result = part.copy(tasks = tasks)
          } yield result).run
        })
      } yield partsWithTasks).run
    }

    /**
     * Find all Parts belonging to a given Project.
     *
     * @param project The project to return parts from.
     * @return a vector of the returned Projects
     */
    override def list(project: Project)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[Part]]] = {
      (for {
        partList <- lift(queryList(SelectByProjectId, Seq[Any](project.id.bytes)))
        partsWithTasks <- liftSeq(partList.map{ part =>
          (for {
            tasks <- lift(taskRepository.list(part))
            result = part.copy(tasks = tasks)
          } yield result).run
        })
      } yield partsWithTasks).run
    }

    /**
     * Find all Parts belonging to a given Component.
     */
    override def list(component: Component)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[Part]]] = {
      (for {
        partList <- lift(queryList(SelectByComponentId, Seq[Any](component.id.bytes)))
        partsWithTasks <- liftSeq(partList.map{ part =>
          (for {
            tasks <- lift(taskRepository.list(part))
            result = part.copy(tasks = tasks)
          } yield result).run
        })
      } yield partsWithTasks).run
    }

    /**
     * Find a single entry by ID.
     *
     * @param id the 128-bit UUID, as a byte array, to search for.
     * @return an optional Project if one was found
     */
    override def find(id: UUID)(implicit conn: Connection): Future[\/[Fail, Part]] = {
      (for {
        part <- lift(queryOne(SelectOne, Array[Any](id.bytes)))
        taskList <- lift(taskRepository.list(part))
      } yield part.copy(tasks = taskList)).run
    }

    /**
     * Find a single entry by its position within a project.
     *
     * @param project The project to search within.
     * @param position The part's position within the project.
     * @return an optional RowData object containing the results
     */
    override def find(project: Project, position: Int)(implicit conn: Connection): Future[\/[Fail, Part]] = {
      (for {
        part <- lift(queryOne(FindByProjectPosition, Seq[Any](project.id.bytes, position)))
        taskList <- lift(taskRepository.list(part))
      } yield part.copy(tasks = taskList)).run
    }

    /**
     * Save a Part row.
     *
     * @param part The part to be inserted
     * @return the new part
     */
    def insert(part: Part)(implicit conn: Connection): Future[\/[Fail, Part]] = {
      val params = Seq(
        part.id.bytes, 1, new DateTime, new DateTime,
        part.projectId.bytes, part.name, part.position, part.enabled
      )

      queryOne(Insert, params).recover {
        case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
          case Some(nField) =>
            if (nField == "parts_pkey") -\/(UniqueFieldConflict(s"A part with key ${part.id.string} already exists"))
            else if (nField == "parts_project_id_fkey") -\/(ReferenceConflict(s"Tried to create a part for a project that doesn't exist."))
            else throw exception
          case _ => throw exception
        }
        case exception: Throwable => throw exception
      }
    }

    /**
     * Update a part.
     *
     * @param part The part
     * @return id of the saved/new Part.
     */
    def update(part: Part)(implicit conn: Connection): Future[\/[Fail, Part]] = {
      val params = Seq(
        part.projectId.bytes, part.name, part.position, part.enabled,
        part.version + 1, new DateTime, part.id.bytes, part.version
      )

      queryOne(Update, params).recover {
        case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
          case Some(nField) =>
            if (nField == "parts_pkey") -\/(UniqueFieldConflict(s"A part with key ${part.id.string} already exists"))
            else if (nField == "parts_project_id_fkey") -\/(ReferenceConflict(s"Tried to move this part to a project that doesn't exist."))
            else throw exception
          case _ => throw exception
        }
        case exception: Throwable => throw exception
      }
    }

    /**
     * Delete a part.
     *
     * @param part The part to delete.
     * @return A boolean indicating whether the operation was successful.
     */
    def delete(part: Part)(implicit conn: Connection): Future[\/[Fail, Part]] = {
      queryOne(Delete, Seq(part.id.bytes, part.version))
    }

    /**
     * Delete all parts in a project.
     *
     * @param project Delete all parts belonging to this project
     * @return A boolean indicating whether the operation was successful.
     */
    def delete(project: Project)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[Part]]] = {
      (for {
        partList <- lift(list(project))
        deletedParts <- lift(queryList(DeleteByProject, Array(project.id.bytes)))
      } yield deletedParts).run
    }
  }
}
