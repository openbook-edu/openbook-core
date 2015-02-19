package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.fail.Fail
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future
import scalaz.{\/, EitherT}

trait SessionRepositoryComponent extends FutureMonad {
  val sessionRepository: SessionRepository

  trait SessionRepository {
    def list(userId: UUID): Future[\/[Fail, IndexedSeq[Session]]]
    def find(sessionId: UUID): Future[\/[Fail, Session]]
    def create(session: Session): Future[\/[Fail, Session]]
    def update(session: Session): Future[\/[Fail, Session]]
    def delete(session: Session): Future[\/[Fail, Session]]
  }
}
