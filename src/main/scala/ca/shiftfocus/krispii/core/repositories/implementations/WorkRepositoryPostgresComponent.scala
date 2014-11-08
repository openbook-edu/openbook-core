package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import com.github.mauricio.async.db.{RowData, Connection}
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks.{MatchingTask, Task}
import ca.shiftfocus.krispii.core.models.work._
import org.joda.time.DateTime
import scala.concurrent.Future

trait WorkRepositoryPostgresComponent extends WorkRepositoryComponent {
  self: PostgresDB =>

  val workRepository: WorkRepository = new WorkRepositoryPostgres

  private class WorkRepositoryPostgres extends WorkRepository {

    // -- Common query components --------------------------------------------------------------------------------------

    val Select =
      s"""
         |SELECT work.student_id, work.task_id, work.section_id, work.revision
         |       work.is_complete, work.created_at, work.updated_at
         |       long_answer_work.answer,
         |       short_answer_work.answer,
         |       multiple_choice_work.answer,
         |       ordering_work.answer,
         |       matching_work.answer
       """.stripMargin

    val From = "FROM work"

    val Join =
      s"""
         |LEFT JOIN long_answer_work ON work.id = long_answer_work.task_id
         |LEFT JOIN short_answer_work ON work.id = short_answer_work.task_id
         |LEFT JOIN multiple_choice_work ON work.id = multiple_choice_work.task_id
         |LEFT JOIN ordering_work ON work.id = ordering_work.task_id
         |LEFT JOIN matching_work ON work.id = matching_work.task_id
       """.stripMargin

    // -- Select queries -----------------------------------------------------------------------------------------------

    val ListLatestByProjectForUser =
      s"""
         |$Select
         |$From, projects, parts, tasks
         |$Join
         |WHERE projects.id = ?
         |  AND user_id = ?
         |  AND section_id = ?
         |  AND parts.id = tasks.part_id
         |  AND projects.id = parts.project_id
         |  AND student_responses.task_id = tasks.id
         |  AND student_responses.status = 1
         |  AND revision = (SELECT MAX(revision) FROM student_responses WHERE user_id= ? AND task_id=tasks.id)
       """.stripMargin

    val ListRevisionsById =
      s"""
         |$Select
         |$From
         |$Join
         |WHERE user_id = ?
         |  AND task_id = ?
         |  AND section_id = ?
       """.stripMargin

    val SelectLatestById =
      s"""
         |$Select
         |$From
         |$Join
         |WHERE user_id = ?
         |  AND task_id = ?
         |  AND section_id = ?
         |  AND revision = (SELECT MAX(revision) FROM student_responses WHERE user_id = ? AND task_id = ? AND section_id = ?)
         |LIMIT 1
       """.stripMargin

    val SelectById =
      s"""
         |$Select
         |$From
         |$Join
         |WHERE user_id = ?
         |  AND task_id = ?
         |  AND section_id = ?
         |  AND revision = ?
         |LIMIT 1
       """.stripMargin

    // -- Insert and Update  -------------------------------------------------------------------------------------------

    val Insert =
      s"""
         |INSERT INTO work (student_id, task_id, section_id, version, is_complete, created_at, updated_at
         |VALUES (?, ?, ?, 1, ?, ?, ?)
         |RETURNING student_id, task_id, section_id, version, is_complete, created_at, updated_at
       """.stripMargin

    def InsertIntoAny(table: String): String =
      s"""
         |WITH work AS (
         |  $Insert
         |)
         |INSERT INTO $table (student_id, task_id, section_id, revision, answer)
         |  SELECT work.student_id as student_id,
         |         work.task_id as task_id,
         |         work.section_id as section_id,
         |         ? as revision,
         |         ? as answer
         |RETURNING student_id, task_id, section_id, revision, version, answer, is_complete, created_at, updated_at
       """.stripMargin

    val Update =
      s"""
         |UPDATE work
         |SET version = ?,
         |    is_complete = ?,
         |    updated_at = ?
         |WHERE student_id = ?
         |  AND task_id = ?
         |  AND section_id = ?
         |  AND version = ?
       """.stripMargin

    def UpdateKeepLatestRevision(table: String): String =
      s"""
         |WITH work AS (
         |  $Update
         |)
         |UPDATE $table
         |SET answer = ?
         |WHERE student_id = work.student_id
         |  AND task_id = work.task_id
         |  AND section_id = work.section_id
         |  AND revision = ?
         |RETURNING student_id, task_id, section_id, revision, version, answer, is_complete, created_at, updated_at
       """.stripMargin

    def UpdateWithNewRevision(table: String): String =
      s"""
         |WITH work AS (
         |  $Update
         |)
         |INSERT INTO $table (student_id, task_id, section_id, revision, answer)
         |  SELECT work.student_id as student_id,
         |         work.task_id as task_id,
         |         work.section_id as section_id,
         |         ? as revision,
         |         ? as answer
         |RETURNING student_id, task_id, section_id, revision, version, answer, is_complete, created_at, updated_at
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
         |  AND section_id = ?
       """.stripMargin

    val DeleteRevision =
      s"""
         |DELETE work
         |$From
         |$Join
         |WHERE user_id = ?
         |  AND task_id = ?
         |  AND section_id = ?
         |  AND revision = ?
       """.stripMargin

    val DeleteByTask =
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
     * @param section
     * @return
     */
    override def list(user: User, section: Section, project: Project): Future[IndexedSeq[Work]] = {
      db.pool.sendPreparedStatement(ListLatestByProjectForUser, Array[Any](
        project.id.bytes,
        user.id.bytes,
        section.id.bytes
      )).map { result =>
        result.rows.get.map { item: RowData => Work(item) }
      }.recover {
        case exception => throw exception
      }
    }

    /**
     * List all reivisons of a specific work.
     *
     * @param task
     * @param user
     * @param section
     * @return
     */
    override def list(user: User, task: Task, section: Section): Future[IndexedSeq[Work]] = {
      db.pool.sendPreparedStatement(ListRevisionsById, Array[Any](
        task.id.bytes,
        user.id.bytes,
        section.id.bytes
      )).map { result =>
        result.rows.get.map { item: RowData => Work(item) }
      }.recover {
        case exception => throw exception
      }
    }

    /**
     * Find the latest revision of a single work.
     *
     * @param task
     * @param user
     * @param section
     * @return
     */
    override def find(user: User, task: Task, section: Section): Future[Option[Work]] = {
      db.pool.sendPreparedStatement(SelectLatestById, Array[Any](
        user.id.bytes,
        task.id.bytes,
        section.id.bytes
      )).map { result =>
        result.rows.get.headOption match {
          case Some(rowData) => Some(Work(rowData))
          case None => None
        }
      }.recover {
        case exception => throw exception
      }
    }

    /**
     * Find a specific revision for a single work.
     *
     * @param task
     * @param user
     * @param section
     * @return
     */
    override def find(user: User, task: Task, section: Section, revision: Long): Future[Option[Work]] = {
      db.pool.sendPreparedStatement(SelectLatestById, Array[Any](
        user.id.bytes,
        task.id.bytes,
        section.id.bytes,
        revision
      )).map { result =>
        result.rows.get.headOption match {
          case Some(rowData) => Some(Work(rowData))
          case None => None
        }
      }.recover {
        case exception => throw exception
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
    override def insert(work: Work)(implicit conn: Connection): Future[Work] = {
      val tableName = work match {
        case specific: LongAnswerWork => "long_answer_work"
        case specific: ShortAnswerWork => "short_answer_work"
        case specific: MultipleChoiceWork => "multiple_choice_work"
        case specific: OrderingWork => "ordering_work"
        case specific: MatchingWork => "matching_work"
      }
      conn.sendPreparedStatement(InsertIntoAny(tableName), Array[Any](
        work.studentId.bytes,
        work.taskId.bytes,
        work.sectionId.bytes,
        work.isComplete,
        new DateTime,
        new DateTime,
        work.revision,
        work.answer match {
          case matchList: IndexedSeq[MatchingTask.Match] => matchList.map { item => s"${item.left}:${item.right}"}
          case anything => anything
        }
      )).map { result =>
        Work(result.rows.get.head)
      }.recover {
        case exception => throw exception
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
    override def update(work: Work, newRevision: Boolean = true)(implicit conn: Connection): Future[Work] = {
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
          work.sectionId.bytes,
          work.version,
          work.revision + 1,
          work.answer
        ))
      }
      else {
        conn.sendPreparedStatement(UpdateKeepLatestRevision(tableName), Array[Any](
          work.version +1,
          work.isComplete,
          new DateTime,
          work.studentId.bytes,
          work.taskId.bytes,
          work.sectionId.bytes,
          work.version,
          work.answer,
          work.revision
        ))
      }

      future.map { result =>
        Work(result.rows.get.head)
      }.recover {
        case exception => throw exception
      }
    }

    /**
     * Delete a specific revision of a work.
     *
     * @param work
     * @param conn
     * @return
     */
    override def delete(work: Work, thisRevisionOnly: Boolean = false)(implicit conn: Connection): Future[Boolean] = {
      val fDelete = if (thisRevisionOnly) {
        conn.sendPreparedStatement(DeleteRevision, Array[Any](
          work.studentId.bytes,
          work.taskId.bytes,
          work.sectionId.bytes,
          work.revision
        ))
      }
      else {
        conn.sendPreparedStatement(Delete, Array[Any](
          work.studentId.bytes,
          work.taskId.bytes,
          work.sectionId.bytes
        ))
      }

      fDelete.map { result => result.rowsAffected > 0 }.recover {
        case exception => throw exception
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
    override def delete(task: Task)(implicit conn: Connection): Future[Boolean] = {
      conn.sendPreparedStatement(DeleteRevision, Array[Any](task.id.bytes)).map {
        result => result.rowsAffected > 0
      }.recover {
        case exception => throw exception
      }
    }
  }
}