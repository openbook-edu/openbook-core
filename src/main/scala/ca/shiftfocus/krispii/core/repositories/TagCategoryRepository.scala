package ca.shiftfocus.krispii.core.repositories

/**
 * Created by vzaytseva on 20/06/16.
 */

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

/**
 * Created by vzaytseva on 31/05/16.
 */
trait TagCategoryRepository extends Repository {
  def create(tagCategory: TagCategory)(implicit conn: Connection): Future[\/[RepositoryError.Fail, TagCategory]]
  def delete(tagCategoryName: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, TagCategory]]
  def listByLanguage(lang: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[TagCategory]]]
}
