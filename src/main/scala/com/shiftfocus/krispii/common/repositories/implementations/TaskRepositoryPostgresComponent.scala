package com.shiftfocus.krispii.common.repositories

import com.github.mauricio.async.db.{RowData, Connection}
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import com.shiftfocus.krispii.lib.{UUID, ExceptionWriter}
import com.shiftfocus.krispii.common.models._
import play.api.Play.current
import play.api.cache.Cache
import play.api.Logger
import scala.concurrent.Future
import org.joda.time.DateTime
import com.shiftfocus.krispii.common.services.datasource.PostgresDB

trait TaskRepositoryPostgresComponent extends TaskRepositoryComponent {
  self: PartRepositoryComponent with
        PostgresDB =>

  /**
   * Override with this trait's version of the ProjectRepository.
   */
  override val taskRepository: TaskRepository = new TaskRepositoryPSQL

  /**
   * A concrete implementation of the ProjectRepository class.
   */
  private class TaskRepositoryPSQL extends TaskRepository {
    def fields = Seq("part_id", "dependency_id", "name", "description", "position", "notes_allowed")
    def table = "tasks"
    def orderBy = "position ASC"
    val fieldsText = fields.mkString(", ")
    val questions = fields.map(_ => "?").mkString(", ")

    // User CRUD operations
    val SelectAll = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE status = 1
      ORDER BY position ASC
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

    val DeleteByPart = s"""
      DELETE FROM $table WHERE part_id = ?
    """

    val Purge = s"""
      DELETE FROM $table WHERE id = ? AND version = ?
    """

    val SelectByPartId = s"""
      SELECT id, version, created_at, updated_at, $fieldsText
      FROM $table
      WHERE part_id = ?
        AND status = 1
    """

    val SelectByProjectIdPartNum = s"""
      SELECT tasks.id as id, tasks.version as version, tasks.created_at as created_at,
             tasks.updated_at as updated_at, tasks.part_id, tasks.dependency_id, tasks.name, tasks.description, tasks.position,
             tasks.notes_allowed
      FROM tasks, parts, projects
      WHERE projects.id = ?
        AND projects.id = parts.project_id
        AND parts.position = ?
        AND parts.id = tasks.part_id
        AND projects.status = 1 AND parts.status = 1 AND tasks.status = 1
      ORDER BY parts.position ASC, tasks.position ASC
    """

    val SelectByProjectId = s"""
      SELECT tasks.id as id, tasks.version as version, tasks.created_at as created_at,
             tasks.updated_at as updated_at, tasks.part_id, tasks.dependency_id, tasks.name, tasks.description, tasks.position,
             tasks.notes_allowed
      FROM tasks, parts, projects
      WHERE projects.id = ?
        AND projects.id = parts.project_id
        AND parts.id = tasks.part_id
        AND projects.status = 1 AND parts.status = 1 AND tasks.status = 1
      ORDER BY parts.position ASC, tasks.position ASC
    """

    val SelectActiveByProjectId = s"""
      SELECT tasks.id as id, tasks.version as version, tasks.created_at as created_at,
             tasks.updated_at as updated_at, tasks.part_id, tasks.dependency_id, tasks.name, tasks.description, tasks.position,
             tasks.notes_allowed
      FROM tasks, parts, projects, sections, sections_projects, users_sections
      WHERE projects.id = ?
        AND projects.id = parts.project_id
        AND parts.id = tasks.part_id
        AND projects.status = 1 AND parts.status = 1 AND tasks.status = 1
        AND users_sections.user_id = ?
        AND users_sections.section_id = sections_projects.section_id
        AND sections_projects.project_id = projects.id
      ORDER BY parts.position ASC, tasks.position ASC
    """

    val SelectByPosition = s"""
      SELECT tasks.id as id, tasks.version as version, tasks.created_at as created_at, tasks.updated_at as updated_at, tasks.part_id as part_id, tasks.dependency_id as dependency_id, tasks.name as name, tasks.description as description, tasks.position as position,
             tasks.notes_allowed
      FROM tasks, parts, projects
      WHERE projects.id = ?
        AND parts.position = ?
        AND tasks.position = ?
        AND projects.id = parts.project_id
        AND parts.id = tasks.part_id
        AND tasks.status = 1 AND parts.status = 1 AND projects.status = 1
    """

    val SelectNowByUserId = s"""
      SELECT tasks.id as id, tasks.version as version, tasks.created_at as created_at,
             tasks.updated_at as updated_at, tasks.part_id as part_id,
             tasks.dependency_id as dependency_id, tasks.name as name, tasks.description as description,
             COALESCE(sr.is_complete, FALSE) AS is_complete,
             tasks.position as position, tasks.notes_allowed
      FROM tasks
      INNER JOIN parts ON parts.id = tasks.part_id
      INNER JOIN projects ON parts.project_id = projects.id
      INNER JOIN users ON users.id = ?
      INNER JOIN users_sections ON users.id = users_sections.user_id
      INNER JOIN scheduled_sections_parts ON users_sections.section_id = scheduled_sections_parts.section_id AND scheduled_sections_parts.part_id = parts.id
      LEFT JOIN (SELECT user_id, task_id, revision, is_complete FROM student_responses ORDER BY revision DESC) as sr ON users.id = sr.user_id AND tasks.id = sr.task_id
      WHERE projects.slug = ?
        AND scheduled_sections_parts.active = TRUE
        AND COALESCE(sr.is_complete, FALSE) = FALSE
      ORDER BY parts.position ASC, tasks.position ASC
      LIMIT 1
    """

    /**
     * Cache a task into the in-memory cache.
     *
     * @param task the [[Task]] to be cached
     * @return the [[Task]] that was cached
     */
    private def cache(task: Task): Task = {
      Logger.debug(s"Caching ${task}")
      Cache.set(s"tasks[${task.id.string}]", task, db.cacheExpiry)
      Cache.remove(s"task.id_list.part[${task.partId.string}]")
      Cache.set(s"task.mapping.[${task.partId.string},${task.position}]", task.id, db.cacheExpiry)
      task
    }

    /**
     * Remove a task from the in-memory cache.
     *
     * @param task the [[Task]] to be uncached
     * @return the [[Task]] that was uncached
     */
    private def uncache(task: Task): Task = {
      Cache.remove(s"tasks[${task.id.string}]")
      Cache.remove(s"task.id_list.part[${task.partId.string}]")
      Cache.remove(s"task.mapping.[${task.partId.string},${task.position}]")
      task
    }

    /**
     * Find all tasks.
     *
     * @param conn An implicit connection object. Can be used in a transactional chain.
     * @return a vector of the returned tasks
     */
    override def list: Future[IndexedSeq[Task]] = {
      Cache.getAs[IndexedSeq[UUID]]("tasks.list") match {
        case Some(taskIdList) => {
          Future.sequence(taskIdList.map { taskId =>
            find(taskId).map(_.get)
          })
        }
        case None => {
          db.pool.sendQuery(SelectAll).map { queryResult =>
            val taskList = queryResult.rows.get.map {
              item: RowData => cache(Task(item))
            }
            Cache.set(s"tasks.list", taskList.map(_.id), db.cacheExpiry)
            taskList
          }.recover {
            case exception => {
              throw exception
            }
          }
        }
      }
    }

    /**
     * Find all tasks belonging to a given part.
     *
     * @param part The part to return tasks from.
     * @param conn An implicit connection object. Can be used in a transactional chain.
     * @return a vector of the returned tasks
     */
    override def list(part: Part): Future[IndexedSeq[Task]] = {
      Logger.trace("taskRepository.list(part) - entry")
      Cache.getAs[IndexedSeq[UUID]](s"task.id_list.part[${part.id.string}]") match {
        case Some(taskIdList) => {
          Future.sequence(taskIdList.map { taskId =>
            find(taskId).map(_.get)
          }).map { taskList =>
            val sorted = taskList.sortBy(_.position)
            Logger.debug("Tasks: \n" + sorted.map("\t" + _.toString()).mkString("\n"))
            sorted
          }
        }
        case None => {
          db.pool.sendPreparedStatement(SelectByPartId, Array[Any](part.id.bytes)).map { queryResult =>
            Logger.trace("taskRepository.list - task ID list found in database")
            val taskList = queryResult.rows.get.map {
              item: RowData => cache(Task(item))
            }.sortBy(_.position)
            Logger.debug("Tasks: \n" + taskList.map("\t" + _.toString()).mkString("\n"))
            Cache.set(s"task.id_list.part[${part.id.string}]", taskList.map(_.id), db.cacheExpiry)
            taskList
          }.recover {
            case exception => {
              throw exception
            }
          }
        }
      }
    }

    /**
     * Find all tasks belonging to a given project.
     *
     * @param project The project to return parts from.
     * @param conn An implicit connection object. Can be used in a transactional chain.
     * @return a vector of the returned tasks
     */
    override def list(project: Project): Future[IndexedSeq[Task]] = {
      Logger.trace("taskRepository.list(project) - entry")
      Cache.getAs[IndexedSeq[UUID]](s"part.id_list.project[${project.id.string}]") match {
        // Part ID list was found in the cache
        case Some(partIdList) => {
          Logger.trace("taskRepository.list(project) - part id list found in cache")
          Logger.debug(partIdList.toString())
          Future.sequence { partIdList.map { partId =>
            partRepository.find(partId).flatMap {
              case Some(part) => {
                Logger.trace(s"taskRepository.list(project) - listing tasks in part ${part.id.string}")
                list(part)
              }
              case None => {
                Logger.trace(s"taskRepository.list(project) - part ${partId.string} not found")
                Future.successful(IndexedSeq())
              }
            }
          }}.map(_.flatten)
        }
        // Nothing found in cache, fetch tasks directly from database
        case None => {
          db.pool.sendPreparedStatement(SelectByProjectId, Array[Any](project.id.bytes)).map { queryResult =>
            Logger.trace("taskRepository.list(project) - part id list not cached, found tasks from database")
            val taskList = queryResult.rows.get.map {
              item: RowData => cache(Task(item))
            }
            Logger.trace(taskList.map(_.partId).distinct.toString())
            Cache.set(s"part.id_list.project[${project.id.string}]", taskList.map(_.partId).distinct, db.cacheExpiry)
            taskList
          }.recover {
            case exception => {
              throw exception
            }
          }
        }

      }
    }

    /**
     * List tasks in a part based on project ID and part number.
     *
     * @param project the [[Project]] to list tasks from.
     * @param partNum the position of the part to list tasks from.
     */
    override def list(project: Project, partNum: Int): Future[IndexedSeq[Task]] = {
      Logger.trace("taskRepository.list(project, partNum) - entry")
      partRepository.find(project, partNum).flatMap { partOption =>
        list(partOption.get)
      }.recover {
        case exception => throw exception
      }
    }

    /**
     * Find a single entry by ID.
     *
     * @param id the UUID to search for
     * @param conn An implicit connection object. Can be used in a transactional chain.
     * @return an optional task if one was found
     */
    override def find(id: UUID): Future[Option[Task]] = {
      Cache.getAs[Task](s"tasks[${id.string}]") match {
        case Some(task) => {
          Logger.debug(s"Loaded from cache: ${task}")
          Future.successful(Option(task))
        }
        case None => {
          db.pool.sendPreparedStatement(SelectOne, Array[Any](id.bytes)).map { result =>
            result.rows.get.headOption match {
              case Some(rowData) => {
                Logger.debug(s"Loaded from database: ${Task(rowData)}")
                Some(cache(Task(rowData)))
              }
              case None => None
            }
          }.recover {
            case exception => {
              throw exception
            }
          }
        }
      }
    }

    /**
     * Find a task given its position within a part, its part's position within
     * a project, and its project slug.
     *
     * @param project the project to search within
     * @param partNum the number of the part within its project
     * @param taskNum the number of the task within its part
     * @return an optional task if one was found
     */
    override def find(project: Project, partNum: Int, taskNum: Int): Future[Option[Task]] = {
      partRepository.find(project, partNum).flatMap { partOption =>
        Cache.getAs[UUID](s"task.mapping.[${partOption.get.id.string},${taskNum}]") match {
          case Some(taskId) => find(taskId)
          case None => {
            db.pool.sendPreparedStatement(SelectByPosition, Array[Any](project.id.bytes, partNum, taskNum)).map { result =>
              result.rows.get.headOption match {
                case Some(rowData) => Some(cache(Task(rowData)))
                case None => None
              }
            }.recover {
              case exception => {
                throw exception
              }
            }
          }
        }
      }
    }

    /**
     * Create a new task.
     *
     * @param task The task to be inserted
     * @return the new task
     */
    override def insert(task: Task)(implicit conn: Connection): Future[Task] = {
      conn.sendPreparedStatement(Insert, Array(
        task.id.bytes,
        new DateTime,
        new DateTime,
        task.partId.bytes,
        task.dependencyId match {
          case Some(id) => Some(id.bytes)
          case None => None
        },
        task.name,
        task.description,
        task.position,
        task.notesAllowed
      )).map {
        result => {
          Cache.remove(s"task.id_list.part[${task.partId.string}]")
          cache(Task(result.rows.get.head))
        }
      }.recover {
        case exception => {
          Cache.remove(s"task.id_list.part[${task.partId.string}]")
          uncache(task)
          throw exception
        }
      }
    }

    /**
     * Update a task.
     *
     * @param task The task to be updated.
     * @return the updated task
     */
    override def update(task: Task)(implicit conn: Connection): Future[Task] = {
      Logger.debug(task.toString())
      conn.sendPreparedStatement(Update, Array(
        task.partId.bytes,
        task.dependencyId match {
          case Some(id) => Some(id.bytes)
          case None => None
        },
        task.name,
        task.description,
        task.position,
        task.notesAllowed,
        (task.version + 1),
        new DateTime,
        task.id.bytes,
        task.version
      )).map {
        result => {
          Cache.remove(s"task.id_list.part[${task.partId.string}]")
          Logger.debug(result.toString())
          cache(Task(result.rows.get.head))
        }
      }.recover {
        case exception => {
          Cache.remove(s"task.id_list.part[${task.partId.string}]")
          uncache(task)
          throw exception
        }
      }
    }

    override def clearPartCache(part: Part) = {
      Cache.remove(s"task.id_list.part[${part.id.string}]")
    }

    /**
     * Delete a task.
     *
     * @param task The task to delete.
     * @return A boolean indicating whether the operation was successful.
     */
    override def delete(task: Task)(implicit conn: Connection): Future[Boolean] = {
      conn.sendPreparedStatement(Purge, Array(task.id.bytes, task.version)).map {
        result => {
          Cache.remove(s"task.id_list.part[${task.partId.string}]")
          uncache(task)
          (result.rowsAffected > 0)
        }
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Delete a task.
     *
     * @param part the [[Part]] to delete tasks from.
     * @return A boolean indicating whether the operation was successful.
     */
    override def delete(part: Part)(implicit conn: Connection): Future[Boolean] = {
      Logger.debug("Deleting tasks in part: " + part.id.string)
      list(part).flatMap { taskList =>
        conn.sendPreparedStatement(DeleteByPart, Array[Any](part.id.bytes)).map {
          result => {
            val deleted = (result.rowsAffected == taskList.size)
            taskList.map(uncache)
            Cache.remove(s"task.id_list.part[${part.id.string}]")
            deleted
          }
        }
      }.recover {
        case exception => {
          throw exception
        }
      }
    }
  }
}
