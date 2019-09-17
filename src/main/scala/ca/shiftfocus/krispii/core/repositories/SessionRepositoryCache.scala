package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models._
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scalaz.{-\/, \/, \/-}

class SessionRepositoryCache(val cacheRepository: CacheRepository) extends SessionRepository {

  override val ttl = Some(2.days)

  /**
   * List all sessions for a given user ID.
   *
   * @param userId the UUID of the user to load sessions for.
   * @return a list of sessions for this user
   */
  override def list(userId: UUID): Future[\/[RepositoryError.Fail, IndexedSeq[Session]]] = {
    val fSessionList = for {
      sessionList <- lift {
        cacheRepository.cacheSeqSession.getCached(userId.toString).map {
          case \/-(sessions: IndexedSeq[Session]) => \/-(sessions)
          case _ => \/-(IndexedSeq())
        }.recover {
          case exception => {
            \/.left(RepositoryError.DatabaseError("Internal error: could not list sessions", Some(exception)))
          }
        }
      }
      // Find and return only active sessions list
      activeSessionList <- lift {
        serializedT(sessionList)(session =>
          find(session.id).map {
            case \/-(activeSession) => \/-(Some(activeSession))
            case -\/(error: RepositoryError.NoResults) => \/-(None)
            case -\/(error) => -\/(error)
          }).map {
          case \/-(activeSessionList) => \/-(activeSessionList.flatten)
          case -\/(error) => -\/(error)
        }
      }
      // Update session list only with active sessions
      updatedList <- lift {
        cacheRepository.cacheSeqSession.putCache(userId.toString)(activeSessionList, ttl).map {
          result => \/-(activeSessionList)
        }.recover {
          case exception => throw exception
        }
      }
    } yield updatedList

    fSessionList.run.recover {
      case exception => {
        \/.left(RepositoryError.DatabaseError("Internal error: could not list sessions for user", Some(exception)))
      }
    }
  }

  /**
   * Find a session by its session ID.
   *
   * @param sessionId the UUID of the session to lookup.
   * @return an Option[Session] if one was found
   */
  override def find(sessionId: UUID): Future[\/[RepositoryError.Fail, Session]] = {
    cacheRepository.cacheSession.getCached(sessionId.toString)
  }

  /**
   * Create a new session
   *
   * @param session the new session to create
   * @return the newly created session
   */
  override def create(session: Session): Future[\/[RepositoryError.Fail, Session]] = {
    val sessionWithDates = session.copy(
      createdAt = Some(new DateTime),
      updatedAt = Some(new DateTime)
    )

    val fSession = for {
      newSession <- lift {
        cacheRepository.cacheSession.putCache(session.id.toString)(sessionWithDates, ttl).map {
          result => \/-(sessionWithDates)
        }.recover {
          case exception => throw exception
        }
      }
      sessionList <- lift(addToList(session.userId, sessionWithDates))
    } yield newSession

    fSession.run.recover {
      case exception => {
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
  override def update(session: Session): Future[\/[RepositoryError.Fail, Session]] = {
    val sessionWithDates = session.copy(updatedAt = Some(new DateTime))

    val fUpdate = for {
      updatedSession <- lift {
        cacheRepository.cacheSession.putCache(session.id.toString)(sessionWithDates, ttl).map {
          result => \/-(sessionWithDates)
        }.recover {
          case exception => throw exception
        }
      }
      sessionList <- lift(addToList(session.userId, updatedSession))
    } yield updatedSession

    fUpdate.run.recover {
      case exception => {
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
  override def delete(session: Session): Future[\/[RepositoryError.Fail, Session]] = {
    val fRemove = for {
      deletedSession <- lift {
        cacheRepository.cacheSession.removeCached(session.id.toString).map {
          result => \/-(session)
        }.recover {
          case exception => throw exception
        }
      }
      updatedList <- lift(deleteFromList(session.userId, session))
    } yield deletedSession

    fRemove.run.recover {
      case exception => {
        \/.left(RepositoryError.DatabaseError("Internal error: could not delete session", Some(exception)))
      }
    }
  }

  private def addToList(userId: UUID, session: Session): Future[\/[RepositoryError.Fail, IndexedSeq[Session]]] = {
    for {
      sessionList <- lift(list(userId))
      newList = sessionList :+ session
      updatedList <- lift {
        cacheRepository.cacheSeqSession.putCache(session.userId.toString)(newList, ttl).map {
          result => \/-(newList)
        }.recover {
          case exception => throw exception
        }
      }
    } yield newList
  }

  private def deleteFromList(userId: UUID, session: Session): Future[\/[RepositoryError.Fail, IndexedSeq[Session]]] = {
    for {
      sessionList <- lift(list(userId))
      newList = sessionList.filter(_.id != session.id)
      updatedList <- lift {
        cacheRepository.cacheSeqSession.putCache(session.userId.toString)(newList, ttl).map {
          result => \/-(newList)
        }.recover {
          case exception => throw exception
        }
      }
    } yield newList
  }
}
