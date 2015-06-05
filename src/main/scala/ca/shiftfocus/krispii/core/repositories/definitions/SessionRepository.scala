package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.error.RepositoryError
import java.util.UUID
import scala.concurrent.Future
import scalacache.ScalaCache
import scalaz.{\/, EitherT}

trait SessionRepository extends Repository {
  def list(userId: UUID)(implicit cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[Session]]]
  def find(sessionId: UUID)(implicit cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Session]]
  def create(session: Session)(implicit cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Session]]
  def update(session: Session)(implicit cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Session]]
  def delete(session: Session)(implicit cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Session]]
}
