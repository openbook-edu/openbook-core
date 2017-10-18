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

trait TagRepository extends Repository {
  def create(tag: Tag)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Tag]]
  def delete(tag: Tag)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Tag]]
  def listByEntity(entityId: UUID, entityType: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Tag]]]
  def listOrganizationalByEntity(entityId: UUID, entityType: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Tag]]]
  def find(name: String, lang: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Tag]]
  def find(tagId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Tag]]
  def isOrganizational(tagName: String, tagLang: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Boolean]]
  def untag(entityId: UUID, entityType: String, tagName: String, tagLang: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]
  def tag(entityId: UUID, entityType: String, tagName: String, tagLang: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]]
  def listByCategory(category: String, lang: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Tag]]]
  def trigramSearch(key: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Tag]]]
  def trigramSearchAdmin(key: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Tag]]]
  def trigramSearchAdmin(key: String, userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Tag]]]
  def update(tag: Tag)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Tag]]
}
