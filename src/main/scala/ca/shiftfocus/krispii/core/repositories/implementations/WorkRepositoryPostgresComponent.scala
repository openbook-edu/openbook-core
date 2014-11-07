package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import com.github.mauricio.async.db.Connection
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
         |WHERE user_id = ?
         |  AND projects.id = ?
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
  }
}