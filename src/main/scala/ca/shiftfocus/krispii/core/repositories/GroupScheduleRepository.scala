package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.group.Group
import com.github.mauricio.async.db.Connection
import scalaz.\/

import scala.concurrent.Future

trait GroupScheduleRepository extends Repository {
  def list(group: Group)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[GroupSchedule]]]

  def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, GroupSchedule]]

  def insert(groupSchedule: GroupSchedule)(implicit conn: Connection): Future[\/[RepositoryError.Fail, GroupSchedule]]
  def update(groupSchedule: GroupSchedule)(implicit conn: Connection): Future[\/[RepositoryError.Fail, GroupSchedule]]
  def delete(groupSchedule: GroupSchedule)(implicit conn: Connection): Future[\/[RepositoryError.Fail, GroupSchedule]]
}
