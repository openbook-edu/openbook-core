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

trait PartRepositoryPostgresComponent extends PartRepositoryComponent with PostgresRepository {
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

    // TODO - not used
//    val Delete = s"""
//      DELETE FROM $table WHERE id = ? AND version = ?
//    """

    val DeleteByProject = s"""
      DELETE FROM $table WHERE project_id = ?
    """

    // TODO - not used
//    val Restore = s"""
//      UPDATE $table SET status = 1 WHERE id = ? AND version = ? AND status = 0
//    """

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

    val SelectByComponentId = s"""
      SELECT parts.id as id, parts.version as version, parts.created_at as created_at, parts.updated_at as updated_at,
             project_id, parts.name as name, parts.position as position, parts.enabled as enabled
      FROM $table
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
    override def list: Future[\/[Fail, IndexedSeq[Part]]] = {
      val fPartList = db.pool.sendQuery(SelectAll).map(res => buildEntityList(res.rows, Part.apply))
      val fResult = for {
        partList <- lift(fPartList)
        partsWithTasks <- liftSeq(partList.map{ part =>
          (for {
            tasks <- lift(taskRepository.list(part))
            result = part.copy(tasks = tasks)
          } yield result).run
        })
      } yield partsWithTasks

      fResult.run.recover {
        case exception: Throwable => throw exception
      }
    }

    /**
     * Find all Parts belonging to a given Project.
     *
     * @param project The project to return parts from.
     * @return a vector of the returned Projects
     */
    override def list(project: Project): Future[\/[Fail, IndexedSeq[Part]]] = {
      val fPartList = db.pool.sendPreparedStatement(SelectByProjectId, Array[Any](project.id.bytes)).map(res => buildEntityList(res.rows, Part.apply))
      val fResult = for {
        partList <- lift(fPartList)
        partsWithTasks <- liftSeq(partList.map{ part =>
          (for {
            tasks <- lift(taskRepository.list(part))
            result = part.copy(tasks = tasks)
          } yield result).run
        })
      } yield partsWithTasks

      fResult.run.recover {
        case exception: Throwable => throw exception
      }
    }

    /**
     * Find all Parts belonging to a given Component.
     */
    override def list(component: Component): Future[\/[Fail, IndexedSeq[Part]]] = {
      val fPartList = db.pool.sendPreparedStatement(SelectByComponentId, Array[Any](component.id.bytes)).map(res => buildEntityList(res.rows, Part.apply))
      val fResult = for {
        partList <- lift(fPartList)
        partsWithTasks <- liftSeq(partList.map{ part =>
          (for {
            tasks <- lift(taskRepository.list(part))
            result = part.copy(tasks = tasks)
          } yield result).run
        })
      } yield partsWithTasks

      fResult.run.recover {
        case exception: Throwable => throw exception
      }
    }

    /**
     * Find a single entry by ID.
     *
     * @param id the 128-bit UUID, as a byte array, to search for.
     * @return an optional Project if one was found
     */
    override def find(id: UUID): Future[\/[Fail, Part]] = {
      val partQuery = db.pool.sendPreparedStatement(SelectOne, Array[Any](id.bytes)).map {
        result => buildEntity(result.rows, Part.apply)
      }

      val result = for {
        part <- lift(partQuery)
        taskList <- lift(taskRepository.list(part))
      } yield part.copy(tasks = taskList)

      result.run.recover {
        case exception: NoSuchElementException => -\/(GenericFail("Invalid data returned from db."))
        case exception: Throwable => throw exception
      }
    }

    /**
     * Find a single entry by its position within a project.
     *
     * @param project The project to search within.
     * @param position The part's position within the project.
     * @return an optional RowData object containing the results
     */
    override def find(project: Project, position: Int): Future[\/[Fail, Part]] = {
      val partQuery = db.pool.sendPreparedStatement(FindByProjectPosition, Array[Any](project.id.bytes, position)).map {
        result => buildEntity(result.rows, Part.apply)
      }

      val result = for {
        part <- lift(partQuery)
        taskList <- lift(taskRepository.list(part))
      } yield part.copy(tasks = taskList)

      result.run.recover {
        case exception: NoSuchElementException => -\/(GenericFail("Invalid data returned from db."))
        case exception: Throwable => throw exception
      }
    }

    /**
     * Save a Part row.
     *
     * @param part The part to be inserted
     * @return the new part
     */
    def insert(part: Part)(implicit conn: Connection): Future[\/[Fail, Part]] = {
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
        result => buildEntity(result.rows, Part.apply)
      }.recover {
        case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
          case Some(nField) =>
            if (nField == "parts_pkey") -\/(UniqueFieldConflict(s"A part with key ${part.id.string} already exists"))
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
      conn.sendPreparedStatement(Update, Array(
        part.projectId.bytes,
        part.name,
      // TODO remove description
        //        part.description,
        part.position,
        part.enabled,
        part.version + 1,
        new DateTime,
        part.id.bytes,
        part.version
      )).map {
        result => buildEntity(result.rows, Part.apply)
      }.recover {
        case exception: GenericDatabaseException => exception.errorMessage.fields.get('n') match {
          case Some(nField) =>
            if (nField == "parts_pkey") -\/(UniqueFieldConflict(s"A part with key ${part.id.string} already exists"))
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
      conn.sendPreparedStatement(Purge, Array(part.id.bytes, part.version)).map {
        result =>
          if (result.rowsAffected == 1) \/-(part)
          else -\/(GenericFail("Query ran without errors but a part was not deleted."))
      }.recover {
        case exception: Throwable => throw exception
      }
    }

    /**
     * Delete all parts in a project.
     *
     * @param project Delete all parts belonging to this project
     * @return A boolean indicating whether the operation was successful.
     */
    def delete(project: Project)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[Part]]] = {
      val result = for {
        partList <- lift(list(project))
        deletedParts <- lift(conn.sendPreparedStatement(DeleteByProject, Array(project.id.bytes)).map {
          result =>
            if (result.rowsAffected == 1) \/-(partList)
            else -\/(GenericFail("Query ran without errors but a part was not deleted."))
        })
      } yield deletedParts

      result.run.recover {
        case exception: Throwable => throw exception
      }
    }

    // TODO - what is difference between this method and update?
    /**
     * Re-order parts.
     */
    override def reorder(project: Project, parts: IndexedSeq[Part])(implicit conn: Connection): Future[\/[Fail, IndexedSeq[Part]]] = {
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
