package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import com.github.mauricio.async.db.Connection

import scala.concurrent.Future
import scalaz.\/

trait DemoProjectRepository extends Repository {
  def find(projectId: UUID, lang: String)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, DemoProject]]
  def list(lang: String)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[DemoProject]]]

  def insert(demoProject: DemoProject)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, DemoProject]]
  def delete(demoProject: DemoProject)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, DemoProject]]
}
