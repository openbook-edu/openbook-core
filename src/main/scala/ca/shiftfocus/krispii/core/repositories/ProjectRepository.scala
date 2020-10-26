package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.Connection
import ca.shiftfocus.krispii.core.models._
import java.util.UUID

import ca.shiftfocus.krispii.core.models.group.Course

import scala.concurrent.Future
import scalaz.\/

trait ProjectRepository extends Repository {
  val partRepository: PartRepository

  def list(showMasters: Option[Boolean] = None, enabled: Option[Boolean] = None)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Project]]]
  def list(course: Course)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Project]]]
  def list(course: Course, fetchParts: Boolean)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Project]]]
  def listByTags(tags: IndexedSeq[(String, String)], distinct: Boolean = true)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Project]]]

  def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Project]]
  def find(projectId: UUID, user: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Project]]
  def find(slug: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Project]]

  def find(id: UUID, fetchParts: Boolean)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Project]]
  def find(projectId: UUID, user: User, fetchParts: Boolean)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Project]]
  def find(slug: String, fetchParts: Boolean)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Project]]

  def insert(project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Project]]
  def update(project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Project]]
  def delete(project: Project)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Project]]
  def trigramSearch(key: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Project]]]

  def cloneProject(projectId: UUID, courseId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Project]]

  def cloneProjectParts(projectId: UUID, ownerId: UUID, newProjectId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Part]]]

  def cloneComponents(components: IndexedSeq[Component], ownerId: UUID, isMaster: Boolean): IndexedSeq[Component]

  def cloneProjectComponents(projectId: UUID, ownerId: UUID, isMaster: Boolean)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Component]]]
}
