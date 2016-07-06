package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.Tag
import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.Connection
import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import java.util.UUID
import scala.concurrent.Future
import scalacache.ScalaCache
import scalaz.{ \/, EitherT }

/**
 * Created by vzaytseva on 31/05/16.
 */
trait TagRepository extends Repository {
  def create(tag: Tag)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Tag]]
  def delete(tagName: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Tag]]
  def listByProjectId(projectId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Tag]]]
  def find(name: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Tag]]
  def untag(projectId: UUID, tagName: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]
  def tag(projectId: UUID, tagName: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]
  def listByCategory(category: String, lang: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Tag]]]
  def trigramSearch(key: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Tag]]]
  def update(tag: Tag)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Tag]]
}
