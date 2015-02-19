package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.fail._
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
import scalaz.{-\/, \/-, \/}

trait SessionRepositoryCacheComponent extends SessionRepositoryComponent {
  self: SessionRepositoryComponent =>

  override val sessionRepository: SessionRepository = new SessionRepositoryCache

  private class SessionRepositoryCache extends SessionRepository {

    implicit val scalaCache = ScalaCache(RedisCache("127.0.0.1", 6379))
    val ttl = Some(2.days)

    /**
     * List all sessions for a given user ID.
     *
     * @param userId the [[UUID]] of the user to load sessions for.
     * @return a list of sessions for this user
     */
    override def list(userId: UUID): Future[\/[Fail, IndexedSeq[Session]]] = {
      get[IndexedSeq[Session]](userId.string).map {
        case Some(sessions: IndexedSeq[Session]) => \/-(sessions)
        case _ => \/-(IndexedSeq())
      }
    }

    /**
     * Find a session by its session ID.
     *
     * @param sessionId the [[UUID]] of the session to lookup.
     * @return an [[Option[Session]]] if one was found
     */
    override def find(sessionId: UUID): Future[\/[Fail, Session]] = {
      get[Session](sessionId.string).map {
        case Some(session) => \/-(session)
        case None => -\/(NoResults(s"No session with id ${sessionId.string} could be found."))
      }.recover {
        case exception => -\/(ExceptionalFail("Uncaught exception", exception))
      }
    }

    /**
     * Create a new session
     *
     * @param session the new [[Session]] to create
     * @return the newly created [[Session]]
     */
    override def create(session: Session): Future[\/[Fail, Session]] = {
      val sessionWithDates = session.copy(
        createdAt = Some(new DateTime),
        updatedAt = Some(new DateTime)
      )

      put[Session](session.id.string)(session, ttl).map {
        result => \/-(session)
      }.recover {
        case exception => -\/(ExceptionalFail("Uncaught exception", exception))
      }
    }

    /**
     * Update an existing session
     *
     * @param session the [[Session]] to update
     * @return the updated [[Session]]
     */
    override def update(session: Session): Future[\/[Fail, Session]] = {
      val sessionWithDates = session.copy(updatedAt = Some(new DateTime))

      val fUpdate = for {
        existing <- lift(list(session.userId))
        newSessions = existing.filter(_.id != session.id) :+ sessionWithDates
        updatedList <- lift {
          put(session.userId.string)(newSessions).map {
            result => \/-(newSessions)
          }.recover {
            case exception => -\/(ExceptionalFail("Uncaught exception", exception))
          }
        }
        updatedSession <- lift {
          put(session.id.string)(sessionWithDates).map {
            result => \/-(sessionWithDates)
          }.recover {
            case exception => -\/(ExceptionalFail("Uncaught exception", exception))
          }
        }
      } yield updatedSession

      fUpdate.run.recover { case exception => -\/(ExceptionalFail("Uncaught exception", exception)) }
    }

    /**
     * Delete an existing session.
     *
     * @param session the [[Session]] to be deleted
     * @return the deleted [[Session]]
     */
    override def delete(session: Session): Future[\/[Fail, Session]] = {
      val fRemove = for {
        userSessions <- lift(list(session.userId))
        updatedList = userSessions.filter(_.id != session.id)
        savedList <- lift {
          put(session.userId.string)(updatedList).map {
            result => \/-(updatedList)
          }.recover {
            case exception => -\/(ExceptionalFail("Uncaught exception", exception))
          }
        }
        deletedSession <- lift {
          remove(session.id.string).map {
            result => \/-(session)
          }.recover {
            case exception => -\/(ExceptionalFail("Uncaught exception", exception))
          }
        }
      } yield deletedSession

      fRemove.run.recover { case exception => -\/(ExceptionalFail("Uncaught exception", exception)) }
    }

  }

}
