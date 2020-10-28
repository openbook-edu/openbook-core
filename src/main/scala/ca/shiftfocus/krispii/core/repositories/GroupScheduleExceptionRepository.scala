package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.group.Group
import com.github.mauricio.async.db.Connection
import scalaz.\/

import scala.concurrent.Future

trait GroupScheduleExceptionRepository extends Repository {
  val userRepository: UserRepository
  val groupScheduleRepository: GroupScheduleRepository

  def list(group: Group)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[GroupScheduleException]]]
  def list(user: User, group: Group)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[GroupScheduleException]]]

  def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, GroupScheduleException]]

  def insert(groupSchedule: GroupScheduleException) // format: OFF
            (implicit conn: Connection): Future[\/[RepositoryError.Fail, GroupScheduleException]]

  def update(groupSchedule: GroupScheduleException)
            (implicit conn: Connection): Future[\/[RepositoryError.Fail, GroupScheduleException]]

  def delete(groupSchedule: GroupScheduleException)
            (implicit conn: Connection): Future[\/[RepositoryError.Fail, GroupScheduleException]] // format: ON
}
