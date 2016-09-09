package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.{ Connection, RowData }

import scala.concurrent.Future
import scalaz.{ -\/, \/, \/- }

class LimitRepositoryPostgres extends LimitRepository with PostgresRepository[Long] {
  override val entityName = "Limit"
  override def constructor(row: RowData): Long = {
    row("limited").toString.toLong
  }

  val GetTeacherLimit =
    """
      |SELECT teacher_id, type, limited
      |FROM teacher_limit
      |WHERE teacher_id = ?
      | AND type = ?
    """.stripMargin

  val GetCourseLimit =
    """
      |SELECT course_id, type, limited
      |FROM course_limit
      |WHERE course_id = ?
      | AND type = ?
    """.stripMargin

  val GetPlanLimit =
    """
      |SELECT plan_id, type, limited
      |FROM plan_limit
      |WHERE plan_id = ?
      | AND type = ?
    """.stripMargin

  // Use DISTINCT ON, because teacher can have many media components with links to one file on s3
  // TODO add book component
  val GetStorageUsed =
    """
      |WITH vc AS (SELECT SUM(vc_data.size) as limited
      |            FROM (
      |             SELECT DISTINCT ON (video_data::jsonb->>'data') video_data::jsonb->>'data', cast(video_data::jsonb->>'size' as bigint) as size
      |             FROM video_components
      |             INNER JOIN components
      |             ON components.id = video_components.component_id
      |             WHERE components.owner_id = ?
      |               AND video_components.video_data::jsonb->>'host' = 's3'
      |             ORDER BY video_data::jsonb->>'data'
      |            )
      |            AS vc_data),
      |
      |     ac AS (SELECT SUM(ac_data.size) as limited
      |            FROM (
      |             SELECT DISTINCT ON (audio_data::jsonb->>'data') audio_data::jsonb->>'data', cast(audio_data::jsonb->>'size' as bigint) as size
      |             FROM audio_components
      |             INNER JOIN components
      |             ON components.id = audio_components.component_id
      |             WHERE components.owner_id = ?
      |               AND audio_components.audio_data::jsonb->>'host' = 's3'
      |             ORDER BY audio_data::jsonb->>'data'
      |            )
      |            AS ac_data),
      |
      |     ic AS (SELECT SUM(ic_data.size) as limited
      |            FROM (
      |             SELECT DISTINCT ON (image_data::jsonb->>'data') image_data::jsonb->>'data', cast(image_data::jsonb->>'size' as bigint) as size
      |             FROM image_components
      |             INNER JOIN components
      |             ON components.id = image_components.component_id
      |             WHERE components.owner_id = ?
      |               AND image_components.image_data::jsonb->>'host' = 's3'
      |             ORDER BY image_data::jsonb->>'data'
      |            )
      |            AS ic_data),
      |
      |     bc AS (SELECT SUM(bc_data.size) as limited
      |            FROM (
      |             SELECT DISTINCT ON (file_data::jsonb->>'data') file_data::jsonb->>'data', cast(file_data::jsonb->>'size' as bigint) as size
      |             FROM book_components
      |             INNER JOIN components
      |             ON components.id = book_components.component_id
      |             WHERE components.owner_id = ?
      |               AND book_components.file_data::jsonb->>'host' = 's3'
      |             ORDER BY file_data::jsonb->>'data'
      |            )
      |            AS bc_data),
      |
      |     mw AS (SELECT SUM(cast(file_data::jsonb->>'size' as bigint)) as limited FROM media_work
      |            INNER JOIN courses    ON courses.teacher_id = ?
      |            INNER JOIN projects   ON projects.course_id = courses.id
      |            INNER JOIN parts      ON parts.project_id = projects.id
      |            INNER JOIN tasks      ON tasks.part_id = parts.id
      |            INNER JOIN work       ON work.task_id = tasks.id
      |            WHERE media_work.work_id = work.id)
      |
      |SELECT (COALESCE(vc.limited, 0) + COALESCE(ac.limited, 0) + COALESCE(ic.limited, 0) + COALESCE(bc.limited, 0) + COALESCE(mw.limited, 0)) as limited
      |FROM vc, ac, ic, bc, mw
    """.stripMargin

  val InsertTeacherLimit =
    """
      |INSERT INTO teacher_limit (teacher_id, type, limited)
      |VALUES (?, ?, ?)
      |RETURNING teacher_id, type, limited
    """.stripMargin

  val InsertCourseLimit =
    """
      |INSERT INTO course_limit (course_id, type, limited)
      |VALUES (?, ?, ?)
      |RETURNING course_id, type, limited
    """.stripMargin

  val InsertPlanLimit =
    """
      |INSERT INTO plan_limit (plan_id, type, limited)
      |VALUES (?, ?, ?)
      |RETURNING plan_id, type, limited
    """.stripMargin

  val UpdateTeacherLimit =
    """
      |UPDATE teacher_limit
      |SET limited = ?
      |WHERE teacher_id = ?
      | AND type = ?
      |RETURNING teacher_id, type, limited
    """.stripMargin

  val UpdateCourseLimit =
    """
      |UPDATE course_limit
      |SET limited = ?
      |WHERE course_id = ?
      | AND type = ?
      |RETURNING course_id, type, limited
    """.stripMargin

  val UpdatePlanLimit =
    """
      |UPDATE plan_limit
      |SET limited = ?
      |WHERE plan_id = ?
      | AND type = ?
      |RETURNING plan_id, type, limited
    """.stripMargin

  /**
   * Get number of courses that teacher is allowed to have
   *
   * @param teacherId
   * @return
   */
  def getCourseLimit(teacherId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]] = {
    getTeacherLimit(teacherId, Limits.course).flatMap {
      case \/-(limit) => Future successful \/-(limit.toInt)
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Get number of courses that teacher is allowed to have within indicated plan
   *
   * @param planId
   * @return
   */
  def getPlanCourseLimit(planId: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]] = {
    getPlanLimit(planId, Limits.course).flatMap {
      case \/-(limit) => Future successful \/-(limit.toInt)
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Get storage (in GB) limit that teacher is allowed to have
   *
   * @param teacherId
   * @return Limit in GB
   */
  def getStorageLimit(teacherId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Float]] = {
    getTeacherLimit(teacherId, Limits.storage).flatMap {
      // We store limit in database in MB, convert them to GB
      case \/-(limit) => Future successful \/-(limit.toFloat / 1000)
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Get storage (in GB) limit that teacher is allowed to have within indicated plan
   *
   * @param planId
   * @return Limit in GB
   */
  def getPlanStorageLimit(planId: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Float]] = {
    getPlanLimit(planId, Limits.storage).flatMap {
      // We store limit in database in MB, convert them to GB
      case \/-(limit) => Future successful \/-(limit.toFloat / 1000)
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Get storage (in GB) that teacher has used
   *
   * @param teacherId
   * @return Used space in GB
   */
  def getStorageUsed(teacherId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Float]] = {
    queryOne(GetStorageUsed, Seq[Any](teacherId, teacherId, teacherId, teacherId, teacherId)).flatMap {
      // We store file size in database in Bytes, convert them to GB
      case \/-(limit) => {
        Future successful \/-(limit.toFloat / 1000 / 1000 / 1000)
      }
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Get number of students that course is allowed to have
   *
   * @param courseId
   * @return
   */
  def getStudentLimit(courseId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]] = {
    getCourseLimit(courseId, Limits.student).flatMap {
      case \/-(limit) => Future successful \/-(limit.toInt)
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Get number of students that course is allowed to have
   *
   * @param planId
   * @return
   */
  def getPlanStudentLimit(planId: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]] = {
    getPlanLimit(planId, Limits.student).flatMap {
      case \/-(limit) => Future successful \/-(limit.toInt)
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Upsert course limit for teachers
   */
  def setCourseLimit(teacherId: UUID, limit: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]] = {
    queryOne(UpdateTeacherLimit, Seq[Any](limit, teacherId, Limits.course)).flatMap {
      case \/-(limit) => Future successful \/-(limit.toInt)
      case -\/(error: RepositoryError.NoResults) => {
        for {
          insert <- lift(queryOne(InsertTeacherLimit, Seq[Any](teacherId, Limits.course, limit)))
        } yield insert.toInt
      }
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Upsert storage limit for teachers
   *
   * @param teacherId
   * @param limit GB
   * @return GB
   */
  def setStorageLimit(teacherId: UUID, limit: Float)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Float]] = {
    // We store limit in database in MB, convert GB in MB
    queryOne(UpdateTeacherLimit, Seq[Any]((limit * 1000).toInt, teacherId, Limits.storage)).flatMap {
      // Convert back into GB
      case \/-(limit) => Future successful \/-(limit.toFloat / 1000)
      case -\/(error: RepositoryError.NoResults) => {
        for {
          insert <- lift(queryOne(InsertTeacherLimit, Seq[Any](teacherId, Limits.storage, (limit * 1000).toInt)))
          // Convert back into GB
        } yield insert.toFloat / 1000
      }
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Upsert student limit within course
   */
  def setStudentLimit(courseId: UUID, limit: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]] = {
    queryOne(UpdateCourseLimit, Seq[Any](limit, courseId, Limits.student)).flatMap {
      case \/-(limit) => Future successful \/-(limit.toInt)
      case -\/(error: RepositoryError.NoResults) => {
        for {
          insert <- lift(queryOne(InsertCourseLimit, Seq[Any](courseId, Limits.student, limit)))
        } yield insert.toInt
      }
      case -\/(error) => Future successful -\/(error)
    }
  }

  def setPlanStorageLimit(planId: String, limit: Float)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Float]] = {
    // We store limit in database in MB, convert GB in MB
    queryOne(UpdatePlanLimit, Seq[Any]((limit * 1000).toInt, planId, Limits.storage)).flatMap {
      // Convert back into GB
      case \/-(limit) => Future successful \/-(limit.toFloat / 1000)
      case -\/(error: RepositoryError.NoResults) => {
        for {
          insert <- lift(queryOne(InsertPlanLimit, Seq[Any](planId, Limits.storage, (limit * 1000).toInt)))
          // Convert back into GB
        } yield insert.toFloat / 1000
      }
      case -\/(error) => Future successful -\/(error)
    }
  }

  def setPlanCourseLimit(planId: String, limit: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]] = {
    queryOne(UpdatePlanLimit, Seq[Any](limit, planId, Limits.course)).flatMap {
      case \/-(limit) => Future successful \/-(limit.toInt)
      case -\/(error: RepositoryError.NoResults) => {
        for {
          insert <- lift(queryOne(InsertPlanLimit, Seq[Any](planId, Limits.course, limit)))
        } yield insert.toInt
      }
      case -\/(error) => Future successful -\/(error)
    }
  }

  def setPlanStudentLimit(planId: String, limit: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]] = {
    queryOne(UpdatePlanLimit, Seq[Any](limit, planId, Limits.student)).flatMap {
      case \/-(limit) => Future successful \/-(limit.toInt)
      case -\/(error: RepositoryError.NoResults) => {
        for {
          insert <- lift(queryOne(InsertPlanLimit, Seq[Any](planId, Limits.student, limit)))
        } yield insert.toInt
      }
      case -\/(error) => Future successful -\/(error)
    }
  }

  private def getTeacherLimit(teacherId: UUID, limitType: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Long]] = {
    queryOne(GetTeacherLimit, Seq[Any](teacherId, limitType))
  }

  private def getCourseLimit(courseId: UUID, limitType: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Long]] = {
    queryOne(GetCourseLimit, Seq[Any](courseId, limitType))
  }

  private def getPlanLimit(planId: String, limitType: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Long]] = {
    queryOne(GetPlanLimit, Seq[Any](planId, limitType))
  }
}

/**
 * Types of limits
 */
object Limits {
  val course: String = "course"
  val student: String = "student"
  val storage: String = "storage"
}
