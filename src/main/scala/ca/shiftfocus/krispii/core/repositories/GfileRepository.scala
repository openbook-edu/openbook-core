package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.work.Work
import ca.shiftfocus.krispii.core.models.{Account, Gfile}
import com.github.mauricio.async.db.Connection

import scala.concurrent.Future
import scalaz.\/

trait GfileRepository extends Repository {
  def listByWork(work: Work)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Gfile]]]
  def insert(gfile: Gfile)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Gfile]]
  def delete(gfile: Gfile)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Gfile]]
}
