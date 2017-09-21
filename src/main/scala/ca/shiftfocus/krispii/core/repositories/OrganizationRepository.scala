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
  def insert(organization: Organization)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Organization]]
  def update(organization: Organization)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Organization]]
  def delete(organization: Organization)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Organization]]
}
