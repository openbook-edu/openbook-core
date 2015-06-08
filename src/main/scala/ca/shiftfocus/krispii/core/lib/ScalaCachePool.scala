package ca.shiftfocus.krispii.core.lib

import ca.shiftfocus.krispii.core.error.RepositoryError
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.Random
import scalacache.ScalaCache
import scalacache.redis.RedisCache
import scalaz.{ -\/, \/ }

case class ScalaCachePool(master: ScalaCache, slaves: Seq[ScalaCache] = IndexedSeq()) {
  val pool = slaves.toIndexedSeq

  def randomInstance: ScalaCache = {
    if (slaves.isEmpty) {
      master
    }
    else {
      val rand = new Random(System.currentTimeMillis())
      val random_index = rand.nextInt(pool.length)
      pool(random_index)
    }
  }

  def getCached[V](key: String): Future[\/[RepositoryError.Fail, V]] = {
    //Logger.debug("Fetching from cache: " + key)
    randomInstance.cache.get[V](key).map {
      case Some(entity) => \/.right[RepositoryError.Fail, V](entity)
      case None => \/.left[RepositoryError.Fail, V](RepositoryError.NoResults(s"Cache miss on key: $key"))
    }.recover {
      case failed: RuntimeException => -\/(RepositoryError.DatabaseError("Failed to read from cache.", Some(failed)))
      case exception: Throwable => throw exception
    }
  }

  def putCache[V](key: String)(value: V, ttl: Option[Duration] = None): Future[\/[RepositoryError.Fail, Unit]] = {
    master.cache.put[V](key, value, ttl).map({ unit => \/.right[RepositoryError.Fail, Unit](unit) }).recover {
      case failed: RuntimeException => -\/(RepositoryError.DatabaseError("Failed to write to cache.", Some(failed)))
      case exception: Throwable => throw exception
    }
  }

  def removeCached(key: String): Future[\/[RepositoryError.Fail, Unit]] = {
    master.cache.remove(key).map({ unit => \/.right[RepositoryError.Fail, Unit](unit) }).recover {
      case failed: RuntimeException => -\/(RepositoryError.DatabaseError("Failed to remove from cache.", Some(failed)))
      case exception: Throwable => throw exception
    }
  }
}
object ScalaCachePool {
  def apply(masterConfig: (String, Int), slaveConfigs: Seq[(String, Int)]): ScalaCachePool = {
    val (masterHost, masterPort) = masterConfig
    lazy val master = ScalaCache(RedisCache(masterHost, masterPort))
    lazy val slaves = slaveConfigs.map { case ((slaveHost, slavePort)) => ScalaCache(RedisCache(slaveHost, slavePort)) }
    new ScalaCachePool(master, slaves.toIndexedSeq)
  }
}
