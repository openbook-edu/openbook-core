package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import scala.concurrent.Future

trait SessionRepositoryComponent {
  val sessionRepository: SessionRepository

  trait SessionRepository {
    def list(userId: UUID): Future[IndexedSeq[Session]]
    def find(sessionId: UUID): Future[Option[Session]]
    def create(session: Session): Future[Session]
    def update(session: Session): Future[Session]
    def delete(session: Session): Future[Session]
  }
}
