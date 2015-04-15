package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks.MatchingTask.Match
import ca.shiftfocus.krispii.core.models.tasks.Task._
import ca.shiftfocus.krispii.core.models.tasks.{MatchingTask, Task}
import ca.shiftfocus.krispii.core.models.work._
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.{ResultSet, RowData, Connection}
import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.DateTime
import scala.concurrent.Future
import scalaz.{\/, -\/, \/-}

class WorkRepositoryPostgres(val documentRepository: DocumentRepository) extends WorkRepository with PostgresRepository[Work] {

  override def constructor(row: RowData): Work = {
    row("work_type").asInstanceOf[Int] match {
      case LongAnswer => constructLongAnswerWork(row)
      case ShortAnswer => constructShortAnswerWork(row)
      case MultipleChoice => constructMultipleChoiceWork(row)
      case Ordering => constructOrderingWork(row)
      case Matching => constructMatchingWork(row)
      case _ => throw new Exception("Retrieved an unknown task type from the database. You dun messed up now!")
    }
  }

  private def constructLongAnswerWork(row: RowData): LongAnswerWork = {
    LongAnswerWork(
      id         = UUID(row("id").asInstanceOf[Array[Byte]]),
      studentId  = UUID(row("user_id").asInstanceOf[Array[Byte]]),
      taskId     = UUID(row("task_id").asInstanceOf[Array[Byte]]),
      documentId = UUID(row("la_document_id").asInstanceOf[Array[Byte]]),
      version    = row("version").asInstanceOf[Long],
      response   = None,
      isComplete = row("is_complete").asInstanceOf[Boolean],
      createdAt  = row("created_at").asInstanceOf[DateTime],
      updatedAt  = row("updated_at").asInstanceOf[DateTime]
    )
  }

  private def constructShortAnswerWork(row: RowData): ShortAnswerWork = {
    ShortAnswerWork(
      id         = UUID(row("id").asInstanceOf[Array[Byte]]),
      studentId  = UUID(row("user_id").asInstanceOf[Array[Byte]]),
      taskId     = UUID(row("task_id").asInstanceOf[Array[Byte]]),
      documentId = UUID(row("sa_document_id").asInstanceOf[Array[Byte]]),
      version    = row("version").asInstanceOf[Long],
      response   = None,
      isComplete = row("is_complete").asInstanceOf[Boolean],
      createdAt  = row("created_at").asInstanceOf[DateTime],
      updatedAt  = row("updated_at").asInstanceOf[DateTime]
    )
  }

  private def constructMultipleChoiceWork(row: RowData): MultipleChoiceWork = {
    MultipleChoiceWork(
      id         = UUID(row("id").asInstanceOf[Array[Byte]]),
      studentId  = UUID(row("user_id").asInstanceOf[Array[Byte]]),
      taskId     = UUID(row("task_id").asInstanceOf[Array[Byte]]),
      version    = row("mc_version").asInstanceOf[Long],
      response   = row("mc_response").asInstanceOf[IndexedSeq[Int]],
      isComplete = row("is_complete").asInstanceOf[Boolean],
      createdAt  = row("created_at").asInstanceOf[DateTime],
      updatedAt  = row("updated_at").asInstanceOf[DateTime]
    )
  }

  private def constructOrderingWork(row: RowData): OrderingWork = {
    OrderingWork(
      id         = UUID(row("id").asInstanceOf[Array[Byte]]),
      studentId  = UUID(row("user_id").asInstanceOf[Array[Byte]]),
      taskId     = UUID(row("task_id").asInstanceOf[Array[Byte]]),
      version    = row("ord_version").asInstanceOf[Long],
      response   = row("ord_response").asInstanceOf[IndexedSeq[Int]],
      isComplete = row("is_complete").asInstanceOf[Boolean],
      createdAt  = row("created_at").asInstanceOf[DateTime],
      updatedAt  = row("updated_at").asInstanceOf[DateTime]
    )
  }

  private def constructMatchingWork(row: RowData): MatchingWork = {
    MatchingWork(
      id         = UUID(row("id").asInstanceOf[Array[Byte]]),
      studentId  = UUID(row("user_id").asInstanceOf[Array[Byte]]),
      taskId     = UUID(row("task_id").asInstanceOf[Array[Byte]]),
      version    = row("mat_version").asInstanceOf[Long],
      response   = row("mat_response").asInstanceOf[IndexedSeq[IndexedSeq[Int]]].map { element =>
        Match(element(0), element(1))
      },
      isComplete = row("is_complete").asInstanceOf[Boolean],
      createdAt  = row("created_at").asInstanceOf[DateTime],
      updatedAt  = row("updated_at").asInstanceOf[DateTime]
    )
  }

  // -- Common query components --------------------------------------------------------------------------------------

  val Table                 = "work"
  val CommonFields          = "id, user_id, task_id, version, is_complete, work_type, created_at, updated_at"
  def CommonFieldsWithTable(table: String = Table): String = {
    CommonFields.split(", ").map({ field => s"${table}." + field}).mkString(", ")
  }
  val SpecificFields =
    s"""
       |long_answer_work.document_id as la_document_id,
       |short_answer_work.document_id as sa_document_id,
       |multiple_choice_work.response as mc_response,
       |multiple_choice_work.version as mc_version,
       |ordering_work.response as ord_response,
       |ordering_work.version as ord_version,
       |matching_work.response as mat_response,
       |matching_work.version as mat_version
     """.stripMargin

  val QMarks  = "?, ?, ?, ?, ?, ?, ?, ?"
  val OrderBy = s"${Table}.position ASC"
  val Join =
    s"""
      |LEFT JOIN long_answer_work ON $Table.id = long_answer_work.work_id
      |LEFT JOIN short_answer_work ON $Table.id = short_answer_work.work_id
      |LEFT JOIN multiple_choice_work ON $Table.id = multiple_choice_work.work_id
      |LEFT JOIN ordering_work ON $Table.id = ordering_work.work_id
      |LEFT JOIN matching_work ON $Table.id = matching_work.work_id
     """.stripMargin

  // -- Select queries -----------------------------------------------------------------------------------------------

//  val Fields =
//    s"""
//       |work.id as id,
//       |       work.user_id as user_id,
//       |       work.task_id as task_id,
//       |       work.is_complete as is_complete,
//       |       work.created_at as created_at,
//       |       work.updated_at as updated_at,
//       |       work.work_type as work_type,
//       |       work.version as version,
//       |       long_answer_work.document_id as long_answer_document_id,
//       |       short_answer_work.document_id as short_answer_document_id,
//       |       multiple_choice_work.response as multiple_choice_response,
//       |       multiple_choice_work.version as multiple_choice_version,
//       |       ordering_work.response as ordering_response,
//       |       ordering_work.version as ordering_version,
//       |       matching_work.response as matching_response,
//       |       matching_work.version as matching_version
//     """.stripMargin
//
  val Select =
    s"""
       |SELECT $CommonFields
     """.stripMargin

  val From = "FROM work"

  val JoinMatchVersion =
    s"""
       |LEFT JOIN long_answer_work ON work.id = long_answer_work.work_id
       |LEFT JOIN short_answer_work ON work.id = short_answer_work.work_id
       |LEFT JOIN multiple_choice_work ON work.id = multiple_choice_work.work_id
       |  AND work.version = multiple_choice_work.version
       |LEFT JOIN ordering_work ON work.id = ordering_work.work_id
       |  AND work.version = ordering_work.version
       |LEFT JOIN matching_work ON work.id = matching_work.work_id
       |  AND work.version = matching_work.version
     """.stripMargin

  // -- Select queries -----------------------------------------------------------------------------------------------

  val SelectAllForUserProject =
    s"""
        |SELECT ${CommonFieldsWithTable()}, $SpecificFields
        |FROM $Table
        |$Join
        |INNER JOIN projects
        | ON projects.id = ?
        |INNER JOIN parts
        | ON parts.project_id = projects.id
        |INNER JOIN tasks
        | ON tasks.part_id = parts.id
        |WHERE $Table.user_id = ?
        | AND $Table.task_id = tasks.id
     """.stripMargin

  // TODO - maybe $Join instead of $JoinMatchVersion
  val SelectAllForTask =
    s"""
        |SELECT ${CommonFieldsWithTable()}, $SpecificFields
        |FROM $Table
        |$JoinMatchVersion
        |INNER JOIN tasks
        | ON tasks.id = ?
        |WHERE $Table.task_id = tasks.id
     """.stripMargin

  val ListRevisionsById =
    s"""
      |SELECT ${CommonFieldsWithTable()}, $SpecificFields
      |FROM $Table
      |$Join
      |WHERE $Table.user_id = ?
      | AND $Table.task_id = ?
     """.stripMargin

  val SelectByStudentTask =
    s"""
       |SELECT ${CommonFieldsWithTable()}, $SpecificFields
       |$From
       |$JoinMatchVersion
       |WHERE user_id = ?
       |  AND task_id = ?
       |LIMIT 1
     """.stripMargin

  val SelectById =
    s"""
      |SELECT ${CommonFieldsWithTable()}, $SpecificFields
      |FROM $Table
      |$JoinMatchVersion
      |WHERE id = ?
      |LIMIT 1
     """.stripMargin

  // -- Insert queries  -------------------------------------------------------------------------------------------

  val Insert =
    s"""
       |INSERT INTO work (id, user_id, task_id, version, is_complete, created_at, updated_at, work_type)
       |VALUES (?, ?, ?, 1, ?, ?, ?, ?)
       |RETURNING id, user_id, task_id, version, is_complete, created_at, updated_at, work_type
     """.stripMargin

  def InsertIntoDocumentWork(table: String): String = {
    val response = table match {
      case "long_answer_work" => "la_document_id"
      case "short_answer_work" => "sa_document_id"
    }
    s"""
       |WITH w AS ($Insert),
       |     x AS (INSERT INTO $table (work_id, document_id)
       |           SELECT w.id as work_id, ? as document_id
       |           FROM w
       |           RETURNING *)
       |SELECT w.id, w.user_id, w.task_id, w.version, w.is_complete, w.created_at, w.updated_at, w.work_type, x.document_id as $response
       |FROM w, x
     """.stripMargin
  }

  def InsertIntoVersionedWork(table: String): String = {
    val version = table match {
      case "multiple_choice_work" => "mc_version"
      case "ordering_work" => "ord_version"
      case "matching_work" => "mat_version"
    }
    val response = table match {
      case "multiple_choice_work" => "mc_response"
      case "ordering_work" => "ord_response"
      case "matching_work" => "mat_response"
    }
    s"""
       |WITH w AS ($Insert),
       |     x AS (INSERT INTO $table (work_id, version, response)
       |           SELECT w.id as work_id, w.version as version, ? as response
       |           FROM w
       |           RETURNING *)
       |SELECT w.id, w.user_id, w.task_id, w.version as $version, w.is_complete, w.created_at, w.updated_at, w.work_type, x.response as $response
       |FROM w, x
     """.stripMargin
  }

  // -- Update queries  -------------------------------------------------------------------------------------------

  val Update =
    s"""
       |UPDATE work
       |SET version = ?,
       |    is_complete = ?,
       |    updated_at = ?
       |WHERE id = ?
       |  AND version = ?
       |RETURNING *
     """.stripMargin

  def UpdateKeepLatestRevision(table: String): String = {
    val (version, response, documentId) = table match {
      case "long_answer_work" => ("version", "not_used", "la_document_id")
      case "short_answer_work" => ("version", "not_used", "sa_document_id")
      case "multiple_choice_work" => ("mc_version", "mc_response", "not_used")
      case "ordering_work" => ("ord_version", "ord_response", "not_used")
      case "matching_work" => ("mat_version", "mat_response", "not_used")
    }

    s"""
       |WITH w AS ($Update)
       |     x AS (SELECT *
       |           FROM $table
       |           WHERE work_id = w.id
       |             AND version = w.version
       |SELECT w.id, w.user_id, w.task_id, w.version as $version, x.response as $response, x.document_id as $documentId, w.is_complete, w.work_type, w.created_at, w.updated_at
       |FROM w,x
     """.stripMargin
  }

  def UpdateWithNewRevision(table: String): String = {
    val (version, response) = table match {
      case "multiple_choice_work" => ("mc_version", "mc_response")
      case "ordering_work" => ("ord_version", "ord_response")
      case "matching_work" => ("mat_version", "mat_response")
    }

    s"""
       |WITH w AS ($Update),
       |     x AS (INSERT INTO $table (work_id, version, response)
       |           SELECT w.id as work_id,
       |                  w.version as version,
       |                  ? as response
       |           FROM w
       |           RETURNING *)
       |SELECT w.id, w.user_id, w.task_id, w.version as $version, x.response as $response, w.is_complete, w.work_type, w.created_at, w.updated_at
       |FROM w,x
     """.stripMargin
  }

  // -- Delete queries -----------------------------------------------------------------------------------------------
  // NB: the delete queries should never be used unless you know what you're doing. Due to work revisioning, the
  //     proper way to "clear" a work is to create an empty revision.

  val Delete =
    s"""
       |DELETE work
       |$From
       |$Join
       |WHERE user_id = ?
       |  AND task_id = ?
     """.stripMargin

  val DeleteRevision =
    s"""
       |DELETE work
       |$From
       |$Join
       |WHERE user_id = ?
       |  AND task_id = ?
       |  AND revision = ?
     """.stripMargin

  val DeleteAllForTask =
    s"""
       |DELETE work
       |$From
       |$Join
       |WHERE task.id = ?
     """.stripMargin


  /**
   * List the latest revision of work for each task in a project for a user.
   *
   * @param project
   * @param user
   * @return
   */
  override def list(user: User, project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Work]]] = {
    queryList(SelectAllForUserProject, Seq[Any](project.id.bytes, user.id.bytes))
  }

  // TODO create separate methods for LongAnswer, ShortAnswer and MultipleChoice, Ordering, Matching
  /**
   * List latest revisions of a specific work for a user.
   *
   * @param task
   * @param user
   * @return
   */
  override def list(user: User, task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Work]]] = {
    queryList(ListRevisionsById, Seq[Any](user.id.bytes, task.id.bytes))
  }

  /**
   * List latest revisions of a specific work for all users.
   *
   * @param task
   * @return
   */
  override def list(task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Work]]] = {
    queryList(SelectAllForTask, Seq[Any](task.id.bytes))
  }

  /**
   * Find the latest revision of a single work.
   *
   * @param workId
   * @return
   */
  override def find(workId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Work]] = {
    queryOne(SelectById, Seq[Any](workId.bytes)).flatMap {
      case \/-(work: LongAnswerWork) => documentRepository.find(work.documentId).map {
        case \/-(document) => \/.right(work.copy(response = Some(document)))
        case -\/(error: RepositoryError.Fail) => \/.left(error)
      }
      case \/-(work: ShortAnswerWork) => documentRepository.find(work.documentId).map {
        case \/-(document) => \/.right(work.copy(response = Some(document)))
        case -\/(error: RepositoryError.Fail) => \/.left(error)
      }
      case otherWorkTypes => Future successful otherWorkTypes
    }
  }

  /**
   * Find the latest revision of a single work for a user.
   *
   * @param task
   * @param user
   * @return
   */
  override def find(user: User, task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Work]] = {
    queryOne(SelectByStudentTask, Seq[Any](user.id.bytes, task.id.bytes)).flatMap {
      case \/-(work: LongAnswerWork) => documentRepository.find(work.documentId).map {
        case \/-(document) => \/.right(work.copy(response = Some(document)))
        case -\/(error: RepositoryError.Fail) => \/.left(error)
      }
      case \/-(work: ShortAnswerWork) => documentRepository.find(work.documentId).map {
        case \/-(document) => \/.right(work.copy(response = Some(document)))
        case -\/(error: RepositoryError.Fail) => \/.left(error)
      }
      case otherWorkTypes => Future successful otherWorkTypes
    }
  }

  /**
   * Find a specific revision for a single work for a user.
   *
   * @param task
   * @param user
   * @return
   */
  override def find(user: User, task: Task, version: Long)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Work]] = {
    queryOne(SelectByStudentTask, Seq[Any](user.id.bytes, task.id.bytes, version)).flatMap {
      case \/-(work: LongAnswerWork) => documentRepository.find(work.documentId).map {
        case \/-(document) => \/.right(work.copy(response = Some(document)))
        case -\/(error: RepositoryError.Fail) => \/.left(error)
      }
      case \/-(work: ShortAnswerWork) => documentRepository.find(work.documentId).map {
        case \/-(document) => \/.right(work.copy(response = Some(document)))
        case -\/(error: RepositoryError.Fail) => \/.left(error)
      }
      case otherWorkTypes => Future successful otherWorkTypes
    }
  }

  /**
   * Insert a new work into the database.
   *
   * This explicitly inserts a new, fresh work. To insert a new revision for an existing work, use the
   * update method.
   *
   * Since all work types have the same fields (varying only by the type of 'answer')
   *
   * @param work
   * @param conn
   * @return
   */
  override def insert(work: Work)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Work]] = {
    val query = work match {
      case specific: LongAnswerWork => InsertIntoDocumentWork("long_answer_work")
      case specific: ShortAnswerWork => InsertIntoDocumentWork("short_answer_work")
      case specific: MultipleChoiceWork => InsertIntoVersionedWork("multiple_choice_work")
      case specific: OrderingWork => InsertIntoVersionedWork("ordering_work")
      case specific: MatchingWork => InsertIntoVersionedWork("matching_work")
    }

    val baseParams = Seq[Any](
      work.id.bytes,
      work.studentId.bytes,
      work.taskId.bytes,
      work.isComplete,
      new DateTime,
      new DateTime
    )

    val params = work match {
      case specific: LongAnswerWork => baseParams ++ Array[Any](Task.LongAnswer, specific.documentId.bytes)
      case specific: ShortAnswerWork => baseParams ++ Array[Any](Task.ShortAnswer, specific.documentId.bytes)
      case specific: MultipleChoiceWork => baseParams ++ Array[Any](Task.MultipleChoice, specific.response)
      case specific: OrderingWork => baseParams ++ Array[Any](Task.Ordering, specific.response)
      case specific: MatchingWork => baseParams ++ Array[Any](Task.Matching, specific.response.asInstanceOf[IndexedSeq[MatchingTask.Match]].map { item => IndexedSeq(item.left, item.right)})
    }

    queryOne(query, params)
  }

  /**
   * Update a work.
   *
   * By default, updating a work inserts a new revision, but it can optionally update an existing revision (for
   * example, if the domain logic only calls for new revisions after some other condition has been satisfied,
   * like an amount of time passing since the previous revision.
   *
   * @param work
   * @param newRevision
   * @param conn
   * @return
   */
  override def update(work: Work)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Work]] = update(work, false)
  override def update(work: Work, newRevision: Boolean)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Work]] = {
    val tableName = work match {
      case specific: LongAnswerWork => "long_answer_work"
      case specific: ShortAnswerWork => "short_answer_work"
      case specific: MultipleChoiceWork => "multiple_choice_work"
      case specific: OrderingWork => "ordering_work"
      case specific: MatchingWork => "matching_work"
    }

   if (newRevision) {
      queryOne(UpdateWithNewRevision(tableName), Seq[Any](
        work.version +1,
        work.isComplete,
        new DateTime,
        work.id.bytes,
        work.version,
        work.response
      ))
    }
    else {
      queryOne(UpdateKeepLatestRevision(tableName), Seq[Any](
        work.version,
        work.isComplete,
        new DateTime,
        work.studentId.bytes,
        work.taskId.bytes,
        work.version,
        work.response
      ))
    }
  }

  /**
   * Delete a specific revision of a work.
   *
   * @param work
   * @param conn
   * @return
   */
  override def delete(work: Work, thisRevisionOnly: Boolean = false)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Work]] = {
    if (thisRevisionOnly) {
      queryOne(DeleteRevision, Seq[Any](
        work.studentId.bytes,
        work.taskId.bytes,
        work.version
      ))
    }
    else {
      queryOne(Delete, Seq[Any](
        work.studentId.bytes,
        work.taskId.bytes
      ))
    }
  }

  /**
   * Delete all work for a given task.
   *
   * This is needed when we want to delete the task itself... before that can be done,
   * all that task's work must itself be deleted.
   *
   * @param task
   * @param conn
   * @return
   */
  override def delete(task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Work]]] = {
    (for {
      works <- lift(list(task))
      deletedWorks <- lift(queryNumRows(DeleteAllForTask, Seq[Any](task.id.bytes))(0 < _))
    } yield works).run
  }
}
