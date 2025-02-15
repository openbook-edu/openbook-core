package ca.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.Connection
import ca.shiftfocus.krispii.core.models._
import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.user.User

import scala.concurrent.Future
import scalaz.\/

trait ComponentRepository extends Repository {

  def list(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Component]]]
  def listMasterLimit(limit: Int = 0, offset: Int = 0)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Component]]]
  def list(part: Part)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Component]]]
  def list(project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Component]]]
  def list(teacher: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Component]]]
  def list(project: Project, user: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Component]]]

  def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Component]]

  def insert(component: Component)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Component]]
  def update(component: Component)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Component]]
  def delete(component: Component)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Component]]

  def addToPart(component: Component, part: Part)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]
  def removeFromPart(component: Component, part: Part)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]
  def removeFromPart(part: Part)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Component]]]
}
