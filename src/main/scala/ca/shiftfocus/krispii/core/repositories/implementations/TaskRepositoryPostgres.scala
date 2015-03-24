package ca.shiftfocus.krispii.core.repositories

import java.util.NoSuchElementException

import ca.shiftfocus.krispii.core.models.tasks.MatchingTask.Match
import ca.shiftfocus.krispii.core.models.tasks.Task
import ca.shiftfocus.krispii.core.error._
import com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException
import com.github.mauricio.async.db.{ResultSet, RowData, Connection}
import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.lib.exceptions.ExceptionWriter
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks._
import ca.shiftfocus.uuid.UUID
import play.api.Logger
import scala.concurrent.Future
import org.joda.time.DateTime
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB

import scalaz.{\/-, -\/, \/}

class TaskRepositoryPostgres extends TaskRepository with PostgresRepository[Task] with SpecificTaskConstructors {

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

  // -- Common query components --------------------------------------------------------------------------------------

  val Table                 = "tasks"
  val CommonFields          = "id, version, created_at, updated_at, part_id, dependency_id, name, description, position, notes_allowed, task_type"
  val CommonFieldsWithTable = CommonFields.split(", ").map({ field => s"${Table}." + field}).mkString(", ")
  val SpecificFields =
    s"""
       |  short_answer_tasks.max_length,
       |  multiple_choice_tasks.choices,
       |  multiple_choice_tasks.answers,
       |  multiple_choice_tasks.allow_multiple,
       |  multiple_choice_tasks.randomize,
       |  ordering_tasks.elements,
       |  ordering_tasks.answers,
       |  ordering_tasks.randomize,
       |  matching_tasks.choices_left,
       |  matching_tasks.choices_right,
       |  matching_Tasks.answers,
       |  matching_Tasks.randomize
     """.stripMargin

  val QMarks  = "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?"
  val OrderBy = s"${Table}.position ASC"
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
       |SELECT $CommonFields, $SpecificFields
       |FROM $Table
       |$Join
       |ORDER BY $OrderBy
     """.stripMargin

  val SelectOne =
    s"""
       |SELECT $CommonFields, $SpecificFields
       |FROM $Table
       |$Join
       |WHERE tasks.id = ?
     """.stripMargin

  val SelectByPartId =
    s"""
       |SELECT $CommonFields, $SpecificFields
       |FROM $Table
       |$Join
       |WHERE part_id = ?
     """.stripMargin

  // TODO - not used
//  val SelectByProjectIdPartNum =
//    s"""
//       |SELECT $CommonFields, $SpecificFields
//       |FROM $Table, parts, projects
//       |WHERE projects.id = ?
//       |  AND projects.id = parts.project_id
//       |  AND parts.position = ?
//       |  AND parts.id = $Table.part_id
//       |ORDER BY parts.position ASC, $Table.position ASC
//     """.stripMargin

  val SelectByProjectId =
    s"""
      |SELECT $CommonFields, $SpecificFields
      |FROM $Table, parts, projects
      |WHERE projects.id = ?
      | AND projects.id = parts.project_id
      | AND parts.id = $Table.part_id
      |ORDER BY parts.position ASC, $OrderBy
  """.stripMargin

  // TODO - not used
//  val SelectActiveByProjectId = s"""
//    $Select
//    $From, parts, projects, classes, courses_projects, users_courses
//    WHERE projects.id = ?
//      AND projects.id = parts.project_id
//      AND parts.id = tasks.part_id
//      AND users_courses.user_id = ?
//      AND users_courses.course_id = courses_projects.course_id
//      AND courses_projects.project_id = projects.id
//    ORDER BY parts.position ASC, tasks.position ASC
//  """

  val SelectByPosition =
    s"""
      |SELECT $CommonFields, $SpecificFields
      |FROM $Table
      |$Join
      |INNER JOIN parts
      | ON parts.id = tasks.part_id
      | AND parts.position = ?
      |INNER JOIN projects
      | ON projects.id = parts.project_id
      | AND projects.id = ?
      |WHERE tasks.position = ?
  """.stripMargin

  val SelectNowByUserId = s"""
    |SELECT $CommonFields, $SpecificFields, COALESCE(work.is_complete, FALSE) AS is_complete
    |FROM $Table
    |$Join
    |INNER JOIN users ON users.id = ?
    |INNER JOIN projects ON projects.id = ?
    |INNER JOIN parts ON parts.project_id = projects.id AND parts.enabled = 't'
    |INNER JOIN users_courses ON users_courses.course_id = projects.course_id AND users_courses.user_id = users.id
    |LEFT JOIN work ON users.id = work.user_id AND tasks.id = work.task_id
    |WHERE COALESCE(work.is_complete, FALSE) = FALSE
    |ORDER BY parts.position ASC, $OrderBy
    |LIMIT 1
  """

  // -- Insert queries -----------------------------------------------------------------------------------------------

  val Insert =
    s"""
       |INSERT INTO $Table ($CommonFields)
       |VALUES ($QMarks)
       |RETURNING $CommonFields
    """.stripMargin

  val InsertLongAnswer =
    s"""
       |WITH task AS (${Insert}),
       |     la_task AS (INSERT INTO long_answer_tasks (task_id)
       |                 SELECT task.id as task_id
       |                 FROM task
       |                 RETURNING task_id)
       |SELECT $CommonFieldsWithTable
       |FROM task, la_task
     """.stripMargin

  val InsertShortAnswer =
    s"""
       |WITH task AS (${Insert}),
       |     sa_task AS (INSERT INTO short_answer_tasks (task_id, max_length)
       |                 SELECT task.id as task_id, ? as max_length
       |                 FROM task
       |                 RETURNING max_length)
       |SELECT $CommonFieldsWithTable, sa_task.max_length
       |FROM task, sa_task
     """.stripMargin

  val InsertMultipleChoice =
    s"""
       |WITH task AS (${Insert}),
       |     mc_task AS (INSERT INTO multiple_choice_tasks (task_id, choices, answers, allow_multiple, randomize)
       |                 SELECT task.id as task_id, ? as choices, ? as answers, ? as allow_multiple, ? as randomize
       |                 FROM task
       |                 RETURNING *)
       |SELECT $CommonFieldsWithTable, mc_task.choices, mc_task.answers, mc_task.allow_multiple, mc_task.randomize
       |FROM task, mc_task
     """.stripMargin

  val InsertOrdering =
    s"""
       |WITH task AS (${Insert}),
       |     ord_task AS (INSERT INTO ordering_tasks (task_id, elements, answers, randomize)
       |                  SELECT task.id as task_id, ? as choices, ? as answers, ? as randomize
       |                  FROM task
       |                  RETURNING *)
       |SELECT $CommonFieldsWithTable, ord_task.choices, ord_task.answers, ord_task.randomize
       |FROM task, ord_task
     """.stripMargin

  val InsertMatching =
    s"""
       |WITH task AS (${Insert}),
       |     mat_task (INSERT INTO matching_tasks (task_id, choices_left, choices_right, answers, randomize)
       |               SELECT task.id as task_id, ? as choices_left, ? as choices_right, ? as answers, ? as randomize
       |               FROM task
       |               RETURNING *)
       |SELECT $CommonFieldsWithTable, mat_task.choices_left, mat_task.choices_right, mat_task.answers, mat_task.randomize
       |FROM task, mat_task
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
       |RETURNING $CommonFields
     """.stripMargin

  val UpdateLongAnswer =
    s"""
       |WITH task AS (
       |  ${Update}
       |)
       |UPDATE long_answer_tasks
       |SET task_id = task.id
       |RETURNING $CommonFields
     """.stripMargin

  val UpdateShortAnswer =
    s"""
       |WITH task AS (
       |  ${Update}
       |)
       |UPDATE short_answer_tasks
       |SET task_id = task.id, max_length = ?
       |RETURNING $CommonFields
     """.stripMargin

  val UpdateMultipleChoice =
    s"""
       |WITH task AS (
       |  ${Update}
       |)
       |UPDATE multiple_choice_tasks
       |SET task_id = task.id, choices = ?, answers = ?, allow_multiple = ?, randomize = ?
       |RETURNING $CommonFields
     """.stripMargin

  val UpdateOrdering =
    s"""
       |WITH task AS (
       |  ${Update}
       |)
       |UPDATE ordering_tasks
       |SET task_id = task.id, elements = ?, answers = ?, randomize = ?
       |RETURNING $CommonFields
     """.stripMargin

  val UpdateMatching =
    s"""
       |WITH task AS (
       |  ${Update}
       |)
       |UPDATE matching_tasks
       |SET task_id = task.id, choices_left = ?, choices_right = ?, answers = ?, randomize = ?
       |RETURNING $CommonFields
     """.stripMargin

  // -- Delete queries -----------------------------------------------------------------------------------------------

  val DeleteByPart = s"""
    DELETE FROM $Table WHERE part_id = ?
  """

  val Delete = s"""
    DELETE FROM $Table WHERE id = ? AND version = ?
  """

  // -- Methods ------------------------------------------------------------------------------------------------------

  /**
   * Find all tasks.
   *
   * @return a vector of the returned tasks
   */
  override def list(implicit conn: Connection):Future[\/[RepositoryError.Fail, IndexedSeq[Task]]] = {
    queryList(SelectAll)
  }

  /**
   * Find all tasks belonging to a given part.
   *
   * @param part The part to return tasks from.
   * @return a vector of the returned tasks
   */
  override def list(part: Part)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Task]]] = {
    queryList(SelectByPartId, Array[Any](part.id.bytes))
  }

  /**
   * Find all tasks belonging to a given project.
   *
   * @param project The project to return parts from.
   * @return a vector of the returned tasks
   */
  override def list(project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Task]]] = {
    queryList(SelectByProjectId, Array[Any](project.id.bytes))
  }

  /**
   * Find a single entry by ID.
   *
   * @param id the UUID to search for
   * @return an optional task if one was found
   */
  override def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Task]] = {
    queryOne(SelectOne, Seq[Any](id.bytes))
  }

  /**
   * Find a task on which user is working on now.
   *
   * @param user
   * @param project
   * @return
   */
  override def findNow(user: User, project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Task]] = {
    queryOne(SelectNowByUserId, Seq[Any](user.id.bytes, project.id.bytes))
  }

  /**
   * Find a task given its position within a part, its part's position within
   * a project, and its project.
   *
   * @param project the project to search within
   * @param part    the part to get position
   * @param taskNum the number of the task within its part
   * @return an optional task if one was found
   */
  override def find(project: Project, part: Part, taskNum: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Task]] = {
    (for {
      task <- lift(queryOne(SelectByPosition, Seq[Any](part.position, project.id.bytes, taskNum)))
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
  override def insert(task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Task]] = {
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
  override def update(task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Task]] = {
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

    val query = task match {
      case longAnswer: LongAnswerTask => UpdateLongAnswer
      case shortAnswer: ShortAnswerTask => UpdateShortAnswer
      case multipleChoice: MultipleChoiceTask => UpdateMultipleChoice
      case ordering: OrderingTask => UpdateOrdering
      case matching: MatchingTask => UpdateMatching
    }

    // Send the query
    queryOne(query, dataArray)
  }

  /**
   * Delete a task.
   *
   * @param task The task to delete.
   * @return A boolean indicating whether the operation was successful.
   */
  override def delete(task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Task]] = {
    queryOne(Delete, Seq(task.id.bytes, task.version))
  }

  /**
   * Delete all tasks belonging to a part.
   *
   * @param part the [[Part]] to delete tasks from.
   * @return A boolean indicating whether the operation was successful.
   */
  override def delete(part: Part)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Task]]] = {
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
      elementsLeft = Option(row("choices_left").asInstanceOf[IndexedSeq[String]]).getOrElse(IndexedSeq.empty[String]),
      elementsRight = Option(row("choices_right").asInstanceOf[IndexedSeq[String]]).getOrElse(IndexedSeq.empty[String]),
      answer = Option(row("answers").asInstanceOf[IndexedSeq[Int]]).getOrElse(IndexedSeq.empty[Int]).map { element =>
println(println(Console.RED + Console.BOLD + element + Console.RESET))
//        val split = element.split(":")
//        Match(split(0).toInt, split(1).toInt)
        Match(element, element)
      },
      randomizeChoices = row("randomize").asInstanceOf[Boolean],
      createdAt = row("created_at").asInstanceOf[DateTime],
      updatedAt = row("updated_at").asInstanceOf[DateTime]
    )
  }
}
