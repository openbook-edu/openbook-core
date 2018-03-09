package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.lib.{ ScalaCacheConfig }
import com.github.mauricio.async.db.{ Connection, RowData }
import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.models._
import java.util.UUID
import scala.concurrent.Future
import org.joda.time.DateTime
import scalaz.{ -\/, \/, \/- }

class PartRepositoryPostgres(val taskRepository: TaskRepository, val componentRepository: ComponentRepository, val scalaCacheConfig: ScalaCacheConfig)
    extends PartRepository with PostgresRepository[Part] with CacheRepository {

  override val entityName = "Part"

  override def constructor(row: RowData): Part = {
    Part(
      id = row("id").asInstanceOf[UUID],
      version = row("version").asInstanceOf[Long],
      projectId = row("project_id").asInstanceOf[UUID],
      name = row("name").asInstanceOf[String],
      position = row("position").asInstanceOf[Int],
      enabled = row("enabled").asInstanceOf[Boolean],
      createdAt = row("created_at").asInstanceOf[DateTime],
      updatedAt = row("updated_at").asInstanceOf[DateTime]
    )
  }

  val Table = "parts"
  val Fields = "id, version, created_at, updated_at, project_id, name, position, enabled"
  val FieldsWithTable = Fields.split(", ").map({ field => s"${Table}." + field }).mkString(", ")
  val QMarks = "?, ?, ?, ?, ?, ?, ?, ?"
  val OrderBy = "position ASC"

  // CRUD operations
  val SelectAll =
    s"""
       |SELECT $Fields
       |FROM $Table
       |ORDER BY $OrderBy
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
       |WITH deleted AS (DELETE FROM $Table
       |WHERE project_id = ?
       |RETURNING $Fields)
       |SELECT $Fields
       |FROM deleted
       |ORDER BY $OrderBy
     """.stripMargin

  val Delete =
    s"""
       |DELETE FROM $Table
       |WHERE id = ?
       |  AND version = ?
       |RETURNING $Fields
     """.stripMargin

  val SelectByProjectId =
    s"""
      |SELECT $Fields
      |FROM $Table
      |WHERE project_id = ?
      |ORDER BY $OrderBy
    """.stripMargin

  val FindByProjectPosition =
    s"""
      |SELECT $Fields
      |FROM $Table
      |WHERE project_id = ?
      |  AND position = ?
      |LIMIT 1
    """.stripMargin

  val SelectByComponentId =
    s"""
      |SELECT project_id, $FieldsWithTable
      |FROM $Table
      |INNER JOIN parts_components
      |  ON $Table.id = parts_components.part_id
      |WHERE parts_components.component_id = ?
      |ORDER BY $Table.$OrderBy
    """.stripMargin

  // TODO - not used
  //  val ReorderParts1 =
  //    s"""
  //      |UPDATE $Table AS p
  //      |SET position = c.position
  //      |FROM (VALUES
  //    """.stripMargin
  //
  //  val ReorderParts2 =
  //    s"""
  //      |) AS c(id, position)
  //      |WHERE c.id = p.id
  //    """.stripMargin

  /**
   * Find all parts.
   *
   * @return a vector of the returned Projects
   */
  override def list(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Part]]] = {
    (for {
      partList <- lift(queryList(SelectAll))
      partsWithTasks <- liftSeq(partList.map { part =>
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
  override def list(project: Project) // format: OFF
                   (implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Part]]] = // format: ON
    list(project, true)

  override def list(project: Project, fetchTasks: Boolean, fetchComponents: Boolean = false) // format: OFF
                   (implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Part]]] = { // format: ON
    (for {
      partList <- lift(cache[IndexedSeq[Part]].getCached(cachePartsKey(project.id)).flatMap {
        case \/-(partList) => { Future successful \/-(partList) }

        case -\/(noResults: RepositoryError.NoResults) =>
          for {
            partList <- lift(queryList(SelectByProjectId, Seq[Any](project.id)))
            _ <- lift(cache[IndexedSeq[Part]].putCache(cachePartsKey(project.id))(partList, ttl))
          } yield partList
        case -\/(error) => Future successful -\/(error)
      })
      partsWithTasks <- if (fetchTasks && fetchComponents) {
        liftSeq(partList.map { part =>
          (for {
            tasks <- lift(taskRepository.list(part))
            components <- lift(componentRepository.list(part))
            result = part.copy(tasks = tasks, components = components)
          } yield result).run
        })
      }
      else if (fetchTasks) {
        liftSeq(partList.map { part =>
          (for {
            tasks <- lift(taskRepository.list(part))
            result = part.copy(tasks = tasks)
          } yield result).run
        })
      }
      else if (fetchComponents) {
        liftSeq(partList.map { part =>
          (for {
            components <- lift(componentRepository.list(part))
            result = part.copy(components = components)
          } yield result).run
        })
      }
      else {
        lift(Future successful \/-(partList))
      }
    } yield partsWithTasks).run
  }

  /**
   * Find all Parts belonging to a given Component.
   */
  override def list(component: Component)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Part]]] = {
    (for {
      partList <- lift(queryList(SelectByComponentId, Seq[Any](component.id)))
      partsWithTasks <- liftSeq(partList.map { part =>
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
  override def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Part]] = find(id, true)
  override def find(id: UUID, fetchTasks: Boolean)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Part]] = {
    (for {
      part <- lift(cache[Part].getCached(cachePartKey(id)).flatMap {
        case \/-(part) => Future successful \/-(part)
        case -\/(noResults: RepositoryError.NoResults) =>
          for {
            part <- lift(queryOne(SelectOne, Seq[Any](id)))
            _ <- lift(cache[Part].putCache(cachePartsKey(part.id))(part, ttl))
          } yield part
        case -\/(error) => Future successful -\/(error)
      })
      taskList <- lift(if (fetchTasks) taskRepository.list(part) else Future successful \/-(IndexedSeq()))
    } yield part.copy(tasks = taskList)).run
  }

  /**
   * Find a single entry by its position within a project.
   *
   * @param project The project to search within.
   * @param position The part's position within the project.
   * @return an optional RowData object containing the results
   */
  override def find(project: Project, position: Int) // format: OFF
                   (implicit conn: Connection): Future[\/[RepositoryError.Fail, Part]] = // format: ON
    find(project, position, true)

  override def find(project: Project, position: Int, fetchTasks: Boolean) // format: OFF
                   (implicit conn: Connection): Future[\/[RepositoryError.Fail, Part]] = { // format: ON
    cache[UUID].getCached(cachePartPosKey(project.id, position)).flatMap {
      case \/-(partId) => find(partId)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          part <- lift(queryOne(FindByProjectPosition, Seq[Any](project.id, position)))
          taskList <- lift(if (fetchTasks) taskRepository.list(part) else Future successful \/-(IndexedSeq()))
          _ <- lift(cache[Part].putCache(cachePartsKey(part.id))(part, ttl))
          _ <- lift(cache[UUID].putCache(cachePartPosKey(project.id, part.position))(part.id, ttl))
        } yield part.copy(tasks = taskList)
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Save a Part row.
   *
   * @param part The part to be inserted
   * @return the new part
   */
  def insert(part: Part)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Part]] = {
    val params = Seq[Any](
      part.id, 1, new DateTime, new DateTime,
      part.projectId, part.name, part.position, part.enabled
    )

    for {
      inserted <- lift(queryOne(Insert, params))
      _ <- lift(cache.removeCached(cachePartsKey(part.projectId)))
    } yield inserted
  }

  /**
   * Update a part.
   *
   * @param part The part
   * @return id of the saved/new Part.
   */
  def update(part: Part)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Part]] = {
    val params = Seq[Any](
      part.projectId, part.name, part.position, part.enabled,
      part.version + 1, new DateTime, part.id, part.version
    )

    (for {
      updatedPart <- lift(queryOne(Update, params))
      _ <- lift(cache.removeCached(cachePartKey(part.id)))
      _ <- lift(cache.removeCached(cachePartsKey(part.projectId)))
      _ <- lift(cache.removeCached(cachePartPosKey(part.projectId, part.position)))
      oldTasks = part.tasks
    } yield updatedPart.copy(tasks = oldTasks)).run
  }

  /**
   * Delete a part.
   *
   * @param part The part to delete.
   * @return A boolean indicating whether the operation was successful.
   */
  def delete(part: Part)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Part]] = {
    (for {
      deletedPart <- lift(queryOne(Delete, Seq[Any](part.id, part.version)))
      _ <- lift(cache.removeCached(cachePartKey(part.id)))
      _ <- lift(cache.removeCached(cachePartsKey(part.projectId)))
      _ <- lift(cache.removeCached(cachePartPosKey(part.projectId, part.position)))
      oldTasks = part.tasks
    } yield deletedPart.copy(tasks = oldTasks)).run
  }

  /**
   * Delete all parts in a project.
   *
   * @param project Delete all parts belonging to this project
   * @return A boolean indicating whether the operation was successful.
   */
  def delete(project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Part]]] = {
    (for {
      deletedParts <- lift(queryList(DeleteByProject, Seq[Any](project.id)))
      _ <- liftSeq(deletedParts.map({ part => cache.removeCached(cachePartKey(part.id)) }))
      _ <- lift(cache.removeCached(cachePartsKey(project.id)))
      deletedPartsWithTasks <- liftSeq(deletedParts.map { part =>
        (for {
          tasks <- lift(taskRepository.list(part))
          result = part.copy(tasks = tasks)
        } yield result).run
      })
    } yield deletedPartsWithTasks).run
  }
}
