package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks.questions.Question
import ca.shiftfocus.krispii.core.models.tasks.{Task, _}
import ca.shiftfocus.krispii.core.models.user.User
import com.github.mauricio.async.db.{Connection, RowData}
import org.joda.time.DateTime
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.{-\/, \/, \/-}

class TaskRepositoryPostgres(val cacheRepository: CacheRepository) extends TaskRepository with PostgresRepository[Task] with SpecificTaskConstructors {

  override val entityName = "Task"

  override def constructor(row: RowData): Task = {
    row("task_type").asInstanceOf[Int] match {
      case Task.Document => constructDocumentTask(row)
      case Task.Question => constructQuestionTask(row)
      case Task.Media => constructMediaTask(row)
      case _ => throw new Exception("Invalid task type.")
    }
  }

  // -- Common query components --------------------------------------------------------------------------------------

  val Table = "tasks"
  val CommonFields = "id, version, part_id, name, description, instructions, tagline, position, notes_allowed, response_title, hide_response, allow_gfile, notes_title, help_text, media_data, layout, parent_id, max_grade, created_at, updated_at, task_type"
  def CommonFieldsWithTable(table: String = Table): String = {
    CommonFields.split(", ").map({ field => s"${table}." + field }).mkString(", ")
  }
  val SpecificFields = "document_tasks.dependency_id as dependency_id, question_tasks.questions as questions, media_tasks.media_type as media_type"

  val QMarks = "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?"
  val OrderBy = s"${Table}.position ASC"
  val Join =
    s"""
       |LEFT JOIN document_tasks ON $Table.id = document_tasks.task_id
       |LEFT JOIN question_tasks ON $Table.id = question_tasks.task_id
       |LEFT JOIN media_tasks ON $Table.id = media_tasks.task_id
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
       |ORDER BY $OrderBy
     """.stripMargin

  val SelectByProjectId =
    s"""
       |SELECT ${CommonFieldsWithTable()}, $SpecificFields
       |FROM $Table
       |$Join
       |INNER JOIN projects
       | ON projects.id = ?
       |INNER JOIN  parts
       | ON parts.id = $Table.part_id
       | AND parts.project_id = projects.id
       |ORDER BY parts.position ASC, $OrderBy
  """.stripMargin

  val SelectByTeacherId =
    s"""
       |SELECT ${CommonFieldsWithTable()}, $SpecificFields
       |FROM $Table
       |$Join
       |INNER JOIN parts
       | ON parts.id = $Table.part_id
       |INNER JOIN projects
       | ON parts.project_id = projects.id
       |INNER JOIN courses
       | ON courses.id = projects.course_id
       | AND courses.teacher_id = ?
  """.stripMargin

  val SelectByPosition =
    s"""
       |SELECT ${CommonFieldsWithTable()}, $SpecificFields
       |FROM $Table
       |$Join
       |INNER JOIN parts
       | ON parts.id = $Table.part_id
       | AND parts.position = ?
       |INNER JOIN projects
       | ON projects.id = parts.project_id
       | AND projects.id = ?
       |WHERE $Table.position = ?
  """.stripMargin

  val SelectNowByUserId = s"""
                             |SELECT ${CommonFieldsWithTable()}, $SpecificFields
                             |FROM $Table
                             |$Join
                             |INNER JOIN users
                             | ON users.id = ?
                             |INNER JOIN projects
                             | ON projects.id = ?
                             |INNER JOIN parts
                             | ON parts.id = $Table.part_id
                             | AND parts.project_id = projects.id
                             | AND parts.enabled = 't'
                             |INNER JOIN users_courses
                             | ON users_courses.course_id = projects.course_id
                             | AND users_courses.user_id = users.id
                             |LEFT JOIN work
                             | ON users.id = work.user_id
                             | AND $Table.id = work.task_id
                             |WHERE COALESCE(work.is_complete, FALSE) = FALSE
                             |ORDER BY parts.position ASC, $OrderBy
                             |LIMIT 1
  """.stripMargin

  val SelectNowFromAll = s"""
                            |SELECT ${CommonFieldsWithTable()}, $SpecificFields
                            |FROM $Table
                            |$Join
                            |LEFT JOIN work
                            | ON $Table.id = work.task_id
                            |WHERE COALESCE(work.is_complete, FALSE) = FALSE
                            |ORDER BY $OrderBy
                            |LIMIT 1
  """.stripMargin

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
       |     la_task AS (INSERT INTO document_tasks (task_id, dependency_id)
       |                 SELECT task.id as task_id, ? as dependency_id
       |                 FROM task
       |                 RETURNING dependency_id)
       |SELECT ${CommonFieldsWithTable("task")}, la_task.dependency_id
       |FROM task, la_task
     """.stripMargin

  val InsertQuestion =
    s"""
       |WITH task AS (${Insert}),
       |     q_task AS (INSERT INTO question_tasks (task_id, questions)
       |                SELECT task.id as task_id, ? as questions
       |                FROM task
       |                RETURNING questions)
       |SELECT ${CommonFieldsWithTable("task")},
       |       q_task.questions
       |FROM task, q_task
     """.stripMargin

  val InsertMedia =
    s"""
       |WITH task AS (${Insert}),
       |     m_task AS (INSERT INTO media_tasks (task_id, media_type)
       |                 SELECT task.id as task_id, ? as media_type
       |                 FROM task
       |                 RETURNING media_type)
       |SELECT ${CommonFieldsWithTable("task")}, m_task.media_type
       |FROM task, m_task
     """.stripMargin

  // -- Update queries -----------------------------------------------------------------------------------------------

  val Update =
    s"""
       |UPDATE $Table
       |SET part_id = ?, name = ?, description = ?, instructions = ?, tagline = ?,
       |    position = ?, notes_allowed = ?,
       |    response_title = ?, hide_response = ?, allow_gfile = ?, notes_title = ?, help_text = ?,
       |    media_data = ?, layout = ?, parent_id = ?, version = ?, max_grade = ?, updated_at = ?
       |WHERE id = ?
       |  AND version = ?
       |RETURNING $CommonFields
     """.stripMargin

  val UpdateLongAnswer =
    s"""
       |WITH task AS (${Update})
       |UPDATE document_tasks AS l_task
       |SET dependency_id = ?
       |FROM task
       |WHERE task_id = task.id
       |RETURNING $CommonFields, l_task.dependency_id
     """.stripMargin

  val UpdateQuestion =
    s"""
       |WITH task AS (${Update})
       |UPDATE question_tasks AS q_task
       |SET questions = ?
       |FROM task
       |WHERE task_id = task.id
       |RETURNING $CommonFields, q_task.questions
     """.stripMargin

  val UpdateMedia =
    s"""
       |WITH task AS (${Update})
       |UPDATE media_tasks AS m_task
       |SET media_type = ?
       |FROM task
       |WHERE task_id = task.id
       |RETURNING $CommonFields, m_task.media_type
     """.stripMargin

  // -- Delete queries -----------------------------------------------------------------------------------------------

  val DeleteWhere =
    s"""
       |document_tasks.task_id = $Table.id
       | OR question_tasks.task_id = $Table.id
       | OR media_tasks.task_id = $Table.id
     """.stripMargin

  val DeleteByPart =
    s"""
       |DELETE FROM $Table
       |USING
       | document_tasks,
       | question_tasks,
       | media_tasks
       |WHERE part_id = ?
       | AND ($DeleteWhere)
       |RETURNING $CommonFields, $SpecificFields
    """.stripMargin

  val DeleteDocumentTask =
    s"""
       |DELETE FROM $Table
       |USING document_tasks
       |WHERE $Table.id = ?
       | AND $Table.version = ?
       | AND document_tasks.task_id = $Table.id
       |RETURNING $CommonFields, document_tasks.dependency_id as dependency_id
    """.stripMargin

  val DeleteQuestionTask =
    s"""
       |DELETE FROM $Table
       |USING question_tasks
       |WHERE $Table.id = ?
       | AND $Table.version = ?
       | AND (question_tasks.task_id = $Table.id)
       |RETURNING $CommonFields, question_tasks.questions as questions
    """.stripMargin

  val DeleteMediaTask =
    s"""
       |DELETE FROM $Table
       |USING media_tasks
       |WHERE $Table.id = ?
       | AND $Table.version = ?
       | AND (media_tasks.task_id = $Table.id)
       |RETURNING $CommonFields, media_tasks.media_type as media_type
    """.stripMargin
  // -- Methods ------------------------------------------------------------------------------------------------------

  /**
   * Find all tasks.
   *
   * @return a vector of the returned tasks
   */
  override def list(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Task]]] = {
    queryList(SelectAll)
  }

  /**
   * Find all tasks belonging to a given part.
   *
   * @param part The part to return tasks from.
   * @return a vector of the returned tasks
   */
  override def list(part: Part)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Task]]] = {
    cacheRepository.cacheSeqTask.getCached(cacheTasksKey(part.id)).flatMap {
      case \/-(taskList) => Future successful \/-(taskList)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          taskList <- lift(queryList(SelectByPartId, Array[Any](part.id)))
          _ <- lift(cacheRepository.cacheSeqTask.putCache(cacheTasksKey(part.id))(taskList, ttl))
        } yield taskList
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Find all tasks belonging to a given project.
   *
   * @param project The project to return parts from.
   * @return a vector of the returned tasks
   */
  override def list(project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Task]]] = {
    queryList(SelectByProjectId, Array[Any](project.id))
  }

  /**
   * Find all tasks belonging to a given teacher.
   *
   * @param teacher
   * @return a vector of the returned tasks
   */
  override def list(teacher: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Task]]] = {
    queryList(SelectByTeacherId, Array[Any](teacher.id))
  }

  /**
   * Find a single entry by ID.
   *
   * @param id the UUID to search for
   * @return an optional task if one was found
   */
  override def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Task]] = {
    cacheRepository.cacheTask.getCached(cacheTaskKey(id)).flatMap {
      case \/-(task) => Future successful \/-(task)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          task <- lift(queryOne(SelectOne, Seq[Any](id)))
          _ <- lift(cacheRepository.cacheTask.putCache(cacheTaskKey(id))(task, ttl))
        } yield task
      case -\/(error) => Future successful -\/(error)
    }
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
    cacheRepository.cacheUUID.getCached(cacheTaskPosKey(project.id, part.id, taskNum)).flatMap {
      case \/-(taskId) => this.find(taskId)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          task <- lift(queryOne(SelectByPosition, Seq[Any](part.position, project.id, taskNum)))
          _ <- lift(cacheRepository.cacheTask.putCache(cacheTaskKey(task.id))(task, ttl))
          _ <- lift(cacheRepository.cacheTask.putCache(cacheTaskPosKey(project.id, part.id, taskNum))(task, ttl))
        } yield task
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Find a task on which user is working on now.
   *
   * @param user
   * @param project
   * @return
   */
  override def findNow(user: User, project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Task]] = {
    queryOne(SelectNowByUserId, Seq[Any](user.id, project.id))
  }

  /**
   * Find a task from all tasks on which someone is working on now.
   *
   * @param conn
   * @return
   */
  override def findNowFromAll(implicit conn: Connection): Future[\/[RepositoryError.Fail, Task]] = {
    queryOne(SelectNowFromAll)
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
      task.id,
      1,
      task.partId,
      task.settings.title,
      task.settings.description,
      task.settings.instructions,
      task.settings.tagline,
      task.position,
      task.settings.notesAllowed,
      task.settings.responseTitle,
      task.settings.hideResponse,
      task.settings.allowGfile,
      task.settings.notesTitle,
      task.settings.help,
      task.settings.mediaData.flatMap(d => Some(Json.toJson(d).toString())),
      task.settings.layout,
      task.settings.parentId,
      task.maxGrade,
      new DateTime,
      new DateTime
    )

    // Prepare the additional data to be sent depending on the type of task
    val dataArray: Seq[Any] = task match {
      case longAnswer: DocumentTask => commonData ++ Seq[Any](Task.Document, longAnswer.dependencyId)
      case question: QuestionTask => {
        commonData ++ Seq[Any](Task.Question, Json.toJson(question.questions.map(Question.writes.writes)).toString) // TODO replace with some method like question.getJsonString
      }
      case media: MediaTask => commonData ++ Seq[Any](Task.Media, media.mediaType)
    }

    val query = task match {
      case longAnswer: DocumentTask => InsertLongAnswer
      case question: QuestionTask => InsertQuestion
      case media: MediaTask => InsertMedia

    }

    // Send the query
    for {
      inserted <- lift(queryOne(query, dataArray))
      _ <- lift(cacheRepository.cacheSeqTask.removeCached(cacheTasksKey(task.partId)))
    } yield inserted
  }

  /**
   * Update a task.
   *
   * @param task The task to be updated.
   * @return the updated task
   */
  override def update(task: Task, oldPartId: Option[UUID] = None)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Task]] = {
    // Start with the data common to all task types.
    val commonData = Seq[Any](
      task.partId,
      task.settings.title,
      task.settings.description,
      task.settings.instructions,
      task.settings.tagline,
      task.position,
      task.settings.notesAllowed,
      task.settings.responseTitle,
      task.settings.hideResponse,
      task.settings.allowGfile,
      task.settings.notesTitle,
      task.settings.help,
      task.settings.mediaData.flatMap(d => Some(Json.toJson(d).toString())),
      task.settings.layout,
      task.settings.parentId,
      task.version + 1,
      task.maxGrade,
      new DateTime,
      task.id,
      task.version
    )

    // Throw in the task type-specific data.
    val dataArray: Seq[Any] = task match {
      case longAnswer: DocumentTask => commonData ++ Seq[Any](longAnswer.dependencyId)
      case media: MediaTask => commonData ++ Seq[Any](media.mediaType)
      case question: QuestionTask => commonData ++ Seq[Any](Json.toJson(question.questions.map(Question.writes.writes)).toString) // TODO replace with some method like question.getJsonString
    }

    val query = task match {
      case longAnswer: DocumentTask => UpdateLongAnswer
      case shortAnswer: QuestionTask => UpdateQuestion
      case media: MediaTask => UpdateMedia
    }

    // Send the query
    for {
      updated <- lift(queryOne(query, dataArray))
      _ <- lift(oldPartId match {
        case Some(oldId) => cacheRepository.cacheSeqTask.removeCached(cacheTasksKey(oldId))
        case None => Future successful \/-(())
      })
      _ <- lift(cacheRepository.cacheTask.removeCached(cacheTaskKey(task.id)))
      _ <- lift(cacheRepository.cacheSeqTask.removeCached(cacheTasksKey(task.partId)))
    } yield updated
  }

  /**
   * Delete a task.
   *
   * @param task The task to delete.
   * @return A boolean indicating whether the operation was successful.
   */
  override def delete(task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Task]] = {
    val query = task match {
      case document: DocumentTask => DeleteDocumentTask
      case question: QuestionTask => DeleteQuestionTask
      case media: MediaTask => DeleteMediaTask
    }
    for {
      deleted <- lift(queryOne(query, Seq(task.id, task.version)))
      _ <- lift(cacheRepository.cacheTask.removeCached(cacheTaskKey(task.id)))
      _ <- lift(cacheRepository.cacheSeqTask.removeCached(cacheTasksKey(task.partId)))
    } yield deleted
  }

  /**
   * Delete all tasks belonging to a part.
   *
   * @param part the part to delete tasks from.
   * @return A boolean indicating whether the operation was successful.
   */
  override def delete(part: Part)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Task]]] = {
    for {
      deleted <- lift(queryList(DeleteByPart, Array[Any](part.id)))
      _ <- liftSeq(deleted.map({ task => cacheRepository.cacheTask.removeCached(cacheTaskKey(task.id)) }))
      _ <- lift(cacheRepository.cacheSeqTask.removeCached(cacheTasksKey(part.id)))
    } yield deleted
  }
}

trait SpecificTaskConstructors {
  protected def constructDocumentTask(row: RowData): DocumentTask = {
    DocumentTask(
      id = row("id").asInstanceOf[UUID],
      partId = row("part_id").asInstanceOf[UUID],
      position = row("position").asInstanceOf[Int],
      version = row("version").asInstanceOf[Long],
      settings = CommonTaskSettings(row),
      dependencyId = Option(row("dependency_id")).map(_.asInstanceOf[UUID]),
      maxGrade = row("max_grade").asInstanceOf[String],
      createdAt = row("created_at").asInstanceOf[DateTime],
      updatedAt = row("updated_at").asInstanceOf[DateTime]
    )
  }

  protected def constructQuestionTask(row: RowData): QuestionTask = {
    QuestionTask(
      id = row("id").asInstanceOf[UUID],
      partId = row("part_id").asInstanceOf[UUID],
      position = row("position").asInstanceOf[Int],
      version = row("version").asInstanceOf[Long],
      settings = CommonTaskSettings(row),
      questions = Json.parse(row("questions").asInstanceOf[String]).as[IndexedSeq[Question]],
      maxGrade = row("max_grade").asInstanceOf[String],
      createdAt = row("created_at").asInstanceOf[DateTime],
      updatedAt = row("updated_at").asInstanceOf[DateTime]
    )
  }
  protected def constructMediaTask(row: RowData): MediaTask = {
    MediaTask(
      id = row("id").asInstanceOf[UUID],
      partId = row("part_id").asInstanceOf[UUID],
      position = row("position").asInstanceOf[Int],
      version = row("version").asInstanceOf[Long],
      settings = CommonTaskSettings(row),
      mediaType = row("media_type").asInstanceOf[Int],
      maxGrade = row("max_grade").asInstanceOf[String],
      createdAt = row("created_at").asInstanceOf[DateTime],
      updatedAt = row("updated_at").asInstanceOf[DateTime]
    )
  }
}
