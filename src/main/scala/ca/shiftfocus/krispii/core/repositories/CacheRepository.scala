package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.lib.{ ScalaCacheConfig, ScalaCachePool }
import scala.reflect.ClassTag

trait CacheRepository {

  val scalaCacheConfig: ScalaCacheConfig

  def cache[A: ClassTag]: ScalaCachePool[A] = ScalaCachePool.buildRedis[A](scalaCacheConfig)
}
