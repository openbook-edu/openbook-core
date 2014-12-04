package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.models.tasks.Task
import com.github.mauricio.async.db.{RowData, Connection}
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib.ExceptionWriter
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks._
import ca.shiftfocus.uuid.UUID
import play.api.Logger
import scala.concurrent.Future
import org.joda.time.DateTime
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB

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
    def fields = Seq("part_id", "dependency_id", "name", "description", "position",
                     "notes_allowed", "max_length", "choices", "answers", "allow_multiple",
                     "choices_left", "choices_right", "randomize")

    def table = "tasks"
    def orderBy = "position ASC"
    val fieldsText = fields.mkString(", ")
    val questions = fields.map(_ => "?").mkString(", ")

    // -- Common query components --------------------------------------------------------------------------------------

    val Select =
      s"""
         |SELECT tasks.id, tasks.version, tasks.created_at, tasks.updated_at, tasks.part_id, tasks.dependency_id, tasks.name,
         |  tasks.description, tasks.position, tasks.notes_allowed, tasks.task_type,
         |  short_answer_tasks.max_length,
         |  multiple_choice_tasks.choices,
         |  multiple_choice_tasks.answers,
         |  ordering_tasks.choices,
         |  ordering_tasks.answers,
         |  matching_tasks.choices_left,
         |  matching_tasks.choices_right,
         |  matching_Tasks.answers
       """.stripMargin

    val From = "FROM tasks"

    val Join =
      s"""
         |LEFT JOIN long_answer_tasks ON tasks.id = long_answer_tasks.task_id
         |LEFT JOIN short_answer_tasks ON tasks.id = short_answer_tasks.task_id
         |LEFT JOIN multiple_choice_tasks ON tasks.id = multiple_choice_tasks.task_id
         |LEFT JOIN ordering_tasks ON tasks.id = ordering_tasks.task_id
         |LEFT JOIN matching_tasks ON tasks.id = matching_tasks.task_id
       """.stripMargin

    // -- Select queries -----------------------------------------------------------------------------------------------

    val SelectAll =
      s"""
         |$Select
         |$From
         |$Join
         |ORDER BY position ASC
       """.stripMargin

    val SelectOne =
      s"""
         |$Select
         |$From
         |$Join
         |WHERE tasks.id = ?
         |  AND status = 1
       """.stripMargin

    val SelectByPartId =
      s"""
         |$Select
         |$From
         |$Join
         |WHERE part_id = ?
         |  AND status = 1
       """.stripMargin

    val SelectByProjectIdPartNum =
      s"""
         |$Select
         |$From, parts, projects
         |WHERE projects.id = ?
         |  AND projects.id = parts.project_id
         |  AND parts.position = ?
         |  AND parts.id = tasks.part_id
         |  AND projects.status = 1 AND parts.status = 1 AND tasks.status = 1
         |ORDER BY parts.position ASC, tasks.position ASC
       """.stripMargin

    val SelectByProjectId = s"""
      $Select
      $From, parts, projects
      WHERE projects.id = ?
        AND projects.id = parts.project_id
        AND parts.id = tasks.part_id
        AND projects.status = 1 AND parts.status = 1 AND tasks.status = 1
      ORDER BY parts.position ASC, tasks.position ASC
    """

    val SelectActiveByProjectId = s"""
      $Select
      $From, parts, projects, sections, sections_projects, users_sections
      WHERE projects.id = ?
        AND projects.id = parts.project_id
        AND parts.id = tasks.part_id
        AND projects.status = 1 AND parts.status = 1 AND tasks.status = 1
        AND users_sections.user_id = ?
        AND users_sections.class_id = sections_projects.class_id
        AND sections_projects.project_id = projects.id
      ORDER BY parts.position ASC, tasks.position ASC
    """

    val SelectByPosition = s"""
      $Select
      $From, parts, projects
      WHERE projects.id = ?
        AND parts.position = ?
        AND tasks.position = ?
        AND projects.id = parts.project_id
        AND parts.id = tasks.part_id
        AND tasks.status = 1 AND parts.status = 1 AND projects.status = 1
    """

    val SelectNowByUserId = s"""
      $Select
        COALESCE(sr.is_complete, FALSE) AS is_complete
      $From
      $Join
      INNER JOIN parts ON parts.id = tasks.part_id
      INNER JOIN projects ON parts.project_id = projects.id
      INNER JOIN users ON users.id = ?
      INNER JOIN users_sections ON users.id = users_sections.user_id
      INNER JOIN scheduled_sections_parts ON users_sections.class_id = scheduled_sections_parts.class_id AND scheduled_sections_parts.part_id = parts.id
      LEFT JOIN (SELECT user_id, task_id, revision, is_complete FROM student_responses ORDER BY revision DESC) as sr ON users.id = sr.user_id AND tasks.id = sr.task_id
      WHERE projects.slug = ?
        AND scheduled_sections_parts.active = TRUE
        AND COALESCE(sr.is_complete, FALSE) = FALSE
      ORDER BY parts.position ASC, tasks.position ASC
      LIMIT 1
    """

    // -- Insert queries -----------------------------------------------------------------------------------------------

    val Insert = {
      val extraFields = fields.mkString(",")
      val questions = fields.map(_ => "?").mkString(",")
      s"""
         |INSERT INTO tasks (id, version, created_at, updated_at, part_id, dependency_id,
         |                   name, description, position, notes_allowed, task_type)
         |VALUES (?, 1, ?, ?, ?, ?, ?, ?, ?, ?, ?)
         |RETURNING id, version, created_at, updated_at, part_id, dependency_id,
         |          name, description, position, notes_allowed, task_type
      """.stripMargin
    }

    val InsertLongAnswer =
      s"""
         |WITH task AS (
         |  ${Insert}
         |)
         |INSERT INTO long_answer_tasks (task_id)
         |  SELECT id as task_id
       """.stripMargin

    val InsertShortAnswer =
      s"""
         |WITH task AS (
         |  ${Insert}
         |)
         |INSERT INTO short_answer_tasks (task_id, max_length)
         |  SELECT id as task_id, ? as max_length
       """.stripMargin

    val InsertMultipleChoice =
      s"""
         |WITH task AS (
         |  ${Insert}
         |)
         |INSERT INTO multiple_choice_tasks (task_id, choices, answers, allow_multiple, randomize)
         |  SELECT id as task_id, ? as choices, ? as answers, ? as allow_multiple, ? as randomize
       """.stripMargin

    val InsertOrdering =
      s"""
         |WITH task AS (
         |  ${Insert}
         |)
         |INSERT INTO ordering_tasks (task_id, choices, answers, randomize)
         |  SELECT id as task_id, ? as choices, ? as answers, ? as randomize
       """.stripMargin

    val InsertMatching =
      s"""
         |WITH task AS (
         |  ${Insert}
         |)
         |INSERT INTO matching_tasks (task_id, choices_left, choices_right, answers, randomize)
         |  SELECT id as task_id, ? as choices_left, ? as choices_right, ? as answers, ? as randomize
       """.stripMargin

    // -- Update queries -----------------------------------------------------------------------------------------------

    val Update =
      s"""
         |UPDATE tasks
         |SET part_id = ?, dependency_id = ?,
         |    name = ?, description = ?,
         |    position = ?, notes_allowed = ?,
         |    version = ?
         |WHERE id = ?
         |  AND version = ?
         |RETURNING id, version, created_at, updated_at, part_id, dependency_id,
         |          name, description, position, notes_allowed, task_type
       """.stripMargin

    val UpdateLongAnswer =
      s"""
         |WITH task AS (
         |  ${Update}
         |)
         |UPDATE long_answer_tasks
         |SET task_id = task.id
         |RETURNING id, version, created_at, updated_at, part_id, dependency_id,
         |          name, description, position, notes_allowed, task_type
       """.stripMargin

    val UpdateShortAnswer =
      s"""
         |WITH task AS (
         |  ${Update}
         |)
         |UPDATE short_answer_tasks
         |SET task_id = task.id, max_length = ?
         |RETURNING id, version, created_at, updated_at, part_id, dependency_id,
         |          name, description, position, notes_allowed, task_type
       """.stripMargin

    val UpdateMultipleChoice =
      s"""
         |WITH task AS (
         |  ${Update}
         |)
         |UPDATE multiple_choice_tasks
         |SET task_id = task.id, choices = ?, answers = ?, allow_multiple = ?, randomize = ?
         |RETURNING id, version, created_at, updated_at, part_id, dependency_id,
         |          name, description, position, notes_allowed, task_type
       """.stripMargin

    val UpdateOrdering =
      s"""
         |WITH task AS (
         |  ${Update}
         |)
         |UPDATE ordering_tasks
         |SET task_id = task.id, choices = ?, answers = ?, randomize = ?
         |RETURNING id, version, created_at, updated_at, part_id, dependency_id,
         |          name, description, position, notes_allowed, task_type
       """.stripMargin

    val UpdateMatching =
      s"""
         |WITH task AS (
         |  ${Update}
         |)
         |UPDATE matching_tasks
         |SET task_id = task.id, choices_left = ?, choices_right = ?, answers = ?, randomize = ?
         |RETURNING id, version, created_at, updated_at, part_id, dependency_id,
         |          name, description, position, notes_allowed, task_type
       """.stripMargin

    // -- Delete queries -----------------------------------------------------------------------------------------------

    val DeleteByPart = s"""
      DELETE FROM $table WHERE part_id = ?
    """

    val Delete = s"""
      DELETE FROM $table WHERE id = ? AND version = ?
    """

    // -- Methods ------------------------------------------------------------------------------------------------------

    /**
     * Find all tasks.
     *
     * @param conn An implicit connection object. Can be used in a transactional chain.
     * @return a vector of the returned tasks
     */
    override def list: Future[IndexedSeq[Task]] = {
      db.pool.sendQuery(SelectAll).map { queryResult =>
        val taskList = queryResult.rows.get.map {
          item: RowData => Task(item)
        }
        taskList
      }.recover {
        case exception => {
          throw exception
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
      db.pool.sendPreparedStatement(SelectByPartId, Array[Any](part.id.bytes)).map { queryResult =>
        val taskList = queryResult.rows.get.map {
          item: RowData => Task(item)
        }.sortBy(_.position)
        taskList
      }.recover {
        case exception => {
          throw exception
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
      db.pool.sendPreparedStatement(SelectByProjectId, Array[Any](project.id.bytes)).map { queryResult =>
        val taskList = queryResult.rows.get.map {
          item: RowData => Task(item)
        }
        taskList
      }.recover {
        case exception => {
          throw exception
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
      db.pool.sendPreparedStatement(SelectOne, Array[Any](id.bytes)).map { result =>
        result.rows.get.headOption match {
          case Some(rowData) => {
            Some(Task(rowData))
          }
          case None => None
        }
      }.recover {
        case exception => {
          throw exception
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
        db.pool.sendPreparedStatement(SelectByPosition, Array[Any](project.id.bytes, partNum, taskNum)).map { result =>
          result.rows.get.headOption match {
            case Some(rowData) => Some(Task(rowData))
            case None => None
          }
        }.recover {
          case exception => {
            throw exception
          }
        }
      }
    }

    /**
     * Insert a new task into the database.
     *
     * This method handles all task types in one place.
     *
     * @param task The task to be inserted
     * @return the new task
     */
    override def insert(task: Task)(implicit conn: Connection): Future[Task] = {
      // All tasks have these properties.
      val commonData =  Array[Any](
        task.id.bytes, new DateTime, new DateTime, task.partId.bytes,
        task.settings.dependencyId match {
          case Some(id) => Some(id.bytes)
          case None => None
        },
        task.settings.title, task.settings.description, task.position,
        task.settings.notesAllowed, Task.LongAnswer
      )

      // Prepare the additional data to be sent depending on the type of task
      val dataArray = task match {
        case longAnswer: LongAnswerTask => commonData
        case shortAnswer: ShortAnswerTask => commonData ++ Array[Any](
          shortAnswer.maxLength
        )
        case multipleChoice: MultipleChoiceTask => commonData ++ Array[Any](
          multipleChoice.choices,
          multipleChoice.answer,
          multipleChoice.allowMultiple,
          multipleChoice.randomizeChoices
        )
        case ordering: OrderingTask => commonData ++ Array[Any](
          ordering.elements,
          ordering.answer,
          ordering.randomizeChoices
        )
        case matching: MatchingTask => commonData ++ Array[Any](
          matching.elementsLeft,
          matching.elementsRight,
          matching.answer.map { element => s"${element.left}:${element.right}" },
          matching.randomizeChoices
        )
        case _ => throw new Exception("I don't know how you did this, but you sent me a task type that doesn't exist.")
      }

      // Send the query
      conn.sendPreparedStatement(Insert, dataArray).map {
        result => {
          Task(result.rows.get.head)
        }
      }.recover {
        case exception => {
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
      // Start with the data common to all task types.
      val commonData = Array[Any](
        task.partId.bytes,
        task.settings.dependencyId match {
          case Some(id) => Some(id.bytes)
          case None => None
        },
        task.settings.title,
        task.settings.description,
        task.position,
        task.settings.notesAllowed,
        (task.version +1),
        task.id.bytes, task.version
      )

      // Throw in the task type-specific data.
      val dataArray = task match {
        case longAnswer: LongAnswerTask => commonData
        case shortAnswer: ShortAnswerTask => commonData ++ Array[Any](
          shortAnswer.maxLength
        )
        case multipleChoice: MultipleChoiceTask => commonData ++ Array[Any](
          multipleChoice.choices,
          multipleChoice.answer,
          multipleChoice.allowMultiple,
          multipleChoice.randomizeChoices
        )
        case ordering: OrderingTask => commonData ++ Array[Any](
          ordering.elements,
          ordering.answer,
          ordering.randomizeChoices
        )
        case matching: MatchingTask => commonData ++ Array[Any](
          matching.elementsLeft,
          matching.elementsRight,
          matching.answer.map { element => s"${element.left}:${element.right}" },
          matching.randomizeChoices
        )
        case _ => throw new Exception("I don't know how you did this, but you sent me a task type that doesn't exist.")
      }

      // Execute the query
      conn.sendPreparedStatement(Update, dataArray).map {
        result => Task(result.rows.get.head)
      }.recover {
        case exception => {
          throw exception
        }
      }
    }

    /**
     * Delete a task.
     *
     * @param task The task to delete.
     * @return A boolean indicating whether the operation was successful.
     */
    override def delete(task: Task)(implicit conn: Connection): Future[Boolean] = {
      conn.sendPreparedStatement(Delete, Array(task.id.bytes, task.version)).map {
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
     * Delete a task.
     *
     * @param part the [[Part]] to delete tasks from.
     * @return A boolean indicating whether the operation was successful.
     */
    override def delete(part: Part)(implicit conn: Connection): Future[Boolean] = {
      list(part).flatMap { taskList =>
        conn.sendPreparedStatement(DeleteByPart, Array[Any](part.id.bytes)).map {
          result => {
            val deleted = (result.rowsAffected == taskList.size)
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
