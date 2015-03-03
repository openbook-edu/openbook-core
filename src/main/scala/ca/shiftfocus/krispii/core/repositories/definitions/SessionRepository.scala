package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future
import scalaz.{\/, EitherT}

trait SessionRepository extends Repository {
  def list(userId: UUID): Future[\/[RepositoryError.Fail, IndexedSeq[Session]]]
  def find(sessionId: UUID): Future[\/[RepositoryError.Fail, Session]]
  def create(session: Session): Future[\/[RepositoryError.Fail, Session]]
  def update(session: Session): Future[\/[RepositoryError.Fail, Session]]
  def delete(session: Session): Future[\/[RepositoryError.Fail, Session]]
}