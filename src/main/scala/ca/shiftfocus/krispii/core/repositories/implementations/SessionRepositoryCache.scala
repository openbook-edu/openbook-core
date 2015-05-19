package ca.shiftfocus.krispii.core.repositories

import _root_.redis.clients.jedis.Jedis
import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.lib.exceptions.ExceptionWriter
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import play.api.Logger
import scala.concurrent.ExecutionContext.Implicits.global
import scalacache._
import redis._
import play.api.Play.current
import org.joda.time.DateTime
import scala.concurrent.Future
import collection.concurrent.TrieMap
import concurrent.duration._
import scalaz.{-\/, \/-, \/}

class SessionRepositoryCache extends SessionRepository {

  override val ttl = Some(2.days)

  /**
   * List all sessions for a given user ID.
   *
   * @param userId the UUID of the user to load sessions for.
   * @return a list of sessions for this user
   */
  override def list(userId: UUID)(implicit cache: ScalaCachePool): Future[\/[RepositoryError.Fail, IndexedSeq[Session]]] = {
    cache.getCached[IndexedSeq[Session]](userId.string).map {
      case \/-(sessions: IndexedSeq[Session]) => \/-(sessions)
      case _ => \/-(IndexedSeq())
    }.recover {
      case exception => {
        Logger.error("Could not list sessions")
        \/.left(RepositoryError.DatabaseError("Internal error: could not list sessions", Some(exception)))
      }
    }
  }

  /**
   * Find a session by its session ID.
   *
   * @param sessionId the UUID of the session to lookup.
   * @return an Option[Session] if one was found
   */
  override def find(sessionId: UUID)(implicit cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Session]] = {
    cache.getCached[Session](sessionId.string)
  }

  /**
   * Create a new session
   *
   * @param session the new session to create
   * @return the newly created session
   */
  override def create(session: Session)(implicit cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Session]] = {
    val sessionWithDates = session.copy(
      createdAt = Some(new DateTime),
      updatedAt = Some(new DateTime)
    )

    cache.putCache[Session](session.id.string)(session, ttl).map {
      result => \/-(session)
    }.recover {
      case exception => {
        Logger.error("Could not create session")
        \/.left(RepositoryError.DatabaseError("Internal error: could not create session", Some(exception)))
      }
    }
  }

  /**
   * Update an existing session
   *
   * @param session the session to update
   * @return the updated session
   */
  override def update(session: Session)(implicit cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Session]] = {
    val sessionWithDates = session.copy(updatedAt = Some(new DateTime))

    val fUpdate = for {
      existing <- lift(list(session.userId))
      newSessions = existing.filter(_.id != session.id) :+ sessionWithDates
      updatedList <- lift {
        cache.putCache(session.userId.string)(newSessions).map {
          result => \/-(newSessions)
        }.recover {
          case exception => throw exception
        }
      }
      updatedSession <- lift {
        cache.putCache[Session](session.id.string)(sessionWithDates).map {
          result => \/-(sessionWithDates)
        }.recover {
          case exception => throw exception
        }
      }
    } yield updatedSession

    fUpdate.run.recover {
      case exception => {
        Logger.error("Could not update session")
        \/.left(RepositoryError.DatabaseError("Internal error: could not update session", Some(exception)))
      }
    }
  }

  /**
   * Delete an existing session.
   *
   * @param session the session to be deleted
   * @return the deleted session
   */
  override def delete(session: Session)(implicit cache: ScalaCachePool): Future[\/[RepositoryError.Fail, Session]] = {
    val fRemove = for {
      userSessions <- lift(list(session.userId))
      updatedList = userSessions.filter(_.id != session.id)
      savedList <- lift {
        cache.putCache(session.userId.string)(updatedList).map {
          result => \/-(updatedList)
        }.recover {
          case exception => throw exception
        }
      }
      deletedSession <- lift {
        cache.removeCached(session.id.string).map {
          result => \/-(session)
        }.recover {
          case exception => throw exception
        }
      }
    } yield deletedSession

    fRemove.run.recover {
      case exception => {
        Logger.error("Could not delete session")
        \/.left(RepositoryError.DatabaseError("Internal error: could not delete session", Some(exception)))
      }
    }
  }

}
