package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.fail._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks.{MatchingTask, Task}
import ca.shiftfocus.krispii.core.models.work._
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.{ResultSet, RowData, Connection}
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import org.joda.time.DateTime
import scala.concurrent.Future
import scalaz.{\/, -\/, \/-}

trait WorkRepositoryPostgresComponent extends WorkRepositoryComponent {
  self: PostgresDB =>

  val workRepository: WorkRepository = new WorkRepositoryPostgres

  private class WorkRepositoryPostgres extends WorkRepository {

    // -- Common query components --------------------------------------------------------------------------------------

    val Select =
      s"""
         |SELECT work.id as id,
         |       work.user_id as user_id,
         |       work.task_id as task_id,
         |       work.course_id as course_id,
         |       work.is_complete as is_complete,
         |       work.created_at as created_at,
         |       work.updated_at as updated_at,
         |       work.work_type as work_type,
         |       work.version as version,
         |       long_answer_work.document_id as long_answer_document_id,
         |       short_answer_work.document_id as short_answer_document_id,
         |       multiple_choice_work.answer as multiple_choice_answer,
         |       multiple_choice_work.version as multiple_choice_version,
         |       ordering_work.answer as ordering_answer,
         |       ordering_work.version as ordering_version,
         |       matching_work.answer as matching_answer,
         |       matching_work.version as matching_version
       """.stripMargin

    val From = "FROM work"

    val Join =
      s"""
         |LEFT JOIN long_answer_work ON work.id = long_answer_work.work_id
         |LEFT JOIN short_answer_work ON work.id = short_answer_work.work_id
         |LEFT JOIN multiple_choice_work ON work.id = multiple_choice_work.work_id
         |LEFT JOIN ordering_work ON work.id = ordering_work.work_id
         |LEFT JOIN matching_work ON work.id = matching_work.work_id
       """.stripMargin

    val JoinMatchVersion =
      s"""
         |LEFT JOIN long_answer_work ON work.id = long_answer_work.work_id AND work.version = long_answer_work.version
         |LEFT JOIN short_answer_work ON work.id = short_answer_work.work_id AND work.version = short_answer_work.version
         |LEFT JOIN multiple_choice_work ON work.id = multiple_choice_work.work_id AND work.version = multiple_choice_work.version
         |LEFT JOIN ordering_work ON work.id = ordering_work.work_id AND work.version = ordering_work.version
         |LEFT JOIN matching_work ON work.id = matching_work.work_id AND work.version = matching_work.version
       """.stripMargin

    // -- Select queries -----------------------------------------------------------------------------------------------

    val SelectAllForUserProject =
      s"""
         |$Select
         |$From, projects, parts, tasks
         |$Join
         |WHERE projects.id = ?
         |  AND user_id = ?
         |  AND course_id = ?
         |  AND parts.id = tasks.part_id
         |  AND projects.id = parts.project_id
         |  AND student_responses.task_id = tasks.id
         |  AND version = (SELECT MAX(version) FROM student_responses WHERE user_id= ? AND task_id=tasks.id)
       """.stripMargin

    val SelectAllForTask =
      s"""
         |$Select
         |$From, tasks
         |$JoinMatchVersion
         |WHERE work.task_id = tasks.id
       """.stripMargin

    val ListRevisionsById =
      s"""
         |$Select
         |$From
         |$Join
         |WHERE user_id = ?
         |  AND task_id = ?
         |  AND course_id = ?
       """.stripMargin

    val SelectByStudentTaskCourse =
      s"""
         |$Select
         |$From
         |$Join
         |WHERE user_id = ?
         |  AND task_id = ?
         |  AND course_id = ?
         |LIMIT 1
       """.stripMargin

    val SelectById =
      s"""
         |$Select
         |$From
         |$Join
         |WHERE id = ?
         |LIMIT 1
       """.stripMargin

    // -- Insert and Update  -------------------------------------------------------------------------------------------

    val Insert =
      s"""
         |INSERT INTO work (id, user_id, task_id, course_id, version, is_complete, created_at, updated_at, work_type)
         |VALUES (?, ?, ?, ?, 1, ?, ?, ?, ?)
         |RETURNING id, user_id, task_id, course_id, is_complete, created_at, updated_at
       """.stripMargin

    // TODO - We need RETURNING for every Work type
    def InsertIntoDocumentWork(table: String): String =
      s"""
         |WITH w AS (
         |  $Insert
         |)
         |INSERT INTO $table (work_id, document_id)
         |  SELECT w.id as work_id,
         |         ? as document_id
         |  FROM w
       """.stripMargin

    def InsertIntoVersionedWork(table: String): String =
      s"""
         |WITH w AS (
         |  $Insert
         |)
         |INSERT INTO $table (work_id, version, answer)
         |  SELECT work.id as work_id, work.version as version, ? as answer
         |  FROM w
       """

    val Update =
      s"""
         |UPDATE work
         |SET version = ?,
         |    is_complete = ?,
         |    updated_at = ?
         |WHERE user_id = ?
         |  AND task_id = ?
         |  AND course_id = ?
         |  AND version = ?
       """.stripMargin

    def UpdateKeepLatestRevision(table: String): String =
      s"""
         |WITH work AS (
         |  $Update
         |)
         |UPDATE $table
         |SET answer = ?
         |WHERE work_id = work.id
         |RETURNING id, user_id, task_id, course_id, version, answer, is_complete, created_at, updated_at
       """.stripMargin

    def UpdateWithNewRevision(table: String): String =
      s"""
         |WITH work AS (
         |  $Update
         |)
         |INSERT INTO $table (user_id, task_id, course_id, version, answer)
         |  SELECT work.user_id as user_id,
         |         work.task_id as task_id,
         |         work.course_id as course_id,
         |         ? as version,
         |         ? as answer
         |RETURNING user_id, task_id, course_id, revision, version, answer, is_complete, created_at, updated_at
       """.stripMargin

    // -- Delete -------------------------------------------------------------------------------------------------------
    // NB: the delete queries should never be used unless you know what you're doing. Due to work revisioning, the
    //     proper way to "clear" a work is to create an empty revision.

    val Delete =
      s"""
         |DELETE work
         |$From
         |$Join
         |WHERE user_id = ?
         |  AND task_id = ?
         |  AND course_id = ?
       """.stripMargin

    val DeleteRevision =
      s"""
         |DELETE work
         |$From
         |$Join
         |WHERE user_id = ?
         |  AND task_id = ?
         |  AND course_id = ?
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
     * List the latest revision of work for each task in a project.
     *
     * @param project
     * @param user
     * @return
     */
    override def list(user: User, project: Project): Future[\/[Fail, IndexedSeq[Work]]] = {
      db.pool.sendPreparedStatement(SelectAllForUserProject, Array[Any](
        project.id.bytes,
        user.id.bytes
      )).map {
        result => buildWorkList(result.rows)
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * List all revisions of a specific work.
     *
     * @param task
     * @param user
     * @return
     */
    override def list(user: User, task: Task): Future[\/[Fail, IndexedSeq[Work]]] = {
      db.pool.sendPreparedStatement(ListRevisionsById, Array[Any](
        task.id.bytes,
        user.id.bytes
      )).map {
        result => buildWorkList(result.rows)
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * List all revisions of a specific work.
     *
     * @param task
     * @return
     */
    override def list(task: Task): Future[\/[Fail, IndexedSeq[Work]]] = {
      db.pool.sendPreparedStatement(SelectAllForTask, Array[Any](
        task.id.bytes
      )).map {
        result => buildWorkList(result.rows)
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * Find the latest revision of a single work.
     *
     * @param workId
     * @return
     */
    override def find(workId: UUID): Future[\/[Fail, Work]] = {
      db.pool.sendPreparedStatement(SelectById, Array[Any](
        workId.bytes
      )).map {
        result => buildWork(result.rows)
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    // TODO - update ScalaDoc.
    /**
     * Find the latest revision of a single work.
     *
     * @param task
     * @param user
     * @param course
     * @return
     */
    override def find(user: User, task: Task): Future[\/[Fail, Work]] = {
      db.pool.sendPreparedStatement(SelectByStudentTaskCourse, Array[Any](
        user.id.bytes,
        task.id.bytes
      )).map {
        result => buildWork(result.rows)
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * Find a specific revision for a single work.
     *
     * @param task
     * @param user
     * @param course
     * @return
     */
    override def find(user: User, task: Task, version: Long): Future[\/[Fail, Work]] = {
      db.pool.sendPreparedStatement(SelectByStudentTaskCourse, Array[Any](
        user.id.bytes,
        task.id.bytes,
        version
      )).map {
        result => buildWork(result.rows)
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
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
    override def insert(work: Work)(implicit conn: Connection): Future[\/[Fail, Work]] = {
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
        case specific: MultipleChoiceWork => baseParams ++ Array[Any](Task.MultipleChoice, specific.answer)
        case specific: OrderingWork => baseParams ++ Array[Any](Task.Ordering, specific.answer)
        case specific: MatchingWork => baseParams ++ Array[Any](Task.Matching, specific.answer.asInstanceOf[IndexedSeq[MatchingTask.Match]].map { item => s"${item.left}:${item.right}"})
      }

      conn.sendPreparedStatement(query, params).map {
        result => buildWork(result.rows)
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
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
    override def update(work: Work)(implicit conn: Connection): Future[\/[Fail, Work]] = update(work, false)
    override def update(work: Work, newRevision: Boolean)(implicit conn: Connection): Future[\/[Fail, Work]] = {
      val tableName = work match {
        case specific: LongAnswerWork => "long_answer_work"
        case specific: ShortAnswerWork => "short_answer_work"
        case specific: MultipleChoiceWork => "multiple_choice_work"
        case specific: OrderingWork => "ordering_work"
        case specific: MatchingWork => "matching_work"
      }

      val future = if (newRevision) {
        conn.sendPreparedStatement(UpdateWithNewRevision(tableName), Array[Any](
          work.version +1,
          work.isComplete,
          new DateTime,
          work.studentId.bytes,
          work.taskId.bytes,
          work.version,
          work.version +1,
          work.answer
        ))
      }
      else {
        conn.sendPreparedStatement(UpdateKeepLatestRevision(tableName), Array[Any](
          work.version,
          work.isComplete,
          new DateTime,
          work.studentId.bytes,
          work.taskId.bytes,
          work.version,
          work.answer
        ))
      }

      future.map {
        result => buildWork(result.rows)
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    // TODO - as in Work table ID field is primary (unique constraint and a not-null constraint), how we can have two works with the same id but different versions?
    /**
     * Delete a specific revision of a work.
     *
     * @param work
     * @param conn
     * @return
     */
    override def delete(work: Work, thisRevisionOnly: Boolean = false)(implicit conn: Connection): Future[\/[Fail, Work]] = {
      val fDelete = if (thisRevisionOnly) {
        conn.sendPreparedStatement(DeleteRevision, Array[Any](
          work.studentId.bytes,
          work.taskId.bytes,
          work.version
        ))
      }
      else {
        conn.sendPreparedStatement(Delete, Array[Any](
          work.studentId.bytes,
          work.taskId.bytes
        ))
      }

      fDelete.map {
        result => {
          if (result.rowsAffected == 0) {
            -\/(GenericFail("No rows were modified"))
          } else {
            \/-(work)
          }
        }
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
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
    override def delete(task: Task)(implicit conn: Connection): Future[\/[Fail, Work]] = {
      for {
        tasks <- lift(list(task))
        deletedWorks <- lift(conn.sendPreparedStatement(DeleteAllForTask, Array[Any](task.id.bytes)).map({
          result =>
            if (result.rowsAffected == tasks.length) \/-(tasks)
            else -\/(GenericFail("Failed to delete all tasks"))
        }))
      } yield deletedWorks

      conn.sendPreparedStatement(DeleteRevision, Array[Any](task.id.bytes)).map {
        result => {
          if (result.rowsAffected == 0) {
            -\/(GenericFail("No rows were modified"))
          } else {
            \/-(work)
          }
        }
      }.recover {
        case exception: Throwable => -\/(ExceptionalFail("Unexpected exception", exception))
      }
    }

    /**
     * Transform result rows into a single work.
     *
     * @param maybeResultSet
     * @return
     */
    private def buildWork(maybeResultSet: Option[ResultSet]): \/[Fail, Work] = {
      try {
        maybeResultSet match {
          case Some(resultSet) => resultSet.headOption match {
            case Some(firstRow) => \/-(Work(firstRow))
            case None => -\/(NoResults("The query was successful but ResultSet was empty."))
          }
          case None => -\/(NoResults("The query was successful but no ResultSet was returned."))
        }
      }
      catch {
        case exception: NoSuchElementException => -\/(ExceptionalFail(s"Invalid data: could not build a Work from the row returned.", exception))
      }
    }

    /**
     * Converts an optional result set into works list
     *
     * @param maybeResultSet
     * @return
     */
    private def buildWorkList(maybeResultSet: Option[ResultSet]): \/[Fail, IndexedSeq[Work]] = {
      try {
        maybeResultSet match {
          case Some(resultSet) => \/-(resultSet.map(Work.apply))
          case None => -\/(NoResults("The query was successful but no ResultSet was returned."))
        }
      }
      catch {
        case exception: NoSuchElementException => -\/(ExceptionalFail(s"Invalid data: could not build a Work List from the rows returned.", exception))
      }
    }

  }
}
