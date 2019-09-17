package ca.shiftfocus.krispii.core.lib

import ca.shiftfocus.krispii.core.error.RepositoryError
import redis.clients.jedis.JedisPool

import scalacache.modes.scalaFuture._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.Random
import scalacache.{Cache, CacheConfig}
import scalacache.redis.RedisCache
import scalacache.serialization.Codec
import scalaz.{-\/, \/, \/-}

case class ScalaCacheConfig(masterConfig: (String, Int), slaveConfigs: Seq[(String, Int)])

case class ScalaCachePool[A](val scalaCacheConfig: ScalaCacheConfig)(implicit config: CacheConfig, codec: Codec[A]) {
  private val (masterHost, masterPort) = scalaCacheConfig.masterConfig
  private val master: Cache[A] = new RedisCache[A](new JedisPool(masterHost, masterPort))
  private val slaves: Seq[Cache[A]] = scalaCacheConfig.slaveConfigs.map { case ((slaveHost, slavePort)) => new RedisCache[A](new JedisPool(masterHost, masterPort)) }
  private val pool = slaves.toIndexedSeq

  def randomInstance: Cache[A] = {
    if (slaves.isEmpty) {
      master
    }
    else {
      val rand = new Random(System.currentTimeMillis())
      val random_index = rand.nextInt(pool.length)
      pool(random_index)
    }
  }

  def get(key: String): Future[Option[A]] = {
    randomInstance.get[Future](key).map {
      case Some(value) => Some(value.asInstanceOf[A])
      case _ => None
    }
  }

  def put(key: String)(value: A, ttl: Option[Duration] = None): Future[Any] = {
    master.put[Future](key)(value, ttl)
  }

  def remove(key: String): Future[Any] = {
    master.remove[Future](key)
  }

  def getCached(key: String): Future[\/[RepositoryError.Fail, A]] = {
    randomInstance.get[Future](key).map {
      case Some(entity) => \/.right[RepositoryError.Fail, A](entity)
      case None => \/.left[RepositoryError.Fail, A](RepositoryError.NoResults(s"Cache miss on key: $key"))
    }.recover {
      case failed: RuntimeException => -\/(RepositoryError.DatabaseError("Failed to read from cache.", Some(failed)))
      case exception: Throwable => throw exception
    }
  }

  def putCache(key: String)(value: A, ttl: Option[Duration] = None): Future[\/[RepositoryError.Fail, Unit]] = {
    master.put[Future](key)(value, ttl).map { unit =>
      \/-((): Unit)
    }.recover {
      case failed: RuntimeException => -\/(RepositoryError.DatabaseError("Failed to write to cache.", Some(failed)))
      case exception: Throwable => throw exception
    }
  }

  def removeCached(key: String): Future[\/[RepositoryError.Fail, Unit]] = {
    master.remove[Future](key).map { unit =>
      \/-((): Unit)
    }.recover {
      case failed: RuntimeException => -\/(RepositoryError.DatabaseError("Failed to remove from cache.", Some(failed)))
      case exception: Throwable => throw exception
    }
  }
}
