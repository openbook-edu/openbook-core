package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.{ LinkWord, Link }
import com.github.mauricio.async.db.Connection

import scala.concurrent.Future
import scalaz.\/

trait LinkRepository extends Repository {

  def create(link: Link)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Link]]
  def delete(link: Link)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Link]]
  def deleteByCourse(courseId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Link]]
  def find(link: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Link]]

}
