package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.models.{Chat, User}
import ca.shiftfocus.krispii.core.error.RepositoryError
import java.util.UUID

import ca.shiftfocus.krispii.core.models.course.Group
import ca.shiftfocus.krispii.core.models.Team

import com.github.mauricio.async.db.Connection

import concurrent.Future
import scalaz.\/

trait ChatRepository extends Repository {
  def list(Group: Group)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Chat]]]
  def list(Group: Group, num: Long, offset: Long)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Chat]]]
  def list(Group: Group, user: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Chat]]]
  def list(Group: Group, user: User, num: Long, offset: Long)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Chat]]]
  def find(Group: Group, messageNum: Long)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Chat]]

  // as long as Team isn't made a descendant of Group, these functions need to be duplicated
  def list(team: Team)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Chat]]]
  def list(team: Team, num: Long, offset: Long)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Chat]]]
  def list(team: Team, user: User)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Chat]]]
  def list(team: Team, user: User, num: Long, offset: Long)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Chat]]]
  def find(team: Team, messageNum: Long)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Chat]]

  def insert(chat: Chat)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Chat]]
  def update(chat: Chat)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Chat]]
  def delete(groupId: UUID, messageNum: Long)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Chat]]
}
