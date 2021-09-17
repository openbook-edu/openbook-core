package ca.shiftfocus.krispii.core.repositories

import java.util.UUID
import play.api.Logger
import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.{Connection, RowData}
import org.joda.time.DateTime
import scala.concurrent.Future
import scalaz.{-\/, \/, \/-}

class LimitRepositoryPostgres extends LimitRepository with PostgresRepository[Long] {
  override val entityName = "Limit"
  override def constructor(row: RowData): Long = {
    row("limited").toString.toLong
  }

  private def Select(suffix: String): String =
    s"""
      |SELECT ${suffix}_id, type, limited
      |FROM ${suffix}_limit
      |WHERE ${suffix}_id = ?
      | AND type = ?
    """.stripMargin

  // Use DISTINCT ON, because teacher can have many media components with links to one file on s3
  private val GetStorageUsed =
    """
      |WITH vc AS (SELECT SUM(vc_data.size) as limited
      |            FROM (
      |             SELECT DISTINCT ON (video_data::jsonb->>'data') video_data::jsonb->>'data', cast(video_data::jsonb->>'size' as bigint) as size
      |             FROM video_components
      |             INNER JOIN components
      |             ON components.id = video_components.component_id
      |             WHERE components.owner_id = ?
      |               AND video_components.video_data::jsonb->>'host' = 's3'
      |               AND components.parent_id IS NULL
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
      |               AND components.parent_id IS NULL
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
      |               AND components.parent_id IS NULL
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
      |               AND components.parent_id IS NULL
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

  private def Insert(suffix: String): String =
    s"""
      |INSERT INTO ${suffix}_limit (${suffix}_id, type, limited)
      |VALUES (?, ?, ?)
      |RETURNING ${suffix}_id, type, limited
    """.stripMargin

  private def Update(suffix: String): String =
    s"""
      |UPDATE ${suffix}_limit
      |SET limited = ?
      |WHERE ${suffix}_id = ?
      | AND type = ?
      |RETURNING ${suffix}_id, type, limited
    """.stripMargin

  private def Delete(suffix: String): String =
    s"""
      |DELETE FROM ${suffix}_limit
      |WHERE ${suffix}_id = ?
      | AND type = ?
      |RETURNING ${suffix}_id, type, limited
    """.stripMargin

  // ###### TEACHERS ###################################################################################################

  // --- GET -----------------------------------------------------------------------------------------------------------

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
   * Get storage (in GB) that teacher has used
   *
   * @param teacherId
   * @return Used space in GB
   */
  def getStorageUsed(teacherId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Float]] = {
    queryOne(GetStorageUsed, Seq[Any](teacherId, teacherId, teacherId, teacherId, teacherId)).flatMap {
      // We store file size in database in Bytes, convert them to GB
      case \/-(limit) =>
        Future successful \/-(limit.toFloat / 1000 / 1000 / 1000)
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Number of students that are allowed for teacher per course
   *
   * @param teacherId
   * @param conn
   * @return
   */
  def getTeacherStudentLimit(teacherId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]] = {
    getTeacherLimit(teacherId, Limits.student).flatMap {
      case \/-(limit) => Future successful \/-(limit.toInt)
      case -\/(error) => Future successful -\/(error)
    }
  }

  // --- SET -----------------------------------------------------------------------------------------------------------

  /**
   * Upsert course limit for teachers
   */
  def setCourseLimit(teacherId: UUID, limit: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]] =
    queryOne(Update("teacher"), Seq[Any](limit, teacherId, Limits.course)).flatMap {
      case \/-(limit) => Future successful \/-(limit.toInt)
      case -\/(_: RepositoryError.NoResults) =>
        lift(queryOne(Insert("teacher"), Seq[Any](teacherId, Limits.course, limit)))
          .map(insert => insert.toInt)
      case -\/(error) => Future successful -\/(error)
    }

  /**
   * Upsert storage limit for teachers
   *
   * @param teacherId
   * @param limit GB
   * @return GB
   */
  def setStorageLimit(teacherId: UUID, limit: Float)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Float]] =
    // We store limit in database in MB, convert GB in MB
    queryOne(Update("teacher"), Seq[Any]((limit * 1000).toInt, teacherId, Limits.storage)).flatMap {
      // Convert back into GB
      case \/-(limit) => Future successful \/-(limit.toFloat / 1000)
      case -\/(_: RepositoryError.NoResults) =>
        lift(queryOne(Insert("teacher"), Seq[Any](teacherId, Limits.storage, (limit * 1000).toInt)))
          .map(insert => insert.toFloat / 1000)
      case -\/(error) => Future successful -\/(error)
    }

  /**
   * Set Number of students that are allowed for teacher per course
   *
   * @param teacherId
   * @param limit
   * @param conn
   * @return
   */
  def setTeacherStudentLimit(teacherId: UUID, limit: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]] =
    queryOne(Update("teacher"), Seq[Any](limit, teacherId, Limits.student)).flatMap {
      case \/-(limit) => Future successful \/-(limit.toInt)
      case -\/(_: RepositoryError.NoResults) =>
        lift(queryOne(Insert("teacher"), Seq[Any](teacherId, Limits.student, limit)))
          .map(insert => insert.toInt)
      case -\/(error) => Future successful -\/(error)
    }

  // ###### COURSES ###################################################################################################

  // --- GET -----------------------------------------------------------------------------------------------------------

  /**
   * Get number of students that course is allowed to have
   *
   * @param courseId
   * @return
   */
  def getCourseStudentLimit(courseId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]] =
    getCourseLimit(courseId, Limits.student).flatMap {
      case \/-(limit) => Future successful \/-(limit.toInt)
      case -\/(error) => Future successful -\/(error)
    }

  // --- SET -----------------------------------------------------------------------------------------------------------

  /**
   * Upsert student limit within course
   */
  def setCourseStudentLimit(courseId: UUID, limit: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]] =
    queryOne(Update("course"), Seq[Any](limit, courseId, Limits.student)).flatMap {
      case \/-(limit) => Future successful \/-(limit.toInt)
      case -\/(_: RepositoryError.NoResults) =>
        lift(queryOne(Insert("course"), Seq[Any](courseId, Limits.student, limit)))
          .map(insert => insert.toInt)
      case -\/(error) => Future successful -\/(error)
    }

  // --- DELETE --------------------------------------------------------------------------------------------------------

  def deleteCourseStudentLimit(courseId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]] =
    deleteCourseLimit(courseId, Limits.student).flatMap {
      case \/-(_) => Future successful \/-((): Unit)
      case -\/(_: RepositoryError.NoResults) => Future successful \/-((): Unit)
      case -\/(error) => Future successful -\/(error)
    }

  // ###### PLANS ######################################################################################################

  // --- GET -----------------------------------------------------------------------------------------------------------

  /**
   * Get number of courses that teacher is allowed to have within indicated plan
   *
   * @param planId
   * @return
   */
  def getPlanCourseLimit(planId: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]] =
    getPlanLimit(planId, Limits.course).flatMap {
      case \/-(limit) => Future successful \/-(limit.toInt)
      case -\/(error) => Future successful -\/(error)
    }

  /**
   * Get storage (in GB) limit that teacher is allowed to have within indicated plan
   *
   * @param planId
   * @return Limit in GB
   */
  def getPlanStorageLimit(planId: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Float]] =
    getPlanLimit(planId, Limits.storage).flatMap {
      // We store limit in database in MB, convert them to GB
      case \/-(limit) => Future successful \/-(limit.toFloat / 1000)
      case -\/(error) => Future successful -\/(error)
    }

  /**
   * Get number of students that course is allowed to have
   *
   * @param planId
   * @return
   */
  def getPlanStudentLimit(planId: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]] =
    getPlanLimit(planId, Limits.student).flatMap {
      case \/-(limit) => Future successful \/-(limit.toInt)
      case -\/(error) => Future successful -\/(error)
    }

  /**
   * Get number of student copies that an organization is allowed to score
   *
   * @param planId
   * @return
   */
  def getPlanCopiesLimit(planId: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Long]] =
    getPlanLimit(planId, Limits.maxCopies)

  // --- SET -----------------------------------------------------------------------------------------------------------

  def setPlanStorageLimit(planId: String, limit: Float)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Float]] =
    // We store limit in database in MB, convert GB in MB
    queryOne(Update("plan"), Seq[Any]((limit * 1000).toInt, planId, Limits.storage)).flatMap {
      // Convert back into GB
      case \/-(limit) => Future successful \/-(limit.toFloat / 1000)
      case -\/(_: RepositoryError.NoResults) => {
        lift(queryOne(Insert("plan"), Seq[Any](planId, Limits.storage, (limit * 1000).toInt)))
          .map(insert => insert.toFloat / 1000)
      }
      case -\/(error) => Future successful -\/(error)
    }

  def setPlanCourseLimit(planId: String, limit: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]] =
    queryOne(Update("plan"), Seq[Any](limit, planId, Limits.course)).flatMap {
      case \/-(limit) => Future successful \/-(limit.toInt)
      case -\/(_: RepositoryError.NoResults) => {
        lift(queryOne(Insert("plan"), Seq[Any](planId, Limits.course, limit)))
          .map(insert => insert.toInt)
      }
      case -\/(error) => Future successful -\/(error)
    }

  def setPlanStudentLimit(planId: String, limit: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]] =
    queryOne(Update("plan"), Seq[Any](limit, planId, Limits.student)).flatMap {
      case \/-(limit) => Future successful \/-(limit.toInt)
      case -\/(_: RepositoryError.NoResults) =>
        lift(queryOne(Insert("plan"), Seq[Any](planId, Limits.student, limit)))
          .map(insert => insert.toInt)
      case -\/(error) => Future successful -\/(error)
    }

  def setPlanCopiesLimit(planId: String, limit: Long)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Long]] =
    queryOne(Update("plan"), Seq[Any](limit, planId, Limits.maxCopies)).flatMap {
      case \/-(limit) => Future successful \/-(limit)
      case -\/(_: RepositoryError.NoResults) =>
        lift(queryOne(Insert("plan"), Seq[Any](planId, Limits.maxCopies, limit)))
      case -\/(error) => Future successful -\/(error)
    }

  // ###### ORGANIZATIONS ##############################################################################################

  // --- GET -----------------------------------------------------------------------------------------------------------

  def getOrganizationStorageLimit(organizationId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Float]] =
    getOrganizationLimit(organizationId, Limits.storage).flatMap {
      // We store limit in database in MB, convert them to GB
      case \/-(limit) => Future successful \/-(limit.toFloat / 1000)
      case -\/(error) => Future successful -\/(error)
    }

  def getOrganizationCourseLimit(organizationId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]] =
    getOrganizationLimit(organizationId, Limits.course).flatMap {
      case \/-(limit) => Future successful \/-(limit.toInt)
      case -\/(error) => Future successful -\/(error)
    }

  def getOrganizationStudentLimit(organizationId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]] =
    getOrganizationLimit(organizationId, Limits.student).flatMap {
      case \/-(limit) => Future successful \/-(limit.toInt)
      case -\/(error) => Future successful -\/(error)
    }

  def getOrganizationDateLimit(organizationId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, DateTime]] =
    // Limit is unix timestamp
    getOrganizationLimit(organizationId, Limits.activeUntil).flatMap {
      // We need milliseconds here
      case \/-(limit) => Future successful \/-({ Logger.info(s"Org data limit: ${limit} ms"); new DateTime(limit * 1000) })
      case -\/(error) => Future successful -\/(error)
    }

  def getOrganizationMemberLimit(organizationId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]] =
    getOrganizationLimit(organizationId, Limits.maxUsers).flatMap {
      case \/-(limit) => Future successful \/-(limit.toInt)
      case -\/(error) => Future successful -\/(error)
    }

  def getOrganizationCopiesLimit(organizationId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Long]] =
    getOrganizationLimit(organizationId, Limits.maxCopies)

  // --- SET -----------------------------------------------------------------------------------------------------------

  def setOrganizationStorageLimit(organizationId: UUID, limit: Float)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Float]] =
    // We store limit in database in MB, convert GB in MB
    queryOne(Update("organization"), Seq[Any]((limit * 1000).toInt, organizationId, Limits.storage)).flatMap {
      // Convert back into GB
      case \/-(limit) => Future successful \/-(limit.toFloat / 1000)
      case -\/(_: RepositoryError.NoResults) =>
        lift(queryOne(Insert("organization"), Seq[Any](organizationId, Limits.storage, (limit * 1000).toInt)))
          .map(insert => insert.toFloat / 1000)
      case -\/(error) => Future successful -\/(error)
    }

  def setOrganizationCourseLimit(organizationId: UUID, limit: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]] =
    queryOne(Update("organization"), Seq[Any](limit, organizationId, Limits.course)).flatMap {
      case \/-(limit) => Future successful \/-(limit.toInt)
      case -\/(_: RepositoryError.NoResults) =>
        lift(queryOne(Insert("organization"), Seq[Any](organizationId, Limits.course, limit)))
          .map(insert => insert.toInt)
      case -\/(error) => Future successful -\/(error)
    }

  def setOrganizationStudentLimit(organizationId: UUID, limit: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]] =
    queryOne(Update("organization"), Seq[Any](limit, organizationId, Limits.student)).flatMap {
      case \/-(limit) => Future successful \/-(limit.toInt)
      case -\/(_: RepositoryError.NoResults) =>
        lift(queryOne(Insert("organization"), Seq[Any](organizationId, Limits.student, limit)))
          .map(insert => insert.toInt)
      case -\/(error) => Future successful -\/(error)
    }

  def setOrganizationDateLimit(organizationId: UUID, limit: DateTime)(implicit conn: Connection): Future[\/[RepositoryError.Fail, DateTime]] =
    queryOne(Update("organization"), Seq[Any](limit.getMillis / 1000, organizationId, Limits.activeUntil)).flatMap {
      case \/-(limit) => Future successful \/-(new DateTime(limit * 1000))
      case -\/(_: RepositoryError.NoResults) =>
        lift(queryOne(Insert("organization"), Seq[Any](organizationId, Limits.activeUntil, limit.getMillis / 1000)))
          .map(insert => new DateTime(insert * 1000))
      case -\/(error) => Future successful -\/(error)
    }

  def setOrganizationMemberLimit(organizationId: UUID, limit: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]] =
    queryOne(Update("organization"), Seq[Any](limit, organizationId, Limits.maxUsers)).flatMap {
      case \/-(limit) => Future successful \/-(limit.toInt)
      case -\/(_: RepositoryError.NoResults) =>
        lift(queryOne(Insert("organization"), Seq[Any](organizationId, Limits.maxUsers, limit)))
          .map(insert => insert.toInt)
      case -\/(error) => Future successful -\/(error)
    }

  def setOrganizationCopiesLimit(organizationId: UUID, limit: Long)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Long]] =
    queryOne(Update("organization"), Seq[Any](limit, organizationId, Limits.maxCopies)).flatMap {
      case \/-(limit) => Future successful \/-(limit)
      case -\/(_: RepositoryError.NoResults) =>
        lift(queryOne(Insert("organization"), Seq[Any](organizationId, Limits.maxCopies, limit)))
      case -\/(error) => Future successful -\/(error)
    }

  // ###### PRIVATE METHODS ############################################################################################

  private def getTeacherLimit(teacherId: UUID, limitType: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Long]] = {
    // Logger.info(s"Checking $limitType limits for teacher no. $teacherId")
    queryOne(Select("teacher"), Seq[Any](teacherId, limitType))
  }

  private def getCourseLimit(courseId: UUID, limitType: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Long]] = {
    // Logger.info(s"Checking $limitType limits for course no. $courseId")
    queryOne(Select("course"), Seq[Any](courseId, limitType))
  }

  private def getPlanLimit(planId: String, limitType: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Long]] = {
    // Logger.info(s"Checking $limitType limits for plan no. $planId")
    queryOne(Select("plan"), Seq[Any](planId, limitType))
  }

  private def getOrganizationLimit(organizationId: UUID, limitType: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Long]] = {
    // Logger.info(s"Checking ${limitType} limits for organization no. ${organizationId}")
    queryOne(Select("organization"), Seq[Any](organizationId, limitType))
  }

  private def deleteCourseLimit(courseId: UUID, limitType: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Long]] = {
    Logger.info(s"Deleting $limitType limits for course no. $courseId")
    queryOne(Delete("course"), Seq[Any](courseId, limitType))
  }
}

/**
 * Types of limits
 */
object Limits {
  val course: String = "course"
  val student: String = "student"
  val storage: String = "storage"
  val activeUntil: String = "active_until"
  val maxUsers: String = "max_users"
  val maxCopies: String = "max_copies"
}
