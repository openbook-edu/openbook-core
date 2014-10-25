package com.shiftfocus.krispii.core.repositories

import com.shiftfocus.krispii.core.lib._
import com.shiftfocus.krispii.core.models._
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
