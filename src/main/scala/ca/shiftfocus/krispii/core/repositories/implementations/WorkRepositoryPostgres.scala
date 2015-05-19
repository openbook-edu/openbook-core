package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks.MatchingTask.Match
import ca.shiftfocus.krispii.core.models.tasks.Task.Ordering
import ca.shiftfocus.krispii.core.models.tasks.Task._
import ca.shiftfocus.krispii.core.models.tasks._
import ca.shiftfocus.krispii.core.models.work._
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.{ResultSet, RowData, Connection}
import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.DateTime
import scala.concurrent.Future
import scalaz.{\/, -\/, \/-}

class WorkRepositoryPostgres(val documentRepository: DocumentRepository,
                             val revisionRepository: RevisionRepository)
  extends WorkRepository with PostgresRepository[Work] {

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
      updatedAt  = row("mc_created_at").asInstanceOf[DateTime]
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
      updatedAt  = row("ord_created_at").asInstanceOf[DateTime]
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
      updatedAt  = row("mat_created_at").asInstanceOf[DateTime]
    )
  }

  // -- Common query components --------------------------------------------------------------------------------------

  val Table                 = "work"
  val CommonFields          = "id, user_id, task_id, version, is_complete, created_at, updated_at, work_type"
  def CommonFieldsWithTable(table: String = Table): String = {
    CommonFields.split(", ").map({ field => s"${table}." + field}).mkString(", ")
  }

  val QMarks  = "?, ?, ?, ?, ?, ?, ?, ?"

  val SpecificOrderBy = s"""
     |multiple_choice_work.version DESC,
     |ordering_work.version DESC,
     |matching_work.version DESC
     """.stripMargin

  val SpecificFields =
    s"""
       |long_answer_work.document_id as la_document_id,
       |short_answer_work.document_id as sa_document_id,
       |multiple_choice_work.response as mc_response,
       |multiple_choice_work.version as mc_version,
       |multiple_choice_work.created_at as mc_created_at,
       |ordering_work.response as ord_response,
       |ordering_work.version as ord_version,
       |ordering_work.created_at as ord_created_at,
       |matching_work.response as mat_response,
       |matching_work.version as mat_version,
       |matching_work.created_at as mat_created_at
     """.stripMargin

  val Join =
    s"""
      |LEFT JOIN long_answer_work ON $Table.id = long_answer_work.work_id
      |LEFT JOIN short_answer_work ON $Table.id = short_answer_work.work_id
      |LEFT JOIN multiple_choice_work ON $Table.id = multiple_choice_work.work_id
      |LEFT JOIN ordering_work ON $Table.id = ordering_work.work_id
      |LEFT JOIN matching_work ON $Table.id = matching_work.work_id
     """.stripMargin

  def JoinMatchVersion(table: String = Table): String = {
    s"""
      |LEFT JOIN long_answer_work ON $table.id = long_answer_work.work_id
      |LEFT JOIN short_answer_work ON $table.id = short_answer_work.work_id
      |LEFT JOIN multiple_choice_work ON $table.id = multiple_choice_work.work_id
      |  AND $table.version = multiple_choice_work.version
      |LEFT JOIN ordering_work ON $table.id = ordering_work.work_id
      |  AND $table.version = ordering_work.version
      |LEFT JOIN matching_work ON $table.id = matching_work.work_id
      |  AND $table.version = matching_work.version
     """.stripMargin
    }

  // -- Select queries -----------------------------------------------------------------------------------------------
  val SelectForUserProject =
    s"""
        |SELECT ${CommonFieldsWithTable()}, $SpecificFields
        |FROM $Table
        |${JoinMatchVersion()}
        |INNER JOIN projects
        | ON projects.id = ?
        |INNER JOIN parts
        | ON parts.project_id = projects.id
        |INNER JOIN tasks
        | ON tasks.part_id = parts.id
        |WHERE $Table.user_id = ?
        | AND $Table.task_id = tasks.id
     """.stripMargin

  val SelectForTask =
    s"""
        |SELECT ${CommonFieldsWithTable()}, $SpecificFields
        |FROM $Table
        |${JoinMatchVersion()}
        |INNER JOIN tasks
        | ON tasks.id = ?
        |WHERE $Table.task_id = tasks.id
     """.stripMargin

  val SelectAllForUserTask =
    s"""
      |SELECT ${CommonFieldsWithTable()}, $SpecificFields
      |FROM $Table
      |$Join
      |WHERE $Table.user_id = ?
      | AND $Table.task_id = ?
      |ORDER BY $SpecificOrderBy
     """.stripMargin

  val FindByStudentTask =
    s"""
       |SELECT ${CommonFieldsWithTable()}, $SpecificFields
       |FROM $Table
       |${JoinMatchVersion()}
       |WHERE user_id = ?
       |  AND task_id = ?
       |LIMIT 1
     """.stripMargin

  val FindByStudentTaskVersion =
    s"""
       |SELECT ${CommonFieldsWithTable()}, $SpecificFields
       |FROM $Table
       |$Join
       |WHERE user_id = ?
       |  AND task_id = ?
       |  AND (multiple_choice_work.version = ?
       |       OR ordering_work.version = ?
       |       OR matching_work.version = ?)
       |LIMIT 1
     """.stripMargin

  val FindById =
    s"""
      |SELECT ${CommonFieldsWithTable()}, $SpecificFields
      |FROM $Table
      |${JoinMatchVersion()}
      |WHERE id = ?
      |LIMIT 1
     """.stripMargin

  val FindByIdVersion =
  s"""
      |SELECT ${CommonFieldsWithTable()}, $SpecificFields
      |FROM $Table
      |$Join
      |WHERE id = ?
      | AND (multiple_choice_work.version = ?
      |      OR ordering_work.version = ?
      |      OR matching_work.version = ?)
      |LIMIT 1
     """.stripMargin

  // -- Insert queries  -------------------------------------------------------------------------------------------

  val Insert =
    s"""
       |INSERT INTO $Table ($CommonFields)
       |VALUES (?, ?, ?, 1, ?, ?, ?, ?)
       |RETURNING $CommonFields
     """.stripMargin

  def InsertIntoDocumentWork(table: String): String = {
    val response = table match {
      case "long_answer_work"  => "la_document_id"
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
    val (version, response, created) = table match {
      case "multiple_choice_work" => ("mc_version", "mc_response", "mc_created_at")
      case "ordering_work"        => ("ord_version", "ord_response", "ord_created_at")
      case "matching_work"        => ("mat_version", "mat_response", "mat_created_at")
    }
    s"""
       |WITH w AS ($Insert),
       |     x AS (INSERT INTO $table (work_id, version, response, created_at)
       |           SELECT w.id as work_id, w.version as version, ? as response, w.updated_at as created_at
       |           FROM w
       |           RETURNING *)
       |SELECT w.id, w.user_id, w.task_id, w.version as $version, w.is_complete, w.created_at, w.updated_at as $created, w.work_type, x.response as $response
       |FROM w, x
     """.stripMargin
  }

  // -- Update queries  -------------------------------------------------------------------------------------------

  val Update =
    s"""
       |UPDATE $Table
       |SET version = ?,
       |    is_complete = ?,
       |    updated_at = ?
       |WHERE id = ?
       |  AND version = ?
       |RETURNING *
     """.stripMargin

  def UpdateKeepLatestRevision(table: String): String = {
    val (version, response, and, updatedAt) = table match {
      case "long_answer_work"     => ("version", "x.document_id as la_document_id", "", "updated_at")
      case "short_answer_work"    => ("version", "x.document_id as sa_document_id", "", "updated_at")
      case "multiple_choice_work" => ("mc_version", "x.response as mc_response", s"AND $table.version = w.version", "mc_created_at")
      case "ordering_work"        => ("ord_version", "x.response as ord_response", s"AND $table.version = w.version", "ord_created_at")
      case "matching_work"        => ("mat_version", "x.response as mat_response", s"AND $table.version = w.version", "mat_created_at")
    }
    s"""
       |WITH w AS ($Update),
       |     x AS (SELECT *
       |           FROM $table, w
       |           WHERE work_id = w.id
       |            $and)
       |SELECT w.id, w.user_id, w.task_id, w.version as $version, $response, w.is_complete, w.work_type, w.created_at, w.updated_at as $updatedAt
       |FROM w, x
     """.stripMargin
  }

  def UpdateWithNewRevision(table: String): String = {
    val (version, response, updatedAt) = table match {
      case "multiple_choice_work" => ("mc_version", "mc_response", "mc_created_at")
      case "ordering_work"        => ("ord_version", "ord_response", "ord_created_at")
      case "matching_work"        => ("mat_version", "mat_response", "mat_created_at")
    }
    s"""
       |WITH w AS ($Update),
       |     x AS (INSERT INTO $table (work_id, version, response, created_at)
       |           SELECT w.id as work_id,
       |                  w.version as version,
       |                  ? as response,
       |                  created_at
       |           FROM w
       |           RETURNING *)
       |SELECT w.id, w.user_id, w.task_id, w.version as $version, x.response as $response, w.is_complete, w.work_type, w.created_at, w.updated_at as $updatedAt
       |FROM w,x
     """.stripMargin
  }

  // -- Delete queries -----------------------------------------------------------------------------------------------
  // NB: the delete queries should never be used unless you know what you're doing. Due to work revisioning, the
  //     proper way to "clear" a work is to create an empty revision.

  val DeleteWhere =
    s"""
      |long_answer_work.work_id = $Table.id
      | OR short_answer_work.work_id = $Table.id
      | OR (multiple_choice_work.work_id = $Table.id
      |     AND $Table.version = multiple_choice_work.version)
      | OR (ordering_work.work_id = $Table.id
      |     AND $Table.version = ordering_work.version)
      | OR (matching_work.work_id = $Table.id
      |     AND $Table.version = matching_work.version)
     """.stripMargin

  val DeleteAllRevisions =
    s"""
       |DELETE FROM $Table
       |USING
       |  long_answer_work,
       |  short_answer_work,
       |  multiple_choice_work,
       |  ordering_work,
       |  matching_work
       |WHERE id = ?
       | AND ($DeleteWhere)
       |RETURNING ${CommonFieldsWithTable()}, $SpecificFields
     """.stripMargin

  val DeleteAllForTask =
    s"""
       |DELETE FROM $Table
       |WHERE task_id = ?
     """.stripMargin


  /**
   * List the latest revision of work for each task in a project for a user.
   *
   * @param project
   * @param user
   * @return
   */
  override def list(user: User, project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Work]]] = {
    for {
      workList <- lift(queryList(SelectForUserProject, Seq[Any](project.id.bytes, user.id.bytes)))
      result   <- lift(serializedT(workList) {
        case documentWork: DocumentWork => {
          for  {
            document <- lift(documentRepository.find(documentWork.documentId))
            result = documentWork.copy(
              response = Some(document)
//              version = document.version
            )
          } yield result
        }
        case otherWorkTypes => lift(Future successful \/.right(otherWorkTypes))
      })
    } yield result
  }

  /**
   * List all revisions of a specific work for a user within a Task.
   *
   * @param task
   * @param user
   * @return
   */
  override def list(user: User, task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Either[DocumentWork, IndexedSeq[ListWork[_ >: Int with MatchingTask.Match]]]]] = {
    task match {
      case longAnswerTask: LongAnswerTask         => listDocumentWork(user, longAnswerTask).map(_.map { documentWork =>  Left(documentWork)})
      case shortAnswerTask: ShortAnswerTask       => listDocumentWork(user, shortAnswerTask).map(_.map { documentWork =>  Left(documentWork)})
      case multipleChoiceTask: MultipleChoiceTask => listListWork(user, multipleChoiceTask).map(_.map { listWork =>  Right(listWork)})
      case orderingTask: OrderingTask             => listListWork(user, orderingTask).map(_.map { listWork =>  Right(listWork)})
      case matchingTask: MatchingTask             => listListWork(user, matchingTask).map(_.map { listWork =>  Right(listWork)})
    }
  }

  /**
   * @see list(user: User, task: Task)
   *
   * @param user
   * @param task
   * @param conn
   * @return
   */
  private def listDocumentWork(user: User, task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, DocumentWork]] = {
   (for {
      work   <- lift(queryOne(SelectAllForUserTask, Seq[Any](user.id.bytes, task.id.bytes)))
      result <- { work match {
        case documentWork: DocumentWork => {
          val res: Future[\/[RepositoryError.Fail, DocumentWork]] = for {
            document  <- lift(documentRepository.find(documentWork.documentId))
            revisions <- lift(revisionRepository.list(document, toVersion = document.version))
            result    = documentWork.copy(
              response = Some(document.copy(revisions = revisions))
//              version = document.version
            )
          } yield result
          lift(res)
        }
        case _ => lift[DocumentWork](Future.successful(-\/(RepositoryError.NoResults)))
      }}
    } yield result).run
  }

  /**
   * @see list(user: User, task: Task)
   *
   * @param user
   * @param task
   * @param conn
   * @return
   */
  private def listListWork(user: User, task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[ListWork[_ >: Int with MatchingTask.Match]]]] = {
    (for {
      workList <- lift(queryList(SelectAllForUserTask, Seq[Any](user.id.bytes, task.id.bytes)))
      result   <- lift(Future.successful(workList match {
        case IndexedSeq(work, rest @ _ *) => {
          work match {
            case intListWork: IntListWork => \/.right(IndexedSeq(intListWork) ++ rest.asInstanceOf[IndexedSeq[IntListWork]])
            case matchListWork: MatchListWork => \/.right(IndexedSeq(matchListWork) ++ rest.asInstanceOf[IndexedSeq[MatchListWork]])
            case _ => \/.left(RepositoryError.NoResults)
          }
        }
        case _ => \/.left(RepositoryError.NoResults)
      }))
    } yield result).run
  }

  /**
   * List latest revisions of a specific work for all users within a Task.
   *
   * @param task
   * @return
   */
  override def list(task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Work]]] = {
    for {
      workList <- lift(queryList(SelectForTask, Seq[Any](task.id.bytes)))
      result <- lift(serializedT(workList){
        case documentWork: DocumentWork => {
          for  {
            document <- lift(documentRepository.find(documentWork.documentId))
            result = documentWork.copy(
              response = Some(document)
//              version = document.version
            )
          } yield result
        }
        case otherWorkTypes => lift(Future successful \/.right(otherWorkTypes))
      })
    } yield result
  }

  /**
   * Find the latest revision of a single work.
   *
   * @param workId
   * @return
   */
  override def find(workId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Work]] = {
    queryOne(FindById, Seq[Any](workId.bytes)).flatMap {
      case \/-(documentWork: DocumentWork) => documentRepository.find(documentWork.documentId).map {
        case \/-(document) => \/.right(documentWork.copy(
          response = Some(document)
//          version  = document.version
        ))
        case -\/(error: RepositoryError.Fail) => \/.left(error)
      }
      case otherWorkTypes => Future successful otherWorkTypes
    }
  }

  /**
   * Find a specific revision of a single work.
   *
   * @param workId
   * @return
   */
  override def find(workId: UUID, version: Long)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Work]] = {
    queryOne(FindById, Seq[Any](workId.bytes)).flatMap {
      case \/-(documentWork: DocumentWork) => documentRepository.find(documentWork.documentId, version).map {
        case \/-(document) => \/.right(documentWork.copy(
          response  = Some(document),
          version   = document.version,
          updatedAt = document.updatedAt
        ))
        case -\/(error: RepositoryError.Fail) => \/.left(error)
      }
      case \/-(otherWorkTypes) => lift(queryOne(FindByIdVersion, Seq[Any](workId.bytes, version, version, version)))
      case -\/(error: RepositoryError.Fail) => Future successful \/.left(error)
    }
  }

  /**
   * Find the latest revision of a single work for a user within a Task.
   *
   * @param task
   * @param user
   * @return
   */
  override def find(user: User, task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Work]] = {
    queryOne(FindByStudentTask, Seq[Any](user.id.bytes, task.id.bytes)).flatMap {
      case \/-(documentWork: DocumentWork) => documentRepository.find(documentWork.documentId).map {
        case \/-(document) => \/.right(documentWork.copy(
          version   = document.version,
          response  = Some(document),
          updatedAt = document.updatedAt
        ))
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
    queryOne(FindByStudentTask, Seq[Any](user.id.bytes, task.id.bytes)).flatMap {
      case \/-(documentWork: DocumentWork) => documentRepository.find(documentWork.documentId, version).map {
        case \/-(document) => \/.right(documentWork.copy(
          version   = document.version,
          response  = Some(document),
          updatedAt = document.updatedAt
        ))
        case -\/(error: RepositoryError.Fail) => \/.left(error)
      }
      case \/-(otherWorkTypes) => lift(queryOne(FindByStudentTaskVersion, Seq[Any](user.id.bytes, task.id.bytes, version, version, version)))
      case -\/(error: RepositoryError.Fail) => Future successful \/.left(error)
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
      case specific: LongAnswerWork     => "long_answer_work"
      case specific: ShortAnswerWork    => "short_answer_work"
      case specific: MultipleChoiceWork => "multiple_choice_work"
      case specific: OrderingWork       => "ordering_work"
      case specific: MatchingWork       => "matching_work"
    }

    if (newRevision) {
      tableName match {
        case "long_answer_work" | "short_answer_work" => Future successful  \/.left(RepositoryError.BadParam("Adding new Revisions to a DocumentWork should be done in the Document Repository and Revision Repository"))
        case _ => queryOne(UpdateWithNewRevision(tableName), Seq[Any](
          work.version +1,
          work.isComplete,
          new DateTime,
          work.id.bytes,
          work.version,
          work match {
            case specific: MultipleChoiceWork => specific.response
            case specific: OrderingWork => specific.response
            case specific: MatchingWork => specific.response.asInstanceOf[IndexedSeq[MatchingTask.Match]].map { item => IndexedSeq(item.left, item.right)}
            case specific: DocumentWork => throw new Exception("Tried to use a list-work table on a document-work entity. Epic fail.")
          }
        ))
      }
    }
    else {
      queryOne(UpdateKeepLatestRevision(tableName), Seq[Any](
        work.version,
        work.isComplete,
        new DateTime,
        work.id.bytes,
        work.version
      )).flatMap {
        case \/-(documentWork: DocumentWork) => documentRepository.find(documentWork.documentId, documentWork.version).map {
         case \/-(document) => \/.right(documentWork.copy(
           response  = Some(document)
         ))
         case -\/(error: RepositoryError.Fail) => \/.left(error)
        }
        case \/-(otherWorkTypes) => Future successful \/.right(otherWorkTypes)
        case -\/(error: RepositoryError.Fail) => Future successful \/.left(error)
      }
    }
  }



  /**
   *  Delete all revisions of a work.
   *
   * @param work
   * @param conn
   * @return
   */
  override def delete(work: Work)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Work]] = {
    queryOne(DeleteAllRevisions, Seq[Any](work.id.bytes)).flatMap {
      case \/-(documentWork: DocumentWork) => documentRepository.find(documentWork.documentId).map {
        case \/-(document) => \/.right(documentWork.copy(
          // TODO - versions should match, DocumentService.push should update a Work version  and updatedAt field also
//          version = document.version,
          response = Some(document)
        ))
        case -\/(error: RepositoryError.Fail) => \/.left(error)
      }
      case otherWorkTypes => Future successful otherWorkTypes
    }
  }

  /**
   * Delete all work for a given task.
   * Delete all revisions for MultipleChoice, Ordering, Matcing works and
   * for DocumentWork we delete only in Work table and don't touch Documents table,
   * that should be done in DocumentRepository
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
