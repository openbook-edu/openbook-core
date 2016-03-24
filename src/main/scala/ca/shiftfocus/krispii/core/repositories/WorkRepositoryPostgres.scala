package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks.Task._
import ca.shiftfocus.krispii.core.models.tasks._
import ca.shiftfocus.krispii.core.models.work._
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import java.util.UUID
import com.github.mauricio.async.db.{ ResultSet, RowData, Connection }
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.DateTime
import scala.concurrent.Future
import scala.util.{ Success, Try }
import scalaz.{ \/, -\/, \/- }

class WorkRepositoryPostgres(
    val documentRepository: DocumentRepository,
    val revisionRepository: RevisionRepository
) extends WorkRepository with PostgresRepository[Work] {

  override val entityName = "Work"

  override def constructor(row: RowData): Work = {
    row("work_type").asInstanceOf[Int] match {
      case Task.Document => constructDocumentWork(row)
      case Task.Question => constructQuestionWork(row)
      case Task.Media => constructMediaWork(row)
      case _ => throw new Exception("Retrieved an unknown task type from the database. You dun messed up now!")
    }
  }

  private def constructDocumentWork(row: RowData): DocumentWork = {
    DocumentWork(
      id = row("id").asInstanceOf[UUID],
      studentId = row("user_id").asInstanceOf[UUID],
      taskId = row("task_id").asInstanceOf[UUID],
      documentId = row("document_id").asInstanceOf[UUID],
      version = row("version").asInstanceOf[Long],
      response = None,
      isComplete = row("is_complete").asInstanceOf[Boolean],
      createdAt = row("created_at").asInstanceOf[DateTime],
      updatedAt = row("updated_at").asInstanceOf[DateTime]
    )
  }

  private def constructQuestionWork(row: RowData): QuestionWork = {
    val version = Try(row("q_version")).toOption match {
      case Some(version) => version.asInstanceOf[Long]
      case _ => row("version").asInstanceOf[Long]
    }

    val updated_at = Try(row("q_created_at")).toOption match {
      case Some(created_at) => created_at.asInstanceOf[DateTime]
      case _ => row("updated_at").asInstanceOf[DateTime]
    }

    QuestionWork(
      id = row("id").asInstanceOf[UUID],
      studentId = row("user_id").asInstanceOf[UUID],
      taskId = row("task_id").asInstanceOf[UUID],
      version = version,
      response = Json.parse(row("answers").asInstanceOf[String]).as[Answers],
      isComplete = row("is_complete").asInstanceOf[Boolean],
      createdAt = row("created_at").asInstanceOf[DateTime],
      updatedAt = updated_at
    )
  }

  private def constructMediaWork(row: RowData): MediaWork = {
    val version = Try(row("m_version")).toOption match {
      case Some(version) => version.asInstanceOf[Long]
      case _ => row("version").asInstanceOf[Long]
    }

    val updated_at = Try(row("m_created_at")).toOption match {
      case Some(created_at) => created_at.asInstanceOf[DateTime]
      case _ => row("updated_at").asInstanceOf[DateTime]
    }

    MediaWork(
      id = row("id").asInstanceOf[UUID],
      studentId = row("user_id").asInstanceOf[UUID],
      taskId = row("task_id").asInstanceOf[UUID],
      version = version,
      fileData = Json.parse(row("file_data").asInstanceOf[String]).as[MediaAnswer],
      isComplete = row("is_complete").asInstanceOf[Boolean],
      createdAt = row("created_at").asInstanceOf[DateTime],
      updatedAt = updated_at
    )
  }

  // -- Common query components --------------------------------------------------------------------------------------

  val Table = "work"
  val CommonFields = "id, user_id, task_id, version, is_complete, created_at, updated_at, work_type"
  def CommonFieldsWithTable(table: String = Table): String = {
    CommonFields.split(", ").map({ field => s"${table}." + field }).mkString(", ")
  }

  val QMarks = "?, ?, ?, ?, ?, ?, ?, ?"

  object LastWork {
    val SpecificFields =
      """
        |document_work.document_id as document_id,
        |question_work.answers as answers,
        |media_work.file_data as file_data
      """.stripMargin

    val Join =
      s"""
         |LEFT JOIN document_work ON $Table.id = document_work.work_id
         |LEFT JOIN question_work ON $Table.id = question_work.work_id
         |LEFT JOIN media_work ON $Table.id = media_work.work_id
     """.stripMargin
  }

  object AllWork {
    val SpecificFields =
      """
        |document_work.document_id as document_id,
        |question_work_answers.answers as answers,
        |question_work_answers.version as q_version,
        |question_work_answers.created_at as q_created_at,
        |media_work_data.file_data as file_data,
        |media_work_data.version as m_version,
        |media_work_data.created_at as m_created_at
      """.stripMargin

    val Join =
      s"""
         |LEFT JOIN document_work ON $Table.id = document_work.work_id
         |LEFT JOIN question_work_answers ON $Table.id = question_work_answers.work_id
         |LEFT JOIN media_work_data ON $Table.id = media_work_data.work_id
     """.stripMargin
  }

  // -- Select queries -----------------------------------------------------------------------------------------------
  val SelectForUserProject =
    s"""
        |SELECT ${CommonFieldsWithTable()}, ${LastWork.SpecificFields}
        |FROM $Table
        |${LastWork.Join}
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
        |SELECT ${CommonFieldsWithTable()}, ${LastWork.SpecificFields}
        |FROM $Table
        |${LastWork.Join}
        |INNER JOIN tasks
        | ON tasks.id = ?
        |WHERE $Table.task_id = tasks.id
     """.stripMargin

  val SelectAllForUserTask =
    s"""
      |SELECT ${CommonFieldsWithTable()}, ${AllWork.SpecificFields}
      |FROM $Table
      |${AllWork.Join}
      |WHERE $Table.user_id = ?
      | AND $Table.task_id = ?
     """.stripMargin

  // TODO - not used
  //  val SelectAllQuestionWorksForTask =
  //    s"""
  //       |SELECT ${CommonFieldsWithTable()}, question_work_answers.answers AS answers
  //       |FROM work
  //       |INNER JOIN question_work ON work.id = question_work.work_id
  //       |INNER JOIN question_work_answers ON work.id = question_work_answers.work_id
  //       |WHERE work.user_id = ?
  //       |  AND work.task_id = ?
  //     """.stripMargin

  val FindByStudentTask =
    s"""
       |SELECT ${CommonFieldsWithTable()}, ${LastWork.SpecificFields}
       |FROM $Table
       |${LastWork.Join}
       |WHERE user_id = ?
       |  AND task_id = ?
       |LIMIT 1
     """.stripMargin

  val FindByStudentTaskVersionQuestion =
    s"""
       |SELECT ${CommonFieldsWithTable()},${AllWork.SpecificFields}
       |FROM $Table
       |${AllWork.Join}
       |WHERE user_id = ?
       |  AND task_id = ?
       |  AND question_work_answers.version = ?
       |LIMIT 1
     """.stripMargin

  val FindByStudentTaskVersionMedia =
    s"""
       |SELECT ${CommonFieldsWithTable()},${AllWork.SpecificFields}
       |FROM $Table
       |${AllWork.Join}
       |WHERE user_id = ?
       |  AND task_id = ?
       |  AND media_work_data.version = ?
       |LIMIT 1
     """.stripMargin

  val FindById =
    s"""
      |SELECT ${CommonFieldsWithTable()}, ${LastWork.SpecificFields}
      |FROM $Table
      |${LastWork.Join}
      |WHERE id = ?
      |LIMIT 1
     """.stripMargin

  val FindByIdVersionQuestion =
    s"""
      |SELECT ${CommonFieldsWithTable()}, ${AllWork.SpecificFields}
      |FROM $Table
      |${AllWork.Join}
      |WHERE id = ?
      | AND question_work_answers.version = ?
      |LIMIT 1
     """.stripMargin

  val FindByIdVersionMedia =
    s"""
      |SELECT ${CommonFieldsWithTable()}, ${AllWork.SpecificFields}
      |FROM $Table
      |${AllWork.Join}
      |WHERE id = ?
      | AND media_work_data.version = ?
      |LIMIT 1
     """.stripMargin

  // -- Insert queries  -------------------------------------------------------------------------------------------

  val Insert =
    s"""
       |INSERT INTO $Table ($CommonFields)
       |VALUES (?, ?, ?, 1, ?, ?, ?, ?)
       |RETURNING $CommonFields
     """.stripMargin

  val InsertIntoDocumentWork = {
    s"""
       |WITH w AS ($Insert),
       |     x AS (INSERT INTO document_work (work_id, document_id)
       |           SELECT w.id as work_id, ? as document_id
       |           FROM w
       |           RETURNING *)
       |SELECT w.id, w.user_id, w.task_id, w.version, w.is_complete, w.created_at, w.updated_at, w.work_type, x.document_id
       |FROM w, x
     """.stripMargin
  }

  val InsertIntoQuestionWork = {
    s"""
       |WITH w AS ($Insert),
       |     x AS (INSERT INTO question_work (work_id, answers)
       |           SELECT w.id as work_id, '{}' as answers
       |           FROM w
       |           RETURNING *)
       |SELECT w.id, w.user_id, w.task_id, w.version, w.is_complete, w.created_at, w.updated_at, w.work_type, x.answers
       |FROM w, x
     """.stripMargin
  }

  val InsertIntoMediaWork = {
    s"""
       |WITH w AS ($Insert),
       |     x AS (INSERT INTO media_work (work_id, file_data)
       |           SELECT w.id as work_id, ? as file_data
       |           FROM w
       |           RETURNING *)
       |SELECT w.id, w.user_id, w.task_id, w.version, w.is_complete, w.created_at, w.updated_at, w.work_type, x.file_data
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

  val UpdateDocumentWork = {
    s"""
       |WITH w AS ($Update),
       |     x AS (SELECT *
       |           FROM document_work, w
       |           WHERE work_id = w.id)
       |SELECT w.id, w.user_id, w.task_id, w.version, x.document_id, w.is_complete, w.work_type, w.created_at, w.updated_at
       |FROM w, x
     """.stripMargin
  }

  val UpdateQuestionWork = {
    s"""
       |WITH w AS ($Update),
       |     x AS (UPDATE question_work SET answers = ? WHERE work_id = ? RETURNING *),
       |     y AS (INSERT INTO question_work_answers (work_id, version, answers, created_at)
       |           SELECT w.id as work_id,
       |                  w.version as version,
       |                  x.answers as answers,
       |                  w.updated_at as created_at
       |           FROM w, x
       |           RETURNING *)
       |SELECT w.id, w.user_id, w.task_id, w.version, x.answers, w.is_complete, w.work_type, w.created_at, w.updated_at
       |FROM w,x,y
     """.stripMargin
  }

  val UpdateMediaWork = {
    s"""
       |WITH w AS ($Update),
       |     x AS (UPDATE media_work SET file_data = ? WHERE work_id = ? RETURNING *),
       |     y AS (INSERT INTO media_work_data (work_id, version, file_data, created_at)
       |           SELECT w.id as work_id,
       |                  w.version as version,
       |                  x.file_data as file_data,
       |                  w.updated_at as created_at
       |           FROM w, x
       |           RETURNING *)
       |SELECT w.id, w.user_id, w.task_id, w.version, x.file_data, w.is_complete, w.work_type, w.created_at, w.updated_at
       |FROM w,x,y
     """.stripMargin
  }

  // -- Delete queries -----------------------------------------------------------------------------------------------
  // NB: the delete queries should never be used unless you know what you're doing. Due to work revisioning, the
  //     proper way to "clear" a work is to create an empty revision.

  val DeleteAllRevisions =
    s"""
       |DELETE FROM $Table
       |USING
       |  document_work,
       |  question_work,
       |  media_work
       |WHERE id = ?
       | AND (document_work.work_id = $Table.id
       |      OR question_work.work_id = $Table.id
       |      OR media_work.work_id = $Table.id)
       |RETURNING ${CommonFieldsWithTable()}, ${LastWork.SpecificFields}
     """.stripMargin

  val DeleteAllForTask =
    s"""
       |DELETE FROM $Table
       |WHERE task_id = ?
     """.stripMargin

  // -- Answer queries ---------

  // TODO not used
  //  val SelectLatestAnswer =
  //    s"""
  //       |SELECT
  //     """.stripMargin

  /**
   * List the latest revision of work for each task in a project for a user.
   *
   * @param project
   * @param user
   * @return
   */
  override def list(user: User, project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Work]]] = {
    for {
      workList <- lift(queryList(SelectForUserProject, Seq[Any](project.id, user.id)))
      result <- lift(serializedT(workList) {
        case documentWork: DocumentWork => {
          for {
            document <- lift(documentRepository.find(documentWork.documentId))
            result = documentWork.copy(
              response = Some(document),
              version = document.version
            )
          } yield result
        }
        case questionWork: QuestionWork => lift(Future successful \/.right(questionWork))
        case mediaWork: MediaWork => lift(Future successful \/.right(mediaWork))
      })
    } yield result
  }

  /**
   * TODO - not used, will list revisions by each work type
   *
   * List all revisions of a specific work for a user within a Task.
   *
   * @param task
   * @param user
   * @return
   */
  override def list(user: User, task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Either[DocumentWork, IndexedSeq[QuestionWork]]]] = { // scalastyle: ignore
    task match {
      case longAnswerTask: DocumentTask => listDocumentWork(user, longAnswerTask).map(_.map { documentWork => Left(documentWork) })
      case questionTask: QuestionTask => listQuestionWork(user, questionTask).map(_.map { questionWork => Right(questionWork) })
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
  private def listDocumentWork(user: User, task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, DocumentWork]] = { // scalastyle:ignore
    (for {
      work <- lift(queryOne(SelectAllForUserTask, Seq[Any](user.id, task.id)))
      result <- {
        work match {
          case documentWork: DocumentWork => {
            val res: Future[\/[RepositoryError.Fail, DocumentWork]] = for {
              document <- lift(documentRepository.find(documentWork.documentId))
              revisions <- lift(revisionRepository.list(document, toVersion = document.version))
              result = documentWork.copy(
                response = Some(document.copy(revisions = revisions)),
                version = document.version
              )
            } yield result
            lift(res)
          }
          case _ => lift[DocumentWork](Future.successful {
            -\/(RepositoryError.NoResults(s"Could not find document work for user ${user.id.toString} for task ${task.id.toString}"))
          })
        }
      }
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
  private def listQuestionWork(user: User, task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[QuestionWork]]] = { // scalastyle:ignore
    for {
      questionWorkList <- lift(queryList(SelectAllForUserTask, Seq[Any](user.id, task.id)).map(_.map { workList =>
        workList.map {
          case questionWork: QuestionWork => questionWork
          case mediaWork: MediaWork => throw new Exception("Somehow instantiated a MediaWork when selecting QuestionWork")
          case documentWork: DocumentWork => throw new Exception("Somehow instantiated a DocumentWork when selecting QuestionWork")
        }
      }))
      result <- lift(Future successful (if (questionWorkList.nonEmpty) {
        \/-(questionWorkList)
      }
      else {
        -\/(RepositoryError.NoResults(s"Could not find question work for user ${user.id.toString} for task ${task.id.toString}"))
      }))
    } yield result
  }

  /**
   * @see list(user: User, task: Task)
   *
   * @param user
   * @param task
   * @param conn
   * @return
   */
  private def listMediaWork(user: User, task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[MediaWork]]] = { // scalastyle:ignore
    for {
      mediaWorkList <- lift(queryList(SelectAllForUserTask, Seq[Any](user.id, task.id)).map(_.map { workList =>
        workList.map {
          case mediaWork: MediaWork => mediaWork
          case questionWork: QuestionWork => throw new Exception("Somehow instantiated a QuestionWork when selecting MediaWork")
          case documentWork: DocumentWork => throw new Exception("Somehow instantiated a DocumentWork when selecting QuestionWork")
        }
      }))
      result <- lift(Future successful (if (mediaWorkList.nonEmpty) {
        \/-(mediaWorkList)
      }
      else {
        -\/(RepositoryError.NoResults(s"Could not find question work for user ${user.id.toString} for task ${task.id.toString}"))
      }))
    } yield result
  }

  /**
   * List latest revisions of a specific work for all users within a Task.
   *
   * @param task
   * @return
   */
  override def list(task: Task)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Work]]] = {
    for {
      workList <- lift(queryList(SelectForTask, Seq[Any](task.id)))
      result <- lift(serializedT(workList) {
        case documentWork: DocumentWork => {
          for {
            document <- lift(documentRepository.find(documentWork.documentId))
            result = documentWork.copy(
              response = Some(document),
              version = document.version
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
    queryOne(FindById, Seq[Any](workId)).flatMap {
      case \/-(documentWork: DocumentWork) => documentRepository.find(documentWork.documentId).map {
        case \/-(document) => \/.right(documentWork.copy(
          response = Some(document),
          version = document.version,
          updatedAt = getLatestDate(documentWork.updatedAt, document.updatedAt)
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
    queryOne(FindById, Seq[Any](workId)).flatMap {
      case \/-(documentWork: DocumentWork) => documentRepository.find(documentWork.documentId, version).map {
        case \/-(document) => \/.right(documentWork.copy(
          response = Some(document),
          version = document.version,
          // As it is specific revision, we get document.updatedAt
          updatedAt = document.updatedAt
        ))
        case -\/(error: RepositoryError.Fail) => \/.left(error)
      }
      case \/-(questionWork: QuestionWork) => lift(queryOne(FindByIdVersionQuestion, Seq[Any](workId, version)))
      case \/-(mediaWork: MediaWork) => lift(queryOne(FindByIdVersionMedia, Seq[Any](workId, version)))
      case \/-(otherWorkTypes) => Future successful \/.left(RepositoryError.BadParam("Work type is unknown"))
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
    queryOne(FindByStudentTask, Seq[Any](user.id, task.id)).flatMap {
      case \/-(documentWork: DocumentWork) => documentRepository.find(documentWork.documentId).map {
        case \/-(document) => \/.right(documentWork.copy(
          version = document.version,
          response = Some(document),
          updatedAt = getLatestDate(documentWork.updatedAt, document.updatedAt)
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
    queryOne(FindByStudentTask, Seq[Any](user.id, task.id)).flatMap {
      case \/-(documentWork: DocumentWork) => documentRepository.find(documentWork.documentId, version).map {
        case \/-(document) => \/.right(documentWork.copy(
          version = document.version,
          response = Some(document),
          // As it is specific revision, we get document.updatedAt
          updatedAt = document.updatedAt
        ))
        case -\/(error: RepositoryError.Fail) => \/.left(error)
      }
      case \/-(questionWork: QuestionWork) => lift(queryOne(FindByStudentTaskVersionQuestion, Seq[Any](user.id, task.id, version)))
      case \/-(mediaWork: MediaWork) => lift(queryOne(FindByStudentTaskVersionMedia, Seq[Any](user.id, task.id, version)))
      case \/-(otherWorkTypes) => Future successful \/.left(RepositoryError.BadParam("Work type is unknown"))
      case -\/(error: RepositoryError.Fail) => Future successful \/.left(error)
    }
  }

  /**
   * When we update a LongAnswer or a ShortAnswer work without creating new revision, we also update updatedAt field,
   * but the related document updatedAt field remains the same. So when we try to find a LongAnswer or a ShortAnswer work,
   * we should verify what is the latest updatedAt field value.
   *
   * @param date1
   * @param date2
   * @return
   */
  private def getLatestDate(date1: DateTime, date2: DateTime): DateTime = {
    if (date1.getMillis >= date2.getMillis) {
      date1
    }
    else {
      date2
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
      case specific: DocumentWork => InsertIntoDocumentWork
      case specific: QuestionWork => InsertIntoQuestionWork
      case specific: MediaWork => InsertIntoMediaWork
    }

    val baseParams = Seq[Any](
      work.id,
      work.studentId,
      work.taskId,
      work.isComplete,
      new DateTime,
      new DateTime
    )

    val params: Seq[Any] = work match {
      case specific: DocumentWork => baseParams ++ Array[Any](Task.Document, specific.documentId)
      case specific: QuestionWork => baseParams ++ Array[Any](Task.Question)
      case specific: MediaWork => baseParams ++ Array[Any](Task.Media, Json.toJson(specific.fileData))
    }

    queryOne(query, params)
  }

  /**
   * Update a work.
   *
   * @param work the work to be updated
   * @param conn the database connection to update the work on
   * @return
   */
  override def update(work: Work)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Work]] = work match {
    case documentWork: DocumentWork => updateDocumentWork(documentWork)
    case questionWork: QuestionWork => updateQuestionWork(questionWork)
    case mediaWork: MediaWork => updateMediaWork(mediaWork)
  }

  private def updateDocumentWork(work: DocumentWork)(implicit conn: Connection): Future[\/[RepositoryError.Fail, DocumentWork]] = {
    queryOne(UpdateDocumentWork, Seq[Any](
      1L,
      work.isComplete,
      new DateTime,
      work.id,
      1L
    )).flatMap {
      case \/-(documentWork: DocumentWork) => documentRepository.find(documentWork.documentId, documentWork.version).map {
        case \/-(document) => \/.right(documentWork.copy(
          response = Some(document),
          version = document.version
        ))
        case -\/(error: RepositoryError.Fail) => \/.left(error)
      }
      case \/-(otherWorkTypes) => Future successful \/.left(RepositoryError.DatabaseError("Somehow instantiated a QuestionWork when selecting a DocumentWork"))
      case -\/(error: RepositoryError.Fail) => Future successful \/.left(error)
    }
  }

  private def updateQuestionWork(work: QuestionWork)(implicit conn: Connection): Future[\/[RepositoryError.Fail, QuestionWork]] = {
    queryOne(UpdateQuestionWork, Seq[Any](
      work.version + 1,
      work.isComplete,
      new DateTime,
      work.id,
      work.version,
      Json.toJson(work.response),
      work.id
    )).map(_.map(_.asInstanceOf[QuestionWork]))
  }

  private def updateMediaWork(work: MediaWork)(implicit conn: Connection): Future[\/[RepositoryError.Fail, MediaWork]] = {
    queryOne(UpdateMediaWork, Seq[Any](
      work.version + 1,
      work.isComplete,
      new DateTime,
      work.id,
      work.version,
      Json.toJson(work.fileData),
      work.id
    )).map(_.map(_.asInstanceOf[MediaWork]))
  }

  /**
   *  Delete all revisions of a work.
   *
   * @param work
   * @param conn
   * @return
   */
  override def delete(work: Work)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Work]] = {
    queryOne(DeleteAllRevisions, Seq[Any](work.id)).flatMap {
      case \/-(documentWork: DocumentWork) => documentRepository.find(documentWork.documentId).map {
        case \/-(document) => \/.right(documentWork.copy(
          // TODO - versions should match, DocumentService.push should update a Work version  and updatedAt field also
          version = document.version,
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
      deletedWorks <- lift(queryNumRows(DeleteAllForTask, Seq[Any](task.id))(_ > 0))
    } yield works).run
  }
}
