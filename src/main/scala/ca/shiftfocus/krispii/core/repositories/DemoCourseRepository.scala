package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import com.github.mauricio.async.db.Connection

import scala.concurrent.Future
import scalaz.\/

trait DemoCourseRepository extends Repository {
  def find(id: UUID)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, DemoCourse]]
  def find(lang: String)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, DemoCourse]]

  def insert(demoCourse: DemoCourse)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, DemoCourse]]
  def update(demoCourse: DemoCourse)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, DemoCourse]]
  def delete(demoCourse: DemoCourse)(implicit conn: Connection, cache: ScalaCachePool): Future[\/[RepositoryError.Fail, DemoCourse]]
}
