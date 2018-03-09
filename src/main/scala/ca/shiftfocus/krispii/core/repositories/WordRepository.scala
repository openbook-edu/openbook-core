package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.{ LinkWord }
import com.github.mauricio.async.db.Connection
import scala.concurrent.Future
import scalaz.\/

trait WordRepository extends Repository {

  def get(lang: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, LinkWord]]
  def insert(word: LinkWord)(implicit conn: Connection): Future[\/[RepositoryError.Fail, LinkWord]]
  def delete(word: LinkWord)(implicit conn: Connection): Future[\/[RepositoryError.Fail, LinkWord]]

}
