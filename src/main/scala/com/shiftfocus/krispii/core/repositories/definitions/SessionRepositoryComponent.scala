package com.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import com.shiftfocus.krispii.core.lib._
import com.shiftfocus.krispii.core.models._
import scala.concurrent.Future

trait SessionRepositoryComponent {
  val sessionRepository: SessionRepository

  trait SessionRepository {
    def list(userId: UUID): IndexedSeq[Session]
    def find(sessionId: UUID): Option[Session]
    def create(session: Session): Session
    def update(session: Session): Session
    def delete(session: Session): Session
  }
}
