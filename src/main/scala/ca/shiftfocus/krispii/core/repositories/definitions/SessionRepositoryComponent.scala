package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories.error.RepositoryError
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future
import scalaz.{\/, EitherT}

trait SessionRepositoryComponent {
  val sessionRepository: SessionRepository

  trait SessionRepository {
    def list(userId: UUID): Future[\/[RepositoryError, IndexedSeq[Session]]]
    def find(sessionId: UUID): Future[\/[RepositoryError, Session]]
    def create(session: Session): Future[\/[RepositoryError, Session]]
    def update(session: Session): Future[\/[RepositoryError, Session]]
    def delete(session: Session): Future[\/[RepositoryError, Session]]

    protected def lift = EitherT.eitherT[Future, RepositoryError, Session] _
    protected def liftList = EitherT.eitherT[Future, RepositoryError, IndexedSeq[Session]] _
  }
}
