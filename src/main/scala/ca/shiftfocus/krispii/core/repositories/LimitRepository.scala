package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.Connection
import org.joda.time.DateTime
import scala.concurrent.Future
import scalaz.\/

trait LimitRepository extends Repository {

  // TEACHERS
  def getCourseLimit(teacherId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]]
  def getStorageLimit(teacherId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Float]]
  def getStorageUsed(teacherId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Float]]
  def getTeacherStudentLimit(teacherId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]]

  def setCourseLimit(teacherId: UUID, limit: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]]
  def setStorageLimit(teacherId: UUID, limit: Float)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Float]]
  def setTeacherStudentLimit(teacherId: UUID, limit: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]]

  // COURSES
  def getCourseStudentLimit(courseId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]]
  def setCourseStudentLimit(courseId: UUID, limit: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]]
  def deleteCourseStudentLimit(courseId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]

  // PLANS
  def getPlanCourseLimit(planId: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]]
  def getPlanStorageLimit(planId: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Float]]
  def getPlanStudentLimit(planId: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]]
  def getPlanCopiesLimit(planId: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Long]]

  def setPlanStorageLimit(planId: String, limit: Float)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Float]]
  def setPlanCourseLimit(planId: String, limit: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]]
  def setPlanStudentLimit(planId: String, limit: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]]
  def setPlanCopiesLimit(planId: String, limit: Long)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Long]]

  // ORGANIZATIONS
  def getOrganizationStorageLimit(organizationId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Float]]
  def getOrganizationCourseLimit(organizationId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]]
  def getOrganizationStudentLimit(organizationId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]]
  def getOrganizationDateLimit(organizationId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, DateTime]]
  def getOrganizationMemberLimit(organizationId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]]
  def getOrganizationCopiesLimit(organizationId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Long]]

  def setOrganizationStorageLimit(organizationId: UUID, limit: Float)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Float]]
  def setOrganizationCourseLimit(organizationId: UUID, limit: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]]
  def setOrganizationStudentLimit(organizationId: UUID, limit: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]]
  def setOrganizationDateLimit(organizationId: UUID, limit: DateTime)(implicit conn: Connection): Future[\/[RepositoryError.Fail, DateTime]]
  def setOrganizationMemberLimit(organizationId: UUID, limit: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]]
  def setOrganizationCopiesLimit(organizationId: UUID, limit: Long)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Long]]
}
