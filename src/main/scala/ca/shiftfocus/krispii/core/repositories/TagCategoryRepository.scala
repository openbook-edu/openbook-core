package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.Connection

import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import java.util.UUID

import scala.concurrent.Future
import scalacache.ScalaCache
import scalaz.{ EitherT, \/ }

trait TagCategoryRepository extends Repository {
  def find(tagCategoryId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, TagCategory]]
  def findByName(name: String, lang: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, TagCategory]]
  def listByLanguage(lang: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[TagCategory]]]

  def create(tagCategory: TagCategory)(implicit conn: Connection): Future[\/[RepositoryError.Fail, TagCategory]]
  def update(tagCategory: TagCategory)(implicit conn: Connection): Future[\/[RepositoryError.Fail, TagCategory]]
  def delete(tagCategory: TagCategory)(implicit conn: Connection): Future[\/[RepositoryError.Fail, TagCategory]]
}
