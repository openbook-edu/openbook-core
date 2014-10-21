package com.shiftfocus.krispii.common.repositories

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import com.shiftfocus.krispii.lib._
import com.shiftfocus.krispii.common.models._
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
