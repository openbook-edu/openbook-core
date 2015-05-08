package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.lib.concurrent.Lifting
import ca.shiftfocus.uuid.UUID
import scalacache._
import scalacache.redis._
import scala.concurrent.duration._
import scala.concurrent.Future
import scalacache.ScalaCache
import scalaz.{-\/, \/}
import scala.concurrent.ExecutionContext.Implicits.global

trait Repository extends Lifting[RepositoryError.Fail] {
  def cacheUserKey(id: UUID): String = s"user[${id.string}]"
  def cacheUsernameKey(username: String): String = s"userId[$username]"
  def cacheStudentsKey(id: UUID): String = s"students[${id.string}]"

  def getCached[V](key: String)(implicit cache: ScalaCache): Future[\/[RepositoryError.Fail, V]] = {
    get[V](key).map {
      case Some(entity) => \/.right[RepositoryError.Fail, V](entity)
      case None => \/.left[RepositoryError.Fail, V](RepositoryError.NoResults)
    }.recover {
      case failed: RuntimeException => -\/(RepositoryError.DatabaseError("Failed to read from cache.", Some(failed)))
      case exception: Throwable => throw exception
    }
  }

  def putCache[V](key: String)(value: V, ttl: Option[Duration] = None)(implicit cache: ScalaCache): Future[\/[RepositoryError.Fail, Unit]] = {
    put[V](key)(value, ttl).map({unit => \/.right[RepositoryError.Fail, Unit](unit)}).recover {
      case failed: RuntimeException => -\/(RepositoryError.DatabaseError("Failed to write to cache.", Some(failed)))
      case exception: Throwable => throw exception
    }
  }

  def removeCached(key: String)(implicit cache: ScalaCache): Future[\/[RepositoryError.Fail, Unit]] = {
    remove(key).map({unit => \/.right[RepositoryError.Fail, Unit](unit)}).recover {
      case failed: RuntimeException => -\/(RepositoryError.DatabaseError("Failed to remove from cache.", Some(failed)))
      case exception: Throwable => throw exception
    }
  }
}