package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.lib.ExceptionWriter
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.services.datasource.RedisCache
import ca.shiftfocus.uuid.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scalacache._
import redis._
import play.api.Play.current
import org.joda.time.DateTime
import scala.concurrent.Future
import collection.concurrent.TrieMap
import concurrent.duration._

trait SessionRepositoryCacheComponent extends SessionRepositoryComponent {
  self: SessionRepositoryComponent =>

  override val sessionRepository: SessionRepository = new SessionRepositoryCache

  private class SessionRepositoryCache extends SessionRepository {

    implicit val scalaCache = ScalaCache(RedisCache("host1", 6379))
    val ttl = Some(2.days)

    /**
     * List all sessions for a given user ID.
     *
     * @param userId the [[UUID]] of the user to load sessions for.
     * @return a list of sessions for this user
     */
    override def list(userId: UUID): Future[IndexedSeq[Session]] = {
      get[IndexedSeq[Session]](userId.string).map {
        case Some(sessions: IndexedSeq[Session]) => sessions
        case _ => IndexedSeq()
      }
    }

    /**
     * Find a session by its session ID.
     *
     * @param sessionId the [[UUID]] of the session to lookup.
     * @return an [[Option[Session]]] if one was found
     */
    override def find(sessionId: UUID): Future[Option[Session]] = {
      get[Session](sessionId.string)
    }

    /**
     * Create a new session
     *
     * @param session the new [[Session]] to create
     * @return the newly created [[Session]]
     */
    override def create(session: Session): Future[Session] = {
      val sessionWithDates = session.copy(
        createdAt = Some(new DateTime),
        updatedAt = Some(new DateTime)
      )

      put[Session](session.sessionId.string)(session, ttl).map { result =>
        session
      }
    }

    /**
     * Update an existing session
     *
     * @param session the [[Session]] to update
     * @return the updated [[Session]]
     */
    override def update(session: Session): Future[Session] = {
      val sessionWithDates = session.copy(
        updatedAt = Some(new DateTime)
      )
      val fUpdate = for {
        _ <- list(session.userId).map { sessions =>
          val newSessions = sessions.filter(_.sessionId != session.sessionId)
          put(session.userId.string)(newSessions :+ sessionWithDates)
        }
        _ <- put(session.sessionId.string)(sessionWithDates)
      } yield session

      fUpdate.recover { case exception => throw exception }
    }

    /**
     * Delete an existing session.
     *
     * @param session the [[Session]] to be deleted
     * @return the deleted [[Session]]
     */
    override def delete(session: Session): Future[Session] = {
      val fRemove = for {
        _ <- list(session.userId).map { sessions =>
            val newSessions = sessions.filter(_.sessionId != session.sessionId)
            put(session.userId.string)(newSessions)
          }
        _ <- remove(session.sessionId.string)
      } yield session

      fRemove.recover { case exception => throw exception }
    }

  }

}
