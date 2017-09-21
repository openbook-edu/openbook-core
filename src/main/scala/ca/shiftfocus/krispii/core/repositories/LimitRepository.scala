package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.Link
import com.github.mauricio.async.db.Connection
import org.joda.time.DateTime

import scala.concurrent.Future
import scalaz.\/

trait LimitRepository extends Repository {

  // TEACHERS
  def getCourseLimit(teacherId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]]
  def getStorageLimit(treacherId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Float]]
  def getStorageUsed(treacherId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Float]]

  def setCourseLimit(teacherId: UUID, limit: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]]
  def setStorageLimit(teacherId: UUID, limit: Float)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Float]]

  // COURSES
  def getStudentLimit(courseId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]]
  def setStudentLimit(courseId: UUID, limit: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]]

  // PLANS
  def getPlanCourseLimit(planId: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]]
  def getPlanStorageLimit(planId: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Float]]
  def getPlanStudentLimit(planId: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]]

  def setPlanStorageLimit(planId: String, limit: Float)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Float]]
  def setPlanCourseLimit(planId: String, limit: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]]
  def setPlanStudentLimit(planId: String, limit: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]]

  // ORGANIZATIONS
  def getOrganizationStorageLimit(organizationId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Float]]
  def getOrganizationCourseLimit(organizationId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]]
  def getOrganizationStudentLimit(organizationId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]]
  def getOrganizationDateLimit(organizationId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, DateTime]]

  def setOrganizationStorageLimit(organizationId: UUID, limit: Float)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Float]]
  def setOrganizationCourseLimit(organizationId: UUID, limit: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]]
  def setOrganizationStudentLimit(organizationId: UUID, limit: Int)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Int]]
  def setOrganizationDateLimit(organizationId: UUID, limit: DateTime)(implicit conn: Connection): Future[\/[RepositoryError.Fail, DateTime]]
}
