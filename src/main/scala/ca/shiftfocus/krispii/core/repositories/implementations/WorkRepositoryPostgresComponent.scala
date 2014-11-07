package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import com.github.mauricio.async.db.{RowData, Connection}
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks.Task
import ca.shiftfocus.krispii.core.models.work.Work
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

    // -- Delete -------------------------------------------------------------------------------------------------------
    // NB: the delete queries should never be used unless you know what you're doing. Due to work revisioning, the
    //     proper way to "clear" a work is to create an empty revision.

    val Delete =
      s"""
         |DELETE work
         |$From
         |$Join
         |WHERE work.id = ?
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
      }
    }
  }
}