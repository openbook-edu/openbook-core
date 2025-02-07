import ca.shiftfocus.krispii.core.lib.{ScalaCacheConfig, ScalaCachePool}
import ca.shiftfocus.krispii.core.models.tasks.Task
import ca.shiftfocus.krispii.core.repositories.CacheRepository
import org.scalamock.scalatest.MockFactory

import scalacache.Cache
import scalacache.serialization.Codec

class TestCacheRepository(
    override val scalaCacheConfig: ScalaCacheConfig
) extends CacheRepository(scalaCacheConfig) with MockFactory {
  def master[A]: Cache[A] = stub[Cache[A]]
  class TestScalaCachePool[A] extends ScalaCachePool[A](scalaCacheConfig)(master[A].config, codec.asInstanceOf[Codec[A]])

  override val cacheTask: ScalaCachePool[Task] = stub[TestScalaCachePool[Task]]
}