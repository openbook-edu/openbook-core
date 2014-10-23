package com.shiftfocus.krispii.core.repositories

import com.shiftfocus.krispii.core.lib.{UUID, ExceptionWriter}
import com.shiftfocus.krispii.core.models._
import play.api.cache.Cache
import play.api.Play.current
import org.joda.time.DateTime

trait SessionRepositoryCacheComponent extends SessionRepositoryComponent {
  self: SessionRepositoryComponent  =>

  override val sessionRepository: SessionRepository = new SessionRepositoryCache

  private class SessionRepositoryCache extends SessionRepository {

    /**
     * List all sessions for a given user ID.
     *
     * @param userId the [[UUID]] of the user to load sessions for.
     * @return a list of sessions for this user
     */
    override def list(userId: UUID): IndexedSeq[Session] = {
      Cache.getAs[IndexedSeq[Session]](s"session.forUser[${userId.string}]") match {
        case Some(userSessions: IndexedSeq[Session]) => userSessions
        case _ => IndexedSeq[Session]()
      }
    }

    /**
     * Find a session by its session ID.
     *
     * @param sessionId the [[UUID]] of the session to lookup.
     * @return an [[Option[Session]]] if one was found
     */
    override def find(sessionId: UUID): Option[Session] = {
      Cache.getAs[Session](s"session[${sessionId.string}]")
    }

    /**
     * Create a new session
     *
     * @param session the new [[Session]] to create
     * @return the newly created [[Session]]
     */
    override def create(session: Session): Session = {
      val sessionWithDates = session.copy(
        createdAt = Some(new DateTime),
        updatedAt = Some(new DateTime)
      )

      // First cache the individual session by ID
      Cache.set(s"session[${session.sessionId.string}]", sessionWithDates)

      // Then add the session to the user's session list
      Cache.set(s"session.forUser[${session.userId.string}]", list(session.userId) :+ sessionWithDates)

      // Return the new session
      session
    }

    /**
     * Update an existing session
     *
     * @param session the [[Session]] to update
     * @return the updated [[Session]]
     */
    override def update(session: Session): Session = {
      val sessionWithDates = session.copy(
        updatedAt = Some(new DateTime)
      )

      // First cache the individual session by ID
      Cache.set(s"session[${session.sessionId.string}]", sessionWithDates)

      // Then replace this session in the user's session list
      val userSessions = list(session.userId).filter(_.sessionId != session.sessionId)
      Cache.set(s"session.forUser[${session.userId.string}]", userSessions :+ sessionWithDates)

      // Return the updated session
      session
    }

    /**
     * Delete an existing session.
     *
     * @param session the [[Session]] to be deleted
     * @return the deleted [[Session]]
     */
    override def delete(session: Session): Session = {
      // Remove the session from the individual storage
      Cache.remove(s"session[${session.sessionId.string}]")

      // Fetch the user's list of sessions and remove this one from the list
      val userSessions = list(session.userId).filter(_.sessionId != session.sessionId)
      Cache.set(s"session.forUser[${session.userId.string}]", userSessions)

      // Return the now deleted session
      session
    }

  }

}
