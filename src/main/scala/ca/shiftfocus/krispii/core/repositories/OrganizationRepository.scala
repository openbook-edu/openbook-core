package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models._
import com.github.mauricio.async.db.Connection

import scala.concurrent.Future
import scalaz.\/

trait OrganizationRepository extends Repository {
  def find(organizationId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Organization]]
  def list(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Organization]]]
  def listByAdmin(adminEmail: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Organization]]]
  def listByMember(memberEmail: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Organization]]]
  def listByTags(tags: IndexedSeq[(String, String)], distinct: Boolean = true)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Organization]]]
  def addMember(organization: Organization, memberEmail: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Organization]]
  def deleteMember(organization: Organization, memberEmail: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Organization]]
  def addAdmin(organization: Organization, adminEmail: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Organization]]
  def deleteAdmin(organization: Organization, adminEmail: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Organization]]
  def insert(organization: Organization)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Organization]]
  def update(organization: Organization)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Organization]]
  def delete(organization: Organization)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Organization]]
}
