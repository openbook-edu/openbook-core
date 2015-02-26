package ca.shiftfocus.krispii.core.repositories

import java.util.NoSuchElementException

import ca.shiftfocus.krispii.core.models.tasks.MatchingTask.Match
import ca.shiftfocus.krispii.core.models.tasks.Task
import ca.shiftfocus.krispii.core.fail._
import com.github.mauricio.async.db.{ResultSet, RowData, Connection}
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib.ExceptionWriter
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks._
import ca.shiftfocus.uuid.UUID
import play.api.Logger
import scala.concurrent.Future
import org.joda.time.DateTime
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB

import scalaz.{\/-, -\/, \/}

trait TaskRepositoryPostgresComponent extends TaskRepositoryComponent {
  self: ProjectRepositoryComponent with
        PartRepositoryComponent with
        PostgresDB =>

  /**
   * Override with this trait's version of the ProjectRepository.
   */
  override val taskRepository: TaskRepository = new TaskRepositoryPSQL

  /**
   * A concrete implementation of the ProjectRepository class.
   */
  private class TaskRepositoryPSQL extends TaskRepository with PostgresRepository[Task] with SpecificTaskConstructors {

    override def constructor(row: RowData): Task = {
      row("task_type").asInstanceOf[Int] match {
        case Task.LongAnswer => constructLongAnswerTask(row)
        case Task.ShortAnswer => constructShortAnswerTask(row)
        case Task.MultipleChoice => constructMultipleChoiceTask(row)
        case Task.Ordering => constructOrderingTask(row)
        case Task.Matching => constructMatchingTask(row)
        case _ => throw new Exception("Invalid task type.")
      }
    }

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
         |  multiple_choice_tasks.allow_multiple,
         |  multiple_choice_tasks.randomize,
         |  ordering_tasks.choices,
         |  ordering_tasks.answers,
         |  ordering_tasks.randomize,
         |  matching_tasks.choices_left,
         |  matching_tasks.choices_right,
         |  matching_Tasks.answers,
         |  matching_Tasks.randomize
       """.stripMargin

    val Fields =
      s"""
         |tasks.id, tasks.version, tasks.created_at, tasks.updated_at, tasks.part_id, tasks.dependency_id, tasks.name,
         |  tasks.description, tasks.position, tasks.notes_allowed, tasks.task_type,
         |  short_answer_tasks.max_length,
         |  multiple_choice_tasks.choices,
         |  multiple_choice_tasks.answers,
         |  multiple_choice_tasks.allow_multiple,
         |  multiple_choice_tasks.randomize,
         |  ordering_tasks.choices,
         |  ordering_tasks.answers,
         |  ordering_tasks.randomize,
         |  matching_tasks.choices_left,
         |  matching_tasks.choices_right,
         |  matching_Tasks.answers,
         |  matching_Tasks.randomize
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
       """.stripMargin

    val SelectByPartId =
      s"""
         |$Select
         |$From
         |$Join
         |WHERE part_id = ?
       """.stripMargin

    val SelectByProjectIdPartNum =
      s"""
         |$Select
         |$From, parts, projects
         |WHERE projects.id = ?
         |  AND projects.id = parts.project_id
         |  AND parts.position = ?
         |  AND parts.id = tasks.part_id
         |ORDER BY parts.position ASC, tasks.position ASC
       """.stripMargin

    val SelectByProjectId = s"""
      $Select
      $From, parts, projects
      WHERE projects.id = ?
        AND projects.id = parts.project_id
        AND parts.id = tasks.part_id
      ORDER BY parts.position ASC, tasks.position ASC
    """

    val SelectActiveByProjectId = s"""
      $Select
      $From, parts, projects, classes, classes_projects, users_classes
      WHERE projects.id = ?
        AND projects.id = parts.project_id
        AND parts.id = tasks.part_id
        AND users_classes.user_id = ?
        AND users_classes.class_id = classes_projects.class_id
        AND classes_projects.project_id = projects.id
      ORDER BY parts.position ASC, tasks.position ASC
    """

    val SelectByPosition = s"""
      $Select
      $From
      $Join
      INNER JOIN parts ON parts.id = tasks.part_id AND parts.position = ?
      INNER JOIN projects ON projects.id = parts.project_id AND projects.id = ?
      WHERE tasks.position = ?
    """

    val SelectNowByUserId = s"""
      $Select, COALESCE(sr.is_complete, FALSE) AS is_complete
      $From
      $Join
      INNER JOIN users ON users.id = ?
      INNER JOIN projects ON projects.id = ?
      INNER JOIN parts ON parts.project_id = projects.id AND parts.enabled = 't'
      INNER JOIN users_classes ON users_classes.class_id = projects.class_id AND users_classes.user_id = users.id
      LEFT JOIN (SELECT user_id, task_id, revision, is_complete FROM student_responses ORDER BY revision DESC) as sr ON users.id = sr.user_id AND tasks.id = sr.task_id
      WHERE COALESCE(sr.is_complete, FALSE) = FALSE
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
         |  SELECT task.id as task_id
         |  FROM task
         |  RETURNING $Fields
       """.stripMargin

    val InsertShortAnswer =
      s"""
         |WITH task AS (
         |  ${Insert}
         |)
         |INSERT INTO short_answer_tasks (task_id, max_length)
         |  SELECT task.id as task_id, ? as max_length
         |  FROM task
         |  RETURNING $Fields
       """.stripMargin

    val InsertMultipleChoice =
      s"""
         |WITH task AS (
         |  ${Insert}
         |)
         |INSERT INTO multiple_choice_tasks (task_id, choices, answers, allow_multiple, randomize)
         |  SELECT task.id as task_id, ? as choices, ? as answers, ? as allow_multiple, ? as randomize
         |  FROM task
         |  RETURNING $Fields
       """.stripMargin

    val InsertOrdering =
      s"""
         |WITH task AS (
         |  ${Insert}
         |)
         |INSERT INTO ordering_tasks (task_id, choices, answers, randomize)
         |  SELECT task.id as task_id, ? as choices, ? as answers, ? as randomize
         |  FROM task
         |  RETURNING $Fields
       """.stripMargin

    val InsertMatching =
      s"""
         |WITH task AS (
         |  ${Insert}
         |)
         |INSERT INTO matching_tasks (task_id, choices_left, choices_right, answers, randomize)
         |  SELECT task.id as task_id, ? as choices_left, ? as choices_right, ? as answers, ? as randomize
         |  FROM task
         |  RETURNING $Fields
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
     * @return a vector of the returned tasks
     */
    override def list(implicit conn: Connection):Future[\/[Fail, IndexedSeq[Task]]] = {
      queryList(SelectAll)
    }

    /**
     * Find all tasks belonging to a given part.
     *
     * @param part The part to return tasks from.
     * @return a vector of the returned tasks
     */
    override def list(part: Part)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[Task]]] = {
      queryList(SelectByPartId, Array[Any](part.id.bytes))
    }

    /**
     * Find all tasks belonging to a given project.
     *
     * @param project The project to return parts from.
     * @return a vector of the returned tasks
     */
    override def list(project: Project)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[Task]]] = {
      queryList(SelectByProjectId, Array[Any](project.id.bytes))
    }

    /**
     * List tasks in a part based on project ID and part number.
     *
     * @param project the [[Project]] to list tasks from.
     * @param partNum the position of the part to list tasks from.
     */
    override def list(project: Project, partNum: Int)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[Task]]] = {
      (for {
        part <- lift(partRepository.find(project, partNum))
        tasks <- lift(list(part))
      } yield tasks).run
    }

    /**
     * Find a single entry by ID.
     *
     * @param id the UUID to search for
     * @return an optional task if one was found
     */
    override def find(id: UUID)(implicit conn: Connection): Future[\/[Fail, Task]] = {
      queryOne(SelectOne, Seq[Any](id.bytes))
    }

    /**
     * Find a task on which user is working on now.
     *
     * @param user
     * @param project
     * @return
     */
    override def findNow(user: User, project: Project)(implicit conn: Connection): Future[\/[Fail, Task]] = {
      queryOne(SelectNowByUserId, Seq[Any](user.id.bytes, project.id.bytes))
    }

    /**
     * Find a task given its position within a part, its part's position within
     * a project, and its project.
     *
     * @param project the project to search within
     * @param partNum the number of the part within its project
     * @param taskNum the number of the task within its part
     * @return an optional task if one was found
     */
    override def find(project: Project, partNum: Int, taskNum: Int)(implicit conn: Connection): Future[\/[Fail, Task]] = {
      (for {
        part <- lift(partRepository.find(project, partNum))
        task <- lift(queryOne(SelectByPosition, Seq[Any](partNum, project.id.bytes, taskNum)))
      } yield task).run
    }

    /**
     * Insert a new task into the database.
     *
     * This method handles all task types in one place.
     *
     * @param task The task to be inserted
     * @return the new task
     */
    override def insert(task: Task)(implicit conn: Connection): Future[\/[Fail, Task]] = {
      // All tasks have these properties.
      val commonData = Seq[Any](
        task.id.bytes,
        new DateTime,
        new DateTime,
        task.partId.bytes,
        task.settings.dependencyId match {
          case Some(id) => Some(id.bytes)
          case None => None
        },
        task.settings.title,
        task.settings.description,
        task.position,
        task.settings.notesAllowed
      )

      // Prepare the additional data to be sent depending on the type of task
      val dataArray = task match {
        case longAnswer: LongAnswerTask => commonData ++ Array[Any](Task.LongAnswer)

        case shortAnswer: ShortAnswerTask => commonData ++ Array[Any](
          Task.ShortAnswer,
          shortAnswer.maxLength
        )
        case multipleChoice: MultipleChoiceTask => commonData ++ Array[Any](
          Task.MultipleChoice,
          multipleChoice.choices,
          multipleChoice.answer,
          multipleChoice.allowMultiple,
          multipleChoice.randomizeChoices
        )
        case ordering: OrderingTask => commonData ++ Array[Any](
          Task.Ordering,
          ordering.elements,
          ordering.answer,
          ordering.randomizeChoices
        )
        case matching: MatchingTask => commonData ++ Array[Any](
          Task.Matching,
          matching.elementsLeft,
          matching.elementsRight,
          matching.answer.map { element => s"${element.left}:${element.right}" },
          matching.randomizeChoices
        )
        case _ => throw new Exception("I don't know how you did this, but you sent me a task type that doesn't exist.")
      }

      val query = task match {
        case longAnswer: LongAnswerTask => InsertLongAnswer
        case shortAnswer: ShortAnswerTask => InsertShortAnswer
        case multipleChoice: MultipleChoiceTask => InsertMultipleChoice
        case ordering: OrderingTask => InsertOrdering
        case matching: MatchingTask => InsertMatching
      }

      // Send the query
      queryOne(query, dataArray)
    }

    /**
     * Update a task.
     *
     * @param task The task to be updated.
     * @return the updated task
     */
    override def update(task: Task)(implicit conn: Connection): Future[\/[Fail, Task]] = {
      // Start with the data common to all task types.
      val commonData = Seq[Any](
        task.partId.bytes,
        task.settings.dependencyId match {
          case Some(id) => Some(id.bytes)
          case None => None
        },
        task.settings.title,
        task.settings.description,
        task.position,
        task.settings.notesAllowed,
        task.version +1,
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
      queryOne(Update, dataArray)
    }

    /**
     * Delete a task.
     *
     * @param task The task to delete.
     * @return A boolean indicating whether the operation was successful.
     */
    override def delete(task: Task)(implicit conn: Connection): Future[\/[Fail, Task]] = {
      queryOne(Delete, Seq(task.id.bytes, task.version))
    }

    /**
     * Delete all tasks belonging to a part.
     *
     * @param part the [[Part]] to delete tasks from.
     * @return A boolean indicating whether the operation was successful.
     */
    override def delete(part: Part)(implicit conn: Connection): Future[\/[Fail, IndexedSeq[Task]]] = {
      (for {
        tasks <- lift(list(part))
        deletedTasks <- lift(queryList(DeleteByPart, Array[Any](part.id.bytes)))
      }
      yield deletedTasks).run
    }
  }

  trait SpecificTaskConstructors {
    /**
     * Create a LongAnswerTask from a row returned by the database.
     *
     * @param row a [[RowData]] object returned from the db.
     * @return a [[LongAnswerTask]] object
     */
    protected def constructLongAnswerTask(row: RowData): LongAnswerTask = {
      LongAnswerTask(
        id = UUID(row("id").asInstanceOf[Array[Byte]]),
        partId = UUID(row("part_id").asInstanceOf[Array[Byte]]),
        position = row("position").asInstanceOf[Int],
        version = row("version").asInstanceOf[Long],
        settings = CommonTaskSettings(row),
        createdAt = row("created_at").asInstanceOf[DateTime],
        updatedAt = row("updated_at").asInstanceOf[DateTime]
      )
    }

    /**
     * Create a ShortAnswerTask from a row returned by the database.
     *
     * @param row a [[RowData]] object returned from the db.
     * @return a [[ShortAnswerTask]] object
     */
    protected def constructShortAnswerTask(row: RowData): ShortAnswerTask = {
      ShortAnswerTask(
        id = UUID(row("id").asInstanceOf[Array[Byte]]),
        partId = UUID(row("part_id").asInstanceOf[Array[Byte]]),
        position = row("position").asInstanceOf[Int],
        version = row("version").asInstanceOf[Long],
        settings = CommonTaskSettings(row),
        maxLength = row("max_length").asInstanceOf[Int],
        createdAt = row("created_at").asInstanceOf[DateTime],
        updatedAt = row("updated_at").asInstanceOf[DateTime]
      )
    }

    /**
     * Create a MultipleChoiceTask from a row returned by the database.
     *
     * @param row a [[RowData]] object returned from the db.
     * @return a [[MultipleChoiceTask]] object
     */
    protected def constructMultipleChoiceTask(row: RowData): MultipleChoiceTask = {
      MultipleChoiceTask(
        id = UUID(row("id").asInstanceOf[Array[Byte]]),
        partId = UUID(row("part_id").asInstanceOf[Array[Byte]]),
        position = row("position").asInstanceOf[Int],
        version = row("version").asInstanceOf[Long],
        settings = CommonTaskSettings(row),
        choices = Option(row("choices").asInstanceOf[IndexedSeq[String]]).getOrElse(IndexedSeq.empty[String]),
        answer  = Option(row("answers").asInstanceOf[IndexedSeq[Int]]).getOrElse(IndexedSeq.empty[Int]),
        allowMultiple = row("allow_multiple").asInstanceOf[Boolean],
        randomizeChoices = row("randomize").asInstanceOf[Boolean],
        createdAt = row("created_at").asInstanceOf[DateTime],
        updatedAt = row("updated_at").asInstanceOf[DateTime]
      )
    }

    /**
     * Create a OrderingTask from a row returned by the database.
     *
     * @param row a [[RowData]] object returned from the db.
     * @return a [[OrderingTask]] object
     */
    protected def constructOrderingTask(row: RowData): OrderingTask = {
      OrderingTask(
        id = UUID(row("id").asInstanceOf[Array[Byte]]),
        partId = UUID(row("part_id").asInstanceOf[Array[Byte]]),
        position = row("position").asInstanceOf[Int],
        version = row("version").asInstanceOf[Long],
        settings = CommonTaskSettings(row),
        elements = Option(row("choices").asInstanceOf[IndexedSeq[String]]).getOrElse(IndexedSeq.empty[String]),
        answer  = Option(row("answers").asInstanceOf[IndexedSeq[Int]]).getOrElse(IndexedSeq.empty[Int]),
        randomizeChoices = row("randomize").asInstanceOf[Boolean],
        createdAt = row("created_at").asInstanceOf[DateTime],
        updatedAt = row("updated_at").asInstanceOf[DateTime]
      )
    }

    /**
     * Create a MatchingTask from a row returned by the database.
     *
     * @param row a [[RowData]] object returned from the db.
     * @return a [[MatchingTask]] object
     */
    protected def constructMatchingTask(row: RowData): MatchingTask = {
      MatchingTask(
        id = UUID(row("id").asInstanceOf[Array[Byte]]),
        partId = UUID(row("part_id").asInstanceOf[Array[Byte]]),
        position = row("position").asInstanceOf[Int],
        version = row("version").asInstanceOf[Long],
        settings = CommonTaskSettings(row),
        elementsLeft = Option(row("elements_left").asInstanceOf[IndexedSeq[String]]).getOrElse(IndexedSeq.empty[String]),
        elementsRight = Option(row("elements_right").asInstanceOf[IndexedSeq[String]]).getOrElse(IndexedSeq.empty[String]),
        answer = Option(row("answers").asInstanceOf[IndexedSeq[String]]).getOrElse(IndexedSeq.empty[String]).map { element =>
          val split = element.split(":")
          Match(split(0).toInt, split(1).toInt)
        },
        randomizeChoices = row("randomize").asInstanceOf[Boolean],
        createdAt = row("created_at").asInstanceOf[DateTime],
        updatedAt = row("updated_at").asInstanceOf[DateTime]
      )
    }
  }
}